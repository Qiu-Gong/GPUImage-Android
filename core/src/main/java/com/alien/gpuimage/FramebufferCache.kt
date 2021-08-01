package com.alien.gpuimage

import com.alien.gpuimage.utils.Logger

class FramebufferCache {

    companion object {
        private const val TAG = "FramebufferCache"
    }

    private val frameBuffers: MutableMap<String, Framebuffer> = mutableMapOf()
    private val framebufferTypeCounts: MutableMap<String, Int> = mutableMapOf()

    fun fetchFramebuffer(size: Size?, onlyTexture: Boolean): Framebuffer? {
        if (size == null || size.width <= 0 || size.height <= 0) return null
        return fetchFramebuffer(size.width, size.height, onlyTexture, TextureAttributes())
    }

    fun fetchFramebuffer(
        size: Size?,
        onlyTexture: Boolean,
        textureAttributes: TextureAttributes
    ): Framebuffer? {
        if (size == null || size.width <= 0 || size.height <= 0) return null
        return fetchFramebuffer(size.width, size.height, onlyTexture, textureAttributes)
    }

    private fun fetchFramebuffer(
        width: Int,
        height: Int,
        onlyTexture: Boolean,
        textureAttributes: TextureAttributes
    ): Framebuffer {
        var framebufferFromCache: Framebuffer? = null
        val lookupHash = getHash(width, height, onlyTexture, textureAttributes)
        val numberOfMatchingFrameBuffers = framebufferTypeCounts[lookupHash] ?: 0

        Logger.d(
            TAG,
            "fetchFramebuffer lookupHash:$lookupHash numberOfMatching:$numberOfMatchingFrameBuffers"
        )
        if (numberOfMatchingFrameBuffers < 1) {
            Logger.d(TAG, "fetchFramebuffer new Framebuffer")
            framebufferFromCache = Framebuffer(width, height, textureAttributes, onlyTexture)
        } else {
            var curFramebufferId = numberOfMatchingFrameBuffers - 1
            while (framebufferFromCache == null && curFramebufferId >= 0) {
                val framebufferHash = String.format("%s-%d", lookupHash, curFramebufferId)
                if (frameBuffers.containsKey(framebufferHash)) {
                    framebufferFromCache = frameBuffers[framebufferHash]
                    frameBuffers.remove(framebufferHash)
                } else {
                    framebufferFromCache = null
                }
                curFramebufferId--
            }
            curFramebufferId++
            framebufferTypeCounts[lookupHash] = curFramebufferId

            if (framebufferFromCache == null) {
                framebufferFromCache = Framebuffer(width, height, textureAttributes, onlyTexture)
            }
        }
        framebufferFromCache.lock()
        return framebufferFromCache
    }

    fun returnFramebuffer(framebuffer: Framebuffer?) {
        if (framebuffer == null) return

        val width = framebuffer.width
        val height = framebuffer.height
        val textureAttributes = framebuffer.textureAttributes
        val lookupHash = getHash(width, height, framebuffer.onlyTexture, textureAttributes)
        val numberOfMatchingFrameBuffers = framebufferTypeCounts[lookupHash] ?: 0

        val framebufferHash = String.format("%s-%d", lookupHash, numberOfMatchingFrameBuffers)
        frameBuffers[framebufferHash] = framebuffer
        framebufferTypeCounts[lookupHash] = numberOfMatchingFrameBuffers + 1
    }

    private fun getHash(
        width: Int,
        height: Int,
        onlyTexture: Boolean,
        textureAttributes: TextureAttributes
    ): String {
        if (onlyTexture) {
            return String.format(
                "%dx%d-%d:%d:%d:%d:%d:%d:%d-NOFB",
                width,
                height,
                textureAttributes.minFilter,
                textureAttributes.magFilter,
                textureAttributes.wrapS,
                textureAttributes.wrapT,
                textureAttributes.internalFormat,
                textureAttributes.format,
                textureAttributes.type
            )
        } else {
            return String.format(
                "%dx%d-%d:%d:%d:%d:%d:%d:%d",
                width,
                height,
                textureAttributes.minFilter,
                textureAttributes.magFilter,
                textureAttributes.wrapS,
                textureAttributes.wrapT,
                textureAttributes.internalFormat,
                textureAttributes.format,
                textureAttributes.type
            )
        }
    }

    fun release() {
        frameBuffers.values.forEach { it.destroy() }
        frameBuffers.clear()
        framebufferTypeCounts.clear()
    }

    fun gc() {
        release()
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        val iterator = frameBuffers.values.iterator()
        iterator.forEach {
            stringBuilder.append(it.toString()).append("\n")
        }
        return stringBuilder.toString()
    }
}
