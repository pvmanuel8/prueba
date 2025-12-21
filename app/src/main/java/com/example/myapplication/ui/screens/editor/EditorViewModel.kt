package com.example.myapplication.ui.screens.editor

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.CompressionQuality
import com.example.myapplication.data.model.FilterType
import com.example.myapplication.data.model.ImageData
import com.example.myapplication.data.repository.ImageRepository
import com.example.myapplication.domain.processor.HistogramCalculator
import com.example.myapplication.domain.processor.HistogramData
import com.example.myapplication.domain.processor.HistogramStatistics
import com.example.myapplication.domain.processor.ParallelProcessingProgress
import com.example.myapplication.domain.usecase.ApplyFilterUseCase
import com.example.myapplication.domain.usecase.ProcessBatchUseCase
import com.example.myapplication.domain.usecase.SaveImageUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditorViewModel(context: Context) : ViewModel() {

    private val repository = ImageRepository(context)
    private val applyFilterUseCase = ApplyFilterUseCase()
    private val processBatchUseCase = ProcessBatchUseCase()
    private val saveImageUseCase = SaveImageUseCase(context)

    // Instancia de tu calculadora de histograma
    private val histogramCalculator = HistogramCalculator()

    private val _uiState = MutableStateFlow<EditorUiState>(EditorUiState.Loading)
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _imageData = MutableStateFlow<ImageData?>(null)
    val imageData: StateFlow<ImageData?> = _imageData.asStateFlow()

    private val _originalBitmap = MutableStateFlow<Bitmap?>(null)
    val originalBitmap: StateFlow<Bitmap?> = _originalBitmap.asStateFlow()

    private val _editedBitmap = MutableStateFlow<Bitmap?>(null)
    val editedBitmap: StateFlow<Bitmap?> = _editedBitmap.asStateFlow()

    private val _appliedFilters = MutableStateFlow<List<FilterType>>(emptyList())
    val appliedFilters: StateFlow<List<FilterType>> = _appliedFilters.asStateFlow()

    // --- VARIABLES DEL HISTOGRAMA ---
    private val _histogramData = MutableStateFlow<HistogramData?>(null)
    val histogramData: StateFlow<HistogramData?> = _histogramData.asStateFlow()

    private val _histogramStatistics = MutableStateFlow<HistogramStatistics?>(null)
    val histogramStatistics: StateFlow<HistogramStatistics?> = _histogramStatistics.asStateFlow()

    private val _isCalculatingHistogram = MutableStateFlow(false)
    val isCalculatingHistogram: StateFlow<Boolean> = _isCalculatingHistogram.asStateFlow()
    // ----------------------------------------------

    // Stack para deshacer/rehacer
    private val historyStack = mutableListOf<Bitmap>()
    private var historyIndex = -1

    private var currentJob: Job? = null

    companion object {
        private const val TAG = "EditorViewModel"
    }

    /**
     * Carga una imagen para editar
     */
    fun loadImage(imageId: String) {
        viewModelScope.launch {
            _uiState.value = EditorUiState.Loading

            // Log para depuración
            Log.d(TAG, "Intentando cargar imagen con ID: $imageId")

            val image = repository.getImageFromCache(imageId)

            Log.d(TAG, "Imagen encontrada en caché: ${image != null}")
            Log.d(TAG, "Bitmap disponible: ${image?.bitmap != null}")

            if (image != null && image.bitmap != null) {
                _imageData.value = image
                _originalBitmap.value = image.bitmap
                _editedBitmap.value = image.bitmap

                historyStack.clear()
                historyStack.add(image.bitmap)
                historyIndex = 0

                // Calcular histograma inicial
                calculateHistogram(image.bitmap)

                _uiState.value = EditorUiState.Ready
                Log.d(TAG, "Imagen cargada exitosamente")
            } else {
                Log.e(TAG, "Error: Imagen no encontrada o bitmap nulo")
                _uiState.value = EditorUiState.Error("Imagen no encontrada")
            }
        }
    }

    fun applyFilterParallel(filter: FilterType) {
        val bitmap = _editedBitmap.value ?: return
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            _uiState.value = EditorUiState.Processing
            processBatchUseCase.processImageParallel(bitmap, filter).collect { progress ->
                when (progress) {
                    is ParallelProcessingProgress.Completed -> {
                        _editedBitmap.value = progress.result
                        _appliedFilters.value = _appliedFilters.value + filter
                        addToHistory(progress.result)
                        _uiState.value = EditorUiState.Ready
                    }
                    else -> {}
                }
            }
        }
    }

    fun applyFilter(filter: FilterType) {
        val bitmap = _editedBitmap.value ?: return
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            _uiState.value = EditorUiState.Processing
            applyFilterUseCase(bitmap, filter)
                .onSuccess { processedBitmap ->
                    _editedBitmap.value = processedBitmap
                    _appliedFilters.value = _appliedFilters.value + filter
                    addToHistory(processedBitmap)
                    _uiState.value = EditorUiState.Ready
                }
                .onFailure { error ->
                    Log.e(TAG, "Error al aplicar filtro: ${error.message}")
                    _uiState.value = EditorUiState.Error(error.message ?: "Error al aplicar filtro")
                }
        }
    }

    fun generatePreview(filter: FilterType, onResult: (Bitmap?) -> Unit) {
        val bitmap = _editedBitmap.value ?: return
        viewModelScope.launch {
            applyFilterUseCase.generatePreview(bitmap, filter)
                .onSuccess { preview -> onResult(preview) }
                .onFailure { onResult(null) }
        }
    }

    fun undo() {
        if (canUndo()) {
            historyIndex--
            _editedBitmap.value = historyStack[historyIndex]
            if (_appliedFilters.value.isNotEmpty()) {
                _appliedFilters.value = _appliedFilters.value.dropLast(1)
            }
            updateHistogram()
        }
    }

    fun redo() {
        if (canRedo()) {
            historyIndex++
            _editedBitmap.value = historyStack[historyIndex]
            updateHistogram()
        }
    }

    fun canUndo(): Boolean = historyIndex > 0
    fun canRedo(): Boolean = historyIndex < historyStack.size - 1

    fun reset() {
        _editedBitmap.value = _originalBitmap.value
        _appliedFilters.value = emptyList()
        historyStack.clear()
        _originalBitmap.value?.let {
            historyStack.add(it)
            calculateHistogram(it)
        }
        historyIndex = 0
        _uiState.value = EditorUiState.Ready
    }

    fun saveImage(toGallery: Boolean = true, quality: CompressionQuality = CompressionQuality.HIGH) {
        val edited = _editedBitmap.value ?: return
        val image = _imageData.value ?: return
        viewModelScope.launch {
            _uiState.value = EditorUiState.Saving
            val updatedImage = image.copy(bitmap = edited, appliedFilters = _appliedFilters.value)
            repository.saveImage(updatedImage, toGallery, quality)
                .onSuccess { uri -> _uiState.value = EditorUiState.Saved(uri.toString()) }
                .onFailure { error -> _uiState.value = EditorUiState.Error(error.message ?: "Error al guardar") }
        }
    }

    // Método para guardar proyecto
    fun saveAsProject(toGallery: Boolean = false, quality: CompressionQuality = CompressionQuality.HIGH) {
        val image = _imageData.value ?: return
        val filters = _appliedFilters.value
        viewModelScope.launch {
            _uiState.value = EditorUiState.Saving
            saveImageUseCase.saveAsProject(image, filters, toGallery, quality)
                .onSuccess { project -> _uiState.value = EditorUiState.ProjectSaved(project.id) }
                .onFailure { error ->
                    Log.e(TAG, "Error al guardar proyecto: ${error.message}")
                    _uiState.value = EditorUiState.Error(error.message ?: "Error al guardar proyecto")
                }
        }
    }

    private fun calculateHistogram(bitmap: Bitmap) {
        viewModelScope.launch {
            _isCalculatingHistogram.value = true
            try {
                val histogram = histogramCalculator.calculateHistogram(bitmap)
                val statistics = histogramCalculator.calculateStatistics(bitmap)

                _histogramData.value = histogram
                _histogramStatistics.value = statistics
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "Error calculando histograma: ${e.message}")
            } finally {
                _isCalculatingHistogram.value = false
            }
        }
    }

    private fun updateHistogram() {
        _editedBitmap.value?.let { bitmap -> calculateHistogram(bitmap) }
    }

    private fun addToHistory(bitmap: Bitmap) {
        if (historyIndex < historyStack.size - 1) {
            historyStack.subList(historyIndex + 1, historyStack.size).clear()
        }
        historyStack.add(bitmap)
        historyIndex = historyStack.size - 1
        if (historyStack.size > 20) {
            historyStack.removeAt(0)
            historyIndex--
        }
        updateHistogram()
    }

    fun cancelProcessing() {
        currentJob?.cancel()
        applyFilterUseCase.cancel()
        _uiState.value = EditorUiState.Ready
    }

    override fun onCleared() {
        super.onCleared()
        applyFilterUseCase.clearCache()
        currentJob?.cancel()
    }
}

// Estados de la UI
sealed class EditorUiState {
    data object Loading : EditorUiState()
    data object Ready : EditorUiState()
    data object Processing : EditorUiState()
    data object Saving : EditorUiState()
    data class Saved(val uri: String) : EditorUiState()
    data class ProjectSaved(val projectId: String) : EditorUiState()
    data class Error(val message: String) : EditorUiState()
}