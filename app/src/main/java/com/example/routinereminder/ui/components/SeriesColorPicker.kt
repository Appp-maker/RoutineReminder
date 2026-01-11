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

private fun colorToHex(color: Color): String {
    val rgb = color.toArgb() and 0x00FFFFFF
    return String.format("#%06X", rgb)
}

private fun parseHexColor(input: String): Color? {
    val cleaned = input.trim().removePrefix("#")
    if (cleaned.length != 6 && cleaned.length != 8) return null
    if (!cleaned.matches(Regex("[0-9a-fA-F]+"))) return null
    val value = cleaned.toLong(16)
    val argb = if (cleaned.length == 6) {
        0xFF000000L or value
    } else {
        value
    }
    return Color(argb.toInt())
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
    var customColorInput by remember { mutableStateOf(colorToHex(selectedColor)) }
    var customColorError by remember { mutableStateOf(false) }

    LaunchedEffect(selectedColor) {
        customColorInput = colorToHex(selectedColor)
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
                    val parsed = parseHexColor(newValue)
                    customColorError = parsed == null && newValue.isNotBlank()
                    if (parsed != null) {
                        onColorSelected(parsed)
                    }
                },
                label = { Text(stringResource(R.string.custom_color_label)) },
                supportingText = { Text(stringResource(R.string.custom_color_helper)) },
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
