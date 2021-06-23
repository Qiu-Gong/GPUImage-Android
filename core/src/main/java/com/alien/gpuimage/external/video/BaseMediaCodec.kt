package com.alien.gpuimage.external.video

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import java.util.concurrent.atomic.AtomicInteger

abstract class BaseMediaCodec {

    companion object {
        const val ENABLE_LOG = true

        const val OUT_MIME_TYPE = "video/avc"
        const val OUTPUT_TIMEOUT_US = 5000L

        const val STATE_IDLE = 0
        const val STATE_PREPARED = 1
        const val STATE_STARTED = 2
        const val STATE_ENCODING = 3
        const val STATE_STOPPING = 4
        const val STATE_STOPPED = 5
        const val STATE_RELEASE = 6
    }

    abstract fun prepare(format: MediaFormat, texId: Int? = null)

    abstract fun start()

    abstract fun drain()

    abstract fun stop()

    abstract fun release()

    abstract fun getInputSurface(): Surface?

    protected var state: AtomicInteger = AtomicInteger(STATE_RELEASE)
    var mediaCodec: MediaCodec? = null
        protected set
}