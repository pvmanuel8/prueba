package com.example.myapplication.domain.processor

import android.graphics.Bitmap
import android.util.LruCache
import com.example.myapplication.data.model.FilterType
import com.example.myapplication.data.model.ImageData
import com.example.myapplication.util.Constants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Motor de filtros con caché y gestión de trabajos
 */
class FilterEngine {

    private val imageProcessor = ImageProcessor()

    // Caché LRU para resultados de filtros
    private val filterCache = object : LruCache<String, Bitmap>(Constants.MAX_CACHE_ENTRIES) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024 // Tamaño en KB
        }

        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: Bitmap,
            newValue: Bitmap?
        ) {
            // CORRECCIÓN: No reciclar manualmente.
            // Si la UI todavía está usando 'oldValue', llamar a recycle() causará un crash.
            // El Garbage Collector liberará la memoria cuando nadie la esté usando.

            /* if (evicted && oldValue != newValue) {
                oldValue.recycle()
            }
            */
        }
    }

    // Job actual para cancelación
    private var currentJob: Job? = null

    /**
     * Aplica un filtro con caché
     */
    suspend fun applyFilter(
        bitmap: Bitmap,
        filter: FilterType
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        try {
            val cacheKey = generateCacheKey(bitmap, filter)

            // Buscar en caché
            filterCache.get(cacheKey)?.let {
                // Es seguro devolver la misma instancia porque no la reciclamos manualmente
                return@withContext Result.success(it)
            }

            // Aplicar filtro
            val result = imageProcessor.applyFilter(bitmap, filter)

            // Guardar en caché
            filterCache.put(cacheKey, result)

            Result.success(result)
        } catch (e: CancellationException) {
            throw e // Re-lanzar cancelación
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Aplica múltiples filtros en secuencia (pipeline)
     */
    suspend fun applyFilterPipeline(
        bitmap: Bitmap,
        filters: List<FilterType>
    ): Flow<FilterPipelineProgress> = flow {
        if (filters.isEmpty()) {
            emit(FilterPipelineProgress.Completed(bitmap))
            return@flow
        }

        emit(FilterPipelineProgress.Started(filters.size))

        var currentBitmap = bitmap

        filters.forEachIndexed { index, filter ->
            emit(FilterPipelineProgress.Processing(index, filters.size, filter))

            val result = applyFilter(currentBitmap, filter)

            result.onSuccess { processed ->
                currentBitmap = processed
                emit(FilterPipelineProgress.StepCompleted(
                    step = index + 1,
                    total = filters.size,
                    filter = filter,
                    result = processed
                ))
            }.onFailure { error ->
                emit(FilterPipelineProgress.Error(error.message ?: "Error aplicando filtro"))
                return@flow
            }
        }

        emit(FilterPipelineProgress.Completed(currentBitmap))
    }

    /**
     * Aplica un filtro a una ImageData completa
     */
    suspend fun applyFilterToImage(
        imageData: ImageData,
        filter: FilterType
    ): Result<ImageData> = withContext(Dispatchers.Default) {
        try {
            val bitmap = imageData.bitmap
                ?: return@withContext Result.failure(Exception("Bitmap no disponible"))

            val result = applyFilter(bitmap, filter)

            result.map { processedBitmap ->
                imageData.copy(
                    bitmap = processedBitmap,
                    appliedFilters = imageData.appliedFilters + filter
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Preview rápido de un filtro (versión reducida)
     */
    suspend fun generatePreview(
        bitmap: Bitmap,
        filter: FilterType,
        maxSize: Int = 512
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        try {
            // Redimensionar para preview más rápido
            val previewBitmap = if (bitmap.width > maxSize || bitmap.height > maxSize) {
                val scale = minOf(
                    maxSize.toFloat() / bitmap.width,
                    maxSize.toFloat() / bitmap.height
                )
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt(),
                    (bitmap.height * scale).toInt(),
                    true
                )
            } else {
                bitmap
            }

            applyFilter(previewBitmap, filter)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cancela el trabajo actual
     */
    fun cancelCurrentJob() {
        currentJob?.cancel()
        currentJob = null
    }

    /**
     * Limpia el caché
     */
    fun clearCache() {
        // Al limpiar, tampoco reciclamos manualmente por seguridad
        filterCache.evictAll()
    }

    /**
     * Obtiene el tamaño actual del caché
     */
    fun getCacheSize(): Int {
        return filterCache.size()
    }

    /**
     * Genera una clave única para el caché
     */
    private fun generateCacheKey(bitmap: Bitmap, filter: FilterType): String {
        val bitmapHash = bitmap.hashCode()
        val filterString = filter.toString()
        val combined = "$bitmapHash-$filterString"

        return try {
            val digest = MessageDigest.getInstance("MD5")
            val hash = digest.digest(combined.toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            combined
        }
    }
}

/**
 * Progreso del pipeline de filtros
 */
sealed class FilterPipelineProgress {
    data class Started(val totalSteps: Int) : FilterPipelineProgress()
    data class Processing(
        val currentStep: Int,
        val totalSteps: Int,
        val filter: FilterType
    ) : FilterPipelineProgress()
    data class StepCompleted(
        val step: Int,
        val total: Int,
        val filter: FilterType,
        val result: Bitmap
    ) : FilterPipelineProgress()
    data class Completed(val result: Bitmap) : FilterPipelineProgress()
    data class Error(val message: String) : FilterPipelineProgress()
}