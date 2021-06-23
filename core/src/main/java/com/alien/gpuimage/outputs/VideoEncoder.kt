package com.alien.gpuimage.outputs

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Handler
import android.view.Surface
import com.alien.gpuimage.Framebuffer
import com.alien.gpuimage.GLContext
import com.alien.gpuimage.RotationMode
import com.alien.gpuimage.Size
import com.alien.gpuimage.egl.EglSurfaceBase
import com.alien.gpuimage.egl.WindowSurface
import com.alien.gpuimage.external.video.EncoderMediaCodec
import com.alien.gpuimage.external.video.MediaMuxerImpl
import com.alien.gpuimage.sources.Input
import java.nio.ByteBuffer

class VideoEncoder(path: String, private val handler: Handler) : Input {

    private var muxer: MediaMuxerImpl? = null
    private var encoder: EncoderMediaCodec? = null
    private var eglSurface: EglSurfaceBase? = null

    init {
        init(path)
    }

    private fun init(outPath: String) {

        // Configure the encoder
        encoder = EncoderMediaCodec(EncoderMediaCodecCallback(), handler)
//        encoder?.prepare(outFormat)
        encoder?.start()

        // Init muxer
        muxer = MediaMuxerImpl(outPath)
    }

    override fun setInputSize(inputSize: Size?) {
    }

    override fun setInputFramebuffer(framebuffer: Framebuffer?) {
    }

    override fun setInputRotation(inputRotation: RotationMode) {
    }

    override fun newFrameReadyAtTime(time: Long) {
        eglSurface?.setPresentationTime(time * 1000)
        eglSurface?.swapBuffers()
    }

    private inner class EncoderMediaCodecCallback : EncoderMediaCodec.EncoderInfoCallback {
        override fun onPrepared(surface: Surface?) {
            eglSurface = WindowSurface(GLContext.sharedProcessingContext()?.eglCore, surface, true)
            eglSurface?.makeCurrent()
        }

        override fun onOutputFormatChanged(outputFormat: MediaFormat?) {
            muxer?.addVideoTrack(outputFormat)
            muxer?.start()
        }

        override fun onDataAvailable(byteBuffer: ByteBuffer?, bufferInfo: MediaCodec.BufferInfo?) {
            muxer?.writeVideoData(byteBuffer, bufferInfo)
        }

        override fun onFinish() {
        }

        override fun onError(errorCode: Int) {
        }
    }

    fun release() {
        encoder?.release()
        muxer?.close()

        runSynchronouslyGpu {
            eglSurface?.releaseEglSurface()
        }
    }
}