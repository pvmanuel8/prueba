package com.example.myapplication.domain.processor

import android.graphics.Bitmap
import android.graphics.Canvas
import com.example.myapplication.data.model.FilterType
import com.example.myapplication.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.min

/**
 * Procesador paralelo que divide imágenes en bloques para procesamiento concurrente
 */
class ParallelProcessor {

    private val imageProcessor = ImageProcessor()

    // Semáforo para limitar la concurrencia
    private val semaphore = Semaphore(Constants.MAX_CONCURRENT_TASKS)

    /**
     * Aplica un filtro procesando la imagen en bloques paralelos
     */
    suspend fun applyFilterParallel(
        bitmap: Bitmap,
        filter: FilterType,
        tileSize: Int = Constants.TILE_SIZE
    ): Flow<ParallelProcessingProgress> = flow {
        emit(ParallelProcessingProgress.Started)

        val tiles = divideBitmapIntoTiles(bitmap, tileSize)
        val totalTiles = tiles.size

        emit(ParallelProcessingProgress.DividedIntoTiles(totalTiles))

        val processedTiles = mutableListOf<ProcessedTile>()
        var processedCount = 0

        // Procesar tiles en paralelo
        coroutineScope {
            val jobs = tiles.map { tile ->
                async(Dispatchers.Default) {
                    semaphore.acquire()
                    try {
                        val processed = processTile(tile, filter)
                        processedCount++

                        val progress = processedCount.toFloat() / totalTiles
                        emit(ParallelProcessingProgress.TileProcessed(
                            processedCount,
                            totalTiles,
                            progress
                        ))

                        processed
                    } finally {
                        semaphore.release()
                    }
                }
            }

            processedTiles.addAll(jobs.awaitAll())
        }

        emit(ParallelProcessingProgress.Reassembling)

        // Reensamblar imagen
        val result = reassembleTiles(processedTiles, bitmap.width, bitmap.height)

        emit(ParallelProcessingProgress.Completed(result))
    }

    /**
     * Divide un Bitmap en tiles
     */
    private fun divideBitmapIntoTiles(
        bitmap: Bitmap,
        tileSize: Int
    ): List<ImageTile> {
        val tiles = mutableListOf<ImageTile>()
        val width = bitmap.width
        val height = bitmap.height

        val numTilesX = ceil(width.toFloat() / tileSize).toInt()
        val numTilesY = ceil(height.toFloat() / tileSize).toInt()

        for (y in 0 until numTilesY) {
            for (x in 0 until numTilesX) {
                val startX = x * tileSize
                val startY = y * tileSize
                val endX = min(startX + tileSize, width)
                val endY = min(startY + tileSize, height)

                val tileWidth = endX - startX
                val tileHeight = endY - startY

                val tileBitmap = Bitmap.createBitmap(
                    bitmap,
                    startX,
                    startY,
                    tileWidth,
                    tileHeight
                )

                tiles.add(
                    ImageTile(
                        bitmap = tileBitmap,
                        x = startX,
                        y = startY,
                        width = tileWidth,
                        height = tileHeight
                    )
                )
            }
        }

        return tiles
    }

    /**
     * Procesa un tile individual
     */
    private suspend fun processTile(
        tile: ImageTile,
        filter: FilterType
    ): ProcessedTile = withContext(Dispatchers.Default) {
        val processedBitmap = imageProcessor.applyFilter(tile.bitmap, filter)

        ProcessedTile(
            bitmap = processedBitmap,
            x = tile.x,
            y = tile.y
        )
    }

    /**
     * Reensambla los tiles procesados en una imagen completa
     */
    private suspend fun reassembleTiles(
        tiles: List<ProcessedTile>,
        width: Int,
        height: Int
    ): Bitmap = withContext(Dispatchers.Default) {
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        tiles.forEach { tile ->
            canvas.drawBitmap(tile.bitmap, tile.x.toFloat(), tile.y.toFloat(), null)
            // Liberar memoria del tile
            tile.bitmap.recycle()
        }

        result
    }

