package com.example.myapplication.domain.processor

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.example.myapplication.data.model.FilterType
import com.example.myapplication.util.mutableCopy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Procesador de imágenes que aplica filtros
 */
class ImageProcessor {

    /**
     * Aplica un filtro a un Bitmap
     */
    suspend fun applyFilter(
        bitmap: Bitmap,
        filter: FilterType
    ): Bitmap = withContext(Dispatchers.Default) {
        // IMPORTANTE: Crear una copia para no modificar el original
        val workingBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val result = when (filter) {
            is FilterType.Grayscale -> applyGrayscale(workingBitmap)
            is FilterType.Sepia -> applySepia(workingBitmap)
            is FilterType.Negative -> applyNegative(workingBitmap)
            is FilterType.Brightness -> applyBrightness(workingBitmap, filter.value)
            is FilterType.Contrast -> applyContrast(workingBitmap, filter.value)
            is FilterType.Saturation -> applySaturation(workingBitmap, filter.value)
            is FilterType.Blur -> applyBlur(workingBitmap, filter.intensity)
            is FilterType.Sharpen -> applySharpen(workingBitmap)
            is FilterType.EdgeDetection -> applyEdgeDetection(workingBitmap)
            is FilterType.Posterize -> applyPosterize(workingBitmap, filter.levels)
            is FilterType.Vignette -> applyVignette(workingBitmap, filter.intensity)
            is FilterType.Rotate -> applyRotate(workingBitmap, filter.degrees)
            is FilterType.Flip -> applyFlip(workingBitmap, filter.horizontal)
            is FilterType.Crop -> applyCrop(workingBitmap, filter.rect)
            is FilterType.Resize -> applyResize(workingBitmap, filter.scale)
        }

        // Liberar la copia de trabajo si es diferente del resultado
        if (workingBitmap != result && workingBitmap != bitmap) {
            workingBitmap.recycle()
        }

        result
    }

    // ==================== FILTROS BÁSICOS ====================

