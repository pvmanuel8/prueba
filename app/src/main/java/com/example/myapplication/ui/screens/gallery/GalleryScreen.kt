package com.example.myapplication.ui.screens.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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

    // Mostrar mensajes de error
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is GalleryUiState.Error -> {
                android.util.Log.e("GalleryScreen", "Error: ${state.message}")
                snackbarHostState.showSnackbar(state.message)
            }
            is GalleryUiState.Success -> {
                android.util.Log.d("GalleryScreen", "Imagen cargada exitosamente")
            }
            is GalleryUiState.LoadingMultiple -> {
                android.util.Log.d("GalleryScreen", "Cargando ${state.current}/${state.total}")
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Indicador de carga múltiple
            when (val state = uiState) {
                is GalleryUiState.LoadingMultiple -> {
                    LinearProgressIndicator(
                        progress =  state.current.toFloat() / state.total ,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Cargando ${state.current}/${state.total} imágenes...",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                else -> {}
            }

            // --- CAMBIO: Contenedor del Grid con peso (weight) para ocupar espacio ---
            Box(
                modifier = Modifier
                    .weight(1f) // Esto hace que ocupe todo el espacio disponible verticalmente
                    .fillMaxWidth()
            ) {
                if (loadedImages.isEmpty()) {
                    EmptyGalleryContent()
                } else {
                    ImageGrid(
                        images = loadedImages,
                        onImageClick = { imageData ->
                            onImageSelected(imageData.id)
                        }
                    )
                }
            }

            // --- CAMBIO: Botones de acción movidos al final ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp), // Padding para separarlo de los bordes
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Seleccionar imagen individual
                MultipleImagePickerButton(
                    onImagesSelected = { uris ->
                        android.util.Log.d("GalleryScreen", "Imágenes seleccionadas: ${uris.size}")
                        if (uris.size == 1) {
                            android.util.Log.d("GalleryScreen", "Cargando imagen única: ${uris.first()}")
                            viewModel.loadImage(uris.first())
                        } else {
                            android.util.Log.d("GalleryScreen", "Cargando múltiples imágenes")
                            viewModel.loadMultipleImages(uris)
                        }
                    },
                    maxImages = 10,
                    modifier = Modifier.fillMaxWidth()
                )

                // Tomar foto con cámara
                CameraButton(
                    onImageCaptured = { uri ->
                        android.util.Log.d("GalleryScreen", "Foto capturada: $uri")
                        viewModel.loadImage(uri)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Contenido cuando no hay imágenes
 */
@Composable
private fun EmptyGalleryContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
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
}

/**
 * Grid de imágenes
 */
@Composable
private fun ImageGrid(
    images: List<ImageData>,
    onImageClick: (ImageData) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
        Box {
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