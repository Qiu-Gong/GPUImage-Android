package com.alien.gpuimage

import android.opengl.GLES11Ext.GL_BGRA
import android.opengl.GLES20
import com.alien.gpuimage.utils.Logger


class Framebuffer {

    companion object {
        private const val TAG = "Framebuffer"
    }

    var width: Int
        private set
    var height: Int
        private set
    var textureAttributes: TextureAttributes
        private set
    var onlyTexture: Boolean
        private set

    var textureId: Int = -1
        private set
    var framebufferId: Int = -1
        private set

    private var framebufferReferenceCount: Int = 0
    private var referenceCountingDisabled: Boolean = false

    constructor(
        width: Int, height: Int,
        textureAttributes: TextureAttributes,
        onlyTexture: Boolean = false
    ) {
        this.width = width
        this.height = height
        this.textureAttributes = textureAttributes
        this.onlyTexture = onlyTexture
        this.framebufferReferenceCount = 0
        this.referenceCountingDisabled = false

        if (this.onlyTexture) {
            generateTexture()
            this.framebufferId = 0
        } else {
            generateFramebuffer()
        }
    }

    constructor(width: Int, height: Int, inputTexture: Int) {
        val defaultTextureOptions = TextureAttributes()
        defaultTextureOptions.minFilter = GLES20.GL_LINEAR
        defaultTextureOptions.magFilter = GLES20.GL_LINEAR
        defaultTextureOptions.wrapS = GLES20.GL_CLAMP_TO_EDGE
        defaultTextureOptions.wrapT = GLES20.GL_CLAMP_TO_EDGE
        defaultTextureOptions.internalFormat = GLES20.GL_RGBA
        defaultTextureOptions.format = GL_BGRA
        defaultTextureOptions.type = GLES20.GL_UNSIGNED_BYTE

        this.textureAttributes = defaultTextureOptions
        this.width = width
        this.height = height
        this.onlyTexture = true
        this.framebufferReferenceCount = 0
        this.referenceCountingDisabled = true

        this.textureId = inputTexture
    }

    private fun generateFramebuffer() {
        Logger.d(TAG, "generateFramebuffer")

        framebufferId = DataBuffer.intArray { GLES20.glGenFramebuffers(1, it, 0) }
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

        textureId = DataBuffer.intArray { GLES20.glGenTextures(1, it, 0) }
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
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)

        // 检查 fbo 是否和纹理一致
        val buf = IntArray(1)
        GLES20.glGetFramebufferAttachmentParameteriv(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME,
            buf, 0
        )
        if (buf[0] != textureId) {
            Logger.e(TAG, "bind textureId error... ${toString()}")
        }
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