    /**
     * Convierte la imagen a escala de grises
     */
    private suspend fun applyGrayscale(bitmap: Bitmap): Bitmap =
        withContext(Dispatchers.Default) {
            val result = bitmap.mutableCopy()
            val width = bitmap.width
            val height = bitmap.height

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = bitmap.getPixel(x, y)

                    val r = pixel.red
                    val g = pixel.green
                    val b = pixel.blue

                    // Fórmula de luminancia: 0.299*R + 0.587*G + 0.114*B
                    val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

                    val newPixel = Color.rgb(gray, gray, gray)
                    result.setPixel(x, y, newPixel)
                }
            }

            result
        }

    /**
     * Aplica efecto sepia
     */
    private suspend fun applySepia(bitmap: Bitmap): Bitmap =
        withContext(Dispatchers.Default) {
            val result = bitmap.mutableCopy()
            val width = bitmap.width
            val height = bitmap.height

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = bitmap.getPixel(x, y)

                    val r = pixel.red
                    val g = pixel.green
                    val b = pixel.blue

                    val tr = (0.393 * r + 0.769 * g + 0.189 * b).toInt().coerceIn(0, 255)
                    val tg = (0.349 * r + 0.686 * g + 0.168 * b).toInt().coerceIn(0, 255)
                    val tb = (0.272 * r + 0.534 * g + 0.131 * b).toInt().coerceIn(0, 255)

                    val newPixel = Color.rgb(tr, tg, tb)
                    result.setPixel(x, y, newPixel)
                }
            }

            result
        }

    /**
     * Invierte los colores (negativo)
     */
    private suspend fun applyNegative(bitmap: Bitmap): Bitmap =
        withContext(Dispatchers.Default) {
            val result = bitmap.mutableCopy()
            val width = bitmap.width
            val height = bitmap.height

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = bitmap.getPixel(x, y)

                    val r = 255 - pixel.red
                    val g = 255 - pixel.green
                    val b = 255 - pixel.blue

                    val newPixel = Color.rgb(r, g, b)
                    result.setPixel(x, y, newPixel)
                }
            }

            result
        }

    /**
     * Ajusta el brillo de la imagen
     * @param value: -100 a +100
     */
    private suspend fun applyBrightness(bitmap: Bitmap, value: Float): Bitmap =
        withContext(Dispatchers.Default) {
            val result = bitmap.mutableCopy()
            val width = bitmap.width
            val height = bitmap.height

            // Normalizar valor a rango -255 a +255
            val adjustment = (value * 2.55f).toInt()

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = bitmap.getPixel(x, y)

                    val r = (pixel.red + adjustment).coerceIn(0, 255)
                    val g = (pixel.green + adjustment).coerceIn(0, 255)
                    val b = (pixel.blue + adjustment).coerceIn(0, 255)

                    val newPixel = Color.rgb(r, g, b)
                    result.setPixel(x, y, newPixel)
                }
            }

            result
        }

    /**
     * Ajusta el contraste de la imagen
     * @param value: -100 a +100
     */
    private suspend fun applyContrast(bitmap: Bitmap, value: Float): Bitmap =
        withContext(Dispatchers.Default) {
            val result = bitmap.mutableCopy()
            val width = bitmap.width
            val height = bitmap.height

            // Factor de contraste: valor positivo aumenta, negativo disminuye
            val factor = (259f * (value + 255f)) / (255f * (259f - value))

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = bitmap.getPixel(x, y)

                    val r = (factor * (pixel.red - 128) + 128).toInt().coerceIn(0, 255)
                    val g = (factor * (pixel.green - 128) + 128).toInt().coerceIn(0, 255)
                    val b = (factor * (pixel.blue - 128) + 128).toInt().coerceIn(0, 255)

                    val newPixel = Color.rgb(r, g, b)
                    result.setPixel(x, y, newPixel)
                }
            }

            result
        }

    /**
     * Ajusta la saturación de la imagen
     * @param value: -100 a +100
     */
    private suspend fun applySaturation(bitmap: Bitmap, value: Float): Bitmap =
        withContext(Dispatchers.Default) {
            val result = bitmap.mutableCopy()
            val width = bitmap.width
            val height = bitmap.height

            // Factor de saturación: 0 = grayscale, 1 = normal, >1 = más saturado
            val saturationFactor = 1 + (value / 100f)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = bitmap.getPixel(x, y)

                    val r = pixel.red
                    val g = pixel.green
                    val b = pixel.blue

                    // Calcular luminancia
                    val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

                    // Interpolar entre gris y color original
                    val newR = (gray + saturationFactor * (r - gray)).toInt().coerceIn(0, 255)
                    val newG = (gray + saturationFactor * (g - gray)).toInt().coerceIn(0, 255)
                    val newB = (gray + saturationFactor * (b - gray)).toInt().coerceIn(0, 255)

                    val newPixel = Color.rgb(newR, newG, newB)
                    result.setPixel(x, y, newPixel)
                }
            }

            result
        }

    // ==================== FILTROS AVANZADOS ====================

    /**
     * Aplica desenfoque (blur) con Box Blur
     */
    private suspend fun applyBlur(bitmap: Bitmap, intensity: Int): Bitmap =
        withContext(Dispatchers.Default) {
            val result = bitmap.mutableCopy()
            val width = bitmap.width
            val height = bitmap.height
            val radius = intensity.coerceIn(1, 25)

            // Blur horizontal
            val temp = bitmap.mutableCopy()
            for (y in 0 until height) {
                for (x in 0 until width) {
                    var r = 0
                    var g = 0
                    var b = 0
                    var count = 0

                    for (i in -radius..radius) {
                        val nx = (x + i).coerceIn(0, width - 1)
                        val pixel = bitmap.getPixel(nx, y)
                        r += pixel.red
                        g += pixel.green
                        b += pixel.blue
                        count++
                    }

                    val newPixel = Color.rgb(r / count, g / count, b / count)
                    temp.setPixel(x, y, newPixel)
                }
            }

            // Blur vertical
            for (y in 0 until height) {
                for (x in 0 until width) {
                    var r = 0
                    var g = 0
                    var b = 0
                    var count = 0

                    for (i in -radius..radius) {
                        val ny = (y + i).coerceIn(0, height - 1)
                        val pixel = temp.getPixel(x, ny)
                        r += pixel.red
                        g += pixel.green
                        b += pixel.blue
                        count++
                    }

                    val newPixel = Color.rgb(r / count, g / count, b / count)
                    result.setPixel(x, y, newPixel)
                }
            }

            result
        }

    /**
     * Aplica enfoque (sharpen) usando kernel de convolución
     */
    private suspend fun applySharpen(bitmap: Bitmap): Bitmap =
        withContext(Dispatchers.Default) {
            val result = bitmap.mutableCopy()
            val width = bitmap.width
            val height = bitmap.height

            // Kernel de enfoque
            val kernel = arrayOf(
                intArrayOf(0, -1, 0),
                intArrayOf(-1, 5, -1),
                intArrayOf(0, -1, 0)
            )

            for (y in 1 until height - 1) {
                for (x in 1 until width - 1) {
                    var r = 0
                    var g = 0
                    var b = 0

                    for (ky in 0..2) {
                        for (kx in 0..2) {
                            val pixel = bitmap.getPixel(x + kx - 1, y + ky - 1)
                            val weight = kernel[ky][kx]

                            r += pixel.red * weight
                            g += pixel.green * weight
                            b += pixel.blue * weight
                        }
                    }

                    val newPixel = Color.rgb(
                        r.coerceIn(0, 255),
                        g.coerceIn(0, 255),
                        b.coerceIn(0, 255)
                    )
                    result.setPixel(x, y, newPixel)
                }
            }

            result
        }

    /**
     * Detecta bordes usando operador Sobel
     */
    private suspend fun applyEdgeDetection(bitmap: Bitmap): Bitmap =
        withContext(Dispatchers.Default) {
            val result = bitmap.mutableCopy()
            val width = bitmap.width
            val height = bitmap.height

            // Kernels Sobel
            val sobelX = arrayOf(
                intArrayOf(-1, 0, 1),
                intArrayOf(-2, 0, 2),
                intArrayOf(-1, 0, 1)
            )

            val sobelY = arrayOf(
                intArrayOf(-1, -2, -1),
                intArrayOf(0, 0, 0),
                intArrayOf(1, 2, 1)
            )

            for (y in 1 until height - 1) {
                for (x in 1 until width - 1) {
                    var gx = 0
                    var gy = 0

                    for (ky in 0..2) {
                        for (kx in 0..2) {
                            val pixel = bitmap.getPixel(x + kx - 1, y + ky - 1)
                            val gray = (0.299 * pixel.red + 0.587 * pixel.green + 0.114 * pixel.blue).toInt()

                            gx += gray * sobelX[ky][kx]
                            gy += gray * sobelY[ky][kx]
                        }
                    }

                    val magnitude = sqrt((gx * gx + gy * gy).toDouble()).toInt().coerceIn(0, 255)
                    val newPixel = Color.rgb(magnitude, magnitude, magnitude)
                    result.setPixel(x, y, newPixel)
                }
            }

            result
        }

    /**
     * Posteriza la imagen (reduce número de colores)
     */
    private suspend fun applyPosterize(bitmap: Bitmap, levels: Int): Bitmap =
        withContext(Dispatchers.Default) {
            val result = bitmap.mutableCopy()
            val width = bitmap.width
            val height = bitmap.height
            val step = 255 / (levels - 1)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = bitmap.getPixel(x, y)

                    val r = ((pixel.red / step) * step).coerceIn(0, 255)
                    val g = ((pixel.green / step) * step).coerceIn(0, 255)
                    val b = ((pixel.blue / step) * step).coerceIn(0, 255)

                    val newPixel = Color.rgb(r, g, b)
                    result.setPixel(x, y, newPixel)
                }
            }

            result
        }

    /**
     * Aplica efecto viñeta (oscurece esquinas)
     */
    private suspend fun applyVignette(bitmap: Bitmap, intensity: Float): Bitmap =
        withContext(Dispatchers.Default) {
            val result = bitmap.mutableCopy()
            val width = bitmap.width
            val height = bitmap.height
            val centerX = width / 2f
            val centerY = height / 2f
            val maxRadius = sqrt((centerX * centerX + centerY * centerY).toDouble()).toFloat()

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = bitmap.getPixel(x, y)

                    // Calcular distancia desde el centro
                    val dx = x - centerX
                    val dy = y - centerY
                    val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                    // Calcular factor de oscurecimiento
                    val vignetteFactor = 1 - (distance / maxRadius * intensity).coerceIn(0f, 1f)

                    val r = (pixel.red * vignetteFactor).toInt().coerceIn(0, 255)
                    val g = (pixel.green * vignetteFactor).toInt().coerceIn(0, 255)
                    val b = (pixel.blue * vignetteFactor).toInt().coerceIn(0, 255)

                    val newPixel = Color.rgb(r, g, b)
                    result.setPixel(x, y, newPixel)
                }
            }

            result
        }

    // ==================== TRANSFORMACIONES ====================

    /**
     * Rota la imagen
     */
    private fun applyRotate(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Voltea la imagen
     */
    private fun applyFlip(bitmap: Bitmap, horizontal: Boolean): Bitmap {
        val matrix = android.graphics.Matrix()
        if (horizontal) {
            matrix.preScale(-1f, 1f)
        } else {
            matrix.preScale(1f, -1f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Recorta la imagen
     */
    private fun applyCrop(bitmap: Bitmap, rect: com.example.myapplication.data.model.CropRect): Bitmap {
        val safeLeft = max(0, min(rect.left, bitmap.width - 1))
        val safeTop = max(0, min(rect.top, bitmap.height - 1))
        val safeWidth = min(rect.width, bitmap.width - safeLeft)
        val safeHeight = min(rect.height, bitmap.height - safeTop)

        return Bitmap.createBitmap(bitmap, safeLeft, safeTop, safeWidth, safeHeight)
    }

    /**
     * Redimensiona la imagen
     */
    private fun applyResize(bitmap: Bitmap, scale: Float): Bitmap {
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}