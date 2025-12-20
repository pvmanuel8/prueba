package com.example.myapplication.data.model

import android.graphics.Bitmap
import android.net.Uri

/**
 * Representa una imagen en la aplicación
 */
data class ImageData(
    val id: String,
    val uri: Uri? = null,
    val bitmap: Bitmap? = null,
    val name: String,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0,
    val format: ImageFormat = ImageFormat.JPEG,
    val timestamp: Long = System.currentTimeMillis(),
    val appliedFilters: List<FilterType> = emptyList()
) {
    val resolution: String
        get() = "${width}x${height}"

    val sizeFormatted: String
        get() = when {
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
            else -> String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0))
        }
}

enum class ImageFormat {
    JPEG, PNG, WEBP
}