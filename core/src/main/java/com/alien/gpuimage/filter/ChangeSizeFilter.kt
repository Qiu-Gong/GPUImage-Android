package com.alien.gpuimage.filter

import android.opengl.GLES20
import com.alien.gpuimage.GLContext
import com.alien.gpuimage.Size
import java.nio.FloatBuffer


/**
 * 改变尺寸 滤镜
 */
class ChangeSizeFilter : Filter() {

    private var newOutputSize: Size? = null

    override fun renderToTexture(vertices: FloatBuffer, textureCoordinates: FloatBuffer) {
        GLContext.setActiveShaderProgram(filterProgram)

        outputFramebuffer =
            GLContext.sharedFramebufferCache()
                ?.fetchFramebuffer(
                    if (hasNewSize()) newOutputSize else getInputSize(),
                    false,
                    outputTextureOptions
                )

        if (usingNextFrameForImageCapture) {
            outputFramebuffer?.lock()
        }

        outputFramebuffer?.activate()
        setUniformsForProgram()

        GLES20.glClearColor(
            backgroundColor.r, backgroundColor.g,
            backgroundColor.b, backgroundColor.a
        )
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // 激活纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, getInputFramebuffer()?.textureId ?: 0)
        GLES20.glUniform1i(inputImageTextureUniform, 2)

        // GL坐标
        GLES20.glEnableVertexAttribArray(positionAttribute)
        GLES20.glVertexAttribPointer(positionAttribute, 2, GLES20.GL_FLOAT, false, 0, vertices)

        // 纹理坐标
        GLES20.glEnableVertexAttribArray(inputTextureCoordinateAttribute)
        GLES20.glVertexAttribPointer(
            inputTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, textureCoordinates
        )

        // 绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // 关闭 属性
        GLES20.glDisableVertexAttribArray(positionAttribute)
        GLES20.glDisableVertexAttribArray(inputTextureCoordinateAttribute)

        // 输入释放fbo
        getInputFramebuffer()?.unlock()
    }

    override fun informTargetsAboutNewFrameAtTime(time: Long) {
        targets.forEachIndexed { index, input ->
            val textureIndices = targetTextureIndices[index]
            input.setInputRotation(getInputRotation(), textureIndices)
            input.setInputSize(if (hasNewSize()) newOutputSize else getInputSize(), textureIndices)
            input.setInputFramebuffer(outputFramebuffer, textureIndices)
        }

        outputFramebuffer?.unlock()
        if (!usingNextFrameForImageCapture) {
            outputFramebuffer = null
        }

        targets.forEachIndexed { index, input ->
            val textureIndices = targetTextureIndices[index]
            input.newFrameReadyAtTime(time, textureIndices)
        }
    }

    private fun hasNewSize(): Boolean {
        return (newOutputSize != null && newOutputSize?.width!! > 1 && newOutputSize?.height!! > 1) &&
                (newOutputSize?.width != getInputSize()?.width || newOutputSize?.height != getInputSize()?.height)
    }

    fun setNewOutputSize(size: Size) {
        this.newOutputSize = size
    }
}