package com.example.myapplication.data.model


/**
 * Estado del procesamiento de imágenes
 */
sealed class ProcessingState {
    data object Idle : ProcessingState()
    data class Processing(val progress: Float) : ProcessingState()
    data class Success(val result: ImageData) : ProcessingState()
    data class Error(val message: String) : ProcessingState()
    data object Cancelled : ProcessingState()
}

/**
 * Progreso de procesamiento de una imagen individual
 */
data class ImageProgress(
    val imageId: String,
    val imageName: String,
    val progress: Float, // 0.0 a 1.0
    val state: ProcessingState,
    val estimatedTimeMs: Long = 0
)

/**
 * Progreso global del procesamiento por lotes
 */
data class BatchProgress(
    val totalImages: Int,
    val processedImages: Int,
    val imageProgresses: List<ImageProgress>,
    val overallProgress: Float, // 0.0 a 1.0
    val startTimeMs: Long,
    val estimatedTimeRemainingMs: Long
) {
    val completedImages: Int
        get() = imageProgresses.count { it.state is ProcessingState.Success }

    val failedImages: Int
        get() = imageProgresses.count { it.state is ProcessingState.Error }

    val isComplete: Boolean
        get() = processedImages == totalImages
}