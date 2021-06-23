package com.alien.gpuimage.demo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.leon.lfilepickerlibrary.LFilePicker
import java.util.*

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
//            VideoActivity.startActivity(this, "/sdcard/DCIM/Camera/video_20210519_173718.mp4")
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
        LFilePicker().withActivity(this)
            .withRequestCode(FILE_REQUEST_CODE)
            .withStartPath("/storage/emulated/0/DCIM/")
            .withMutilyMode(false)
            .withFileFilter(
                if (type == R.id.select_video) arrayOf(".mp4")
                else arrayOf(".png", ".jpeg", ".JPG")
            )
            .start()
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
                val path = data?.getStringArrayListExtra("paths")?.get(0)
                if (type == R.id.select_picture) {
                    ImageActivity.startActivity(this, path)
                } else if (type == R.id.select_surface_view) {
                    SurfaceViewActivity.startActivity(this, path)
                } else if (type == R.id.select_texture_view) {
                    TextureViewActivity.startActivity(this, path)
                } else if (type == R.id.select_video) {
                    VideoActivity.startActivity(this, path)
                }
            }
        }
    }
}