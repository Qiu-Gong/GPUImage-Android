package com.alien.gpuimage.external.video

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Handler
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer

class EncoderMediaCodec(
    private val callback: EncoderInfoCallback?,
    private val handler: Handler
) : BaseMediaCodec() {

    companion object {
        private const val TAG = "EncoderMediaCodec"
    }

    interface EncoderInfoCallback {
        fun onPrepared(surface: Surface?)

        fun onOutputFormatChanged(outputFormat: MediaFormat?)

        fun onDataAvailable(byteBuffer: ByteBuffer?, bufferInfo: MediaCodec.BufferInfo?)

        fun onFinish()

        fun onError(errorCode: Int)
    }

    private var surface: Surface? = null

    init {
        mediaCodec = MediaCodec.createEncoderByType(OUT_MIME_TYPE)
    }

    override fun prepare(format: MediaFormat, texId: Int?) {
        if (state.get() != STATE_RELEASE) {
            Log.e(TAG, "prepare() Error")
            return
        }

        if (mediaCodec == null) {
            mediaCodec = MediaCodec.createEncoderByType(OUT_MIME_TYPE)
        }
        mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        state.set(STATE_PREPARED)
        surface = mediaCodec?.createInputSurface()
        callback?.onPrepared(surface)
        if (ENABLE_LOG) {
            Log.d(TAG, "prepare")
        }
    }

    override fun start() {
        if (state.get() != STATE_PREPARED) {
            Log.e(TAG, "start() Error")
            return
        }

        mediaCodec?.start()
        state.set(STATE_ENCODING)
        if (ENABLE_LOG) {
            Log.d(TAG, "start()")
        }
    }

    override fun drain() {
        if (state.get() != STATE_ENCODING && state.get() != STATE_STOPPING) {
            Log.e(TAG, "drain() Error state:${state.get()}")
            return
        }

        while (true) {
            val bufferInfo = MediaCodec.BufferInfo()
            val index = mediaCodec?.dequeueOutputBuffer(bufferInfo, OUTPUT_TIMEOUT_US)!!
            if (ENABLE_LOG) {
                Log.d(
                    TAG, "dequeueOutputBuffer status:" + index +
                            " mBufferInfo => flag:" + bufferInfo.flags +
                            " size:" + bufferInfo.size +
                            " offset:" + bufferInfo.offset
                )
            }

            if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (ENABLE_LOG) {
                    Log.d(TAG, "drain info try again later")
                }
                if (state.get() != STATE_STOPPING) {
                    break
                }

            } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (ENABLE_LOG) {
                    Log.d(TAG, "drain info output format changed")
                }
                callback?.onOutputFormatChanged(mediaCodec?.outputFormat)
                break

            } else if (index < 0) {
                Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: $index")

            } else {
                try {
                    val byteBuffer = mediaCodec?.getOutputBuffer(index)
                    if (byteBuffer == null) {
                        throw RuntimeException("encoderOutputBuffer $byteBuffer was null")
                    }

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        if (ENABLE_LOG) {
                            Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG")
                        }
                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size != 0) {
                        byteBuffer.position(bufferInfo.offset)
                        byteBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        callback?.onDataAvailable(byteBuffer, bufferInfo)
                    }

                    mediaCodec?.releaseOutputBuffer(index, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (ENABLE_LOG) {
                            Log.d(TAG, "drain buffer flag end of stream")
                        }
                        if (state.get() != STATE_STOPPING) {
                            Log.e(TAG, "reached end of stream unexpectedly")
                        }
                        state.set(STATE_STOPPED)
                        callback?.onFinish()
                        release()
                        return
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "drain Exception")
                    e.printStackTrace()
                }
            }
        }
    }

    override fun stop() {
        if (ENABLE_LOG) {
            Log.d(TAG, "stop() called")
        }
        if (state.get() == STATE_ENCODING) {
            mediaCodec?.signalEndOfInputStream()
            state.set(STATE_STOPPING)
        }
        if (ENABLE_LOG) {
            Log.d(TAG, "stop() end")
        }
    }

    override fun release() {
        if (ENABLE_LOG) {
            Log.d(TAG, "release() called")
        }
        if (state.get() != STATE_STOPPED) {
            return
        }

        try {
            mediaCodec?.stop()
            mediaCodec?.release()
            mediaCodec = null
        } catch (e: Exception) {
            Log.e(TAG, "stop video encoder throw exception")
        }

        state.set(STATE_RELEASE)
        if (ENABLE_LOG) {
            Log.d(TAG, "release() end")
        }
    }

    override fun getInputSurface(): Surface? {
        return surface
    }
}