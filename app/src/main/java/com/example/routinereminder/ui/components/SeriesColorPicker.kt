package com.example.routinereminder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Slider
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
import kotlin.math.roundToInt

val SeriesColorOptions = SERIES_COLOR_OPTIONS.map { Color(it) }

fun defaultSeriesColorArgb(): Int = DEFAULT_SERIES_COLOR_ARGB

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
    var customColor by remember { mutableStateOf(selectedColor) }

    LaunchedEffect(selectedColor) {
        customColor = selectedColor
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
            val red = (customColor.red * 255).roundToInt().coerceIn(0, 255)
            val green = (customColor.green * 255).roundToInt().coerceIn(0, 255)
            val blue = (customColor.blue * 255).roundToInt().coerceIn(0, 255)
            val hexValue = String.format("#%02X%02X%02X", red, green, blue)

            Text(
                stringResource(R.string.custom_color_label),
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) LocalContentColor.current else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(customColor, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = hexValue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) LocalContentColor.current else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            ColorChannelSlider(
                label = stringResource(R.string.custom_color_red_label),
                value = red,
                enabled = enabled,
                onValueChange = { newRed ->
                    val nextColor = Color(newRed / 255f, green / 255f, blue / 255f)
                    customColor = nextColor
                    onColorSelected(nextColor)
                }
            )
            ColorChannelSlider(
                label = stringResource(R.string.custom_color_green_label),
                value = green,
                enabled = enabled,
                onValueChange = { newGreen ->
                    val nextColor = Color(red / 255f, newGreen / 255f, blue / 255f)
                    customColor = nextColor
                    onColorSelected(nextColor)
                }
            )
            ColorChannelSlider(
                label = stringResource(R.string.custom_color_blue_label),
                value = blue,
                enabled = enabled,
                onValueChange = { newBlue ->
                    val nextColor = Color(red / 255f, green / 255f, newBlue / 255f)
                    customColor = nextColor
                    onColorSelected(nextColor)
                }
            )
        }
    }
}

@Composable
private fun ColorChannelSlider(
    label: String,
    value: Int,
    enabled: Boolean,
    onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) LocalContentColor.current else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) LocalContentColor.current else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(0, 255)) },
            valueRange = 0f..255f,
            steps = 254,
            enabled = enabled
        )
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
