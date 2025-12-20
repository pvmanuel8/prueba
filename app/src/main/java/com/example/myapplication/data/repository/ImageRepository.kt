package com.example.myapplication.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.myapplication.data.local.ImageFileManager
import com.example.myapplication.data.model.CompressionQuality
import com.example.myapplication.data.model.ImageData
import com.example.myapplication.data.model.ImageFormat
import com.example.myapplication.util.correctOrientation
import com.example.myapplication.util.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Repository para gestionar imágenes
 */
class ImageRepository(private val context: Context) {

    private val fileManager = ImageFileManager(context)

    // Caché en memoria para imágenes cargadas
    private val imageCache = mutableMapOf<String, ImageData>()

    /**
     * Carga una imagen desde una URI
     */
    suspend fun loadImageFromUri(
        uri: Uri,
        loadFullResolution: Boolean = false
    ): Result<ImageData> = withContext(Dispatchers.IO) {
        try {
            val bitmap = if (loadFullResolution) {
                // Cargar imagen completa
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } else {
                // Cargar versión redimensionada para preview
                uri.toBitmap(context)
            }

            bitmap?.let {
                // Corregir orientación según EXIF
                val correctedBitmap = it.correctOrientation(context, uri)

                val imageData = ImageData(
                    id = UUID.randomUUID().toString(),
                    uri = uri,
                    bitmap = correctedBitmap,
                    name = getFileNameFromUri(uri),
                    width = correctedBitmap.width,
                    height = correctedBitmap.height,
                    sizeBytes = getFileSizeFromUri(uri),
                    format = getImageFormat(uri)
                )

                // Guardar en caché
                imageCache[imageData.id] = imageData

                Result.success(imageData)
            } ?: Result.failure(Exception("No se pudo cargar la imagen"))

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Carga múltiples imágenes desde URIs
     */
    suspend fun loadMultipleImages(uris: List<Uri>): Result<List<ImageData>> =
        withContext(Dispatchers.IO) {
            try {
                val images = uris.mapNotNull { uri ->
                    loadImageFromUri(uri).getOrNull()
                }

                if (images.isNotEmpty()) {
                    Result.success(images)
                } else {
                    Result.failure(Exception("No se pudieron cargar las imágenes"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Guarda una imagen procesada
     */
    suspend fun saveImage(
        imageData: ImageData,
        toGallery: Boolean = true,
        quality: CompressionQuality = CompressionQuality.HIGH
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val bitmap = imageData.bitmap
                ?: return@withContext Result.failure(Exception("Bitmap no disponible"))

            val uri = if (toGallery) {
                fileManager.saveImageToGallery(bitmap, imageData.name, quality)
            } else {
                fileManager.saveImageToInternalStorage(bitmap, imageData.name, quality)
                    ?.let { Uri.fromFile(it) }
            }

            uri?.let {
                // Guardar miniatura
                fileManager.saveThumbnail(bitmap, imageData.id)
                Result.success(it)
            } ?: Result.failure(Exception("Error al guardar imagen"))

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Guarda múltiples imágenes
     */
    suspend fun saveMultipleImages(
        images: List<ImageData>,
        toGallery: Boolean = true,
        quality: CompressionQuality = CompressionQuality.HIGH
    ): Result<List<Uri>> = withContext(Dispatchers.IO) {
        try {
            val savedUris = images.mapNotNull { imageData ->
                saveImage(imageData, toGallery, quality).getOrNull()
            }

            if (savedUris.isNotEmpty()) {
                Result.success(savedUris)
            } else {
                Result.failure(Exception("No se pudieron guardar las imágenes"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene una imagen del caché
     */
    fun getImageFromCache(imageId: String): ImageData? {
        return imageCache[imageId]
    }

    /**
     * Actualiza una imagen en el caché
     */
    fun updateImageInCache(imageData: ImageData) {
        imageCache[imageData.id] = imageData
    }

    /**
     * Elimina una imagen del caché
     */
    fun removeImageFromCache(imageId: String) {
        imageCache.remove(imageId)
    }

    /**
     * Limpia el caché de imágenes
     */
    fun clearCache() {
        imageCache.values.forEach { it.bitmap?.recycle() }
        imageCache.clear()
    }

    /**
     * Lista imágenes guardadas
     */
    suspend fun listSavedImages(): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            val files = fileManager.listSavedImages()
            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina una imagen
     */
    suspend fun deleteImage(file: File): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val deleted = fileManager.deleteImage(file)
            Result.success(deleted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crea un archivo temporal para la cámara
     */
    suspend fun createTempImageFile(): Result<File> = withContext(Dispatchers.IO) {
        try {
            fileManager.createTempImageFile()?.let {
                Result.success(it)
            } ?: Result.failure(Exception("No se pudo crear archivo temporal"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene el nombre del archivo desde una URI
     */
    private fun getFileNameFromUri(uri: Uri): String {
        var fileName = "image_${System.currentTimeMillis()}.jpg"

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }

        return fileName
    }

    /**
     * Obtiene el tamaño del archivo desde una URI
     */
    private fun getFileSizeFromUri(uri: Uri): Long {
        var size = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (sizeIndex != -1 && cursor.moveToFirst()) {
                size = cursor.getLong(sizeIndex)
            }
        }

        return size
    }

    /**
     * Determina el formato de imagen desde una URI
     */
    private fun getImageFormat(uri: Uri): ImageFormat {
        val mimeType = context.contentResolver.getType(uri)
        return when {
            mimeType?.contains("png") == true -> ImageFormat.PNG
            mimeType?.contains("webp") == true -> ImageFormat.WEBP
            else -> ImageFormat.JPEG
        }
    }
}