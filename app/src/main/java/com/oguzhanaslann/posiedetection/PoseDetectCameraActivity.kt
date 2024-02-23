package com.oguzhanaslann.posiedetection

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.oguzhanaslann.posiedetection.databinding.ActivityPoseDetectCameraBinding
import com.oguzhanaslann.posiedetection.ui.PoseGraphic
import com.oguzhanaslann.posiedetection.util.classification.PoseClassifierProcessor
import com.oguzhanaslann.posiedetection.util.extractNumericValue
import com.oguzhanaslann.posiedetection.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor

class PoseDetectCameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPoseDetectCameraBinding

    private val cameraPermission = Manifest.permission.CAMERA

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission(), ::onPermissionResult)

    private val poseOptions by lazy {
        AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.CPU_GPU)
            .build()
    }

    private val poseDetector: PoseDetector by lazy {
        PoseDetection.getClient(poseOptions)
    }

    private val cameraSelector get() = CameraSelector.DEFAULT_BACK_CAMERA

    private val poseClassifierProcessor: PoseClassifierProcessor by  lazy {
        PoseClassifierProcessor(this@PoseDetectCameraActivity ,true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPoseDetectCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestCameraPermission()
    }

    private fun requestCameraPermission() {
        permissionLauncher.launch(cameraPermission)
    }

    private fun onPermissionResult(granted: Boolean) {
        if (granted) {
            Log.d(TAG, "onPermissionResult: Permission granted ")
            startCamera()
        } else {
            Log.d(TAG, "onPermissionResult: Permission denied")
            toast("Permission denied")
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val cameraExecutor = ContextCompat.getMainExecutor(this)
        cameraProviderFuture.addListener(cameraListener(cameraProviderFuture, cameraExecutor), cameraExecutor)
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun cameraListener(
        cameraProviderFuture: ListenableFuture<ProcessCameraProvider>, cameraExecutor: Executor
    ): Runnable = Runnable {
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
        val preview = getPreview()
        val imageAnalyzer = poseDetectionImageAnalyzer(cameraExecutor)
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun getPreview(): Preview {
        return Preview.Builder()
            .build()
            .apply { setSurfaceProvider(binding.viewFinder.surfaceProvider) }
    }

    private fun poseDetectionImageAnalyzer(cameraExecutor: Executor): ImageAnalysis {
        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalyzer.setAnalyzer(cameraExecutor) { imageProxy ->
            val bitmap = imageProxy.toBitmap()
            val inputData = InputImage.fromBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)
            //val image = InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
            // java.lang.IllegalArgumentException: Only JPEG and YUV_420_888 are supported now

            poseDetector.process(inputData)
                .addOnSuccessListener { onPoseDetectionSucceeded(it, bitmap) }
                .addOnFailureListener(::onPoseDetectionFailed)
                .addOnCompleteListener { imageProxy.close() }
        }
        return imageAnalyzer
    }

    private fun ImageProxy.toBitmap(): Bitmap {
        val bitmapBuffer: Bitmap =  Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmapBuffer.copyPixelsFromBuffer(planes[0].buffer)
        return bitmapBuffer
    }

    private fun onPoseDetectionSucceeded(pose: Pose?, bitmap: Bitmap) {
        val pose = pose ?: return
        val allPose = pose.allPoseLandmarks
        if (allPose.isEmpty()) {
            Log.e(TAG, "onCreate: no pose detected")
            binding.graphicOverlayCamera.clear()
            return
        }
        //squats_down, squats_up;
        //pushups_down, pushups_up;
        binding.graphicOverlayCamera.setImageSourceInfo(bitmap.width, bitmap.height, false)
        binding.graphicOverlayCamera.clear()
        binding.graphicOverlayCamera.add(PoseGraphic(binding.graphicOverlayCamera, pose))

        lifecycleScope.launch(Dispatchers.Default) {
            val repsResult = poseClassifierProcessor.getPoseResult(pose)
                .also {
                    Log.d("TAG", "onPoseDetectionSucceeded: $it")
                }
                ?.first()
                .orEmpty()

            if (repsResult.contains("squats")) {
                extractNumericValue(repsResult)?.let {
                    withContext(Dispatchers.Main) {
                        binding.squads.text = "Squads: $it"
                    }
                }
            } else if (repsResult.contains("pushups")) {
                extractNumericValue(repsResult)?.let {
                    withContext(Dispatchers.Main) {
                        binding.pushUps.text = "Pushups: $it"
                    }
                }
            }


        }
    }

    private fun onPoseDetectionFailed(e: Exception) {
        Log.e(TAG, "onPoseDetectionFailed: $e")
    }

    companion object {
        private const val TAG = "PoseDetectCameraActivity"
    }
}