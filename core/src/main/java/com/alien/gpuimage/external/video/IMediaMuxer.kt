package com.alien.gpuimage.external.video

import android.media.MediaCodec
import android.media.MediaFormat
import java.nio.ByteBuffer

interface IMediaMuxer {

    fun start()

    fun addAudioTrack(format: MediaFormat): Int

    fun addVideoTrack(format: MediaFormat?): Int

    fun writeAudioData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo)

    fun writeVideoData(byteBuffer: ByteBuffer?, bufferInfo: MediaCodec.BufferInfo?)

    fun close(): Long
}