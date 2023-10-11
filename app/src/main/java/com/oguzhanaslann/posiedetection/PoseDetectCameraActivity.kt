package com.oguzhanaslann.posiedetection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.oguzhanaslann.posiedetection.databinding.ActivityPoseDetectCameraBinding

class PoseDetectCameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPoseDetectCameraBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPoseDetectCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}