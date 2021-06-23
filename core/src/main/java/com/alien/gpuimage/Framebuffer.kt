package com.alien.gpuimage

import android.opengl.GLES20
import com.alien.gpuimage.utils.Logger

class Framebuffer(
    var width: Int,
    var height: Int,
    var textureAttributes: TextureAttributes,
    var onlyTexture: Boolean = false
) {

    companion object {
        private const val TAG = "Framebuffer"
    }

    var textureId: Int = -1
        private set
    var framebufferId: Int = -1
        private set

    private var framebufferReferenceCount: Int = 0
    private var referenceCountingDisabled: Boolean = false

    init {
        framebufferReferenceCount = 0
        referenceCountingDisabled = false

        if (onlyTexture) {
            generateTexture()
            framebufferId = 0
        } else {
            generateFramebuffer()
        }
    }

    private fun generateFramebuffer() {
        Logger.d(TAG, "generateFramebuffer")

        framebufferId = intArray { GLES20.glGenFramebuffers(1, it, 0) }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebufferId)
        generateTexture()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            textureAttributes.internalFormat,
            width,
            height,
            0,
            textureAttributes.format,
            textureAttributes.type,
            null
        )
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            textureId,
            0
        )
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    private fun generateTexture() {
        Logger.d(TAG, "generateTexture")

        textureId = intArray { GLES20.glGenTextures(1, it, 0) }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            textureAttributes.minFilter
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            textureAttributes.magFilter
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            textureAttributes.wrapS
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            textureAttributes.wrapT
        )
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            textureAttributes.internalFormat,
            width,
            height,
            0,
            textureAttributes.format,
            textureAttributes.type,
            null
        )
    }

    fun activate() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebufferId)
        GLES20.glViewport(0, 0, width, height)
    }

    fun lock() {
        if (referenceCountingDisabled) {
            return
        }
        framebufferReferenceCount++
    }

    fun unlock() {
        if (referenceCountingDisabled) {
            return
        }

        assert(framebufferReferenceCount > 0) {
            "Tried to overrelease a framebuffer, did you forget to call -useNextFrameForImageCapture before using -imageFromCurrentFramebuffer?"
        }

        framebufferReferenceCount--
        if (framebufferReferenceCount < 1) {
            GLContext.sharedFramebufferCache()?.returnFramebuffer(this)
        }
    }

    fun clearAllLocks() {
        framebufferReferenceCount = 0
    }

    fun disableReferenceCounting() {
        referenceCountingDisabled = true
    }

    fun enableReferenceCounting() {
        referenceCountingDisabled = false
    }

    fun destroy() {
        if (framebufferId > 0) {
            GLES20.glDeleteFramebuffers(1, intArrayOf(framebufferId), 0)
            framebufferId = -1
        }

        if (textureId > 0) {
            GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
            textureId = -1
        }
    }

    override fun toString(): String {
        return "textureId:$textureId framebufferId:$framebufferId width:$width height:$height referenceCount:$framebufferReferenceCount referenceDisabled:$referenceCountingDisabled"
    }
}