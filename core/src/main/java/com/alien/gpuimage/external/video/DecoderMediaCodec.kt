package com.alien.gpuimage.external.video

import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Handler
import android.view.Surface
import com.alien.gpuimage.utils.Logger

class DecoderMediaCodec(
    private val callback: DecoderInfoCallback?,
    private val handler: Handler
) : BaseMediaCodec() {

    companion object {
        private const val TAG = "DecoderMediaCodec"
    }

    interface DecoderInfoCallback {
        fun onPrepared(surface: Surface?)

        fun onOutputFormatChanged(outputFormat: MediaFormat?)

        fun onFrameAvailable(surfaceTexture: SurfaceTexture?, presentationTimeUs: Long)

        fun onFinish()

        fun onAllInputExtracted()

        fun onError(errorCode: Int)
    }

    private var surfaceTexture: SurfaceTexture? = null
    private var outputSurface: Surface? = null
    private var lastPresentationTimeUs: Long = 0

    override fun prepare(format: MediaFormat, texId: Int?) {
        if (mediaCodec != null || state.get() != STATE_RELEASE) {
            Logger.e(TAG, "prepare() Error")
            return
        }

        surfaceTexture = SurfaceTexture(texId ?: -1)
        outputSurface = Surface(surfaceTexture)
        surfaceTexture?.setOnFrameAvailableListener({
            if (ENABLE_LOG) {
                Logger.d(TAG, "Frame available, lastPresentationTimeUs:$lastPresentationTimeUs")
            }
            callback?.onFrameAvailable(surfaceTexture, lastPresentationTimeUs)
        }, handler)

        val type = format.getString(MediaFormat.KEY_MIME) ?: OUT_MIME_TYPE
        mediaCodec = MediaCodec.createDecoderByType(type)
        mediaCodec?.configure(format, outputSurface, null, 0)

        state.set(STATE_PREPARED)
        callback?.onPrepared(outputSurface)
        if (ENABLE_LOG) {
            Logger.d(TAG, "prepare")
        }
    }

    override fun start() {
        if (state.get() != STATE_PREPARED) {
            Logger.e(TAG, "start() Error")
            return
        }

        mediaCodec?.start()
        state.set(STATE_ENCODING)
        if (ENABLE_LOG) {
            Logger.d(TAG, "start()")
        }
    }

    override fun drain() {
        if (state.get() != STATE_ENCODING && state.get() != STATE_STOPPING) {
            Logger.e(TAG, "drain() Error state:${state.get()}")
            return
        }

        while (true) {
            val bufferInfo = MediaCodec.BufferInfo()
            val index = mediaCodec?.dequeueOutputBuffer(bufferInfo, OUTPUT_TIMEOUT_US)!!
            if (ENABLE_LOG) {
                Logger.d(
                    TAG, "dequeueOutputBuffer status:" + index +
                            " mBufferInfo => flag:" + bufferInfo.flags +
                            " size:" + bufferInfo.size +
                            " offset:" + bufferInfo.offset
                )
            }

            // -1
            if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (ENABLE_LOG) {
                    Logger.d(TAG, "drain info try again later")
                }
                if (state.get() != STATE_STOPPING) {
                    break
                }

                // -2
            } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (ENABLE_LOG) {
                    Logger.d(TAG, "drain info output format changed")
                }
                callback?.onOutputFormatChanged(mediaCodec?.outputFormat)
                break

            } else if (index < 0) {
                Logger.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: $index")

            } else {
                try {
                    val render = bufferInfo.size > 0
                    mediaCodec?.releaseOutputBuffer(index, render)

                    if (render) {
                        lastPresentationTimeUs = bufferInfo.presentationTimeUs
                        Logger.d(TAG, "presentationTimeUs:${lastPresentationTimeUs}")
                    }

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (ENABLE_LOG) {
                            Logger.d(TAG, "drain buffer flag end of stream")
                        }
                        if (state.get() != STATE_STOPPING) {
                            if (ENABLE_LOG) {
                                Logger.d(TAG, "reached end of stream unexpectedly")
                            }
                        }
                        state.set(STATE_STOPPED)
                        callback?.onFinish()
                        release()
                        return
                    }

                } catch (e: Exception) {
                    Logger.e(TAG, "drain Exception")
                    e.printStackTrace()
                }
            }
        }
    }

    override fun stop() = Unit

    override fun release() {
        if (ENABLE_LOG) {
            Logger.d(TAG, "release() called")
        }
        if (state.get() != STATE_STOPPED) {
            return
        }

        try {
            mediaCodec?.stop()
            mediaCodec?.release()
            mediaCodec = null
        } catch (e: Exception) {
            Logger.e(TAG, "stop video decoder throw exception")
        }

        outputSurface?.release()
        outputSurface = null

        state.set(STATE_RELEASE)
        if (ENABLE_LOG) {
            Logger.d(TAG, "release() end")
        }
    }

    override fun getInputSurface(): Surface? = outputSurface

    fun feedInputToDecoder(extractor: MediaExtractor? = null) {
        val inBufferId = mediaCodec?.dequeueInputBuffer(OUTPUT_TIMEOUT_US) ?: -1
        if (inBufferId >= 0) {
            val buffer = mediaCodec?.getInputBuffer(inBufferId)
            buffer?.let {
                val sampleSize = extractor?.readSampleData(buffer, 0) ?: -1
                if (sampleSize >= 0) {
                    if (ENABLE_LOG) {
                        Logger.d(TAG, "feedInputToDecoder advance")
                    }
                    mediaCodec?.queueInputBuffer(
                        inBufferId, 0, sampleSize,
                        extractor!!.sampleTime, extractor.sampleFlags
                    )
                    extractor!!.advance()
                } else {
                    if (ENABLE_LOG) {
                        Logger.d(TAG, "feedInputToDecoder allInputExtracted")
                    }
                    mediaCodec!!.queueInputBuffer(
                        inBufferId, 0, 0,
                        0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                    callback?.onAllInputExtracted()
                }
            }
        } else {
            Logger.e(TAG, "feedInputToDecoder dequeueInputBuffer error.")
        }
    }
}