package com.alien.gpuimage.external.video

import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import java.security.InvalidParameterException

/**
 * 视频格式类封装
 */
object VideoFormat {

    fun selectVideoTrack(extractor: MediaExtractor): MediaFormat {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            if (format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) {
                extractor.selectTrack(i)
                return format
            }
        }

        throw InvalidParameterException("File contains no video track")
    }

    fun getOutputFormat(
        width: Int,
        height: Int,
        bitRate: Int = 2000000,
        maxFps: Int = 24,
        iFrameInterval: Int = 1
    ): MediaFormat {
        val outputFormat =
            MediaFormat.createVideoFormat(BaseMediaCodec.OUT_MIME_TYPE, width, height)
        outputFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, maxFps)
        outputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
        outputFormat.setString(MediaFormat.KEY_MIME, BaseMediaCodec.OUT_MIME_TYPE)

        return outputFormat
    }
}