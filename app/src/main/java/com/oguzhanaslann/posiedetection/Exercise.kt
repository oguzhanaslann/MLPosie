package com.oguzhanaslann.posiedetection

import android.widget.TextView
import com.google.mlkit.vision.pose.Pose
import com.oguzhanaslann.posiedetection.util.classification.PoseClassifierProcessor
import com.oguzhanaslann.posiedetection.util.extractNumericValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class Exercise(val reps : Int) {
    class Squats(reps: Int) : Exercise(reps)
    class PushUps(reps: Int) : Exercise(reps)

    fun displayLabel() = when (this) {
        is Squats -> "Squats: $reps"
        is PushUps -> "Pushups: $reps"
    }
}

suspend fun PoseClassifierProcessor.getExerciseResult(pose: Pose): Exercise? {
    val repsResult = withContext(Dispatchers.Default) {
        getPoseResult(pose)
            ?.first()
            .orEmpty()
    }

    return when {
        repsResult.contains("squats") -> {
            extractNumericValue(repsResult)?.let { Exercise.Squats(it) }
        }
        repsResult.contains("pushups") -> {
            extractNumericValue(repsResult)?.let { Exercise.PushUps(it) }
        }
        else -> null
    }
}

fun setExerciseStatistics(
    exercise: Exercise?,
    pushUpTextView: TextView,
    squatTextView: TextView
) {
    exercise?.let {
        when (it) {
            is Exercise.Squats -> { squatTextView.text = it.displayLabel() }
            is Exercise.PushUps -> { pushUpTextView.text = it.displayLabel() }
        }
    }
}