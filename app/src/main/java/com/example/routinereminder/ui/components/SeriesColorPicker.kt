package com.example.routinereminder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.routinereminder.R
import com.example.routinereminder.data.DEFAULT_SERIES_COLOR_ARGB
import com.example.routinereminder.data.SERIES_COLOR_OPTIONS

val SeriesColorOptions = SERIES_COLOR_OPTIONS.map { Color(it) }

fun defaultSeriesColorArgb(): Int = DEFAULT_SERIES_COLOR_ARGB

private data class NamedColor(val name: String, val color: Color)

private fun colorNameForSelection(namedColors: List<NamedColor>, color: Color): String {
    return namedColors.firstOrNull { it.color.toArgb() == color.toArgb() }?.name.orEmpty()
}

private fun parseNamedColor(namedColors: List<NamedColor>, input: String): Color? {
    val cleaned = input.trim().lowercase()
    if (cleaned.isEmpty()) return null
    return namedColors.firstOrNull { it.name.lowercase() == cleaned }?.color
}

@Composable
fun SeriesColorPicker(
    label: String,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colorOptions: List<Color> = SeriesColorOptions,
    showLabel: Boolean = true,
    allowCustomColor: Boolean = true
) {
    val namedColors = listOf(
        NamedColor(stringResource(R.string.color_name_blue), Color(DEFAULT_SERIES_COLOR_ARGB)),
        NamedColor(stringResource(R.string.color_name_green), Color(0xFF43A047)),
        NamedColor(stringResource(R.string.color_name_orange), Color(0xFFF4511E)),
        NamedColor(stringResource(R.string.color_name_purple), Color(0xFF8E24AA)),
        NamedColor(stringResource(R.string.color_name_yellow), Color(0xFFFDD835)),
        NamedColor(stringResource(R.string.color_name_teal), Color(0xFF00897B)),
        NamedColor(stringResource(R.string.color_name_brown), Color(0xFF6D4C41)),
        NamedColor(stringResource(R.string.color_name_pink), Color(0xFFD81B60))
    )
    val namedColorList = namedColors.joinToString(", ") { it.name }
    var customColorInput by remember { mutableStateOf(colorNameForSelection(namedColors, selectedColor)) }
    var customColorError by remember { mutableStateOf(false) }

    LaunchedEffect(selectedColor) {
        customColorInput = colorNameForSelection(namedColors, selectedColor)
        customColorError = false
    }

    Column(modifier = modifier) {
        if (showLabel) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) LocalContentColor.current else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colorOptions.forEach { color ->
                val isSelected = color.toArgb() == selectedColor.toArgb()
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color, CircleShape)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .clickable(enabled = enabled) { onColorSelected(color) }
                )
            }
        }
        if (allowCustomColor) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = customColorInput,
                onValueChange = { newValue ->
                    customColorInput = newValue
                    val parsed = parseNamedColor(namedColors, newValue)
                    customColorError = parsed == null && newValue.isNotBlank()
                    if (parsed != null) {
                        onColorSelected(parsed)
                    }
                },
                label = { Text(stringResource(R.string.custom_color_label)) },
                supportingText = { Text(stringResource(R.string.custom_color_helper, namedColorList)) },
                isError = customColorError,
                enabled = enabled,
                singleLine = true
            )
        }
    }
}

@Composable
fun SeriesColorDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color, CircleShape)
    )
}
