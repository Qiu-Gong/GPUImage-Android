package com.alien.gpuimage.outputs

import android.graphics.Bitmap
import android.opengl.GLES20
import com.alien.gpuimage.Framebuffer
import com.alien.gpuimage.RotationMode
import com.alien.gpuimage.Size
import com.alien.gpuimage.intArray
import com.alien.gpuimage.sources.Input
import com.alien.gpuimage.utils.Logger
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * ImageView 显示
 */
class BitmapView : Input {

    companion object {
        private const val TAG = "BitmapImageView"
    }

    private var inputFramebuffer: Framebuffer? = null
    private var fboId: Int = 0

    private var inBuffer: ByteBuffer? = null
    var bitmap: Bitmap? = null
        private set

    override fun setInputSize(inputSize: Size?, textureIndex: Int) = Unit

    override fun setInputFramebuffer(framebuffer: Framebuffer?, textureIndex: Int) {
        inputFramebuffer = framebuffer
        inputFramebuffer?.lock()
    }

    override fun setInputRotation(inputRotation: RotationMode, textureIndex: Int) = Unit

    override fun newFrameReadyAtTime(time: Long, textureIndex: Int) {
        inputFramebuffer?.let { it ->
            if (!it.onlyTexture) {
                fboId = it.framebufferId
            } else {
                if (fboId == 0) {
                    fboId = intArray { fbo -> GLES20.glGenFramebuffers(1, fbo, 0) }
                }
            }

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
            GLES20.glFramebufferTexture2D(
                GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D,
                it.textureId,
                0
            )
            val fboStatus = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
            if (fboStatus != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                Logger.e(TAG, "initFBO failed, status: $fboStatus")
            }

            Logger.d(TAG, "imageView in ${inputFramebuffer.toString()}")
            readFboToBitmap(fboId, it.width, it.height)
            inputFramebuffer?.unlock()
            inputFramebuffer = null
        }
    }

    fun release() {
        if (inputFramebuffer?.onlyTexture == true) {
            if (fboId > 0) {
                runAsynchronouslyGpu(Runnable {
                    GLES20.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
                    fboId = 0
                })
            }
        }
    }

    private fun readFboToBitmap(fbo: Int, width: Int, height: Int): Bitmap? {
        Logger.d(TAG, "readFboToBitmap fbo:$fbo width:$width height:$height")
        if (inBuffer == null || inBuffer?.capacity() != width * height * 4) {
            Logger.d(TAG, "create ByteBuffer")
            inBuffer = ByteBuffer.allocateDirect(width * height * 4)
            inBuffer?.order(ByteOrder.LITTLE_ENDIAN)
        }
        inBuffer?.rewind()
        inBuffer?.position(0)

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo)
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, inBuffer)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        return rgbaBufferToBitmap(inBuffer, width, height)
    }

    private fun rgbaBufferToBitmap(buffer: Buffer?, width: Int, height: Int): Bitmap? {
        if (bitmap == null || bitmap?.width != width || bitmap?.height != height) {
            Logger.d(TAG, "create Bitmap")
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }

        bitmap?.copyPixelsFromBuffer(buffer)
        return bitmap
    }
}