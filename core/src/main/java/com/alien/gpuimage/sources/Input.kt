package com.alien.gpuimage.sources

import com.alien.gpuimage.Framebuffer
import com.alien.gpuimage.GLContext
import com.alien.gpuimage.RotationMode
import com.alien.gpuimage.Size

interface Input {

    fun setInputSize(inputSize: Size?, textureIndex: Int)

    fun setInputFramebuffer(framebuffer: Framebuffer?)

    fun setInputRotation(inputRotation: RotationMode)

    fun newFrameReadyAtTime(time: Long)

    fun nextAvailableTextureIndex(): Int = 0

    /**
     * 同步运行在 GL 线程
     */
    fun runSynchronouslyGpu(runnable: Runnable) {
        if (GLContext.sharedProcessingContext()?.isCurrentThread() == true) {
            runnable.run()
        } else {
            GLContext.sharedProcessingContext()?.runSynchronous(runnable)
        }
    }

    /**
     * 异步运行在 GL 线程
     */
    fun runAsynchronouslyGpu(runnable: Runnable) {
        GLContext.sharedProcessingContext()?.runAsynchronously(runnable)
    }
}