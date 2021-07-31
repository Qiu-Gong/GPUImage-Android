package com.alien.gpuimage.outputs

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Handler
import android.view.Surface
import com.alien.gpuimage.Framebuffer
import com.alien.gpuimage.RotationMode
import com.alien.gpuimage.Size
import com.alien.gpuimage.external.video.EncoderMediaCodec
import com.alien.gpuimage.external.video.MediaMuxerImpl
import com.alien.gpuimage.outputs.widget.GLView
import com.alien.gpuimage.utils.Logger
import java.nio.ByteBuffer

/**
 * 视频编码
 */
class VideoEncoder(
    path: String,
    private val format: MediaFormat,
    private val handler: Handler
) : Input {

    companion object {
        private const val TAG = "VideoEncoder"
    }

    private var muxer: MediaMuxerImpl? = null
    private var encoder: EncoderMediaCodec? = null
    private val glView: GLView = GLView()

    init {
        init(path)
    }

    private fun init(outPath: String) {
        encoder = EncoderMediaCodec(EncoderMediaCodecCallback())
        encoder?.prepare(format)
        encoder?.start()

        muxer = MediaMuxerImpl(outPath)
    }

    override fun setInputSize(inputSize: Size?, textureIndex: Int) {
        glView.setInputSize(inputSize, textureIndex)
    }

    override fun setInputFramebuffer(framebuffer: Framebuffer?, textureIndex: Int) {
        glView.setInputFramebuffer(framebuffer, textureIndex)
    }

    override fun setInputRotation(inputRotation: RotationMode, textureIndex: Int) {
        glView.setInputRotation(inputRotation, textureIndex)
    }

    override fun newFrameReadyAtTime(time: Long, textureIndex: Int) {
        glView.newFrameReadyAtTime(time, textureIndex)
    }

    private inner class EncoderMediaCodecCallback : EncoderMediaCodec.EncoderInfoCallback {
        override fun onPrepared(surface: Surface?) {
            Logger.d(TAG, "onPrepared")
            val width = format.getInteger(MediaFormat.KEY_WIDTH)
            val height = format.getInteger(MediaFormat.KEY_HEIGHT)
            glView.viewCreate(surface)
            glView.viewChange(width, height)
        }

        override fun onOutputFormatChanged(outputFormat: MediaFormat?) {
            Logger.d(TAG, "onOutputFormatChanged")
            muxer?.addVideoTrack(outputFormat)
            muxer?.start()
        }

        override fun onDataAvailable(byteBuffer: ByteBuffer?, bufferInfo: MediaCodec.BufferInfo?) {
            muxer?.writeVideoData(byteBuffer, bufferInfo)
        }

        override fun onFinish() {
            Logger.d(TAG, "onFinish")
            muxer?.close()
        }

        override fun onError(errorCode: Int) {
            Logger.d(TAG, "onError")
        }
    }

    /**
     * 编码
     */
    fun drainEncoder() {
        Logger.d(TAG, "drainEncoder")
        handler.post {
            encoder?.drain()
        }
    }

    /**
     * 结束合成
     */
    fun finish() {
        Logger.d(TAG, "finish")
        handler.post {
            encoder?.stop()
        }
    }

    fun release() {
        Logger.d(TAG, "release")
        encoder?.release()
        glView.viewDestroyed()
    }
}