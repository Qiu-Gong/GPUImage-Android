package com.alien.gpuimage.demo

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSeekBar
import com.alien.gpuimage.GLContext
import com.alien.gpuimage.filter.BrightnessFilter
import com.alien.gpuimage.filter.Filter
import com.alien.gpuimage.outputs.widget.GLView
import com.alien.gpuimage.outputs.TextureView
import com.alien.gpuimage.sources.Picture
import com.alien.gpuimage.utils.Logger
import java.io.File

class TextureViewActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TextureViewActivity"
        private const val KEY_PATH = "key_path"

        fun startActivity(context: Context, path: String?) {
            val intent = Intent(context, TextureViewActivity::class.java)
            intent.putExtra(KEY_PATH, path)
            context.startActivity(intent)
        }
    }

    private var textureView1: TextureView? = null
    private var textureView2: TextureView? = null
    private var textureView3: TextureView? = null
    private var textureView4: TextureView? = null

    private var picture: Picture? = null
    private var filter: Filter? = null
    private var brightnessFilter: BrightnessFilter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_texture_view)
        textureView1 = findViewById(R.id.tv_1)
        textureView2 = findViewById(R.id.tv_2)
        textureView3 = findViewById(R.id.tv_3)
        textureView4 = findViewById(R.id.tv_4)

        val path = intent.getStringExtra(KEY_PATH)
        if (!File(path ?: "").exists()) return

        val option = BitmapFactory.Options().apply { this.inSampleSize = 2 }
        val bitmap = BitmapFactory.decodeFile(path, option)
        picture = Picture(bitmap, true)
        filter = Filter()
        brightnessFilter = BrightnessFilter()

        picture?.addTarget(filter)
        filter?.addTarget(textureView1)

        picture?.addTarget(brightnessFilter)
        brightnessFilter?.addTarget(textureView2)

        picture?.addTarget(textureView3)

        textureView1?.setCallback(object : GLView.SurfaceViewCallback {
            override fun onViewCreate() {
                picture?.processPicture()
            }

            override fun onViewDestroy() = Unit
            override fun onViewSwapToScreen() = Unit
        })
        textureView2?.setCallback(object : GLView.SurfaceViewCallback {
            override fun onViewCreate() {
                picture?.processPicture()
            }

            override fun onViewDestroy() = Unit
            override fun onViewSwapToScreen() = Unit
        })
        textureView3?.setCallback(object : GLView.SurfaceViewCallback {
            override fun onViewCreate() {
                picture?.processPicture()
            }

            override fun onViewDestroy() = Unit
            override fun onViewSwapToScreen() = Unit
        })

        findViewById<AppCompatSeekBar>(R.id.seekbar).setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                brightnessFilter?.setBrightness(p1 / 100f)
                picture?.processPicture()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.d(TAG, "onDestroy")

        picture?.release()
        brightnessFilter?.release()
        GLContext.print()
    }
}