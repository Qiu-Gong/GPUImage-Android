package com.alien.gpuimage.external.gesture

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import android.view.MotionEvent
import androidx.annotation.IntRange
import com.alien.gpuimage.outputs.TextureView
import com.alien.gpuimage.utils.getCenterArray
import com.alien.gpuimage.utils.getCornersArray
import com.alien.gpuimage.utils.getRectSidesFromCorners
import com.alien.gpuimage.utils.trapToRect
import kotlin.math.*


/**
 *
 */
@SuppressLint("ClickableViewAccessibility")
class ViewProxyGesture(context: Context, private val textureView: TextureView) {

    companion object {
        private const val TAG = "ViewProxyGesture"
        private const val DEFAULT_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION = 500L
    }

    private var minScale = 0.5f
    private var maxScale = 5f
    private val viewMatrix: Matrix = Matrix()
    private val viewRectF = RectF()
    private val tempMatrix = Matrix()
    private var initialImageCorners: FloatArray? = null
    private var initialImageCenter: FloatArray? = null
    private val currentImageCorners = FloatArray(8)
    private val currentImageCenter = FloatArray(2)
    private val matrixValues = FloatArray(9)
    private val gestureHelper: GestureHelper by lazy {
        GestureHelper.create(context, onGestureListener)
    }

    private val onGestureListener: OnGestureListener = object : OnGestureListener {
        override fun onDown(px: Float, py: Float) {
            cancelAllAnimations()
        }

        override fun onScale(deltaScale: Float, px: Float, py: Float, e: MotionEvent) {
            Log.d(TAG, "onScale: $deltaScale")
            postScale(deltaScale, px, py)
        }

        override fun onTranslate(deltaX: Float, deltaY: Float, e1: MotionEvent, e2: MotionEvent) {
            Log.d(TAG, "onTranslate: [$deltaX,$deltaY]")
            postTranslate(deltaX, deltaY)
        }

        override fun onDoubleTranslate(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ) {
            Log.d(TAG, "onDoubleTranslate: [$distanceX,$distanceY]")
            postTranslate(distanceX, distanceY)
        }

        override fun onUp(event: MotionEvent) {
            setImageToWrapViewBounds()
        }
    }

    private var wrapViewBoundsRunnable: WrapViewBoundsRunnable? = null

    init {
        textureView.setOnTouchListener { _, event ->
            gestureHelper.onTouchEvent(event)
        }
    }

    fun initViewCreate() {
        viewRectF.set(0F, 0F, textureView.width.toFloat(), textureView.height.toFloat())
        val initialImageRect = RectF(
            0F, 0F,
            textureView.width.toFloat(), textureView.height.toFloat()
        )
        initialImageCorners = initialImageRect.getCornersArray()
        initialImageCenter = initialImageRect.getCenterArray()
    }

    fun cancelAllAnimations() {
        textureView.removeCallbacks(wrapViewBoundsRunnable)
    }

    private fun postTranslate(deltaX: Float, deltaY: Float) {
        viewMatrix.postTranslate(deltaX, deltaY)
        updateTransform()
    }

    private fun postScale(deltaScale: Float, px: Float, py: Float) {
        if (deltaScale > 1 && getCurrentScale() * deltaScale <= maxScale) {
            viewMatrix.postScale(deltaScale, deltaScale, px, py)
            updateTransform()
        } else if (deltaScale < 1 && getCurrentScale() * deltaScale >= minScale) {
            viewMatrix.postScale(deltaScale, deltaScale, px, py)
            updateTransform()
        }
    }

    private fun zoomInImage(scale: Float, centerX: Float, centerY: Float) {
        if (scale <= maxScale) {
            postScale(scale / getCurrentScale(), centerX, centerY)
        }
    }

    private fun updateTransform() {
        textureView.setTransform(viewMatrix)
        textureView.postInvalidate()

        viewMatrix.mapPoints(currentImageCorners, initialImageCorners)
        viewMatrix.mapPoints(currentImageCenter, initialImageCenter)
    }

    private fun setImageToWrapViewBounds() {
        if (isImageWrapViewBounds()) return

        val currentX = currentImageCenter[0]
        val currentY = currentImageCenter[1]
        val currentScale = getCurrentScale()

        var deltaX = viewRectF.centerX() - currentX
        var deltaY = viewRectF.centerY() - currentY
        var deltaScale = 0f

        tempMatrix.reset()
        tempMatrix.setTranslate(deltaX, deltaY)

        val tempCurrentImageCorners: FloatArray =
            currentImageCorners.copyOf(currentImageCorners.size)
        tempMatrix.mapPoints(tempCurrentImageCorners)

        val willImageWrapViewBoundsAfterTranslate = isImageWrapViewBounds(tempCurrentImageCorners)

        if (willImageWrapViewBoundsAfterTranslate) {
            val imageIndents = calculateImageIndents()
            deltaX = -(imageIndents[0] + imageIndents[2])
            deltaY = -(imageIndents[1] + imageIndents[3])
        } else {
            val tempViewRect = RectF(viewRectF)
            tempMatrix.reset()
            tempMatrix.setRotate(getCurrentAngle())
            tempMatrix.mapRect(tempViewRect)

            val currentImageSides = currentImageCorners.getRectSidesFromCorners()
            deltaScale = max(
                tempViewRect.width() / currentImageSides[0],
                tempViewRect.height() / currentImageSides[1]
            )
            deltaScale = deltaScale * currentScale - currentScale
        }

        wrapViewBoundsRunnable = WrapViewBoundsRunnable(
            DEFAULT_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION,
            currentX,
            currentY,
            deltaX,
            deltaY,
            currentScale,
            deltaScale,
            willImageWrapViewBoundsAfterTranslate
        )
        textureView.post(wrapViewBoundsRunnable)
    }

