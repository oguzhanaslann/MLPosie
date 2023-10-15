package com.oguzhanaslann.posiedetection

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.util.Log
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.oguzhanaslann.posiedetection.databinding.ActivityPoseDetectVideoBinding
import com.oguzhanaslann.posiedetection.ui.PoseGraphic
import com.oguzhanaslann.posiedetection.util.getInputImageFrom
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@UnstableApi
class PoseDetectVideoActivity : AppCompatActivity(), Player.Listener {

    private lateinit var binding: ActivityPoseDetectVideoBinding

    private val exoPlayer by lazy {
        ExoPlayer.Builder(this)
            .setPauseAtEndOfMediaItems(true)
            .build()
    }

    private val retriever by lazy(::MediaMetadataRetriever)

    private val poseOptions by lazy {
        AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.CPU_GPU)
            .build()
    }

    private lateinit var poseDetector: PoseDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPoseDetectVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        poseDetector = PoseDetection.getClient(poseOptions)
        binding.playerView.player = exoPlayer
        exoPlayer.addListener(this)

        val video = getRawResourceUriString(R.raw.video_samp)
        val mediaItem = MediaItem.fromUri(video)
        val assetFileDescriptor = resources.openRawResourceFd(R.raw.video_samp)
        retriever.setDataSource(
            assetFileDescriptor.fileDescriptor,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.length
        )

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        binding.clearButton.setOnClickListener {
            binding.graphicOverlayVideo.clear()
        }
    }

    private fun getRawResourceUriString(@RawRes rawResourceId: Int): String {
        val packageName = packageName
        return "android.resource://$packageName/raw/" + resources.getResourceEntryName(rawResourceId)
    }

    private var job: Job? = null

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        Log.d(TAG, "onIsPlayingChanged: isPlaying : $isPlaying")

        if (job == null && isPlaying) {
            job = lifecycleScope.launch {
                while (isActive) {
                    processFrame()
                    delay(50)
                }
            }
        }

        if (!isPlaying && job != null) {
            job?.cancel()
            job = null
        }
    }

    private fun processFrame() {
        val bitmap = getCurrentPositionFrame() ?: return // scale bitmap to fit the screen
        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap, binding.playerView.width, binding.playerView.height, false
        )
        val inputImage = getInputImageFrom(scaledBitmap)
        poseDetector.process(inputImage)
            .addOnSuccessListener { onPoseDetectionSucceeded(it, scaledBitmap) }
            .addOnFailureListener(::onPoseDetectionFailed)
    }

    private fun getCurrentPositionFrame(): Bitmap? {
        val currentPosMillis = exoPlayer.currentPosition.toDuration(DurationUnit.MILLISECONDS)
        Log.d(TAG, "getCurrentPositionFrame: currentPosMillis : $currentPosMillis")
        val currentPosMicroSec = currentPosMillis.inWholeMicroseconds
        Log.d(TAG, "getCurrentPositionFrame: currentPosMicroSec : $currentPosMicroSec")
        return retriever.getFrameAtTime(currentPosMicroSec, MediaMetadataRetriever.OPTION_CLOSEST)
    }

    private fun onPoseDetectionSucceeded(pose: Pose?, bitmap: Bitmap) {
        val pose = pose ?: return
        val allPose = pose.allPoseLandmarks
        if (allPose.isEmpty()) {
            Log.e(TAG, "onCreate: no pose detected")
            return
        }

        binding.graphicOverlayVideo.setImageSourceInfo(bitmap.width, bitmap.height, false)
        binding.graphicOverlayVideo.clear()
        binding.graphicOverlayVideo.add(PoseGraphic(binding.graphicOverlayVideo, pose))
    }

    private fun onPoseDetectionFailed(e: Exception) {
        Log.e(TAG, "onPoseDetectionFailed: $e")
    }

    override fun onPause() {
        super.onPause()
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!exoPlayer.isPlaying) {
            exoPlayer.play()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
    }

    companion object {
        private const val TAG = "PoseDetectVideoActivity"
        private const val ONE_MILLIS = 1000L
    }
}