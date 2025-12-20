package com.example.myapplication.data.model

/**
 * Tipos de filtros disponibles en la aplicación
 */
sealed class FilterType(
    val name: String,
    val category: FilterCategory
) {
    // Filtros básicos
    data object Grayscale : FilterType("Escala de Grises", FilterCategory.BASIC)
    data object Sepia : FilterType("Sepia", FilterCategory.BASIC)
    data object Negative : FilterType("Negativo", FilterCategory.BASIC)
    data class Brightness(val value: Float = 0f) : FilterType("Brillo", FilterCategory.BASIC) // -100 a +100
    data class Contrast(val value: Float = 0f) : FilterType("Contraste", FilterCategory.BASIC) // -100 a +100
    data class Saturation(val value: Float = 0f) : FilterType("Saturación", FilterCategory.BASIC) // -100 a +100

    // Filtros avanzados
    data class Blur(val intensity: Int = 5) : FilterType("Desenfoque", FilterCategory.ADVANCED)
    data object Sharpen : FilterType("Enfoque", FilterCategory.ADVANCED)
    data object EdgeDetection : FilterType("Detección de Bordes", FilterCategory.ADVANCED)
    data class Posterize(val levels: Int = 4) : FilterType("Posterización", FilterCategory.ADVANCED)
    data class Vignette(val intensity: Float = 0.5f) : FilterType("Viñeta", FilterCategory.ADVANCED)

    // Transformaciones
    data class Rotate(val degrees: Int) : FilterType("Rotar", FilterCategory.TRANSFORM)
    data class Flip(val horizontal: Boolean) : FilterType("Voltear", FilterCategory.TRANSFORM)
    data class Crop(val rect: CropRect) : FilterType("Recortar", FilterCategory.TRANSFORM)
    data class Resize(val scale: Float) : FilterType("Redimensionar", FilterCategory.TRANSFORM)
}

enum class FilterCategory {
    BASIC, ADVANCED, TRANSFORM
}

/**
 * Representa un área de recorte
 */
data class CropRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/**
 * Proporciones de aspecto predefinidas para recorte
 */
enum class AspectRatio(val ratio: Float, val displayName: String) {
    FREE(0f, "Libre"),
    SQUARE(1f, "1:1"),
    RATIO_4_3(4f / 3f, "4:3"),
    RATIO_16_9(16f / 9f, "16:9"),
    RATIO_3_4(3f / 4f, "3:4"),
    RATIO_9_16(9f / 16f, "9:16")
}