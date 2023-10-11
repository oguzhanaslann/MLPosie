package com.oguzhanaslann.posiedetection

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.oguzhanaslann.posiedetection.databinding.ActivityMainBinding
import com.oguzhanaslann.posiedetection.util.openActivity

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.image.setOnClickListener {
            openActivity(PoseDetectImageActivity::class.java)
        }

        binding.video.setOnClickListener {
            openActivity(PoseDetectVideoActivity::class.java)
        }

        binding.camera.setOnClickListener {
            openActivity(PoseDetectCameraActivity::class.java)
        }
    }
}