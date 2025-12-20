package com.example.myapplication.ui.screens.editor


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.components.FilterSelector
import com.example.myapplication.ui.components.HistogramView
import com.example.myapplication.ui.components.ImageComparisonPreview
import com.example.myapplication.ui.components.ImagePreview
import kotlin.properties.ReadOnlyProperty

/**
 * Pantalla principal del editor de imágenes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    imageId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = remember { EditorViewModel(context) }

    val uiState by viewModel.uiState.collectAsState()
    val originalBitmap by viewModel.originalBitmap.collectAsState()
    val editedBitmap by viewModel.editedBitmap.collectAsState()
    val appliedFilters by viewModel.appliedFilters.collectAsState()
    val histogramData by viewModel.histogramData.collectAsState()
    val histogramStatistics by viewModel.histogramStatistics.collectAsState()
    val isCalculatingHistogram by viewModel.isCalculatingHistogram.collectAsState()

    var showComparison by remember { mutableStateOf(false) }
    var showHistogram by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Cargar imagen al iniciar
    LaunchedEffect(imageId) {
        viewModel.loadImage(imageId)
    }

    // Mostrar mensajes
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is EditorUiState.Saved -> {
                snackbarHostState.showSnackbar("Imagen guardada correctamente")
            }
            is EditorUiState.ProjectSaved -> {
                snackbarHostState.showSnackbar("Proyecto guardado")
            }
            is EditorUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Editor")
                        if (appliedFilters.isNotEmpty()) {
                            Text(
                                text = "${appliedFilters.size} filtros aplicados",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    // Toggle histograma
                    IconButton(
                        onClick = { showHistogram = !showHistogram },
                        enabled = uiState is EditorUiState.Ready
                    ) {
                        Icon(
                            imageVector = if (showHistogram) {
                                androidx.compose.material.icons.Icons.Default.BarChart
                            } else {
                                androidx.compose.material.icons.Icons.Default.ShowChart
                            },
                            contentDescription = "Histograma"
                        )
                    }

                    // Comparar
                    IconButton(
                        onClick = { showComparison = !showComparison },
                        enabled = uiState is EditorUiState.Ready && appliedFilters.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = "Comparar"
                        )
                    }

                    // Resetear
                    IconButton(
                        onClick = { viewModel.reset() },
                        enabled = uiState is EditorUiState.Ready && appliedFilters.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Resetear"
                        )
                    }

                    // Guardar
                    IconButton(
                        onClick = { viewModel.saveImage() },
                        enabled = uiState is EditorUiState.Ready && appliedFilters.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Guardar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Deshacer
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = viewModel.canUndo()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Deshacer"
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Rehacer
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = viewModel.canRedo()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = "Rehacer"
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Información
                    if (uiState is EditorUiState.Processing) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(8.dp)
                        )
                        Text(
                            text = "Procesando...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (uiState is EditorUiState.Saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(8.dp)
                        )
                        Text(
                            text = "Guardando...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Vista previa de imagen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    uiState is EditorUiState.Loading -> {
                        LoadingContent()
                    }
                    showComparison && originalBitmap != null && editedBitmap != null -> {
                        ImageComparisonPreview(
                            originalBitmap = originalBitmap,
                            editedBitmap = editedBitmap
                        )
                    }
                    else -> {
                        ImagePreview(
                            bitmap = editedBitmap,
                            isProcessing = uiState is EditorUiState.Processing
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Histograma (opcional)
            if (showHistogram && (uiState is EditorUiState.Ready || uiState is EditorUiState.Processing)) {
                HistogramView(
                    histogramData = histogramData,
                    statistics = histogramStatistics,
                    isCalculating = isCalculatingHistogram,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Selector de filtros
            if (uiState is EditorUiState.Ready || uiState is EditorUiState.Processing) {
                FilterSelector(
                    onFilterSelected = { filter ->
                        viewModel.applyFilter(filter)
                    },
                    currentFilter = appliedFilters.lastOrNull(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                )
            }
        }
    }
}


/**
 * Contenido de carga
 */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Cargando imagen...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}