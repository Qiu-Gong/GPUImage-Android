package com.alien.gpuimage.outputs.widget

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.view.SurfaceHolder
import com.alien.gpuimage.*
import com.alien.gpuimage.egl.EglSurfaceBase
import com.alien.gpuimage.egl.WindowSurface
import com.alien.gpuimage.sources.Input
import com.alien.gpuimage.utils.Logger
import java.nio.FloatBuffer

class GLView : Input {

    companion object {
        private const val TAG = "GLView"

        private const val DEFAULT_VERTEX_SHADER =
            """
            attribute vec4 position;
            attribute vec4 inputTextureCoordinate;
            varying vec2 textureCoordinate;
            
            void main()
            {
                gl_Position = position;
                textureCoordinate = inputTextureCoordinate.xy;
            }
            """

        private const val DEFAULT_FRAGMENT_SHADER =
            """
            varying highp vec2 textureCoordinate;
            uniform sampler2D inputImageTexture;
            
            void main()
            {
                gl_FragColor = texture2D(inputImageTexture, textureCoordinate);
            }
            """
    }

    enum class FillModeType {
        FillModeStretch,     // 全屏
        FillModePreserveAspectRatio,  // 合适比例
        FillModePreserveAspectRatioAndFill  // 全屏合适比例
    }

    private var inputSize: Size? = null
    private var inputFramebuffer: Framebuffer? = null
    private var inputRotation: RotationMode = RotationMode.NoRotation
    private var eglSurface: EglSurfaceBase? = null

    private var displayProgram: GLProgram? = null
    private var positionAttribute: Int = 0
    private var inputTextureCoordinateAttribute: Int = 0
    private var inputImageTextureUniform: Int = 0

    private var backgroundColor: BackgroundColor = BackgroundColor()
    private var currentViewSize: Size? = null
    private var fillMode: FillModeType = FillModeType.FillModePreserveAspectRatio
    private var imageVertices: FloatArray = FloatArray(8)
    private var imageVerticesBuffer: FloatBuffer? = null

    var callback: SurfaceViewCallback? = null

    interface SurfaceViewCallback {
        fun onViewCreate()
        fun onViewDestroy()
        fun onViewSwapToScreen()
    }

    private fun createProgram() {
        eglSurface?.makeCurrent()
        displayProgram = GLContext.program(
            DEFAULT_VERTEX_SHADER,
            DEFAULT_FRAGMENT_SHADER
        )
        displayProgram?.addAttribute("position")
        displayProgram?.addAttribute("inputTextureCoordinate")

        if (displayProgram?.link() == false) {
            Logger.e(TAG, "Program link log: ${displayProgram?.programLog}")
            Logger.e(TAG, "Fragment shader compile log: ${displayProgram?.fragmentShaderLog}")
            Logger.e(TAG, "Vertex shader compile log: ${displayProgram?.vertexShaderLog}")
            displayProgram = null
            assert(false) { "Filter shader link failed" }
        }

        positionAttribute = displayProgram?.attributeIndex("position") ?: 0
        inputTextureCoordinateAttribute =
            displayProgram?.attributeIndex("inputTextureCoordinate") ?: 0
        inputImageTextureUniform = displayProgram?.uniformIndex("inputImageTexture") ?: 0
    }