    private fun isImageWrapViewBounds(): Boolean {
        return isImageWrapViewBounds(currentImageCorners)
    }

    private fun isImageWrapViewBounds(imageCorners: FloatArray): Boolean {
        tempMatrix.reset()
        tempMatrix.setRotate(-getCurrentAngle())

        val unRotatedImageCorners = imageCorners.copyOf(imageCorners.size)
        tempMatrix.mapPoints(unRotatedImageCorners)

        val unRotatedViewBoundsCorners = viewRectF.getCornersArray()
        tempMatrix.mapPoints(unRotatedViewBoundsCorners)

        return unRotatedImageCorners.trapToRect().contains(unRotatedViewBoundsCorners.trapToRect())
    }

    private fun calculateImageIndents(): FloatArray {
        tempMatrix.reset()
        tempMatrix.setRotate(-getCurrentAngle())

        val unRotatedImageCorners: FloatArray = currentImageCorners.copyOf(currentImageCorners.size)
        val unRotatedViewBoundsCorners: FloatArray = viewRectF.getCornersArray()

        tempMatrix.mapPoints(unRotatedImageCorners)
        tempMatrix.mapPoints(unRotatedViewBoundsCorners)

        val unRotatedImageRect = unRotatedImageCorners.trapToRect()
        val unRotatedViewRect = unRotatedViewBoundsCorners.trapToRect()

        val deltaLeft = unRotatedImageRect.left - unRotatedViewRect.left
        val deltaTop = unRotatedImageRect.top - unRotatedViewRect.top
        val deltaRight = unRotatedImageRect.right - unRotatedViewRect.right
        val deltaBottom = unRotatedImageRect.bottom - unRotatedViewRect.bottom

        val indents = FloatArray(4)
        indents[0] = if (deltaLeft > 0) deltaLeft else 0f
        indents[1] = if (deltaTop > 0) deltaTop else 0f
        indents[2] = if (deltaRight < 0) deltaRight else 0f
        indents[3] = if (deltaBottom < 0) deltaBottom else 0f

        tempMatrix.reset()
        tempMatrix.setRotate(getCurrentAngle())
        tempMatrix.mapPoints(indents)

        return indents
    }

    private fun getCurrentScale(): Float {
        return sqrt(
            getMatrixValue(Matrix.MSCALE_X).toDouble().pow(2.0)
                    + getMatrixValue(Matrix.MSKEW_Y).toDouble().pow(2.0)
        ).toFloat()
    }

    private fun getCurrentAngle(): Float {
        return (-(atan2(
            getMatrixValue(Matrix.MSKEW_X).toDouble(),
            getMatrixValue(Matrix.MSCALE_X).toDouble()
        ) * (180 / Math.PI))).toFloat()
    }

    private fun getMatrixValue(@IntRange(from = 0, to = 9) valueIndex: Int): Float {
        viewMatrix.getValues(matrixValues)
        return matrixValues[valueIndex]
    }

    inner class WrapViewBoundsRunnable(
        private val durationMs: Long,
        private val oldX: Float, private val oldY: Float,
        private val centerDiffX: Float, private val centerDiffY: Float,
        private val oldScale: Float, private val deltaScale: Float,
        private val willBeImageInBoundsAfterTranslate: Boolean
    ) : Runnable {

        private val startTime = System.currentTimeMillis()

        override fun run() {

            val now = System.currentTimeMillis()
            val currentMs = min(durationMs, now - startTime)

            val newX = CubicEasing.easeOut(currentMs.toFloat(), 0f, centerDiffX, durationMs)
            val newY = CubicEasing.easeOut(currentMs.toFloat(), 0f, centerDiffY, durationMs)
            val newScale = CubicEasing.easeInOut(currentMs.toFloat(), 0f, deltaScale, durationMs)

            if (currentMs < durationMs) {
                postTranslate(
                    newX - (currentImageCenter[0] - oldX),
                    newY - (currentImageCenter[1] - oldY)
                )
                if (!willBeImageInBoundsAfterTranslate) {
                    zoomInImage(oldScale + newScale, viewRectF.centerX(), viewRectF.centerY())
                }
                if (!isImageWrapViewBounds()) {
                    textureView.post(this)
                }
            }
        }
    }
}