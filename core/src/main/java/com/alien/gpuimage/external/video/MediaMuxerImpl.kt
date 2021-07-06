package com.alien.gpuimage.external.video

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.nio.ByteBuffer

/**
 * MediaMuxer 封装实例类
 */
class MediaMuxerImpl(outputPath: String) : IMediaMuxer {

    companion object {
        private const val TAG = "MediaMuxerImpl"
        private const val DEFAULT_OUTPUT_FORMAT = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
    }

    private var muxer: MediaMuxer? = null

    // muxer 轨道
    private var audioTrack: Int = -1
    private var videoTrack: Int = -1

    // 文件大小
    private var totalFileSize: Long = 0

    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    private var videoCache: MediaMuxerEncodedFrameQueue = MediaMuxerEncodedFrameQueue(5)

    init {
        muxer = MediaMuxer(outputPath, DEFAULT_OUTPUT_FORMAT)

        thread = HandlerThread("MediaMuxer")
        thread?.start()
        handler = Handler(thread!!.looper)
    }

    override fun start() {
        muxer?.start()
        totalFileSize = 0
    }

    override fun addAudioTrack(format: MediaFormat): Int {
        audioTrack = muxer?.addTrack(format) ?: -1
        if (BaseMediaCodec.ENABLE_LOG) {
            Log.d(TAG, "addAudioTrack() called:$audioTrack")
        }
        return audioTrack
    }

    override fun addVideoTrack(format: MediaFormat?): Int {
        format?.let {
            videoTrack = muxer?.addTrack(it) ?: -1
        }
        if (BaseMediaCodec.ENABLE_LOG) {
            Log.d(TAG, "addVideoTrack() called:$videoTrack")
        }
        videoCache.setTrackIndex(videoTrack)
        return videoTrack
    }

    override fun writeAudioData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        totalFileSize += bufferInfo.size
        muxer?.writeSampleData(audioTrack, byteBuffer, bufferInfo)
    }

    override fun writeVideoData(byteBuffer: ByteBuffer?, bufferInfo: MediaCodec.BufferInfo?) {
        totalFileSize += (bufferInfo?.size ?: 0)
        if (byteBuffer != null && bufferInfo != null) {
            videoCache.cacheFrame(byteBuffer, bufferInfo)
            videoCache.writeSampleData(muxer, handler)
        }
    }

    override fun close(): Long {
        muxer?.stop()
        muxer?.release()
        muxer = null

        thread?.quitSafely()
        thread?.join()
        thread = null

        return totalFileSize
    }
}