package com.example.myapplication.domain.processor

import android.graphics.Bitmap
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.example.myapplication.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Calculador de histogramas de color
 */
class HistogramCalculator {

    /**
     * Calcula el histograma RGB de una imagen
     */
    suspend fun calculateHistogram(bitmap: Bitmap): HistogramData =
        withContext(Dispatchers.Default) {
            val width = bitmap.width
            val height = bitmap.height

            val redChannel = IntArray(Constants.HISTOGRAM_BINS)
            val greenChannel = IntArray(Constants.HISTOGRAM_BINS)
            val blueChannel = IntArray(Constants.HISTOGRAM_BINS)

            // Calcular histograma
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = bitmap.getPixel(x, y)

                    redChannel[pixel.red]++
                    greenChannel[pixel.green]++
                    blueChannel[pixel.blue]++
                }
            }

            // Normalizar valores
            val totalPixels = width * height
            val normalizedRed = redChannel.map { it.toFloat() / totalPixels }
            val normalizedGreen = greenChannel.map { it.toFloat() / totalPixels }
            val normalizedBlue = blueChannel.map { it.toFloat() / totalPixels }

            HistogramData(
                red = normalizedRed,
                green = normalizedGreen,
                blue = normalizedBlue,
                luminance = calculateLuminanceHistogram(
                    normalizedRed,
                    normalizedGreen,
                    normalizedBlue
                )
            )
        }

    /**
     * Calcula el histograma de luminancia
     */
    private fun calculateLuminanceHistogram(
        red: List<Float>,
        green: List<Float>,
        blue: List<Float>
    ): List<Float> {
        return red.indices.map { i ->
            // Fórmula de luminancia: 0.299*R + 0.587*G + 0.114*B
            (red[i] * 0.299f + green[i] * 0.587f + blue[i] * 0.114f)
        }
    }

    /**
     * Calcula estadísticas del histograma
     */
    suspend fun calculateStatistics(bitmap: Bitmap): HistogramStatistics =
        withContext(Dispatchers.Default) {
            val width = bitmap.width
            val height = bitmap.height
            val totalPixels = width * height

            var sumRed = 0L
            var sumGreen = 0L
            var sumBlue = 0L

            var minRed = 255
            var maxRed = 0
            var minGreen = 255
            var maxGreen = 0
            var minBlue = 255
            var maxBlue = 0

            // Calcular estadísticas
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = bitmap.getPixel(x, y)

                    val r = pixel.red
                    val g = pixel.green
                    val b = pixel.blue

                    sumRed += r
                    sumGreen += g
                    sumBlue += b

                    minRed = minOf(minRed, r)
                    maxRed = maxOf(maxRed, r)
                    minGreen = minOf(minGreen, g)
                    maxGreen = maxOf(maxGreen, g)
                    minBlue = minOf(minBlue, b)
                    maxBlue = maxOf(maxBlue, b)
                }
            }

            HistogramStatistics(
                meanRed = sumRed.toFloat() / totalPixels,
                meanGreen = sumGreen.toFloat() / totalPixels,
                meanBlue = sumBlue.toFloat() / totalPixels,
                minRed = minRed,
                maxRed = maxRed,
                minGreen = minGreen,
                maxGreen = maxGreen,
                minBlue = minBlue,
                maxBlue = maxBlue
            )
        }
}

/**
 * Datos del histograma RGB
 */
data class HistogramData(
    val red: List<Float>,
    val green: List<Float>,
    val blue: List<Float>,
    val luminance: List<Float>
) {
    val maxValue: Float
        get() = maxOf(
            red.maxOrNull() ?: 0f,
            green.maxOrNull() ?: 0f,
            blue.maxOrNull() ?: 0f
        )
}

/**
 * Estadísticas del histograma
 */
data class HistogramStatistics(
    val meanRed: Float,
    val meanGreen: Float,
    val meanBlue: Float,
    val minRed: Int,
    val maxRed: Int,
    val minGreen: Int,
    val maxGreen: Int,
    val minBlue: Int,
    val maxBlue: Int
) {
    val overallMean: Float
        get() = (meanRed + meanGreen + meanBlue) / 3f
}