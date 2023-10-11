package com.oguzhanaslann.posiedetection

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.oguzhanaslann.posiedetection.databinding.ActivityPoseDetectImageBinding
import com.oguzhanaslann.posiedetection.ui.PoseGraphic
import com.oguzhanaslann.posiedetection.util.loadImageWithMinSize

class PoseDetectImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPoseDetectImageBinding

    private val poseOptions by lazy {
        AccuratePoseDetectorOptions.Builder()
                .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE)
                .build()
    }

    private val imageId get() = R.drawable.ic_image_person_jump

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPoseDetectImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val poseDetector = PoseDetection.getClient(poseOptions)
        binding.imageView.loadImageWithMinSize(imageId) {
            val bitmap = it.toBitmap()
            val inputImage = getInputImageFrom(bitmap)
            Log.d(TAG, "onCreate: image process started")
            poseDetector.process(inputImage)
                    .addOnSuccessListener { onPoseDetectionSucceeded(it, bitmap) }
                    .addOnFailureListener(::onPoseDetectionFailed)
        }
    }

    private fun getInputImageFrom(bitmap: Bitmap): InputImage {
        val zeroRotationDegrees = 0
        return InputImage.fromBitmap(bitmap, zeroRotationDegrees)
    }

    private fun onPoseDetectionSucceeded(pose: Pose?, bitmap: Bitmap) {
        Log.d(TAG, "onCreate: success pose : $pose")
        val pose = pose ?: return
        val allPose = pose.allPoseLandmarks
        if (allPose.isEmpty()) {
            Log.e(TAG, "onCreate: no pose detected")
            return
        }

        binding.graphicOverlay.setImageSourceInfo(
            /* imageWidth = */ bitmap.width,
            /* imageHeight = */bitmap.height,
            /* isFlipped = */ false
        )

        binding.graphicOverlay.add(
            PoseGraphic(
                overlay = binding.graphicOverlay,
                pose = pose,
                showInFrameLikelihood = false,
                visualizeZ = false,
                rescaleZForVisualization = false,
                poseClassification = listOf()
            )
        )
    }

    private fun onPoseDetectionFailed(e: Exception) {
        Log.e(TAG, "onPoseDetectionFailed: $e")
    }

    companion object {
        private const val TAG = "PoseDetectImageActivity"
    }
}