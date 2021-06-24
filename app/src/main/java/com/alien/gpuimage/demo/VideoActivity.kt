package com.alien.gpuimage.demo

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaFormat
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.alien.gpuimage.GLContext
import com.alien.gpuimage.external.video.VideoFormat
import com.alien.gpuimage.filter.WatermarkFilter
import com.alien.gpuimage.outputs.SurfaceView
import com.alien.gpuimage.outputs.VideoEncoder
import com.alien.gpuimage.sources.VideoDecoder
import com.alien.gpuimage.utils.Logger
import java.io.File

class VideoActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "VideoActivity"
        private const val KEY_PATH = "key_path"

        private const val OUTPUT_VIDEO_PATH = "/sdcard/test.mp4"

        fun startActivity(context: Context, path: String?) {
            val intent = Intent(context, VideoActivity::class.java)
            intent.putExtra(KEY_PATH, path)
            context.startActivity(intent)
        }
    }

    private var videoDecoder: VideoDecoder? = null
    private var textFilter: WatermarkFilter? = null
    private var surfaceView: SurfaceView? = null
    private var videoEncoder: VideoEncoder? = null

    private var thread: HandlerThread? = null
    private var handler: Handler? = null

    private var isDecoderFinish: Boolean = false
    private lateinit var running: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        surfaceView = findViewById(R.id.sv_1)

        val watermark = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round)

        findViewById<AppCompatButton>(R.id.extractor).setOnClickListener {
            surfaceView?.post(running)
        }

        val path = intent.getStringExtra(KEY_PATH)
        if (!File(path ?: "").exists()) return

        thread = HandlerThread("CoderHandler")
        thread?.start()
        handler = Handler(thread!!.looper)

        videoDecoder = VideoDecoder(path!!, handler!!)
        videoEncoder = VideoEncoder(OUTPUT_VIDEO_PATH, getVideoEncoderFormat(), handler!!)
        textFilter = WatermarkFilter(watermark)
        videoDecoder?.addTarget(textFilter)
        textFilter?.addTarget(surfaceView)
        textFilter?.addTarget(videoEncoder)
        videoDecoder?.setCallback(object : VideoDecoder.VideoDecoderCallback {

            private var positionX = 0
            private var positionY = 0

            override fun onPrepared() {
            }

            override fun onFrameAvailableBefore(presentationTimeUs: Long) {
                positionX += 10
                positionY += 10
                textFilter?.setPosition(positionX, positionY)
            }

            override fun onFrameAvailableAfter(presentationTimeUs: Long) {
                videoEncoder?.drainEncoder()
            }

            override fun onFinish() {
                isDecoderFinish = true
                videoEncoder?.finish()
            }
        })

        running = Runnable {
            when (isDecoderFinish || isFinishing) {
                true -> {
                }
                false -> {
                    videoDecoder?.feedInputToDecoder()
                    surfaceView?.postDelayed(running, 30)
                }
            }
        }
    }

    private fun getVideoEncoderFormat(): MediaFormat {
        return VideoFormat.getOutputFormat(
            videoDecoder?.videoWidth ?: 0,
            videoDecoder?.videoHeight ?: 0
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.d(TAG, "onDestroy")

        thread?.quitSafely()
        thread?.join()
        thread = null

        videoDecoder?.release()
        videoEncoder?.release()
        textFilter?.release()
        GLContext.print()
    }
}