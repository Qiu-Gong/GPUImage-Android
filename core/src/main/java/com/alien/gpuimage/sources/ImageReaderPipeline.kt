package com.alien.gpuimage.sources

import android.graphics.PixelFormat
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import com.alien.gpuimage.Framebuffer
import com.alien.gpuimage.RotationMode
import com.alien.gpuimage.Size
import com.alien.gpuimage.outputs.widget.GLView
import com.alien.gpuimage.utils.Logger
import java.nio.ByteBuffer

/**
 * 把纹理转 RGBA，一般是用来做数据检测
 */
class ImageReaderPipeline(
    width: Int,
    height: Int,
    rotation: Int = 0,
    format: Int = PixelFormat.RGBA_8888,
    maxImages: Int = 3
) : Input {

    companion object {
        private const val TAG = "ImageReaderPipeline"
    }

    private val glView: GLView = GLView()
    private var imageReader: ImageReader? = null

    private var thread: HandlerThread? = null
    private var handler: Handler? = null

    init {

        thread = HandlerThread("ImageReader")
        thread?.start()
        handler = Handler(thread!!.looper)

        runSynchronouslyGpu(Runnable {
            var w: Int = 0
            var h: Int = 0
            if (rotation == 270 || rotation == 90) {
                w = height
                h = width
            } else {
                w = width
                h = height
            }

            imageReader = ImageReader.newInstance(w, h, format, maxImages)
            imageReader?.setOnImageAvailableListener({
                val image = it.acquireNextImage()
                val planes = image.planes
                val stride = planes[0].rowStride
                val buffer = planes[0].buffer
                detect(buffer, w, h, rotation, stride)
                image.close()
            }, handler)
            glView.viewCreate(imageReader!!.surface)
            glView.viewChange(w, h)
        })
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

    open fun detect(data: ByteBuffer, width: Int, height: Int, rotation: Int, stride: Int) {
        Logger.d(TAG, "width:$width height:$height rotation:$rotation stride:$stride")
    }

    fun release() {
        imageReader?.close()
        glView.viewDestroyed()

        thread?.quitSafely()
        thread?.join()
        thread = null
    }
}