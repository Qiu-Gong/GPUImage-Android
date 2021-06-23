package com.alien.gpuimage.external.video

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.nio.ByteBuffer

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

    init {
        muxer = MediaMuxer(outputPath, DEFAULT_OUTPUT_FORMAT)
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
        return videoTrack
    }

    override fun writeAudioData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        totalFileSize += bufferInfo.size
        muxer?.writeSampleData(audioTrack, byteBuffer, bufferInfo)
    }

    override fun writeVideoData(byteBuffer: ByteBuffer?, bufferInfo: MediaCodec.BufferInfo?) {
        totalFileSize += (bufferInfo?.size ?: 0)
        if (byteBuffer != null && bufferInfo != null) {
            muxer?.writeSampleData(videoTrack, byteBuffer, bufferInfo)
        }
    }

    override fun close(): Long {
        muxer?.stop()
        muxer?.release()
        muxer = null
        return totalFileSize
    }
}