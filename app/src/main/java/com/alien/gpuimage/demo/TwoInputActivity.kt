package com.alien.gpuimage.demo

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.alien.gpuimage.filter.AddBlendFilter
import com.alien.gpuimage.outputs.SurfaceView
import com.alien.gpuimage.outputs.widget.GLView
import com.alien.gpuimage.sources.Picture
import java.io.File

class TwoInputActivity : AppCompatActivity() {

    companion object {
        private const val KEY_PATH = "key_path"

        fun startActivity(context: Context, path: String?) {
            val intent = Intent(context, TwoInputActivity::class.java)
            intent.putExtra(KEY_PATH, path)
            context.startActivity(intent)
        }
    }

    private var surfaceView: SurfaceView? = null
    private var picture1: Picture? = null
    private var picture2: Picture? = null
    private var addBlendFilter: AddBlendFilter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transform)

        surfaceView = findViewById(R.id.surface_view)
        val path = intent.getStringExtra(KEY_PATH)
        if (!File(path ?: "").exists()) return

        val bitmap = BitmapFactory.decodeFile(path)
        val watermark = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round)

        picture1 = Picture(bitmap, true)
        picture2 = Picture(watermark, true)
        addBlendFilter = AddBlendFilter()
        picture1?.addTarget(addBlendFilter)
        picture2?.addTarget(addBlendFilter)
        addBlendFilter?.addTarget(surfaceView)

        surfaceView?.setCallback(object : GLView.SurfaceViewCallback {
            override fun onViewCreate() {
                picture1?.processPictureSynchronously()
                picture2?.processPictureSynchronously()
            }

            override fun onViewDestroy() = Unit
            override fun onViewSwapToScreen() = Unit
            override fun onCaptureFrameToBitmap(bitmap: Bitmap?) = Unit
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        picture1?.release()
        picture2?.release()
        addBlendFilter?.release()
    }
}