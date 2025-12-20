package com.example.myapplication.domain.usecase


import android.graphics.Bitmap
import com.example.myapplication.data.model.FilterType
import com.example.myapplication.data.model.ImageData
import com.example.myapplication.domain.processor.BatchProcessingProgress
import com.example.myapplication.domain.processor.ParallelProcessingProgress
import com.example.myapplication.domain.processor.ParallelProcessor
import kotlinx.coroutines.flow.Flow

/**
 * Use case para procesamiento por lotes de imágenes
 */
class ProcessBatchUseCase {

    private val parallelProcessor = ParallelProcessor()

    /**
     * Procesa una imagen usando procesamiento paralelo
     */
    suspend fun processImageParallel(
        bitmap: Bitmap,
        filter: FilterType
    ): Flow<ParallelProcessingProgress> {
        return parallelProcessor.applyFilterParallel(bitmap, filter)
    }

    /**
     * Procesa múltiples imágenes aplicando el mismo filtro
     */
    suspend fun processBatch(
        bitmaps: List<Bitmap>,
        filter: FilterType
    ): Flow<BatchProcessingProgress> {
        return parallelProcessor.processBatch(bitmaps, filter)
    }

    /**
     * Procesa múltiples imágenes (ImageData) aplicando el mismo filtro
     */
    fun processBatchImages(
        images: List<ImageData>,
        filter: FilterType
    ): Flow<BatchImageProcessingProgress> = kotlinx.coroutines.flow.flow {
        val bitmaps = images.mapNotNull { it.bitmap }

        if (bitmaps.isEmpty()) {
            emit(BatchImageProcessingProgress.Error("No hay imágenes válidas para procesar"))
            return@flow
        }

        emit(BatchImageProcessingProgress.Started(images.size))

        val results = mutableListOf<ImageData>()

        parallelProcessor.processBatch(bitmaps, filter).collect { progress ->
            when (progress) {
                is BatchProcessingProgress.Started -> {
                    emit(BatchImageProcessingProgress.Started(progress.totalImages))
                }
                is BatchProcessingProgress.ProcessingImage -> {
                    emit(BatchImageProcessingProgress.ProcessingImage(
                        currentIndex = progress.currentImage,
                        totalImages = progress.totalImages,
                        imageProgress = progress.imageProgress,
                        imageName = images.getOrNull(progress.currentImage)?.name ?: ""
                    ))
                }
                is BatchProcessingProgress.ImageCompleted -> {
                    emit(BatchImageProcessingProgress.ImageCompleted(
                        completedCount = progress.completedCount,
                        totalImages = progress.totalImages
                    ))
                }
                is BatchProcessingProgress.Completed -> {
                    // Crear ImageData con resultados
                    progress.results.forEachIndexed { index, bitmap ->
                        val originalImage = images.getOrNull(index)
                        if (originalImage != null) {
                            results.add(
                                originalImage.copy(
                                    bitmap = bitmap,
                                    appliedFilters = originalImage.appliedFilters + filter
                                )
                            )
                        }
                    }
                    emit(BatchImageProcessingProgress.Completed(results))
                }
            }
        }
    }

    /**
     * Procesa múltiples imágenes con diferentes filtros para cada una
     */
    fun processBatchWithDifferentFilters(
        imagesWithFilters: List<Pair<Bitmap, FilterType>>
    ): Flow<BatchProcessingProgress> = kotlinx.coroutines.flow.flow {
        emit(BatchProcessingProgress.Started(imagesWithFilters.size))

        val results = mutableListOf<Bitmap>()

        imagesWithFilters.forEachIndexed { index, (bitmap, filter) ->
            emit(BatchProcessingProgress.ProcessingImage(index, imagesWithFilters.size, 0f))

            parallelProcessor.applyFilterParallel(bitmap, filter).collect { progress ->
                when (progress) {
                    is ParallelProcessingProgress.TileProcessed -> {
                        emit(BatchProcessingProgress.ProcessingImage(
                            index,
                            imagesWithFilters.size,
                            progress.progress
                        ))
                    }
                    is ParallelProcessingProgress.Completed -> {
                        results.add(progress.result)
                        emit(BatchProcessingProgress.ImageCompleted(
                            index + 1,
                            imagesWithFilters.size
                        ))
                    }
                    else -> {}
                }
            }
        }

        emit(BatchProcessingProgress.Completed(results))
    }

    /**
     * Estima el tiempo total de procesamiento
     */
    fun estimateBatchTime(
        images: List<ImageData>,
        filter: FilterType
    ): Long {
        return images.sumOf { image ->
            image.bitmap?.let { bitmap ->
                parallelProcessor.estimateProcessingTime(bitmap, filter)
            } ?: 0L
        }
    }
}

/**
 * Progreso del procesamiento por lotes de ImageData
 */
sealed class BatchImageProcessingProgress {
    data class Started(val totalImages: Int) : BatchImageProcessingProgress()
    data class ProcessingImage(
        val currentIndex: Int,
        val totalImages: Int,
        val imageProgress: Float,
        val imageName: String
    ) : BatchImageProcessingProgress()
    data class ImageCompleted(
        val completedCount: Int,
        val totalImages: Int
    ) : BatchImageProcessingProgress()
    data class Completed(val results: List<ImageData>) : BatchImageProcessingProgress()
    data class Error(val message: String) : BatchImageProcessingProgress()
}