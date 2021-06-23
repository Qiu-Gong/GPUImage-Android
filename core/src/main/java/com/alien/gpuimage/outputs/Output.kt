package com.alien.gpuimage.outputs

import com.alien.gpuimage.Framebuffer
import com.alien.gpuimage.GLContext
import com.alien.gpuimage.TextureAttributes
import com.alien.gpuimage.sources.Input

abstract class Output {

    protected val targets: MutableList<Input> = mutableListOf()
    var outputFramebuffer: Framebuffer? = null
    protected var outputTextureOptions: TextureAttributes = TextureAttributes()

    abstract fun release()

    open fun addTarget(input: Input?) {
        input?.let {
            targets.add(input)
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