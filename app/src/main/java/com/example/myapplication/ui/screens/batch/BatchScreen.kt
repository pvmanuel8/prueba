package com.example.myapplication.ui.screens.batch

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.myapplication.data.model.ImageData
import com.example.myapplication.ui.components.BatchProgressCard
import com.example.myapplication.ui.components.FilterSelector
import com.example.myapplication.ui.components.ImageProgressList
import com.example.myapplication.ui.components.MultipleImagePickerButton

/**
 * Pantalla de procesamiento por lotes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchScreen(
    onBack: () -> Unit,
    preloadedImages: List<com.example.myapplication.data.model.ImageData> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = remember { BatchViewModel(context) }

    val uiState by viewModel.uiState.collectAsState()
    val selectedImages by viewModel.selectedImages.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val imageProgresses by viewModel.imageProgresses.collectAsState()
    val overallProgress by viewModel.overallProgress.collectAsState()
    val estimatedTimeRemaining by viewModel.estimatedTimeRemaining.collectAsState()
    val processedImages by viewModel.processedImages.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Cargar imágenes precargadas al iniciar
    LaunchedEffect(preloadedImages) {
        try {
            if (preloadedImages.isNotEmpty() && selectedImages.isEmpty()) {
                android.util.Log.d("BatchScreen", "Cargando ${preloadedImages.size} imágenes precargadas")
                viewModel.selectImages(preloadedImages)
            }
        } catch (e: Exception) {
            android.util.Log.e("BatchScreen", "Error al cargar imágenes: ${e.message}", e)
            snackbarHostState.showSnackbar("Error al cargar imágenes: ${e.message}")
        }
    }

    // Mostrar mensajes
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is BatchUiState.Saved -> {
                snackbarHostState.showSnackbar("${state.savedCount} imágenes guardadas")
            }
            is BatchUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
            }
            is BatchUiState.Completed -> {
                snackbarHostState.showSnackbar("${state.processedCount} imágenes procesadas")
            }
            BatchUiState.Cancelled -> {
                snackbarHostState.showSnackbar("Procesamiento cancelado")
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Procesamiento por Lotes")
                        if (selectedImages.isNotEmpty()) {
                            Text(
                                text = "${selectedImages.size} imágenes seleccionadas",
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
                    if (uiState is BatchUiState.Completed) {
                        IconButton(onClick = { viewModel.reset() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reiniciar"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            // Botón anclado abajo solo cuando hay filtro seleccionado
            if (uiState is BatchUiState.ImagesSelected && selectedFilter != null) {
                androidx.compose.material3.BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Button(
                        onClick = { viewModel.startProcessing() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        enabled = selectedFilter != null
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Iniciar Procesamiento")
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            when (uiState) {
                BatchUiState.Initial -> {
                    item {
                        InitialContent(
                            onImagesSelected = { uris ->
                                // Cargar imágenes desde URIs
                            }
                        )
                    }
                }

                is BatchUiState.ImagesSelected -> {
                    item {
                        ImagesPreviewGrid(images = selectedImages)
                    }

                    item {
                        FilterSelectionContent(
                            selectedFilter = selectedFilter,
                            onFilterSelected = { filter ->
                                viewModel.selectFilter(filter)
                            },
                            onStartProcessing = {
                                viewModel.startProcessing()
                            },
                            canStart = selectedFilter != null
                        )
                    }

                    // Espaciador al final para evitar que el contenido quede oculto
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }

                BatchUiState.Processing -> {
                    item {
                        ProcessingContent(
                            currentImage = imageProgresses.indexOfFirst {
                                it.state == com.example.myapplication.ui.components.ProcessingItemState.PROCESSING
                            } + 1,
                            totalImages = selectedImages.size,
                            overallProgress = overallProgress,
                            imageProgresses = imageProgresses,
                            estimatedTimeRemaining = estimatedTimeRemaining,
                            onCancel = { viewModel.cancelProcessing() }
                        )
                    }
                }

                is BatchUiState.Completed -> {
                    item {
                        CompletedContent(
                            processedCount = processedImages.size,
                            onSaveAll = { viewModel.saveAll() },
                            onReset = { viewModel.reset() }
                        )
                    }

                    item {
                        Text(
                            "Resultados",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    items(imageProgresses) { item ->
                        // Mostrar progreso final
                    }
                }

                BatchUiState.Saving -> {
                    item {
                        SavingContent()
                    }
                }

                is BatchUiState.Saved -> {
                    item {
                        SavedContent(onDone = onBack)
                    }
                }

                BatchUiState.Cancelled -> {
                    item {
                        CancelledContent(onReset = { viewModel.reset() })
                    }
                }

                is BatchUiState.Error -> {
                    item {
                        ErrorContent(
                            message = (uiState as BatchUiState.Error).message,
                            onRetry = { viewModel.reset() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImagesPreviewGrid(images: List<ImageData>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Imágenes Seleccionadas",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (images.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No hay imágenes seleccionadas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(images) { imageData ->
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(120.dp)
                    ) {
                        AsyncImage(
                            model = imageData.uri,
                            contentDescription = "Imagen: ${imageData.name}",
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.medium
                                ),
                            contentScale = ContentScale.Crop
                        )

                        // Nombre de la imagen en la parte inferior
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                )
                                .padding(4.dp)
                        ) {
                            Text(
                                text = imageData.name,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InitialContent(
    onImagesSelected: (List<Uri>) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Selecciona hasta 10 imágenes para procesar",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        MultipleImagePickerButton(
            onImagesSelected = onImagesSelected,
            maxImages = 10,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FilterSelectionContent(
    selectedFilter: com.example.myapplication.data.model.FilterType?,
    onFilterSelected: (com.example.myapplication.data.model.FilterType) -> Unit,
    onStartProcessing: () -> Unit,
    canStart: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Selecciona un filtro para aplicar a todas las imágenes",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        FilterSelector(
            onFilterSelected = onFilterSelected,
            currentFilter = selectedFilter,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
    }
}

@Composable
private fun ProcessingContent(
    currentImage: Int,
    totalImages: Int,
    overallProgress: Float,
    imageProgresses: List<com.example.myapplication.ui.components.ImageProgressItem>,
    estimatedTimeRemaining: Long?,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Tarjeta de progreso principal
        BatchProgressCard(
            currentImage = currentImage,
            totalImages = totalImages,
            overallProgress = overallProgress,
            imageName = imageProgresses.getOrNull(currentImage - 1)?.imageName ?: "",
            imageProgress = imageProgresses.getOrNull(currentImage - 1)?.progress ?: 0f,
            estimatedTimeRemaining = estimatedTimeRemaining?.let { formatTime(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Botón de cancelar
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = null
            )
            Spacer(modifier = Modifier.padding(4.dp))
            Text("Cancelar Procesamiento")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Progreso Individual",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Lista de progreso individual
        ImageProgressList(imageProgresses = imageProgresses)
    }
}

@Composable
private fun CompletedContent(
    processedCount: Int,
    onSaveAll: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "✓ Procesamiento Completado",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "$processedCount imágenes procesadas exitosamente",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Nuevo Lote")
            }

            Button(
                onClick = onSaveAll,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Guardar Todo")
            }
        }
    }
}

@Composable
private fun SavingContent() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Guardando imágenes...")
        }
    }
}

@Composable
private fun SavedContent(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "✓ Imágenes Guardadas",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Finalizar")
        }
    }
}

@Composable
private fun CancelledContent(onReset: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Procesamiento Cancelado",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reintentar")
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Error",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reintentar")
        }
    }
}

/**
 * Formatea tiempo en milisegundos a formato legible
 */
private fun formatTime(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    return when {
        seconds < 60 -> "$seconds seg"
        else -> {
            val minutes = seconds / 60
            val secs = seconds % 60
            "${minutes}m ${secs}s"
        }
    }
}