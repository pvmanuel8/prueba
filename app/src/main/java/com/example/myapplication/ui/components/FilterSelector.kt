@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myapplication.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.FilterCategory
import com.example.myapplication.data.model.FilterType

/**
 * Selector de filtros con preview
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSelector(
    onFilterSelected: (FilterType) -> Unit,
    currentFilter: FilterType? = null,
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(com.example.myapplication.data.model.FilterCategory.BASIC) }

    Column(modifier = modifier) {
        // Categorías de filtros
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            com.example.myapplication.data.model.FilterCategory.entries.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(getCategoryName(category)) }
                )
            }
        }

        // Lista de filtros según categoría
        FilterList(
            category = selectedCategory,
            currentFilter = currentFilter,
            onFilterClick = { filter ->
                onFilterSelected(filter)
            }
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Seleccionar Filtro",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Contenido del bottom sheet
            }
        }
    }
}

/**
 * Lista de filtros por categoría
 */
@Composable
private fun FilterList(
    category: com.example.myapplication.data.model.FilterCategory,
    currentFilter: FilterType?,
    onFilterClick: (FilterType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (category) {
            com.example.myapplication.data.model.FilterCategory.BASIC -> {
                BasicFiltersList(currentFilter, onFilterClick)
            }
            com.example.myapplication.data.model.FilterCategory.ADVANCED -> {
                AdvancedFiltersList(currentFilter, onFilterClick)
            }
            com.example.myapplication.data.model.FilterCategory.TRANSFORM -> {
                TransformationsList(currentFilter, onFilterClick)
            }
        }
    }
}

/**
 * Filtros básicos
 */
@Composable
private fun BasicFiltersList(
    currentFilter: FilterType?,
    onFilterClick: (FilterType) -> Unit
) {
    var brightnessValue by remember { mutableFloatStateOf(0f) }
    var contrastValue by remember { mutableFloatStateOf(0f) }
    var saturationValue by remember { mutableFloatStateOf(0f) }

    // Filtros simples
    FilterItem(
        name = "Escala de Grises",
        icon = Icons.Default.FilterBAndW,
        isSelected = currentFilter is FilterType.Grayscale,
        onClick = { onFilterClick(FilterType.Grayscale) }
    )

    FilterItem(
        name = "Sepia",
        icon = Icons.Default.Palette,
        isSelected = currentFilter is FilterType.Sepia,
        onClick = { onFilterClick(FilterType.Sepia) }
    )

    FilterItem(
        name = "Negativo",
        icon = Icons.Default.Contrast,
        isSelected = currentFilter is FilterType.Negative,
        onClick = { onFilterClick(FilterType.Negative) }
    )

    // Brillo con slider
    FilterItemWithSlider(
        name = "Brillo",
        icon = Icons.Default.Brightness4,
        value = brightnessValue,
        onValueChange = {
            brightnessValue = it
            onFilterClick(FilterType.Brightness(it))
        },
        valueRange = -100f..100f
    )

    // Contraste con slider
    FilterItemWithSlider(
        name = "Contraste",
        icon = Icons.Default.Contrast,
        value = contrastValue,
        onValueChange = {
            contrastValue = it
            onFilterClick(FilterType.Contrast(it))
        },
        valueRange = -100f..100f
    )

    // Saturación con slider
    FilterItemWithSlider(
        name = "Saturación",
        icon = Icons.Default.Palette,
        value = saturationValue,
        onValueChange = {
            saturationValue = it
            onFilterClick(FilterType.Saturation(it))
        },
        valueRange = -100f..100f
    )
}

/**
 * Filtros avanzados
 */
@Composable
private fun AdvancedFiltersList(
    currentFilter: FilterType?,
    onFilterClick: (FilterType) -> Unit
) {
    var blurIntensity by remember { mutableFloatStateOf(5f) }
    var posterizeLevels by remember { mutableFloatStateOf(4f) }
    var vignetteIntensity by remember { mutableFloatStateOf(0.5f) }

    FilterItem(
        name = "Enfoque",
        isSelected = currentFilter is FilterType.Sharpen,
        onClick = { onFilterClick(FilterType.Sharpen) }
    )

    FilterItem(
        name = "Detección de Bordes",
        isSelected = currentFilter is FilterType.EdgeDetection,
        onClick = { onFilterClick(FilterType.EdgeDetection) }
    )

    FilterItemWithSlider(
        name = "Desenfoque",
        value = blurIntensity,
        onValueChange = {
            blurIntensity = it
            onFilterClick(FilterType.Blur(it.toInt()))
        },
        valueRange = 1f..25f
    )

    FilterItemWithSlider(
        name = "Posterización",
        value = posterizeLevels,
        onValueChange = {
            posterizeLevels = it
            onFilterClick(FilterType.Posterize(it.toInt()))
        },
        valueRange = 2f..16f
    )

    FilterItemWithSlider(
        name = "Viñeta",
        value = vignetteIntensity,
        onValueChange = {
            vignetteIntensity = it
            onFilterClick(FilterType.Vignette(it))
        },
        valueRange = 0f..1f
    )
}

/**
 * Transformaciones
 */
@Composable
private fun TransformationsList(
    currentFilter: FilterType?,
    onFilterClick: (FilterType) -> Unit
) {
    FilterItem(
        name = "Rotar 90°",
        onClick = { onFilterClick(FilterType.Rotate(90)) }
    )

    FilterItem(
        name = "Rotar 180°",
        onClick = { onFilterClick(FilterType.Rotate(180)) }
    )

    FilterItem(
        name = "Rotar 270°",
        onClick = { onFilterClick(FilterType.Rotate(270)) }
    )

    FilterItem(
        name = "Voltear Horizontal",
        onClick = { onFilterClick(FilterType.Flip(horizontal = true)) }
    )

    FilterItem(
        name = "Voltear Vertical",
        onClick = { onFilterClick(FilterType.Flip(horizontal = false)) }
    )

    var resizeScale by remember { mutableFloatStateOf(1f) }
    FilterItemWithSlider(
        name = "Redimensionar",
        value = resizeScale,
        onValueChange = {
            resizeScale = it
            onFilterClick(FilterType.Resize(it))
        },
        valueRange = 0.25f..2f,
        valueLabel = "${(resizeScale * 100).toInt()}%"
    )
}

/**
 * Item de filtro simple
 */
@Composable
private fun FilterItem(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

/**
 * Item de filtro con slider
 */
@Composable
private fun FilterItemWithSlider(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Text(
                    text = valueLabel ?: value.toInt().toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Obtiene el nombre de la categoría
 */
private fun getCategoryName(category: com.example.myapplication.data.model.FilterCategory): String {
    return when (category) {
        com.example.myapplication.data.model.FilterCategory.BASIC -> "Básicos"
        com.example.myapplication.data.model.FilterCategory.ADVANCED -> "Avanzados"
        com.example.myapplication.data.model.FilterCategory.TRANSFORM -> "Transformar"
    }
}