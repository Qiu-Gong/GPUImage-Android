package com.alien.gpuimage.demo

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.alien.gpuimage.external.gesture.TextureViewGesture
import com.alien.gpuimage.outputs.TextureView
import com.alien.gpuimage.outputs.widget.GLView
import com.alien.gpuimage.sources.Picture
import java.io.File

class GestureTextureViewActivity : AppCompatActivity() {

    companion object {
        private const val KEY_PATH = "key_path"

        fun startActivity(context: Context, path: String?) {
            val intent = Intent(context, GestureTextureViewActivity::class.java)
            intent.putExtra(KEY_PATH, path)
            context.startActivity(intent)
        }
    }

    private var textureView: TextureView? = null
    private var picture: Picture? = null

    private val textureTextureViewGestureHelper: TextureViewGesture by lazy {
        TextureViewGesture(this.baseContext, textureView!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gesture)

        textureView = findViewById(R.id.texture_view)
        val path = intent.getStringExtra(KEY_PATH)
        if (!File(path ?: "").exists()) return

        val option = BitmapFactory.Options().apply { this.inSampleSize = 2 }
        val bitmap = BitmapFactory.decodeFile(path, option)

        picture = Picture(bitmap, true)
        picture?.addTarget(textureView)

        textureView?.setCallback(object : GLView.SurfaceViewCallback {
            override fun onViewCreate() {
                picture?.processPicture()
                textureTextureViewGestureHelper.initViewCreate()
            }

            override fun onViewDestroy() = Unit
            override fun onViewSwapToScreen() = Unit
        })
    }


    override fun onDestroy() {
        super.onDestroy()
        picture?.release()
        textureTextureViewGestureHelper.cancelAllAnimations()
    }
}