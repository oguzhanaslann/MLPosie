package com.oguzhanaslann.posiedetection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.oguzhanaslann.posiedetection.databinding.ActivityPoseDetectVideoBinding

class PoseDetectVideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPoseDetectVideoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPoseDetectVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*
            TODO: Implement Pose Detection on Video
            TODO: - first, implement exo player.
            TODO: - then, implement frame listener.
            TODO: - then, implement pose detection.
            TODO: - draw pose on the frame.
         */

        initExoPlayer()
    }

    private fun initExoPlayer() {
        TODO("Not yet implemented")
    }
}