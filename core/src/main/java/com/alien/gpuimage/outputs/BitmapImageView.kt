package com.alien.gpuimage.outputs

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.alien.gpuimage.Framebuffer
import com.alien.gpuimage.RotationMode
import com.alien.gpuimage.Size
import com.alien.gpuimage.intArray
import com.alien.gpuimage.sources.Input
import com.alien.gpuimage.utils.Logger
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BitmapImageView(context: Context, attrs: AttributeSet) :
    AppCompatImageView(context, attrs, 0), Input {

    companion object {
        private const val TAG = "BitmapImageView"
    }

    private var inputFramebuffer: Framebuffer? = null
    private var fboId: Int = 0

    private var inBuffer: ByteBuffer? = null
    private var inBitmap: Bitmap? = null

    override fun setInputSize(inputSize: Size?) = Unit

    override fun setInputFramebuffer(framebuffer: Framebuffer?) {
        inputFramebuffer = framebuffer
        inputFramebuffer?.lock()
    }

    override fun setInputRotation(inputRotation: RotationMode) = Unit

    override fun newFrameReadyAtTime(time: Long) {
        inputFramebuffer?.let { framebuffer ->
            if (!framebuffer.onlyTexture) {
                fboId = framebuffer.framebufferId
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
                framebuffer.textureId,
                0
            )
            val fboStatus = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
            if (fboStatus != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                Logger.e(TAG, "initFBO failed, status: $fboStatus")
            }

            Logger.d(TAG, "imageView in ${inputFramebuffer.toString()}")
            val bitmap = readFboToBitmap(fboId, framebuffer.width, framebuffer.height)
            inputFramebuffer?.lock()
            post {
                setImageBitmap(bitmap)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
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
        if (inBuffer == null) {
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
        if (inBitmap == null) {
            Logger.d(TAG, "create Bitmap")
            inBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }

        inBitmap?.copyPixelsFromBuffer(buffer)
        return inBitmap
    }
}