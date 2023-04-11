package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.ActivityCaptureResultBinding
import com.example.myapplication.databinding.ActivityMainBinding

class CaptureResult : AppCompatActivity() {

    private lateinit var viewBinding: ActivityCaptureResultBinding
    private var imageView: ImageView?=null
    private var galleryButton:Button?=null
    private var backButton:Button?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCaptureResultBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val imageUri = intent.getStringExtra("imageUri")

        Glide.with(this)
            .load(imageUri)
            .into(viewBinding.captureResultImage)

        viewBinding.captureResultBtnGallery.setOnClickListener {
            // TODO: Navigate to the gallery
        }

        viewBinding.captureResultBtnProceed.setOnClickListener {
            finish() // Go back to the previous activity
        }
    }
}