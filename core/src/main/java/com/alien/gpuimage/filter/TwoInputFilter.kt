package com.alien.gpuimage.filter

import android.opengl.GLES20
import com.alien.gpuimage.*
import java.nio.FloatBuffer

class TwoInputFilter(
    vertexShader: String? = SHADER_STRING,
    fragmentShader: String?
) : Filter(vertexShader, fragmentShader) {

    companion object {
        private const val SHADER_STRING =
            """
            attribute vec4 position;
            attribute vec4 inputTextureCoordinate;
            attribute vec4 inputTextureCoordinate2;
             
            varying vec2 textureCoordinate;
            varying vec2 textureCoordinate2;
             
            void main()
            {
                gl_Position = position;
                textureCoordinate = inputTextureCoordinate.xy;
                textureCoordinate2 = inputTextureCoordinate2.xy;
            }
            """
    }

    private var secondInputFramebuffer: Framebuffer? = null
    private var filterSecondTextureCoordinateAttribute: Int = 0
    private var filterInputTextureUniform2: Int = 0
    private var inputRotation2: RotationMode = RotationMode.NoRotation
    private var firstFrameTime: Long = -1
    private var secondFrameTime: Long = -1

    private var hasSetFirstTexture: Boolean = false
    private var hasReceivedFirstFrame: Boolean = false
    private var hasReceivedSecondFrame: Boolean = false
    private var firstFrameCheckDisabled: Boolean = false
    private var secondFrameCheckDisabled: Boolean = false

    init {
        runSynchronously(Runnable {
            GLContext.useProcessingContext()
            filterSecondTextureCoordinateAttribute =
                filterProgram?.attributeIndex("inputTextureCoordinate2") ?: 0
            filterInputTextureUniform2 =
                filterProgram?.uniformIndex("inputImageTexture2") ?: 0
            GLES20.glEnableVertexAttribArray(filterSecondTextureCoordinateAttribute)
        })
    }

    override fun initializeAttributes() {
        super.initializeAttributes()
        filterProgram?.addAttribute("inputTextureCoordinate2")
    }


    fun disableFirstFrameCheck() {
        firstFrameCheckDisabled = false
    }

    fun disableSecondFrameCheck() {
        secondFrameCheckDisabled = true
    }

    override fun renderToTexture(vertices: FloatBuffer, textureCoordinates: FloatBuffer) {
        GLContext.setActiveShaderProgram(filterProgram)

        outputFramebuffer =
            GLContext.sharedFramebufferCache()
                ?.fetchFramebuffer(getInputSize(), false, outputTextureOptions)

        outputFramebuffer?.activate()
        setUniformsForProgram()

        GLES20.glClearColor(
            backgroundColor.r, backgroundColor.g,
            backgroundColor.b, backgroundColor.a
        )
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // 激活纹理 2
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, getInputFramebuffer()?.textureId ?: 0)
        GLES20.glUniform1i(inputImageTextureUniform, 2)

        // 激活纹理 3
        GLES20.glActiveTexture(GLES20.GL_TEXTURE3)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, secondInputFramebuffer?.textureId ?: 0)
        GLES20.glUniform1i(inputImageTextureUniform, 3)

        // GL坐标
        GLES20.glEnableVertexAttribArray(positionAttribute)
        GLES20.glVertexAttribPointer(positionAttribute, 2, GLES20.GL_FLOAT, false, 0, vertices)

        // 纹理坐标1
        GLES20.glEnableVertexAttribArray(inputTextureCoordinateAttribute)
        GLES20.glVertexAttribPointer(
            inputTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, textureCoordinates
        )
        // 纹理坐标2
        GLES20.glEnableVertexAttribArray(filterSecondTextureCoordinateAttribute)
        GLES20.glVertexAttribPointer(
            filterSecondTextureCoordinateAttribute,
            2, GLES20.GL_FLOAT, false, 0,
            textureCoordinatesForRotation(inputRotation2, false)
        )

        // 绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // 关闭 属性
        GLES20.glDisableVertexAttribArray(positionAttribute)
        GLES20.glDisableVertexAttribArray(inputTextureCoordinateAttribute)
        GLES20.glDisableVertexAttribArray(filterSecondTextureCoordinateAttribute)

        // 输入释放fbo
        getInputFramebuffer()?.unlock()
        secondInputFramebuffer?.unlock()
    }

    override fun nextAvailableTextureIndex(): Int {
        return if (hasSetFirstTexture) 1 else 0
    }

    override fun setInputFramebuffer(framebuffer: Framebuffer?, textureIndex: Int) {
        if (textureIndex == 0) {
            super.setInputFramebuffer(framebuffer, textureIndex)
            hasSetFirstTexture = true
            getInputFramebuffer()?.lock()
        } else {
            secondInputFramebuffer = framebuffer
            secondInputFramebuffer?.lock()
        }
    }

    override fun setInputSize(inputSize: Size?, textureIndex: Int) {
        super.setInputSize(inputSize, textureIndex)
        if (inputSize?.width == 0 || inputSize?.height == 0) {
            hasSetFirstTexture = false
        }
    }
}