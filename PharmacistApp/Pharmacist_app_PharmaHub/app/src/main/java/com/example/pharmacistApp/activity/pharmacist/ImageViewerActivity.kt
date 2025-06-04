package com.example.pharmacistApp.activity.pharmacist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.pharmacistApp.R

class ImageViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_URL = "extra_image_url"

        fun newIntent(context: Context, imageUrl: String): Intent {
            return Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(EXTRA_IMAGE_URL, imageUrl)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer2)

        val imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL) ?: return

        Glide.with(this)
            .load(imageUrl)
            .into(findViewById<ImageView>(R.id.ivFullImage))

        findViewById<View>(R.id.btnClose).setOnClickListener {
            finish()
        }
    }
}