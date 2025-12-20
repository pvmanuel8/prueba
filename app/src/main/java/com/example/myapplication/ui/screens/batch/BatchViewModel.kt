package com.example.myapplication.ui.screens.batch



import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.CompressionQuality
import com.example.myapplication.data.model.FilterType
import com.example.myapplication.data.model.ImageData
import com.example.myapplication.data.repository.ImageRepository
import com.example.myapplication.domain.usecase.BatchImageProcessingProgress
import com.example.myapplication.domain.usecase.ProcessBatchUseCase
import com.example.myapplication.ui.components.ImageProgressItem
import com.example.myapplication.ui.components.ProcessingItemState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para procesamiento por lotes
 */
class BatchViewModel(context: Context) : ViewModel() {

    private val repository = ImageRepository(context)
    private val processBatchUseCase = ProcessBatchUseCase()

    private val _uiState = MutableStateFlow<BatchUiState>(BatchUiState.Initial)
    val uiState: StateFlow<BatchUiState> = _uiState.asStateFlow()

    private val _selectedImages = MutableStateFlow<List<ImageData>>(emptyList())
    val selectedImages: StateFlow<List<ImageData>> = _selectedImages.asStateFlow()

    private val _selectedFilter = MutableStateFlow<FilterType?>(null)
    val selectedFilter: StateFlow<FilterType?> = _selectedFilter.asStateFlow()

    private val _imageProgresses = MutableStateFlow<List<ImageProgressItem>>(emptyList())
    val imageProgresses: StateFlow<List<ImageProgressItem>> = _imageProgresses.asStateFlow()

    private val _overallProgress = MutableStateFlow(0f)
    val overallProgress: StateFlow<Float> = _overallProgress.asStateFlow()

    private val _estimatedTimeRemaining = MutableStateFlow<Long?>(null)
    val estimatedTimeRemaining: StateFlow<Long?> = _estimatedTimeRemaining.asStateFlow()

    private val _processedImages = MutableStateFlow<List<ImageData>>(emptyList())
    val processedImages: StateFlow<List<ImageData>> = _processedImages.asStateFlow()

    private var processingJob: Job? = null
    private var startTime: Long = 0

    /**
     * Selecciona las imágenes para procesar
     */
    fun selectImages(images: List<ImageData>) {
        if (images.size > 10) {
            _uiState.value = BatchUiState.Error("Máximo 10 imágenes permitidas")
            return
        }

        _selectedImages.value = images

        // Inicializar progreso de cada imagen
        _imageProgresses.value = images.map { image ->
            ImageProgressItem(
                imageName = image.name,
                progress = 0f,
                state = ProcessingItemState.PENDING
            )
        }

        _uiState.value = BatchUiState.ImagesSelected(images.size)
    }

    /**
     * Selecciona el filtro a aplicar
     */
    fun selectFilter(filter: FilterType) {
        _selectedFilter.value = filter

        // Estimar tiempo de procesamiento
        val images = _selectedImages.value
        if (images.isNotEmpty()) {
            val estimatedTime = processBatchUseCase.estimateBatchTime(images, filter)
            _estimatedTimeRemaining.value = estimatedTime
        }
    }

