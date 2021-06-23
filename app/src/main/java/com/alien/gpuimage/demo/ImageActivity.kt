package com.alien.gpuimage.demo

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSeekBar
import com.alien.gpuimage.GLContext
import com.alien.gpuimage.filter.BrightnessFilter
import com.alien.gpuimage.filter.Filter
import com.alien.gpuimage.outputs.BitmapImageView
import com.alien.gpuimage.sources.Picture
import com.alien.gpuimage.utils.Logger
import java.io.File

class ImageActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ImageActivity"
        private const val KEY_PATH = "key_path"

        fun startActivity(context: Context, path: String?) {
            val intent = Intent(context, ImageActivity::class.java)
            intent.putExtra(KEY_PATH, path)
            context.startActivity(intent)
        }
    }

    private var picture: Picture? = null
    private var filter: Filter? = null
    private var brightnessFilter: BrightnessFilter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)
        Logger.d(TAG, "onCreate")

        val path = intent.getStringExtra(KEY_PATH)
        if (!File(path ?: "").exists()) return

        GLContext.print()
        val iv1 = findViewById<BitmapImageView>(R.id.iv_1)
        val iv2 = findViewById<BitmapImageView>(R.id.iv_2)
        val iv3 = findViewById<BitmapImageView>(R.id.iv_3)
        val iv4 = findViewById<BitmapImageView>(R.id.iv_4)

        val option = BitmapFactory.Options().apply { this.inSampleSize = 4 }
        val bitmap = BitmapFactory.decodeFile(path, option)
        picture = Picture(bitmap, true)
        filter = Filter()
        brightnessFilter = BrightnessFilter()

        picture?.addTarget(filter)
        filter?.addTarget(iv1)

        picture?.addTarget(brightnessFilter)
        brightnessFilter?.addTarget(iv2)

        picture?.addTarget(iv3)
//        picture?.addTarget(iv4)
        picture?.processPicture()

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
        filter?.release()
        brightnessFilter?.release()
        GLContext.print()
    }
}