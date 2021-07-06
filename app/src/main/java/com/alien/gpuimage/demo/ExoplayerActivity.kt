package com.alien.gpuimage.demo

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.alien.gpuimage.outputs.TextureView
import com.alien.gpuimage.sources.ExoplayerPipeline
import com.alien.gpuimage.sources.ImageReaderPipeline
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import java.io.File

class ExoplayerActivity : AppCompatActivity() {

    companion object {
        private const val KEY_PATH = "key_path"

        fun startActivity(context: Context, path: String?) {
            val intent = Intent(context, ExoplayerActivity::class.java)
            intent.putExtra(KEY_PATH, path)
            context.startActivity(intent)
        }
    }

    private val simpleExoPlayer: SimpleExoPlayer by lazy {
        ExoPlayerFactory.newSimpleInstance(applicationContext).apply {
            this.repeatMode = Player.REPEAT_MODE_OFF
            this.playWhenReady = false
        }
    }

    private var textureView: TextureView? = null
    private var exoPipeline: ExoplayerPipeline? = null
    private var imageReaderPipeline: ImageReaderPipeline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exo_player)

        val path = intent.getStringExtra(KEY_PATH)
        if (!File(path ?: "").exists()) return
        prepare(path!!)

        // 设置视频宽高方向，如果在exo中获取宽高，则预览会黑
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val width =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
        val height =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt()
                ?: 0
        val rotation =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt()
                ?: 0

        textureView = findViewById(R.id.sv_1)
        exoPipeline = ExoplayerPipeline()
        imageReaderPipeline = ImageReaderPipeline(width, height, rotation)
        exoPipeline?.addTarget(imageReaderPipeline)
        exoPipeline?.addTarget(textureView)
        exoPipeline?.setFormat(width, height, rotation)
        simpleExoPlayer.setVideoSurface(exoPipeline!!.getSurface())

        findViewById<Button>(R.id.start).setOnClickListener {
            simpleExoPlayer.seekTo(0L)
            simpleExoPlayer.playWhenReady = true
        }
    }

    private fun prepare(path: String) {
        val dataSourceFactory = DefaultDataSourceFactory(
            applicationContext,
            Util.getUserAgent(applicationContext, applicationContext.packageName)
        )
        val videoSource: MediaSource = ExtractorMediaSource.Factory(dataSourceFactory)
            .createMediaSource(Uri.parse(path))
        simpleExoPlayer.prepare(videoSource)
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPipeline?.release()
    }
}