    /**
     * Inicia el procesamiento por lotes
     */
    fun startProcessing() {
        val images = _selectedImages.value
        val filter = _selectedFilter.value

        if (images.isEmpty()) {
            _uiState.value = BatchUiState.Error("No hay imágenes seleccionadas")
            return
        }

        if (filter == null) {
            _uiState.value = BatchUiState.Error("No hay filtro seleccionado")
            return
        }

        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            _uiState.value = BatchUiState.Processing
            startTime = System.currentTimeMillis()

            processBatchUseCase.processBatchImages(images, filter).collect { progress ->
                when (progress) {
                    is BatchImageProcessingProgress.Started -> {
                        _overallProgress.value = 0f
                    }

                    is BatchImageProcessingProgress.ProcessingImage -> {
                        val currentProgress = progress.imageProgress
                        val totalProgress = (progress.currentIndex.toFloat() + currentProgress) /
                                progress.totalImages

                        _overallProgress.value = totalProgress

                        // Actualizar progreso individual
                        updateImageProgress(
                            index = progress.currentIndex,
                            progress = currentProgress,
                            state = ProcessingItemState.PROCESSING
                        )

                        // Calcular tiempo restante
                        updateEstimatedTime(progress.currentIndex, progress.totalImages)
                    }

                    is BatchImageProcessingProgress.ImageCompleted -> {
                        // Marcar imagen como completada
                        updateImageProgress(
                            index = progress.completedCount - 1,
                            progress = 1f,
                            state = ProcessingItemState.COMPLETED
                        )
                    }

                    is BatchImageProcessingProgress.Completed -> {
                        _processedImages.value = progress.results
                        _overallProgress.value = 1f
                        _uiState.value = BatchUiState.Completed(progress.results.size)
                    }

                    is BatchImageProcessingProgress.Error -> {
                        _uiState.value = BatchUiState.Error(progress.message)
                    }
                }
            }
        }
    }

    /**
     * Cancela el procesamiento
     */
    fun cancelProcessing() {
        processingJob?.cancel()
        processingJob = null
        _uiState.value = BatchUiState.Cancelled
        _overallProgress.value = 0f
    }

    /**
     * Guarda todas las imágenes procesadas
     */
    fun saveAll(
        toGallery: Boolean = true,
        quality: CompressionQuality = CompressionQuality.HIGH
    ) {
        val images = _processedImages.value
        if (images.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = BatchUiState.Saving

            repository.saveMultipleImages(images, toGallery, quality)
                .onSuccess { uris ->
                    _uiState.value = BatchUiState.Saved(uris.size)
                }
                .onFailure { error ->
                    _uiState.value = BatchUiState.Error(
                        error.message ?: "Error al guardar imágenes"
                    )
                }
        }
    }

    /**
     * Reinicia el estado
     */
    fun reset() {
        processingJob?.cancel()
        _selectedImages.value = emptyList()
        _selectedFilter.value = null
        _imageProgresses.value = emptyList()
        _overallProgress.value = 0f
        _estimatedTimeRemaining.value = null
        _processedImages.value = emptyList()
        _uiState.value = BatchUiState.Initial
    }

    /**
     * Actualiza el progreso de una imagen específica
     */
    private fun updateImageProgress(
        index: Int,
        progress: Float,
        state: ProcessingItemState,
        errorMessage: String? = null
    ) {
        val currentProgresses = _imageProgresses.value.toMutableList()
        if (index in currentProgresses.indices) {
            currentProgresses[index] = currentProgresses[index].copy(
                progress = progress,
                state = state,
                errorMessage = errorMessage
            )
            _imageProgresses.value = currentProgresses
        }
    }

    /**
     * Actualiza el tiempo estimado restante
     */
    private fun updateEstimatedTime(currentImage: Int, totalImages: Int) {
        if (currentImage == 0) return

        val elapsedTime = System.currentTimeMillis() - startTime
        val avgTimePerImage = elapsedTime.toFloat() / currentImage
        val remainingImages = totalImages - currentImage
        val estimatedRemaining = (avgTimePerImage * remainingImages).toLong()

        _estimatedTimeRemaining.value = estimatedRemaining
    }

    override fun onCleared() {
        super.onCleared()
        processingJob?.cancel()
    }
}

/**
 * Estados de la UI de procesamiento por lotes
 */
sealed class BatchUiState {
    data object Initial : BatchUiState()
    data class ImagesSelected(val count: Int) : BatchUiState()
    data object Processing : BatchUiState()
    data class Completed(val processedCount: Int) : BatchUiState()
    data object Saving : BatchUiState()
    data class Saved(val savedCount: Int) : BatchUiState()
    data object Cancelled : BatchUiState()
    data class Error(val message: String) : BatchUiState()
}