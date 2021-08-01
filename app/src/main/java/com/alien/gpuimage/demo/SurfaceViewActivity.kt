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
import com.alien.gpuimage.outputs.SurfaceView
import com.alien.gpuimage.sources.Picture
import com.alien.gpuimage.utils.Logger
import java.io.File

class SurfaceViewActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SurfaceViewActivity"
        private const val KEY_PATH = "key_path"

        fun startActivity(context: Context, path: String?) {
            val intent = Intent(context, SurfaceViewActivity::class.java)
            intent.putExtra(KEY_PATH, path)
            context.startActivity(intent)
        }
    }

    private var surfaceView1: SurfaceView? = null
    private var surfaceView2: SurfaceView? = null
    private var surfaceView3: SurfaceView? = null
    private var surfaceView4: SurfaceView? = null

    private var picture: Picture? = null
    private var filter: Filter? = null
    private var brightnessFilter: BrightnessFilter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_surface_view)
        surfaceView1 = findViewById(R.id.sv_1)
        surfaceView2 = findViewById(R.id.sv_2)
        surfaceView3 = findViewById(R.id.sv_3)
        surfaceView4 = findViewById(R.id.sv_4)

        val path = intent.getStringExtra(KEY_PATH)
        if (!File(path ?: "").exists()) return

        val option = BitmapFactory.Options().apply { this.inSampleSize = 2 }
        val bitmap = BitmapFactory.decodeFile(path, option)
        picture = Picture(bitmap, true)
        filter = Filter()
        brightnessFilter = BrightnessFilter()

        picture?.addTarget(filter)
        filter?.addTarget(surfaceView1)

        picture?.addTarget(brightnessFilter)
        brightnessFilter?.addTarget(surfaceView2)

        picture?.addTarget(surfaceView3)

        surfaceView1?.setCallback(object : GLView.SurfaceViewCallback {
            override fun onViewCreate() {
                picture?.processPicture()
            }

            override fun onViewDestroy() = Unit
            override fun onViewSwapToScreen() = Unit
        })
        surfaceView2?.setCallback(object : GLView.SurfaceViewCallback {
            override fun onViewCreate() {
                picture?.processPicture()
            }

            override fun onViewDestroy() = Unit
            override fun onViewSwapToScreen() = Unit
        })
        surfaceView3?.setCallback(object : GLView.SurfaceViewCallback {
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