package com.example.myapplication.domain.usecase


import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.myapplication.data.local.ImageFileManager
import com.example.myapplication.data.local.ProjectStorage
import com.example.myapplication.data.model.CompressionQuality
import com.example.myapplication.data.model.FilterType
import com.example.myapplication.data.model.ImageData
import com.example.myapplication.data.model.ImageProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Use case para guardar imágenes y proyectos
 */
class SaveImageUseCase(private val context: Context) {

    private val imageFileManager = ImageFileManager(context)
    private val projectStorage = ProjectStorage(context)
    private val json = Json { prettyPrint = true }

    /**
     * Guarda una imagen editada como proyecto
     */
    suspend fun saveAsProject(
        imageData: ImageData,
        appliedFilters: List<FilterType>,
        toGallery: Boolean = false,
        quality: CompressionQuality = CompressionQuality.HIGH
    ): Result<ImageProject> = withContext(Dispatchers.IO) {
        try {
            val bitmap = imageData.bitmap
                ?: return@withContext Result.failure(Exception("Bitmap no disponible"))

            // Generar ID único para el proyecto
            val projectId = UUID.randomUUID().toString()

            // Guardar imagen editada
            val editedFileName = "edited_${projectId}.jpg"
            val editedFile = imageFileManager.saveImageToInternalStorage(
                bitmap,
                editedFileName,
                quality
            ) ?: return@withContext Result.failure(Exception("Error al guardar imagen"))

            // Guardar miniatura
            val thumbnailFile = imageFileManager.saveThumbnail(bitmap, projectId)

            // Opcionalmente guardar en galería
            val galleryUri = if (toGallery) {
                imageFileManager.saveImageToGallery(bitmap, imageData.name, quality)
            } else null

            // Serializar filtros aplicados
            val filtersJson = json.encodeToString(
                appliedFilters.map { filterToString(it) }
            )

            // Crear proyecto
            val project = ImageProject(
                id = projectId,
                name = imageData.name,
                originalImagePath = imageData.uri?.toString() ?: "",
                editedImagePath = editedFile.absolutePath,
                filters = listOf(filtersJson),
                timestamp = System.currentTimeMillis(),
                thumbnailPath = thumbnailFile?.absolutePath
            )

            // Guardar proyecto
            projectStorage.addProject(project)
                .onSuccess {
                    return@withContext Result.success(project)
                }
                .onFailure { error ->
                    return@withContext Result.failure(error)
                }

            Result.success(project)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Guarda solo la imagen sin crear proyecto
     */
    suspend fun saveImageOnly(
        bitmap: Bitmap,
        name: String,
        toGallery: Boolean = true,
        quality: CompressionQuality = CompressionQuality.HIGH
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val uri = if (toGallery) {
                imageFileManager.saveImageToGallery(bitmap, name, quality)
            } else {
                imageFileManager.saveImageToInternalStorage(bitmap, name, quality)
                    ?.let { Uri.fromFile(it) }
            }

            uri?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Error al guardar imagen"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Carga un proyecto guardado
     */
    suspend fun loadProject(projectId: String): Result<LoadedProject> =
        withContext(Dispatchers.IO) {
            try {
                val project = projectStorage.getProject(projectId)
                    .getOrNull()
                    ?: return@withContext Result.failure(Exception("Proyecto no encontrado"))

                // Cargar imagen editada
                val editedFile = File(project.editedImagePath)
                if (!editedFile.exists()) {
                    return@withContext Result.failure(Exception("Imagen no encontrada"))
                }

                val bitmap = android.graphics.BitmapFactory.decodeFile(editedFile.absolutePath)
                    ?: return@withContext Result.failure(Exception("Error al cargar imagen"))

                // Deserializar filtros
                val filters = try {
                    val filtersString = project.filters.firstOrNull() ?: "[]"
                    json.decodeFromString<List<String>>(filtersString)
                        .mapNotNull { stringToFilter(it) }
                } catch (e: Exception) {
                    emptyList()
                }

                Result.success(
                    LoadedProject(
                        project = project,
                        bitmap = bitmap,
                        filters = filters
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Elimina un proyecto
     */
    suspend fun deleteProject(projectId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val project = projectStorage.getProject(projectId).getOrNull()

                // Eliminar archivos
                project?.let {
                    File(it.editedImagePath).delete()
                    it.thumbnailPath?.let { path -> File(path).delete() }
                    imageFileManager.deleteThumbnail(projectId)
                }

                // Eliminar proyecto
                projectStorage.deleteProject(projectId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Lista todos los proyectos
     */
    suspend fun listProjects(): Result<List<ImageProject>> {
        return projectStorage.loadProjects()
    }

    /**
     * Convierte FilterType a String para serialización
     */
    private fun filterToString(filter: FilterType): String {
        return when (filter) {
            is FilterType.Grayscale -> "grayscale"
            is FilterType.Sepia -> "sepia"
            is FilterType.Negative -> "negative"
            is FilterType.Brightness -> "brightness:${filter.value}"
            is FilterType.Contrast -> "contrast:${filter.value}"
            is FilterType.Saturation -> "saturation:${filter.value}"
            is FilterType.Blur -> "blur:${filter.intensity}"
            is FilterType.Sharpen -> "sharpen"
            is FilterType.EdgeDetection -> "edge_detection"
            is FilterType.Posterize -> "posterize:${filter.levels}"
            is FilterType.Vignette -> "vignette:${filter.intensity}"
            is FilterType.Rotate -> "rotate:${filter.degrees}"
            is FilterType.Flip -> "flip:${filter.horizontal}"
            is FilterType.Crop -> "crop:${filter.rect.left},${filter.rect.top},${filter.rect.right},${filter.rect.bottom}"
            is FilterType.Resize -> "resize:${filter.scale}"
        }
    }

    /**
     * Convierte String a FilterType para deserialización
     */
    private fun stringToFilter(str: String): FilterType? {
        return try {
            val parts = str.split(":")
            when (parts[0]) {
                "grayscale" -> FilterType.Grayscale
                "sepia" -> FilterType.Sepia
                "negative" -> FilterType.Negative
                "brightness" -> FilterType.Brightness(parts[1].toFloat())
                "contrast" -> FilterType.Contrast(parts[1].toFloat())
                "saturation" -> FilterType.Saturation(parts[1].toFloat())
                "blur" -> FilterType.Blur(parts[1].toInt())
                "sharpen" -> FilterType.Sharpen
                "edge_detection" -> FilterType.EdgeDetection
                "posterize" -> FilterType.Posterize(parts[1].toInt())
                "vignette" -> FilterType.Vignette(parts[1].toFloat())
                "rotate" -> FilterType.Rotate(parts[1].toInt())
                "flip" -> FilterType.Flip(parts[1].toBoolean())
                "resize" -> FilterType.Resize(parts[1].toFloat())
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Proyecto cargado con bitmap
 */
data class LoadedProject(
    val project: ImageProject,
    val bitmap: Bitmap,
    val filters: List<FilterType>
)