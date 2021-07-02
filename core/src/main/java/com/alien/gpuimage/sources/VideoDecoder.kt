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

/**
 * 视频解码
 */
class VideoDecoder(path: String, private val handler: Handler) : Output() {

    private val glOesTexture: GLOesTexture = GLOesTexture()
    private var extractor: MediaExtractor? = null
    private var decoder: DecoderMediaCodec? = null
    private var inFormat: MediaFormat? = null
    private var callback: VideoDecoderCallback? = null

    var videoWidth: Int = 0
        private set
    var videoHeight: Int = 0
        private set
    var videoRotation: Int = 0
        private set

    interface VideoDecoderCallback {
        /**
         * 解码准备就绪
         */
        fun onPrepared()

        /**
         * 帧可用之前
         */
        fun onFrameAvailableBefore(presentationTimeUs: Long)

        /**
         * 帧可用之后
         */
        fun onFrameAvailableAfter(presentationTimeUs: Long)

        /**
         * 解码结束
         */
        fun onFinish()
    }

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
        inFormat = VideoFormat.selectVideoTrack(extractor!!)

        // 设置宽高方向
        videoWidth = inFormat?.getInteger(MediaFormat.KEY_WIDTH) ?: 0
        videoHeight = inFormat?.getInteger(MediaFormat.KEY_HEIGHT) ?: 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && inFormat?.containsKey(MediaFormat.KEY_ROTATION) == true
        ) {
            videoRotation = inFormat?.getInteger(MediaFormat.KEY_ROTATION) ?: 0
            glOesTexture.setOesRotation(videoRotation)
            if (videoRotation == 270 || videoRotation == 90) {
                glOesTexture.setOesSize(videoHeight, videoWidth)
            } else {
                glOesTexture.setOesSize(videoWidth, videoHeight)
            }
        } else {
            glOesTexture.setOesSize(videoWidth, videoHeight)
        }

        // 创建 Program
        glOesTexture.createProgram()

        // 开启编码
        decoder = DecoderMediaCodec(DecoderMediaCodecCallback(), handler)
        decoder?.prepare(inFormat, glOesTexture.getOesTexture())
        decoder?.start()
    }

    override fun addTarget(input: Input?, textureLocation: Int) {
        glOesTexture.addTarget(input)
    }

    override fun release() {
        glOesTexture.release()
        extractor?.release()
        decoder?.release()
    }

    fun setCallback(callback: VideoDecoderCallback) {
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
            callback?.onFrameAvailableBefore(presentationTimeUs)
            glOesTexture.onFrameAvailable(surfaceTexture, presentationTimeUs)
            callback?.onFrameAvailableAfter(presentationTimeUs)
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