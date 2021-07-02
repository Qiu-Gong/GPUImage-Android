package com.alien.gpuimage.sources

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import com.alien.gpuimage.GLContext
import com.alien.gpuimage.Size
import com.alien.gpuimage.outputs.Output
import com.alien.gpuimage.utils.Logger

/**
 * Bitmap 输入
 */
class Picture : Output {

    companion object {
        private const val TAG = "Picture"
    }

    private var bitmap: Bitmap? = null

    private var hasProcessedImage: Boolean = false
    private var pixelSizeOfImage: Size? = null

    constructor(bitmap: Bitmap, recycle: Boolean) {
        init(bitmap, recycle)
    }

    constructor(path: String) {
        init(BitmapFactory.decodeFile(path), true)
    }

    private fun init(bitmap: Bitmap, recycle: Boolean) {
        val create = !GLContext.contextIsExist()
        if (create) {
            GLContext(true)
        }

        runSynchronously(Runnable {
            this.bitmap = bitmap
            loadImageToFBO()
            if (recycle) {
                this.bitmap?.recycle()
            }
        })
    }

    fun processPicture() {
        runAsynchronously(Runnable {
            targets.forEachIndexed { index, input ->
                val textureIndices = targetTextureIndices[index]
                input.setInputSize(pixelSizeOfImage, textureIndices)
                input.setInputFramebuffer(outputFramebuffer, textureIndices)
                input.newFrameReadyAtTime(System.currentTimeMillis(), textureIndices)
            }
        })
    }

    fun processPictureSynchronously() {
        runSynchronously(Runnable {
            targets.forEachIndexed { index, input ->
                val textureIndices = targetTextureIndices[index]
                input.setInputSize(pixelSizeOfImage, textureIndices)
                input.setInputFramebuffer(outputFramebuffer, textureIndices)
                input.newFrameReadyAtTime(System.currentTimeMillis(), textureIndices)
            }
        })
    }

    private fun loadImageToFBO() {
        hasProcessedImage = false
        assert(bitmap?.width ?: 0 > 0 && bitmap?.height ?: 0 > 0)

        GLContext.useProcessingContext()
        pixelSizeOfImage = GLContext.withinTextureForSize(Size(bitmap!!.width, bitmap!!.height))
        outputFramebuffer =
            GLContext.sharedFramebufferCache()?.fetchFramebuffer(pixelSizeOfImage, true)
        outputFramebuffer?.disableReferenceCounting()
        Logger.d(TAG, "picture out ${outputFramebuffer.toString()}")

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, outputFramebuffer?.textureId ?: 0)
        GLUtils.texImage2D(
            GLES20.GL_TEXTURE_2D, 0,
            GLES20.GL_RGBA, bitmap, GLES20.GL_UNSIGNED_BYTE, 0
        )
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    override fun release() {
        outputFramebuffer?.let {
            it.enableReferenceCounting()
            it.unlock()
        }
    }
}