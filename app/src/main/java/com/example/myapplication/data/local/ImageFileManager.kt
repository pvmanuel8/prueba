package com.example.myapplication.data.local


import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.myapplication.data.model.CompressionQuality
import com.example.myapplication.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gestiona operaciones de archivos de imágenes
 */
class ImageFileManager(private val context: Context) {

    private val appDirectory: File by lazy {
        File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            Constants.IMAGE_DIRECTORY
        ).apply {
            if (!exists()) mkdirs()
        }
    }

    private val thumbnailsDirectory: File by lazy {
        File(appDirectory, Constants.THUMBNAILS_DIRECTORY).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Guarda una imagen en el almacenamiento interno de la app
     */
    suspend fun saveImageToInternalStorage(
        bitmap: Bitmap,
        filename: String? = null,
        quality: CompressionQuality = CompressionQuality.HIGH
    ): File? = withContext(Dispatchers.IO) {
        try {
            val fileName = filename ?: generateFileName("edited")
            val file = File(appDirectory, fileName)

            FileOutputStream(file).use { out ->
                val format = when {
                    fileName.endsWith(".png", ignoreCase = true) ->
                        Bitmap.CompressFormat.PNG

                    else ->
                        Bitmap.CompressFormat.JPEG
                }

                bitmap.compress(format, quality.value, out)
                out.flush()
            }

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Guarda una imagen en la galería del dispositivo
     */
    suspend fun saveImageToGallery(
        bitmap: Bitmap,
        filename: String? = null,
        quality: CompressionQuality = CompressionQuality.HIGH
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val fileName = filename ?: generateFileName("IMG")

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/${Constants.IMAGE_DIRECTORY}"
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            uri?.let { imageUri ->
                resolver.openOutputStream(imageUri)?.use { outputStream ->
                    bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        quality.value,
                        outputStream
                    )
                    outputStream.flush()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                }

                imageUri
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Guarda una miniatura de la imagen
     */
    suspend fun saveThumbnail(
        bitmap: Bitmap,
        imageId: String
    ): File? = withContext(Dispatchers.IO) {
        try {
            val thumbnailFile = File(thumbnailsDirectory, "thumb_$imageId.jpg")

            // Redimensionar a tamaño de miniatura
            val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val thumbWidth: Int
            val thumbHeight: Int

            if (aspectRatio > 1) {
                thumbWidth = Constants.THUMBNAIL_SIZE
                thumbHeight = (Constants.THUMBNAIL_SIZE / aspectRatio).toInt()
            } else {
                thumbHeight = Constants.THUMBNAIL_SIZE
                thumbWidth = (Constants.THUMBNAIL_SIZE * aspectRatio).toInt()
            }

            val thumbnail = Bitmap.createScaledBitmap(
                bitmap,
                thumbWidth,
                thumbHeight,
                true
            )

            FileOutputStream(thumbnailFile).use { out ->
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, out)
                out.flush()
            }

            // Solo reciclar si es una copia diferente
            if (thumbnail != bitmap) {
                thumbnail.recycle()
            }

            thumbnailFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Carga una imagen desde el almacenamiento interno
     */
    suspend fun loadImageFromInternalStorage(fileName: String): File? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(appDirectory, fileName)
                if (file.exists()) file else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    /**
     * Lista todas las imágenes guardadas internamente
     */
    suspend fun listSavedImages(): List<File> = withContext(Dispatchers.IO) {
        try {
            val files = appDirectory.listFiles { file ->
                file.isFile && file.extension.lowercase(Locale.getDefault()) in Constants.SUPPORTED_FORMATS
            }
            files?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Elimina una imagen del almacenamiento interno
     */
    suspend fun deleteImage(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Elimina una miniatura
     */
    suspend fun deleteThumbnail(imageId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val thumbnailFile = File(thumbnailsDirectory, "thumb_$imageId.jpg")
            if (thumbnailFile.exists()) {
                thumbnailFile.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Limpia imágenes antiguas (más de X días)
     */
    suspend fun cleanOldImages(daysOld: Int = 30): Int = withContext(Dispatchers.IO) {
        try {
            val threshold = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
            var deletedCount = 0

            appDirectory.listFiles()?.forEach { file ->
                if (file.isFile && file.lastModified() < threshold) {
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }

            deletedCount
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * Obtiene el tamaño total del directorio de imágenes
     */
    suspend fun getTotalStorageSize(): Long = withContext(Dispatchers.IO) {
        try {
            appDirectory.walkTopDown()
                .filter { it.isFile }
                .map { it.length() }
                .sum()
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    /**
     * Genera un nombre de archivo único
     */
    private fun generateFileName(prefix: String): String {
        val timestamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())
        return "${prefix}_$timestamp.jpg"
    }

    /**
     * Crea un archivo temporal para la cámara
     */
    suspend fun createTempImageFile(): File? = withContext(Dispatchers.IO) {
        try {
            val fileName = generateFileName("TEMP")
            File(context.cacheDir, fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}