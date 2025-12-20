package com.example.myapplication.ui.screens.gallery

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.ImageData
import com.example.myapplication.data.repository.ImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de galería
 */
class GalleryViewModel(context: Context) : ViewModel() {

    private val repository = ImageRepository(context)

    private val _uiState = MutableStateFlow<GalleryUiState>(GalleryUiState.Initial)
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private val _loadedImages = MutableStateFlow<List<ImageData>>(emptyList())
    val loadedImages: StateFlow<List<ImageData>> = _loadedImages.asStateFlow()

    /**
     * Carga una imagen desde una URI
     */
    fun loadImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = GalleryUiState.Loading

            repository.loadImageFromUri(uri)
                .onSuccess { imageData ->
                    _loadedImages.value = _loadedImages.value + imageData
                    _uiState.value = GalleryUiState.Success(imageData)
                }
                .onFailure { error ->
                    _uiState.value = GalleryUiState.Error(
                        error.message ?: "Error al cargar la imagen"
                    )
                }
        }
    }

    /**
     * Carga múltiples imágenes
     */
    fun loadMultipleImages(uris: List<Uri>) {
        viewModelScope.launch {
            _uiState.value = GalleryUiState.LoadingMultiple(0, uris.size)

            val loadedImagesList = mutableListOf<ImageData>()

            uris.forEachIndexed { index, uri ->
                repository.loadImageFromUri(uri)
                    .onSuccess { imageData ->
                        loadedImagesList.add(imageData)
                        _uiState.value = GalleryUiState.LoadingMultiple(
                            index + 1,
                            uris.size
                        )
                    }
                    .onFailure { error ->
                        // Continuar con las demás imágenes aunque falle una
                        _uiState.value = GalleryUiState.LoadingMultiple(
                            index + 1,
                            uris.size
                        )
                    }
            }

            if (loadedImagesList.isNotEmpty()) {
                _loadedImages.value = _loadedImages.value + loadedImagesList
                _uiState.value = GalleryUiState.MultipleSuccess(loadedImagesList)
            } else {
                _uiState.value = GalleryUiState.Error("No se pudo cargar ninguna imagen")
            }
        }
    }

    /**
     * Obtiene una imagen específica
     */
    fun getImage(imageId: String): ImageData? {
        return _loadedImages.value.find { it.id == imageId }
    }

    /**
     * Elimina una imagen de la lista
     */
    fun removeImage(imageId: String) {
        _loadedImages.value = _loadedImages.value.filter { it.id != imageId }
    }

    /**
     * Limpia todas las imágenes cargadas
     */
    fun clearImages() {
        repository.clearCache()
        _loadedImages.value = emptyList()
        _uiState.value = GalleryUiState.Initial
    }

    override fun onCleared() {
        super.onCleared()
        repository.clearCache()
    }
}

/**
 * Estados de la UI de la galería
 */
sealed class GalleryUiState {
    data object Initial : GalleryUiState()
    data object Loading : GalleryUiState()
    data class LoadingMultiple(val current: Int, val total: Int) : GalleryUiState()
    data class Success(val image: ImageData) : GalleryUiState()
    data class MultipleSuccess(val images: List<ImageData>) : GalleryUiState()
    data class Error(val message: String) : GalleryUiState()
}