package com.alien.gpuimage.demo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.alien.gif.GifTranscoder
import com.alien.gpuimage.GLContext
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.engine.impl.GlideEngine
import com.zhihu.matisse.filter.Filter
import com.zhihu.matisse.internal.entity.IncapableCause
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.utils.PhotoMetadataUtils
import java.util.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 0x11
        private const val FILE_REQUEST_CODE = 0x22

        private val permissions: Array<String> = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    private var type: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.select_picture).setOnClickListener {
            clickListener(it.id)
        }

        findViewById<View>(R.id.select_surface_view).setOnClickListener {
            clickListener(it.id)
        }

        findViewById<View>(R.id.select_texture_view).setOnClickListener {
            clickListener(it.id)
        }

        findViewById<View>(R.id.select_video).setOnClickListener {
            clickListener(it.id)
        }

        findViewById<View>(R.id.transform_view).setOnClickListener {
            clickListener(it.id)
        }

        findViewById<View>(R.id.two_input).setOnClickListener {
            clickListener(it.id)
        }

        findViewById<View>(R.id.exo).setOnClickListener {
            clickListener(it.id)
        }

        findViewById<View>(R.id.gesture).setOnClickListener {
            clickListener(it.id)
        }

        findViewById<View>(R.id.memory_print).setOnClickListener {
            GLContext.print()
        }

        findViewById<View>(R.id.gc).setOnClickListener {
            GLContext.gc()
        }

        findViewById<View>(R.id.gif).setOnClickListener {
//            thread {
//                val gif = GifDecode(this)
//                gif.loadGif("sdcard/1.gif")
//                if (gif.width == -1 || gif.height == -1) {
//                    return@thread
//                }
//
//                val bitmap = Bitmap.createBitmap(gif.width, gif.height, Bitmap.Config.ARGB_8888)
//                var delay = 1
//                while (delay != -1 && delay != 0) {
//                    delay = gif.updateFrame(bitmap)
//                    if (delay > 0) {
//                        Thread.sleep(delay.toLong())
//                    }
//                }
//                gif.release()
//            }

            thread {
                GifTranscoder.transcode(this, "sdcard/1.gif", "sdcard/2.gif")
            }
        }
    }

    private fun clickListener(id: Int) {
        if (getPermissions(permissions).isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                permissions,
                PERMISSION_REQUEST_CODE
            )
            return
        }
        type = id
        chooserFile()
    }

    private fun getPermissions(permissions: Array<String>): List<String> {
        val requestPermissions = ArrayList<String>()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_DENIED
            ) {
                requestPermissions.add(permission)
            }
        }
        return requestPermissions
    }

    private fun chooserFile() {
        Matisse.from(this@MainActivity)
            .choose(MimeType.ofImage())
            .theme(R.style.Matisse_Dracula)
            .countable(false)
            .addFilter(GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
            .maxSelectable(1)
            .originalEnable(true)
            .maxOriginalSize(10)
            .imageEngine(GlideEngine())
            .forResult(FILE_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (getPermissions(permissions).isEmpty()) {
                chooserFile()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == FILE_REQUEST_CODE) {
                val path = Matisse.obtainPathResult(data)[0]
                if (type == R.id.select_picture) {
                    ImageActivity.startActivity(this, path)
                } else if (type == R.id.select_surface_view) {
                    SurfaceViewActivity.startActivity(this, path)
                } else if (type == R.id.select_texture_view) {
                    TextureViewActivity.startActivity(this, path)
                } else if (type == R.id.select_video) {
                    VideoActivity.startActivity(this, path)
                } else if (type == R.id.transform_view) {
                    TransformActivity.startActivity(this, path)
                } else if (type == R.id.two_input) {
                    TwoInputActivity.startActivity(this, path)
                } else if (type == R.id.exo) {
                    ExoplayerActivity.startActivity(this, path)
                } else if (type == R.id.gesture) {
                    GestureTextureViewActivity.startActivity(this, path)
                }
            }
        }
    }

    private class GifSizeFilter constructor(
        private val mMinWidth: Int,
        private val mMinHeight: Int,
        private val mMaxSize: Int
    ) : Filter() {

        public override fun constraintTypes(): Set<MimeType> {
            return HashSet<MimeType>().apply { add(MimeType.GIF) }
        }

        override fun filter(context: Context, item: Item): IncapableCause? {
            if (!needFiltering(context, item)) return null
            val size = PhotoMetadataUtils.getBitmapBound(context.contentResolver, item.contentUri)
            return if (size.x < mMinWidth || size.y < mMinHeight || item.size > mMaxSize) {
                IncapableCause(
                    IncapableCause.DIALOG,
                    context.getString(
                        R.string.error_gif, mMinWidth,
                        PhotoMetadataUtils.getSizeInMB(mMaxSize.toLong()).toString()
                    )
                )
            } else null
        }
    }
}