package com.alien.gpuimage.outputs

import android.content.Context
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import com.alien.gpuimage.Framebuffer
import com.alien.gpuimage.R
import com.alien.gpuimage.RotationMode
import com.alien.gpuimage.Size
import com.alien.gpuimage.outputs.widget.GLView
import com.alien.gpuimage.utils.Logger

/**
 * TextureView，可以做动画效果，没有层级问题，相对耗电
 */
class TextureView(context: Context, attrs: AttributeSet) :
    android.view.TextureView(context, attrs), Input {

    companion object {
        private const val TAG = "TextureView"
    }

    private val glView: GLView = GLView()
    private var fillModeType = GLView.FillModeType.FillModePreserveAspectRatio

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.TextureView, 0, 0)
        fillModeType = when (a.getInt(
            R.styleable.TextureView_fillModeType,
            GLView.FillModeType.FillModePreserveAspectRatio.ordinal
        )) {
            0 -> GLView.FillModeType.FillModeStretch
            1 -> GLView.FillModeType.FillModePreserveAspectRatio
            2 -> GLView.FillModeType.FillModePreserveAspectRatioAndFill
            else -> GLView.FillModeType.FillModePreserveAspectRatio
        }
        a.recycle()
        glView.fillMode = fillModeType
        this.surfaceTextureListener = SurfaceTextureCallbackImpl()
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

    private inner class SurfaceTextureCallbackImpl : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            Logger.d(TAG, "onSurfaceTextureAvailable")
            glView.viewCreate(surface)
            glView.viewChange(width, height)
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            Logger.d(TAG, "onSurfaceTextureSizeChanged: width:$width height:$height")
            glView.viewChange(width, height)
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            Logger.d(TAG, "onSurfaceTextureDestroyed")
            glView.viewDestroyed()
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            Logger.d(TAG, "onSurfaceTextureUpdated")
        }
    }

    fun setCallback(callback: GLView.SurfaceViewCallback) {
        glView.callback = callback
    }

    fun getImageRectF(): RectF {
        return glView.getImageRectF()
    }
}