package com.oguzhanaslann.posiedetection

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.oguzhanaslann.posiedetection.databinding.ActivityPoseDetectCameraBinding
import com.oguzhanaslann.posiedetection.databinding.ActivityPoseDetectVideoBinding

class PoseDetectVideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPoseDetectVideoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPoseDetectVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}