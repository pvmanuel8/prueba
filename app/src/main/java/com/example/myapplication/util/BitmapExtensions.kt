package com.example.myapplication.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Extensiones útiles para trabajar con Bitmaps
 */

/**
 * Redimensiona un Bitmap manteniendo la proporción
 */
fun Bitmap.resize(maxWidth: Int, maxHeight: Int): Bitmap {
    val ratio = min(
        maxWidth.toFloat() / width,
        maxHeight.toFloat() / height
    )

    if (ratio >= 1.0f) return this

    val newWidth = (width * ratio).toInt()
    val newHeight = (height * ratio).toInt()

    return Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
}

/**
 * Rota un Bitmap
 */
fun Bitmap.rotate(degrees: Float): Bitmap {
    if (degrees == 0f) return this

    val matrix = Matrix().apply {
        postRotate(degrees)
    }

    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

/**
 * Voltea un Bitmap horizontal o verticalmente
 */
fun Bitmap.flip(horizontal: Boolean = true): Bitmap {
    val matrix = Matrix().apply {
        if (horizontal) {
            postScale(-1f, 1f, width / 2f, height / 2f)
        } else {
            postScale(1f, -1f, width / 2f, height / 2f)
        }
    }

    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

/**
 * Recorta un Bitmap
 */
fun Bitmap.crop(x: Int, y: Int, cropWidth: Int, cropHeight: Int): Bitmap {
    val safeX = max(0, min(x, width - 1))
    val safeY = max(0, min(y, height - 1))
    val safeWidth = min(cropWidth, width - safeX)
    val safeHeight = min(cropHeight, height - safeY)

    return Bitmap.createBitmap(this, safeX, safeY, safeWidth, safeHeight)
}

/**
 * Convierte Bitmap a ByteArray
 */
fun Bitmap.toByteArray(
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    quality: Int = 90
): ByteArray {
    val stream = ByteArrayOutputStream()
    compress(format, quality, stream)
    return stream.toByteArray()
}

/**
 * Guarda un Bitmap en un archivo
 */
fun Bitmap.saveToFile(
    file: File,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    quality: Int = 90
): Boolean {
    return try {
        FileOutputStream(file).use { out ->
            compress(format, quality, out)
            out.flush()
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

/**
 * Crea una copia mutable del Bitmap
 */
fun Bitmap.mutableCopy(): Bitmap {
    return copy(Bitmap.Config.ARGB_8888, true)
}

/**
 * Obtiene el tamaño en bytes del Bitmap
 */
fun Bitmap.getSizeInBytes(): Long {
    return (byteCount).toLong()
}

/**
 * Carga un Bitmap desde una URI con opciones de redimensionamiento
 */
fun Uri.toBitmap(
    context: Context,
    maxWidth: Int = Constants.PREVIEW_MAX_WIDTH,
    maxHeight: Int = Constants.PREVIEW_MAX_HEIGHT
): Bitmap? {
    return try {
        context.contentResolver.openInputStream(this)?.use { inputStream ->
            // Primera pasada: obtener dimensiones
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)

            // Calcular factor de escala
            val scaleFactor = calculateInSampleSize(options, maxWidth, maxHeight)

            // Segunda pasada: cargar imagen redimensionada
            context.contentResolver.openInputStream(this)?.use { secondStream ->
                BitmapFactory.Options().apply {
                    inSampleSize = scaleFactor
                    inJustDecodeBounds = false
                }.let { finalOptions ->
                    BitmapFactory.decodeStream(secondStream, null, finalOptions)
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Calcula el factor de escala para redimensionar la imagen
 */
private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2

        while ((halfHeight / inSampleSize) >= reqHeight &&
            (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}

/**
 * Obtiene la rotación EXIF de una imagen
 */
fun Uri.getExifRotation(context: Context): Int {
    return try {
        context.contentResolver.openInputStream(this)?.use { inputStream ->
            val exif = ExifInterface(inputStream)
            when (exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    } catch (e: Exception) {
        e.printStackTrace()
        0
    }
}

/**
 * Corrige la orientación de un Bitmap según EXIF
 */
fun Bitmap.correctOrientation(context: Context, uri: Uri): Bitmap {
    val rotation = uri.getExifRotation(context)
    return if (rotation != 0) {
        this.rotate(rotation.toFloat())
    } else {
        this
    }
}