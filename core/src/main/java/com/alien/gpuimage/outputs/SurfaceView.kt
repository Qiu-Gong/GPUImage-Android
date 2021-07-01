package com.alien.gpuimage.outputs

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import com.alien.gpuimage.Framebuffer
import com.alien.gpuimage.RotationMode
import com.alien.gpuimage.Size
import com.alien.gpuimage.outputs.widget.GLView
import com.alien.gpuimage.sources.Input
import com.alien.gpuimage.utils.Logger

open class SurfaceView(context: Context, attrs: AttributeSet) :
    android.view.SurfaceView(context, attrs), Input {

    companion object {
        private const val TAG = "SurfaceView"
    }

    private val glView: GLView = GLView()

    init {
        this.holder?.addCallback(SurfaceCallbackImpl())
    }

    private inner class SurfaceCallbackImpl : SurfaceHolder.Callback {
        override fun surfaceCreated(p0: SurfaceHolder) {
            Logger.d(TAG, "surfaceCreated")
            glView.viewCreate(p0.surface)
        }

        override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
            Logger.d(TAG, "surfaceChanged: format:$p1 width:$p2 height:$p3")
            glView.viewChange(p2, p3)
        }

        override fun surfaceDestroyed(p0: SurfaceHolder) {
            Logger.d(TAG, "surfaceDestroyed")
            glView.viewDestroyed()
        }
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

    fun setCallback(callback: GLView.SurfaceViewCallback) {
        glView.callback = callback
    }
}