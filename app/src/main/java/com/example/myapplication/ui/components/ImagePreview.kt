package com.example.myapplication.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/**
 * Componente para previsualizar imágenes (Modo estático)
 */
@Composable
fun ImagePreview(
    bitmap: Bitmap?,
    isProcessing: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Hemos eliminado las variables de estado (scale, offsetX, offsetY)
    // para evitar el movimiento.

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (isProcessing) {
            ProcessingIndicator()
        } else if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Image preview",
                modifier = Modifier
                    .fillMaxSize(), // Ocupa todo el espacio de la caja
                contentScale = ContentScale.Fit // Se ajusta sin recortarse y sin salirse
            )
            // Hemos eliminado el ZoomIndicator porque ya no hay zoom
        } else {
            Text(
                text = "No hay imagen",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Comparación lado a lado con slider
 */
@Composable
fun ImageComparisonPreview(
    originalBitmap: Bitmap?,
    editedBitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    var comparisonValue by remember { mutableFloatStateOf(0.5f) }

    Box(modifier = modifier.fillMaxSize()) {
        // Imagen original (izquierda)
        if (originalBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        clip = true
                        translationX = -(size.width * (1 - comparisonValue))
                    }
            ) {
                Image(
                    bitmap = originalBitmap.asImageBitmap(),
                    contentDescription = "Original",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Imagen editada (derecha)
        if (editedBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        clip = true
                        translationX = size.width * comparisonValue
                    }
            ) {
                Image(
                    bitmap = editedBitmap.asImageBitmap(),
                    contentDescription = "Editada",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Slider de comparación
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Original",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Editada",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Slider(
                value = comparisonValue,
                onValueChange = { comparisonValue = it },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Indicador de procesamiento
 */
@Composable
private fun ProcessingIndicator() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Procesando...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}