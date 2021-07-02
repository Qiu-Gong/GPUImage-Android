package com.alien.gpuimage.sources

import com.alien.gpuimage.Framebuffer
import com.alien.gpuimage.Size
import com.alien.gpuimage.outputs.Output

/**
 * 纹理输入（待使用）
 */
class TextureInput : Output() {

    private var textureSize: Size? = null

    fun initWithTexture(texture: Int, width: Int, height: Int) {
        runSynchronously(Runnable {
            textureSize = Size(width, height)
            outputFramebuffer = Framebuffer(width, height, texture)
        })
    }

    fun processTextureWithFrameTime(frameTime: Long) {
        runAsynchronously(Runnable {
            targets.forEachIndexed { index, input ->
                val textureIndices = targetTextureIndices.indexOf(index)
                input.setInputSize(textureSize, textureIndices)
                input.setInputFramebuffer(outputFramebuffer, textureIndices)
                input.newFrameReadyAtTime(frameTime, textureIndices)
            }
        })
    }

    override fun release() = Unit
}