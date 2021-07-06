package com.alien.gpuimage.outputs

import com.alien.gpuimage.Framebuffer
import com.alien.gpuimage.GLContext
import com.alien.gpuimage.TextureAttributes
import com.alien.gpuimage.sources.Input
import com.alien.gpuimage.utils.Logger

/**
 * 输出
 */
abstract class Output {

    companion object {
        private const val TAG = "Output"
    }

    protected val targets: MutableList<Input> = mutableListOf()
    protected val targetTextureIndices: MutableList<Int> = mutableListOf()
    var outputFramebuffer: Framebuffer? = null
    protected var outputTextureOptions: TextureAttributes = TextureAttributes()

    abstract fun release()

    private fun setInputFramebufferForTarget(input: Input?, inputTextureIndex: Int) {
        if (outputFramebuffer != null) {
            input?.setInputFramebuffer(outputFramebuffer, inputTextureIndex)
        }
    }

    open fun addTarget(input: Input?) {
        val index = input?.nextAvailableTextureIndex() ?: 0
        addTarget(input, index)
    }

    open fun addTarget(input: Input?, textureLocation: Int) {
        if (targets.contains(input)) {
            Logger.e(TAG, "add repeatedly targets.")
            return
        }

        input?.let {
            setInputFramebufferForTarget(it, textureLocation)
            targets.add(input)
            targetTextureIndices.add(textureLocation)
        }
    }

    /**
     * 同步运行在 GL 线程
     */
    fun runSynchronously(runnable: Runnable) {
        if (GLContext.sharedProcessingContext()?.isCurrentThread() == true) {
            runnable.run()
        } else {
            GLContext.sharedProcessingContext()?.runSynchronous(runnable)
        }
    }

    /**
     * 异步运行在 GL 线程
     */
    fun runAsynchronously(runnable: Runnable) {
        GLContext.sharedProcessingContext()?.runAsynchronously(runnable)
    }
}