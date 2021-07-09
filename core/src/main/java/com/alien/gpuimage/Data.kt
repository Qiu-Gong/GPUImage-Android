package com.alien.gpuimage

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/// region Data 数据
data class Size(var width: Int = 0, var height: Int = 0) {

    fun makeSizeWithAspectRation(bounding: Size): Size {
        val srcAspectRatio = (width.toFloat() / height.toFloat())
        val destAspectRatio = (bounding.width.toFloat() / bounding.height.toFloat())

        val resultWidth: Float
        val resultHeight: Float
        if (srcAspectRatio > destAspectRatio) {
            resultWidth = bounding.width.toFloat()
            val scale = width.toFloat() / resultWidth
            resultHeight = height.toFloat() / scale
        } else {
            resultHeight = bounding.height.toFloat()
            val scale = height.toFloat() / resultHeight
            resultWidth = width.toFloat() / scale
        }
        return Size(resultWidth.toInt(), resultHeight.toInt())
    }

    override fun equals(other: Any?): Boolean {
        if (other is Size) {
            return (width == other.width && height == other.height)
        }
        return false
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        return result
    }

    fun setSize(size: Size) {
        this.width = size.width
        this.height = size.height
    }
}

data class TextureAttributes(
    var minFilter: Int = GLES20.GL_LINEAR,
    var magFilter: Int = GLES20.GL_LINEAR,
    var wrapS: Int = GLES20.GL_CLAMP_TO_EDGE,
    var wrapT: Int = GLES20.GL_CLAMP_TO_EDGE,
    var internalFormat: Int = GLES20.GL_RGBA,
    var format: Int = GLES20.GL_RGBA,
    var type: Int = GLES20.GL_UNSIGNED_BYTE
)

data class BackgroundColor(
    var r: Float = 0f,
    var g: Float = 0f,
    var b: Float = 0f,
    var a: Float = 1.0f
)

enum class RotationMode {
    NoRotation,
    RotateLeft,
    RotateRight,
    FlipVertical,
    FlipHorizontal,
    RotateRightFlipVertical,
    RotateRightFlipHorizontal,
    Rotate180
}
/// endregion

/// region Callback
interface Callback {
    fun function()
}
/// endregion

/// region 数据转换
fun intArray(function: (value: IntArray) -> Unit): Int {
    val intArray = IntArray(1)
    function(intArray)
    return intArray[0]
}

fun createFloatBuffer(floatArray: FloatArray): FloatBuffer {
    val byteBuffer = ByteBuffer.allocateDirect(floatArray.size * 4)
    byteBuffer.order(ByteOrder.nativeOrder())
    val floatBuffer = byteBuffer.asFloatBuffer()
    floatBuffer.put(floatArray)
    floatBuffer.position(0)
    return floatBuffer
}

val IMAGE_VERTICES = createFloatBuffer(
    floatArrayOf(
        -1.0f, -1.0f,
        1.0f, -1.0f,
        -1.0f, 1.0f,
        1.0f, 1.0f
    )
)

val displayNoRotationTextureCoordinates =
    createFloatBuffer(floatArrayOf(0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f))
val displayRotateRightTextureCoordinates =
    createFloatBuffer(floatArrayOf(1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f))
val displayRotateLeftTextureCoordinates =
    createFloatBuffer(floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f))
val displayVerticalFlipTextureCoordinates =
    createFloatBuffer(floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f))
val displayHorizontalFlipTextureCoordinates =
    createFloatBuffer(floatArrayOf(1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f))
val displayRotateRightVerticalFlipTextureCoordinates =
    createFloatBuffer(floatArrayOf(1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f))
val displayRotateRightHorizontalFlipTextureCoordinates =
    createFloatBuffer(floatArrayOf(1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f))
val displayRotate180TextureCoordinates =
    createFloatBuffer(floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f))

val noRotationTextureCoordinates =
    createFloatBuffer(floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f))
val rotateLeftTextureCoordinates =
    createFloatBuffer(floatArrayOf(1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f))
val rotateRightTextureCoordinates =
    createFloatBuffer(floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f))
val verticalFlipTextureCoordinates =
    createFloatBuffer(floatArrayOf(0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f))
val horizontalFlipTextureCoordinates =
    createFloatBuffer(floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f))
val rotateRightVerticalFlipTextureCoordinates =
    createFloatBuffer(floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f))
val rotateRightHorizontalFlipTextureCoordinates =
    createFloatBuffer(floatArrayOf(1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f))
val rotate180TextureCoordinates =
    createFloatBuffer(floatArrayOf(1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f))

fun textureCoordinatesForRotation(
    rotationMode: RotationMode,
    isDisplayCoordinate: Boolean
): FloatBuffer {
    if (isDisplayCoordinate) {
        return when (rotationMode) {
            RotationMode.NoRotation -> displayNoRotationTextureCoordinates
            RotationMode.RotateLeft -> displayRotateLeftTextureCoordinates
            RotationMode.RotateRight -> displayRotateRightTextureCoordinates
            RotationMode.FlipVertical -> displayVerticalFlipTextureCoordinates
            RotationMode.FlipHorizontal -> displayHorizontalFlipTextureCoordinates
            RotationMode.RotateRightFlipVertical -> displayRotateRightVerticalFlipTextureCoordinates
            RotationMode.RotateRightFlipHorizontal -> displayRotateRightHorizontalFlipTextureCoordinates
            RotationMode.Rotate180 -> displayRotate180TextureCoordinates
        }
    } else {
        return when (rotationMode) {
            RotationMode.NoRotation -> noRotationTextureCoordinates
            RotationMode.RotateLeft -> rotateLeftTextureCoordinates
            RotationMode.RotateRight -> rotateRightTextureCoordinates
            RotationMode.FlipVertical -> verticalFlipTextureCoordinates
            RotationMode.FlipHorizontal -> horizontalFlipTextureCoordinates
            RotationMode.RotateRightFlipVertical -> rotateRightVerticalFlipTextureCoordinates
            RotationMode.RotateRightFlipHorizontal -> rotateRightHorizontalFlipTextureCoordinates
            RotationMode.Rotate180 -> rotate180TextureCoordinates
        }
    }
}

fun rotationSwapsWidthAndHeight(rotation: RotationMode): Boolean {
    return (rotation == RotationMode.RotateLeft
            || rotation == RotationMode.RotateRight
            || rotation == RotationMode.RotateRightFlipVertical
            || rotation == RotationMode.RotateRightFlipHorizontal)
}

/// endregion