    private fun recalculateView() {
        if (currentViewSize == null || inputSize == null) return

        var widthScaling = 0f
        var heightScaling = 0f
        val insetSize = inputSize!!.makeSizeWithAspectRation(currentViewSize!!)

        when (fillMode) {
            FillModeType.FillModeStretch -> {
                widthScaling = 1.0f
                heightScaling = 1.0f
            }
            FillModeType.FillModePreserveAspectRatio -> {
                widthScaling =
                    insetSize.width.toFloat() / currentViewSize!!.width.toFloat()
                heightScaling =
                    insetSize.height.toFloat() / currentViewSize!!.height.toFloat()
            }
            FillModeType.FillModePreserveAspectRatioAndFill -> {
                widthScaling =
                    currentViewSize!!.height.toFloat() / insetSize.height.toFloat()
                heightScaling =
                    currentViewSize!!.width.toFloat() / insetSize.width.toFloat()
            }
        }
        val float = floatArrayOf(
            -widthScaling, -heightScaling, widthScaling, -heightScaling,
            -widthScaling, heightScaling, widthScaling, heightScaling
        )
        if (!float.contentEquals(imageVertices)) {
            imageVertices[0] = -widthScaling
            imageVertices[1] = -heightScaling
            imageVertices[2] = widthScaling
            imageVertices[3] = -heightScaling
            imageVertices[4] = -widthScaling
            imageVertices[5] = heightScaling
            imageVertices[6] = widthScaling
            imageVertices[7] = heightScaling
            imageVerticesBuffer = createFloatBuffer(imageVertices)
        }
    }

    override fun setInputSize(inputSize: Size?) {
        if ((inputRotation) == RotationMode.RotateLeft
            || (inputRotation) == RotationMode.RotateRight
            || (inputRotation) == RotationMode.RotateRightFlipVertical
            || (inputRotation) == RotationMode.RotateRightFlipHorizontal
        ) {
            this.inputSize?.width = inputSize?.height ?: 0
            this.inputSize?.height = inputSize?.width ?: 0
        }

        if (this.inputSize != inputSize) {
            this.inputSize = inputSize
            recalculateView()
        }
    }

    override fun setInputFramebuffer(framebuffer: Framebuffer?) {
        inputFramebuffer = framebuffer
        inputFramebuffer?.lock()
    }

    override fun setInputRotation(inputRotation: RotationMode) {
        this.inputRotation = inputRotation
    }

    override fun newFrameReadyAtTime(time: Long) {
        if (eglSurface == null) {
            inputFramebuffer?.unlock()
            inputFramebuffer = null
            return
        }

        eglSurface?.makeCurrent()
        GLContext.setActiveShaderProgram(displayProgram)

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        currentViewSize?.let { GLES20.glViewport(0, 0, it.width, it.height) }

        GLES20.glEnableVertexAttribArray(positionAttribute)
        GLES20.glEnableVertexAttribArray(inputTextureCoordinateAttribute)

        GLES20.glClearColor(
            backgroundColor.r,
            backgroundColor.g,
            backgroundColor.b,
            backgroundColor.a
        )
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE4)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputFramebuffer?.textureId ?: 0)
        GLES20.glUniform1i(inputImageTextureUniform, 4)

        GLES20.glVertexAttribPointer(
            positionAttribute,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            imageVerticesBuffer
        )

        val textureCoordinates = textureCoordinatesForRotation(inputRotation, true)
        GLES20.glVertexAttribPointer(
            inputTextureCoordinateAttribute, 2, GLES20.GL_FLOAT,
            false, 0, textureCoordinates
        )

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        eglSurface?.swapBuffers()

        GLES20.glDisableVertexAttribArray(positionAttribute)
        GLES20.glDisableVertexAttribArray(inputTextureCoordinateAttribute)
        inputFramebuffer?.unlock()
        inputFramebuffer = null

        callback?.onViewSwapToScreen()
    }

    fun viewCreate(any: Any) {
        runSynchronouslyGpu {
            if (any is SurfaceHolder) {
                eglSurface =
                    WindowSurface(
                        GLContext.sharedProcessingContext()?.eglCore, any.surface, true
                    )
            } else if (any is SurfaceTexture) {
                eglSurface = WindowSurface(GLContext.sharedProcessingContext()?.eglCore, any)
            }
            createProgram()
        }
    }

    fun viewChange(width: Int, height: Int) {
        runSynchronouslyGpu {
            currentViewSize = Size(width, height)
            recalculateView()

            // surfaceCreated surfaceChanged 完成后，才算创建完成
            callback?.onViewCreate()
        }
    }

    fun viewDestroyed() {
        runSynchronouslyGpu {
            GLContext.deleteProgram(displayProgram)
            eglSurface?.releaseEglSurface()

            // 销毁
            callback?.onViewDestroy()
        }
    }
}