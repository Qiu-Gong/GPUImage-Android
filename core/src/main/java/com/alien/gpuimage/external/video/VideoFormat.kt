package com.alien.gpuimage.external.video

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.util.Size
import java.security.InvalidParameterException

object VideoFormat {

    private var builder = Builder()

    data class Builder(
        val targetWidth: Int = -1,
        val targetHeight: Int = -1,
        val bitRate: Int = 2000000,
        val iFrameInterval: Int = 15,
        val maxFps: Int = Int.MAX_VALUE,
        val colorFormat: Int = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
    )

    fun initGlobalFormat(builder: Builder) {
        this.builder = builder
    }

    @JvmStatic
    fun getGlobalVideoTargetWidth(): Int = builder.targetWidth

    @JvmStatic
    fun getGlobalVideoTargetHeight(): Int = builder.targetHeight

    @JvmStatic
    fun getGlobalVideoBitRate(): Int = builder.bitRate

    @JvmStatic
    fun getGlobalVideoIFrameInterval(): Int = builder.iFrameInterval

    @JvmStatic
    fun getGlobalVideoMaxFps(): Int = builder.maxFps

    @JvmStatic
    fun getGlobalVideoColorFormat(): Int = builder.colorFormat

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

    fun getOutputFormat(mediaCodec: MediaCodec?, inputFormat: MediaFormat): MediaFormat {
        var rotation = 0
        if (inputFormat.containsKey(MediaFormat.KEY_ROTATION)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                rotation = inputFormat.getInteger(MediaFormat.KEY_ROTATION)
            }
        }

        val inputSize = if (rotation == 90 || rotation == 180) {
            Size(
                inputFormat.getInteger(MediaFormat.KEY_HEIGHT),
                inputFormat.getInteger(MediaFormat.KEY_WIDTH)
            )
        } else {
            Size(
                inputFormat.getInteger(MediaFormat.KEY_WIDTH),
                inputFormat.getInteger(MediaFormat.KEY_HEIGHT)
            )
        }

        val outputFormat = MediaFormat.createVideoFormat(
            BaseMediaCodec.OUT_MIME_TYPE, inputSize.width, inputSize.height
        )
        outputFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            builder.colorFormat
        )

        outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, builder.bitRate)
        outputFormat.setInteger(
            MediaFormat.KEY_FRAME_RATE,
            Math.min(inputFormat.getInteger(MediaFormat.KEY_FRAME_RATE), builder.maxFps)
        )
        outputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, builder.iFrameInterval)
        outputFormat.setString(MediaFormat.KEY_MIME, BaseMediaCodec.OUT_MIME_TYPE)

        return outputFormat
    }
}