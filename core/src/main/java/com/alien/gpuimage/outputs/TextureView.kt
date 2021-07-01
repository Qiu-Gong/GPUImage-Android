package com.alien.gpuimage.outputs

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import com.alien.gpuimage.Framebuffer
import com.alien.gpuimage.RotationMode
import com.alien.gpuimage.Size
import com.alien.gpuimage.outputs.widget.GLView
import com.alien.gpuimage.sources.Input
import com.alien.gpuimage.utils.Logger

class TextureView(context: Context, attrs: AttributeSet) :
    android.view.TextureView(context, attrs), Input {

    companion object {
        private const val TAG = "TextureView"
    }

    private val glView: GLView = GLView()

    init {
        this.surfaceTextureListener = SurfaceTextureCallbackImpl()
    }

    override fun setInputSize(inputSize: Size?, textureIndex: Int) {
        glView.setInputSize(inputSize, textureIndex)
    }

    override fun setInputFramebuffer(framebuffer: Framebuffer?) {
        glView.setInputFramebuffer(framebuffer)
    }

    override fun setInputRotation(inputRotation: RotationMode) {
        glView.setInputRotation(inputRotation)
    }

    override fun newFrameReadyAtTime(time: Long) {
        glView.newFrameReadyAtTime(time)
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
}