    /**
     * Procesa múltiples imágenes en paralelo
     */
    suspend fun processBatch(
        bitmaps: List<Bitmap>,
        filter: FilterType
    ): Flow<BatchProcessingProgress> = flow {
        if (bitmaps.isEmpty()) {
            emit(BatchProcessingProgress.Completed(emptyList()))
            return@flow
        }

        emit(BatchProcessingProgress.Started(bitmaps.size))

        val results = mutableListOf<Bitmap>()

        coroutineScope {
            bitmaps.forEachIndexed { index, bitmap ->
                emit(BatchProcessingProgress.ProcessingImage(index, bitmaps.size, 0f))

                // Procesar cada imagen con tiles
                applyFilterParallel(bitmap, filter).collect { progress ->
                    when (progress) {
                        is ParallelProcessingProgress.TileProcessed -> {
                            emit(BatchProcessingProgress.ProcessingImage(
                                index,
                                bitmaps.size,
                                progress.progress
                            ))
                        }
                        is ParallelProcessingProgress.Completed -> {
                            results.add(progress.result)
                            emit(BatchProcessingProgress.ImageCompleted(
                                index + 1,
                                bitmaps.size
                            ))
                        }
                        else -> {}
                    }
                }
            }
        }

        emit(BatchProcessingProgress.Completed(results))
    }

    /**
     * Estima el tiempo de procesamiento
     */
    fun estimateProcessingTime(
        bitmap: Bitmap,
        filter: FilterType,
        tileSize: Int = Constants.TILE_SIZE
    ): Long {
        val numTiles = calculateNumberOfTiles(bitmap, tileSize)

        // Estimación basada en el tipo de filtro (en milisegundos por tile)
        val baseTimePerTile = when (filter) {
            is FilterType.Grayscale -> 10L
            is FilterType.Sepia -> 15L
            is FilterType.Negative -> 10L
            is FilterType.Brightness -> 12L
            is FilterType.Contrast -> 15L
            is FilterType.Saturation -> 15L
            is FilterType.Blur -> filter.intensity * 20L
            is FilterType.Sharpen -> 25L
            is FilterType.EdgeDetection -> 30L
            is FilterType.Posterize -> 20L
            is FilterType.Vignette -> 18L
            is FilterType.Rotate -> 5L
            is FilterType.Flip -> 5L
            is FilterType.Crop -> 3L
            is FilterType.Resize -> 8L
        }

        // Tiempo total considerando paralelismo
        val concurrentTasks = Constants.MAX_CONCURRENT_TASKS
        val sequentialBatches = ceil(numTiles.toFloat() / concurrentTasks).toInt()

        return baseTimePerTile * sequentialBatches + 100L // +100ms overhead
    }

    /**
     * Calcula el número de tiles
     */
    private fun calculateNumberOfTiles(bitmap: Bitmap, tileSize: Int): Int {
        val numTilesX = ceil(bitmap.width.toFloat() / tileSize).toInt()
        val numTilesY = ceil(bitmap.height.toFloat() / tileSize).toInt()
        return numTilesX * numTilesY
    }
}

/**
 * Representa un tile de imagen
 */
data class ImageTile(
    val bitmap: Bitmap,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

/**
 * Representa un tile procesado
 */
data class ProcessedTile(
    val bitmap: Bitmap,
    val x: Int,
    val y: Int
)

/**
 * Progreso del procesamiento paralelo
 */
sealed class ParallelProcessingProgress {
    data object Started : ParallelProcessingProgress()
    data class DividedIntoTiles(val totalTiles: Int) : ParallelProcessingProgress()
    data class TileProcessed(
        val processedCount: Int,
        val totalTiles: Int,
        val progress: Float
    ) : ParallelProcessingProgress()
    data object Reassembling : ParallelProcessingProgress()
    data class Completed(val result: Bitmap) : ParallelProcessingProgress()
}

/**
 * Progreso del procesamiento por lotes
 */
sealed class BatchProcessingProgress {
    data class Started(val totalImages: Int) : BatchProcessingProgress()
    data class ProcessingImage(
        val currentImage: Int,
        val totalImages: Int,
        val imageProgress: Float
    ) : BatchProcessingProgress()
    data class ImageCompleted(
        val completedCount: Int,
        val totalImages: Int
    ) : BatchProcessingProgress()
    data class Completed(val results: List<Bitmap>) : BatchProcessingProgress()
}