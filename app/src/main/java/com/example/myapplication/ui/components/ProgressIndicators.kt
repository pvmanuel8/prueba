package com.example.myapplication.ui.components



import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Indicador de progreso lineal con información
 */
@Composable
fun LinearProgressWithInfo(
    progress: Float,
    label: String,
    info: String? = null,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "progress"
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${(animatedProgress * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress =  animatedProgress ,
            modifier = Modifier.fillMaxWidth()
        )

        if (info != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = info,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Tarjeta de progreso para procesamiento por lotes
 */
@Composable
fun BatchProgressCard(
    currentImage: Int,
    totalImages: Int,
    overallProgress: Float,
    imageName: String,
    imageProgress: Float,
    estimatedTimeRemaining: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Título
            Text(
                text = "Procesando Lote",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Información de imágenes
            Text(
                text = "Imagen $currentImage de $totalImages",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progreso global
            LinearProgressWithInfo(
                progress = overallProgress,
                label = "Progreso Total",
                info = if (estimatedTimeRemaining != null) {
                    "Tiempo restante: $estimatedTimeRemaining"
                } else null
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progreso de imagen actual
            LinearProgressWithInfo(
                progress = imageProgress,
                label = "Imagen Actual",
                info = imageName
            )
        }
    }
}

/**
 * Lista de progreso individual por imagen
 */
@Composable
fun ImageProgressList(
    imageProgresses: List<ImageProgressItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        imageProgresses.forEach { item ->
            ImageProgressItemCard(item)
        }
    }
}

/**
 * Item de progreso individual
 */
@Composable
private fun ImageProgressItemCard(item: ImageProgressItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (item.state) {
                ProcessingItemState.PENDING -> MaterialTheme.colorScheme.surface
                ProcessingItemState.PROCESSING -> MaterialTheme.colorScheme.primaryContainer
                ProcessingItemState.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
                ProcessingItemState.ERROR -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicador de estado
            when (item.state) {
                ProcessingItemState.PROCESSING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
                ProcessingItemState.COMPLETED -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completado",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                ProcessingItemState.ERROR -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
                ProcessingItemState.PENDING -> {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Pendiente",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Información de la imagen
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.imageName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.state == ProcessingItemState.PROCESSING) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress =  item.progress ,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (item.state == ProcessingItemState.ERROR && item.errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Porcentaje o estado
            if (item.state == ProcessingItemState.PROCESSING) {
                Text(
                    text = "${(item.progress * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Indicador circular con texto
 */
@Composable
fun CircularProgressWithLabel(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "circular_progress"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            progress =  animatedProgress ,
            modifier = Modifier.size(64.dp),
            strokeWidth = 6.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "${(animatedProgress * 100).roundToInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Item de progreso de imagen
 */
data class ImageProgressItem(
    val imageName: String,
    val progress: Float,
    val state: ProcessingItemState,
    val errorMessage: String? = null
)

/**
 * Estado del procesamiento de un item
 */
enum class ProcessingItemState {
    PENDING,
    PROCESSING,
    COMPLETED,
    ERROR
}