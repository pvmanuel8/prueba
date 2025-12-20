package com.example.myapplication.ui.screens.history



import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.ImageProject
import com.example.myapplication.domain.usecase.SaveImageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para pantalla de historial
 */
class HistoryViewModel(context: Context) : ViewModel() {

    private val saveImageUseCase = SaveImageUseCase(context)

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _projects = MutableStateFlow<List<ImageProject>>(emptyList())
    val projects: StateFlow<List<ImageProject>> = _projects.asStateFlow()

    init {
        loadProjects()
    }

    /**
     * Carga todos los proyectos
     */
    fun loadProjects() {
        viewModelScope.launch {
            _uiState.value = HistoryUiState.Loading

            saveImageUseCase.listProjects()
                .onSuccess { projectList ->
                    _projects.value = projectList.sortedByDescending { it.timestamp }
                    _uiState.value = if (projectList.isEmpty()) {
                        HistoryUiState.Empty
                    } else {
                        HistoryUiState.Success
                    }
                }
                .onFailure { error ->
                    _uiState.value = HistoryUiState.Error(
                        error.message ?: "Error al cargar proyectos"
                    )
                }
        }
    }

    /**
     * Elimina un proyecto
     */
    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            saveImageUseCase.deleteProject(projectId)
                .onSuccess {
                    loadProjects() // Recargar lista
                }
                .onFailure { error ->
                    _uiState.value = HistoryUiState.Error(
                        error.message ?: "Error al eliminar proyecto"
                    )
                }
        }
    }

    /**
     * Carga un proyecto para editar
     */
    fun loadProject(projectId: String, onLoaded: (String) -> Unit) {
        viewModelScope.launch {
            saveImageUseCase.loadProject(projectId)
                .onSuccess { loadedProject ->
                    // Aquí podrías navegar a la pantalla de edición
                    // con los datos del proyecto cargado
                    onLoaded(projectId)
                }
                .onFailure { error ->
                    _uiState.value = HistoryUiState.Error(
                        error.message ?: "Error al cargar proyecto"
                    )
                }
        }
    }
}

/**
 * Estados de la UI del historial
 */
sealed class HistoryUiState {
    data object Loading : HistoryUiState()
    data object Success : HistoryUiState()
    data object Empty : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}