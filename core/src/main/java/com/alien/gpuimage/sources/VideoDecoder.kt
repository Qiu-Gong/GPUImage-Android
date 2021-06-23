package com.alien.gpuimage.sources

import android.graphics.SurfaceTexture
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.os.Handler
import android.view.Surface
import com.alien.gpuimage.GLContext
import com.alien.gpuimage.external.video.DecoderMediaCodec
import com.alien.gpuimage.external.video.VideoFormat
import com.alien.gpuimage.outputs.Output
import com.alien.gpuimage.sources.widget.GLOesTexture

class VideoDecoder(path: String, private val handler: Handler) : Output() {

    private val glOesTexture: GLOesTexture = GLOesTexture()
    private var extractor: MediaExtractor? = null
    private var decoder: DecoderMediaCodec? = null
    private var callback: GLOesTexture.OesTextureCallback? = null

    init {
        init(path)
    }

    private fun init(inPath: String) {
        val create = !GLContext.contextIsExist()
        if (create) {
            GLContext(true)
        }

        // 编码器初始化
        extractor = MediaExtractor()
        extractor?.setDataSource(inPath)
        val inFormat = VideoFormat.selectVideoTrack(extractor!!)

        // 设置宽高
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && inFormat.containsKey(MediaFormat.KEY_ROTATION)
        ) {
            val rotation = inFormat.getInteger(MediaFormat.KEY_ROTATION)
            glOesTexture.setOesRotation(rotation)
            if (rotation == 270 || rotation == 90) {
                glOesTexture.setOesSize(
                    inFormat.getInteger(MediaFormat.KEY_HEIGHT),
                    inFormat.getInteger(MediaFormat.KEY_WIDTH)
                )
            } else {
                glOesTexture.setOesSize(
                    inFormat.getInteger(MediaFormat.KEY_WIDTH),
                    inFormat.getInteger(MediaFormat.KEY_HEIGHT)
                )
            }
        } else {
            glOesTexture.setOesSize(
                inFormat.getInteger(MediaFormat.KEY_WIDTH),
                inFormat.getInteger(MediaFormat.KEY_HEIGHT)
            )
        }

        // 创建 Program
        glOesTexture.createProgram()

        // 开启编码
        decoder = DecoderMediaCodec(DecoderMediaCodecCallback(), handler)
        decoder?.prepare(inFormat, glOesTexture.getOesTexture())
        decoder?.start()
    }

    override fun addTarget(input: Input?) {
        glOesTexture.addTarget(input)
    }

    override fun release() {
        glOesTexture.release()
        extractor?.release()
        decoder?.release()
    }

    fun setCallback(callback: GLOesTexture.OesTextureCallback) {
        this.callback = callback
    }

    /**
     * 更新编码，执行一次则更新一帧
     */
    fun feedInputToDecoder() {
        handler.post {
            decoder?.feedInputToDecoder(extractor)
            decoder?.drain()
        }
    }

    private inner class DecoderMediaCodecCallback : DecoderMediaCodec.DecoderInfoCallback {

        override fun onPrepared(surface: Surface?) {
            callback?.onPrepared()
        }

        override fun onOutputFormatChanged(outputFormat: MediaFormat?) = Unit

        override fun onFrameAvailable(surfaceTexture: SurfaceTexture?, presentationTimeUs: Long) {
            callback?.onFrameAvailable(presentationTimeUs)
            glOesTexture.onFrameAvailable(surfaceTexture, presentationTimeUs)
        }

        /**
         * 解码结束
         */
        override fun onFinish() {
            callback?.onFinish()
        }

        /**
         * 读取编码数据结束
         */
        override fun onAllInputExtracted() = Unit

        override fun onError(errorCode: Int) = Unit
    }
}