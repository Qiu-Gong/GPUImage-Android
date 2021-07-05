package com.alien.gpuimage.demo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ExoplayerActivity : AppCompatActivity() {

    companion object {
        private const val KEY_PATH = "key_path"

        fun startActivity(context: Context, path: String?) {
            val intent = Intent(context, ExoplayerActivity::class.java)
            intent.putExtra(KEY_PATH, path)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exo_player)
    }
}