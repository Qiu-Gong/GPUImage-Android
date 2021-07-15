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
class ImageReaderPipeline(width: Int, height: Int, rotation: Int = 0) : Input {

    companion object {
        private const val TAG = "ImageReaderPipeline"
    }

    private var glView: GLView? = null
    private var imageReader: ImageReader? = null

    private var thread: HandlerThread? = null
    var handler: Handler? = null
        private set

    private var rotationImageReader: Int = 0
    private var widthImageReader: Int = 0
    private var heightImageReader: Int = 0

    var callback: ImageReaderPipelineCallback? = null

    interface ImageReaderPipelineCallback {
        fun detect(data: ByteBuffer, width: Int, height: Int, rotation: Int, stride: Int)
    }

    private val onImageReaderCallback = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireNextImage()
        val planes = image.planes
        val stride = planes[0].rowStride
        val buffer = planes[0].buffer
        Logger.d(TAG, "width:$widthImageReader height:$heightImageReader rotation:$rotation stride:$stride")
        callback?.detect(buffer, widthImageReader, heightImageReader, rotation, stride)
        image.close()
    }

    init {
        thread = HandlerThread("ImageReader")
        thread?.start()
        handler = Handler(thread!!.looper)

        runSynchronouslyGpu(Runnable {
            if (width != 0 || height != 0) {
                init(rotation, width, height)
            }
        })
    }

    private fun init(rotation: Int, width: Int, height: Int) {
        rotationImageReader = rotation
        if (rotationImageReader == 270 || rotationImageReader == 90) {
            widthImageReader = height
            heightImageReader = width
        } else {
            widthImageReader = width
            heightImageReader = height
        }

        imageReader =
            ImageReader.newInstance(widthImageReader, heightImageReader, PixelFormat.RGBA_8888, 3)
        imageReader?.setOnImageAvailableListener(onImageReaderCallback, handler)

        glView = GLView()
        glView?.viewCreate(imageReader!!.surface)
        glView?.viewChange(widthImageReader, heightImageReader)
    }

    override fun setInputSize(inputSize: Size?, textureIndex: Int) {
        glView?.setInputSize(inputSize, textureIndex)
    }

    override fun setInputFramebuffer(framebuffer: Framebuffer?, textureIndex: Int) {
        glView?.setInputFramebuffer(framebuffer, textureIndex)
    }

    override fun setInputRotation(inputRotation: RotationMode, textureIndex: Int) {
        glView?.setInputRotation(inputRotation, textureIndex)
    }

    override fun newFrameReadyAtTime(time: Long, textureIndex: Int) {
        glView?.newFrameReadyAtTime(time, textureIndex)
    }

    fun resetSize(width: Int, height: Int) {
        imageReader?.close()
        glView?.viewDestroyed()
        init(rotationImageReader, width, height)
    }

    fun release() {
        imageReader?.close()
        glView?.viewDestroyed()

        thread?.quitSafely()
        thread?.join()
        thread = null
    }
}