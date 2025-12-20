package com.example.myapplication.util



object Constants {
    // Límites de procesamiento
    const val MAX_BATCH_IMAGES = 10
    const val MAX_IMAGES_IN_MEMORY = 5
    const val TILE_SIZE = 256 // Tamaño de bloque para procesamiento paralelo
    const val MAX_CONCURRENT_TASKS = 4

    // Tamaños de imagen
    const val PREVIEW_MAX_WIDTH = 1024
    const val PREVIEW_MAX_HEIGHT = 1024
    const val THUMBNAIL_SIZE = 200

    // Caché
    const val CACHE_MAX_SIZE = 50 * 1024 * 1024 // 50 MB
    const val MAX_CACHE_ENTRIES = 20

    // Archivos
    const val PROJECTS_FILE = "projects.json"
    const val PREFS_NAME = "image_editor_prefs"
    const val IMAGE_DIRECTORY = "ImageEditor"
    const val THUMBNAILS_DIRECTORY = "thumbnails"

    // Keys de preferencias
    const val PREF_COMPRESSION_QUALITY = "compression_quality"
    const val PREF_AUTO_SAVE = "auto_save"
    const val PREF_SHOW_HISTOGRAM = "show_histogram"
    const val PREF_DARK_MODE = "dark_mode"

    // Tiempos
    const val DEBOUNCE_TIME_MS = 300L
    const val PROCESSING_TIMEOUT_MS = 30000L // 30 segundos

    // Formatos de imagen
    val SUPPORTED_FORMATS = listOf("jpg", "jpeg", "png")
    const val DEFAULT_IMAGE_FORMAT = "jpg"

    // Histograma
    const val HISTOGRAM_BINS = 256
    const val HISTOGRAM_HEIGHT = 100
}