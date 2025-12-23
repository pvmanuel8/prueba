package com.example.myapplication.ui.screens.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.myapplication.data.model.ImageData
import com.example.myapplication.ui.components.CameraButton
import com.example.myapplication.ui.components.MultipleImagePickerButton

/**
 * Pantalla de galería principal
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onImageSelected: (String) -> Unit,
    onBatchProcessing: () -> Unit,
    onHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = remember { GalleryViewModel(context) }

    val uiState by viewModel.uiState.collectAsState()
    val loadedImages by viewModel.loadedImages.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Mostrar mensajes de error o éxito
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is GalleryUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image Editor") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        // 1. LOS BOTONES VAN AQUÍ (Barra inferior fija)
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(50.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MultipleImagePickerButton(
                        onImagesSelected = { uris ->
                            if (uris.size == 1) {
                                viewModel.loadImage(uris.first())
                            } else {
                                viewModel.loadMultipleImages(uris)
                            }
                        },
                        maxImages = 10,
                        modifier = Modifier.fillMaxWidth()
                    )

                    CameraButton(
                        onImageCaptured = { uri ->
                            viewModel.loadImage(uri)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        // Contenido Principal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Respeta el espacio de la topBar y bottomBar
        ) {
            // Indicador de carga
            when (val state = uiState) {
                is GalleryUiState.LoadingMultiple -> {
                    LinearProgressIndicator(
                        progress =  state.current.toFloat() / state.total ,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "Cargando ${state.current}/${state.total} imágenes...",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                else -> {}
            }

            // 2. GRID O MENSAJE VACÍO
            // Usamos weight(1f) directamente aquí. La columna ocupará todo el espacio vertical
            // disponible entre la barra de arriba y la de abajo.
            if (loadedImages.isEmpty()) {
                EmptyGalleryContent(modifier = Modifier.weight(1f))
            } else {
                ImageGrid(
                    images = loadedImages,
                    onImageClick = { imageData -> onImageSelected(imageData.id) },
                    modifier = Modifier.weight(1f) // Importante: Esto activa el scroll
                )
            }
        }
    }
}

/**
 * Contenido cuando no hay imágenes
 */
@Composable
private fun EmptyGalleryContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(), // Usa el modifier pasado (weight)
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No hay imágenes",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Selecciona o captura una imagen para comenzar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Grid de imágenes
 * AHORA RECIBE UN MODIFIER PARA PODER EXPANDIRSE
 */
@Composable
private fun ImageGrid(
    images: List<ImageData>,
    onImageClick: (ImageData) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize() // Aplica el modifier (weight) y llena el espacio
    ) {
        items(images) { imageData ->
            ImageGridItem(
                imageData = imageData,
                onClick = { onImageClick(imageData) }
            )
        }
    }
}

/**
 * Item individual del grid
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageGridItem(
    imageData: ImageData,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        androidx.compose.foundation.layout.Box {
            AsyncImage(
                model = imageData.bitmap ?: imageData.uri,
                contentDescription = imageData.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Información de la imagen
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = imageData.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = imageData.resolution,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}