package com.example.myapplication.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.myapplication.domain.processor.HistogramStatistics
import kotlin.math.roundToInt

/**
 * Vista del histograma RGB
 */
@Composable
fun HistogramView(
    histogramData: Long,
    statistics: Long,
    isCalculating: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

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
            Text(
                text = "Histograma",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isCalculating) {
                CalculatingIndicator()
            } else if (histogramData != null) {
                // Tabs para seleccionar canal
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("RGB") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Rojo") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Verde") }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Azul") }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = { Text("Lum") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Gráfico del histograma
                when (selectedTab) {
                    0 -> RGBHistogramChart(histogramData)
                    1 -> SingleChannelHistogramChart(
                        data = histogramData.red,
                        color = Color.Red,
                        label = "Canal Rojo"
                    )
                    2 -> SingleChannelHistogramChart(
                        data = histogramData.green,
                        color = Color.Green,
                        label = "Canal Verde"
                    )
                    3 -> SingleChannelHistogramChart(
                        data = histogramData.blue,
                        color = Color.Blue,
                        label = "Canal Azul"
                    )
                    4 -> SingleChannelHistogramChart(
                        data = histogramData.luminance,
                        color = Color.Gray,
                        label = "Luminancia"
                    )
                }

                // Estadísticas
                if (statistics != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    StatisticsView(statistics)
                }
            } else {
                NoDataIndicator()
            }
        }
    }
}

/**
 * Gráfico RGB combinado
 */
@Composable
private fun RGBHistogramChart(
    histogramData: Long,
    modifier: Modifier = Modifier
) {
    val maxValue = histogramData.maxValue

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(Color.Black.copy(alpha = 0.1f))
    ) {
        val width = size.width
        val height = size.height
        val barWidth = width / histogramData.red.size

        // Dibujar canal rojo
        drawHistogramPath(
            data = histogramData.red,
            color = Color.Red.copy(alpha = 0.5f),
            maxValue = maxValue,
            width = width,
            height = height,
            barWidth = barWidth
        )

        // Dibujar canal verde
        drawHistogramPath(
            data = histogramData.green,
            color = Color.Green.copy(alpha = 0.5f),
            maxValue = maxValue,
            width = width,
            height = height,
            barWidth = barWidth
        )

        // Dibujar canal azul
        drawHistogramPath(
            data = histogramData.blue,
            color = Color.Blue.copy(alpha = 0.5f),
            maxValue = maxValue,
            width = width,
            height = height,
            barWidth = barWidth
        )
    }
}

/**
 * Gráfico de un solo canal
 */
@Composable
private fun SingleChannelHistogramChart(
    data: List<Float>,
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(Color.Black.copy(alpha = 0.1f))
        ) {
            val width = size.width
            val height = size.height
            val barWidth = width / data.size
            val maxValue = data.maxOrNull() ?: 1f

            // Dibujar histograma relleno
            drawHistogramPath(
                data = data,
                color = color.copy(alpha = 0.7f),
                maxValue = maxValue,
                width = width,
                height = height,
                barWidth = barWidth,
                filled = true
            )

            // Dibujar contorno
            drawHistogramPath(
                data = data,
                color = color,
                maxValue = maxValue,
                width = width,
                height = height,
                barWidth = barWidth,
                filled = false
            )
        }
    }
}

/**
 * Dibuja el path del histograma
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHistogramPath(
    data: List<Float>,
    color: Color,
    maxValue: Float,
    width: Float,
    height: Float,
    barWidth: Float,
    filled: Boolean = true
) {
    val path = Path()

    data.forEachIndexed { index, value ->
        val x = index * barWidth
        val y = height - (value / maxValue * height).coerceIn(0f, height)

        if (index == 0) {
            path.moveTo(x, height)
            path.lineTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    if (filled) {
        path.lineTo(width, height)
        path.lineTo(0f, height)
        path.close()

        drawPath(
            path = path,
            color = color,
            style = Fill
        )
    } else {
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2f)
        )
    }
}

/**
 * Vista de estadísticas
 */
@Composable
private fun StatisticsView(
    statistics: HistogramStatistics,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Estadísticas",
            style = MaterialTheme.typography.titleSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Media de cada canal
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(
                label = "Media R",
                value = statistics.meanRed.roundToInt().toString(),
                color = Color.Red
            )
            Spacer(modifier = Modifier.width(8.dp))
            StatItem(
                label = "Media G",
                value = statistics.meanGreen.roundToInt().toString(),
                color = Color.Green
            )
            Spacer(modifier = Modifier.width(8.dp))
            StatItem(
                label = "Media B",
                value = statistics.meanBlue.roundToInt().toString(),
                color = Color.Blue
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Rango de cada canal
        Column {
            RangeItem(
                label = "Rojo",
                min = statistics.minRed,
                max = statistics.maxRed,
                color = Color.Red
            )
            RangeItem(
                label = "Verde",
                min = statistics.minGreen,
                max = statistics.maxGreen,
                color = Color.Green
            )
            RangeItem(
                label = "Azul",
                min = statistics.minBlue,
                max = statistics.maxBlue,
                color = Color.Blue
            )
        }
    }
}

/**
 * Item de estadística
 */
@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = MaterialTheme.shapes.small)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Item de rango
 */
@Composable
private fun RangeItem(
    label: String,
    min: Int,
    max: Int,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, shape = MaterialTheme.shapes.small)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(50.dp)
        )
        Text(
            text = "$min - $max",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Indicador de cálculo
 */
@Composable
private fun CalculatingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Calculando histograma...",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Indicador sin datos
 */
@Composable
private fun NoDataIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No hay datos de histograma",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}