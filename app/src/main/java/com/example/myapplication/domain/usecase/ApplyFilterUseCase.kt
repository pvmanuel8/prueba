package com.example.myapplication.domain.usecase

import android.graphics.Bitmap
import com.example.myapplication.data.model.FilterType
import com.example.myapplication.data.model.ImageData
import com.example.myapplication.domain.processor.FilterEngine
import com.example.myapplication.domain.processor.FilterPipelineProgress
import kotlinx.coroutines.flow.Flow

/**
 * Use case para aplicar filtros a imágenes
 */
class ApplyFilterUseCase {

    private val filterEngine = FilterEngine()

    /**
     * Aplica un filtro individual
     */
    suspend operator fun invoke(
        bitmap: Bitmap,
        filter: FilterType
    ): Result<Bitmap> {
        return filterEngine.applyFilter(bitmap, filter)
    }

    /**
     * Aplica un filtro a ImageData
     */
    suspend fun applyToImage(
        imageData: ImageData,
        filter: FilterType
    ): Result<ImageData> {
        return filterEngine.applyFilterToImage(imageData, filter)
    }

    /**
     * Aplica múltiples filtros en secuencia
     */
    suspend fun applyPipeline(
        bitmap: Bitmap,
        filters: List<FilterType>
    ): Flow<FilterPipelineProgress> {
        return filterEngine.applyFilterPipeline(bitmap, filters)
    }

    /**
     * Genera un preview rápido
     */
    suspend fun generatePreview(
        bitmap: Bitmap,
        filter: FilterType
    ): Result<Bitmap> {
        return filterEngine.generatePreview(bitmap, filter)
    }

    /**
     * Cancela el procesamiento actual
     */
    fun cancel() {
        filterEngine.cancelCurrentJob()
    }

    /**
     * Limpia el caché de filtros
     */
    fun clearCache() {
        filterEngine.clearCache()
    }
}