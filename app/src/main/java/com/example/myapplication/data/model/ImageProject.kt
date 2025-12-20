package com.example.myapplication.data.model

import kotlinx.serialization.Serializable

/**
 * Representa un proyecto de edición guardado
 */
@Serializable
data class ImageProject(
    val id: String,
    val name: String,
    val originalImagePath: String,
    val editedImagePath: String? = null,
    val filters: List<String>, // JSON serializado de FilterType
    val timestamp: Long,
    val thumbnailPath: String? = null
)

/**
 * Configuración de calidad para guardar imágenes
 */
enum class CompressionQuality(val value: Int, val displayName: String) {
    LOW(60, "Baja"),
    MEDIUM(80, "Media"),
    HIGH(95, "Alta"),
    MAX(100, "Máxima")
}
