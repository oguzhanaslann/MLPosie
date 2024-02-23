package com.oguzhanaslann.posiedetection

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.util.Log
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
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
import com.oguzhanaslann.posiedetection.util.classification.PoseClassifierProcessor
import com.oguzhanaslann.posiedetection.util.extractNumericValue
import com.oguzhanaslann.posiedetection.util.getInputImageFrom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private val poseDetector: PoseDetector by lazy {
        PoseDetection.getClient(poseOptions)
    }

    private var job: Job? = null

    private val poseClassifierProcessor: PoseClassifierProcessor by lazy {
        PoseClassifierProcessor(this@PoseDetectVideoActivity ,true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPoseDetectVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setPlayer()
        loadAndPrepareVideo()
        initClearButton()
    }

    private fun setPlayer() {
        binding.playerView.player = exoPlayer
        exoPlayer.addListener(this)
        exoPlayer.setPlaybackSpeed(0.25f)
    }

    private fun loadAndPrepareVideo() {
        val video = getRawResourceUriString(R.raw.vieo_push_up)
        val mediaItem = MediaItem.fromUri(video)
        val assetFileDescriptor = resources.openRawResourceFd(R.raw.vieo_push_up)
        retriever.setDataSource(
            assetFileDescriptor.fileDescriptor,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.length
        )

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    private fun initClearButton() {
        binding.clearButton.setOnClickListener {
            binding.graphicOverlayVideo.clear()
        }
    }

    private fun getRawResourceUriString(@RawRes rawResourceId: Int): String {
        val packageName = packageName
        return "android.resource://$packageName/raw/" + resources.getResourceEntryName(rawResourceId)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        Log.d(TAG, "onIsPlayingChanged: isPlaying : $isPlaying")
        startProcessJobIfNeeded(isPlaying)
        cancelProcessJobIfNeeded(isPlaying)
    }

    private fun startProcessJobIfNeeded(isPlaying: Boolean) {
        if (job == null && isPlaying) {
            job = lifecycleScope.launch {
                while (isActive) {
                    processFrame()
                    delay(50)
                }
            }
        }
    }

    private fun cancelProcessJobIfNeeded(isPlaying: Boolean) {
        if (!isPlaying && job != null) {
            job?.cancel()
            job = null
        }
    }

    private fun processFrame() {
        val frame = getCurrentFrame() ?: return
        val inputImage = getInputImageFrom(frame)
        poseDetector.process(inputImage)
            .addOnSuccessListener { onPoseDetectionSucceeded(it, frame) }
            .addOnFailureListener(::onPoseDetectionFailed)
    }

    private fun getCurrentFrame(): Bitmap? {
        val currentPosMillis = exoPlayer.currentPosition.toDuration(DurationUnit.MILLISECONDS)
        val currentPosMicroSec = currentPosMillis.inWholeMicroseconds
        val currentFrame =  retriever.getFrameAtTime(currentPosMicroSec, MediaMetadataRetriever.OPTION_CLOSEST)
        val bitmap =currentFrame?: return null
        return Bitmap.createScaledBitmap(bitmap, binding.playerView.width, binding.playerView.height, false)
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
    }
}