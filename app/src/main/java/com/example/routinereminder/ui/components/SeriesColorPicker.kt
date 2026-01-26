package com.example.routinereminder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.routinereminder.R
import com.example.routinereminder.data.DEFAULT_SERIES_COLOR_ARGB
import com.example.routinereminder.data.NO_EVENT_FOOD_COLOR_ARGB
import com.example.routinereminder.data.SERIES_COLOR_OPTIONS
import com.example.routinereminder.data.isCustomSeriesColor
import kotlin.math.roundToInt

val SeriesColorOptions = SERIES_COLOR_OPTIONS.map { Color(it) }
val EventFoodColorOptions = listOf(Color(NO_EVENT_FOOD_COLOR_ARGB)) +
    SeriesColorOptions.filter { it.toArgb() != NO_EVENT_FOOD_COLOR_ARGB }

fun defaultSeriesColorArgb(): Int = DEFAULT_SERIES_COLOR_ARGB
fun isNoEventFoodColor(colorArgb: Int): Boolean = colorArgb == NO_EVENT_FOOD_COLOR_ARGB
fun resolveEventFoodColor(colorArgb: Int, fallback: Color): Color {
    return if (isNoEventFoodColor(colorArgb)) fallback else Color(colorArgb)
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
    allowCustomColor: Boolean = true,
    recentCustomColors: List<Int> = emptyList()
) {
    var customColor by remember { mutableStateOf(selectedColor) }
    val isCustomSelected = colorOptions.none { it.toArgb() == selectedColor.toArgb() }
    var showCustomPicker by remember { mutableStateOf(false) }
    val savedCustomHistory = remember(recentCustomColors) {
        recentCustomColors.filter { isCustomSeriesColor(it) }.distinct().take(10)
    }

    LaunchedEffect(selectedColor, allowCustomColor, colorOptions) {
        customColor = selectedColor
        if (!allowCustomColor || !isCustomSelected) {
            showCustomPicker = false
        }
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
                SeriesColorSwatch(
                    color = color,
                    size = 30.dp,
                    isSelected = isSelected,
                    enabled = enabled,
                    modifier = Modifier.clickable(enabled = enabled) {
                        onColorSelected(color)
                        showCustomPicker = false
                    }
                )
            }
            if (allowCustomColor) {
                CustomColorSwatch(
                    size = 30.dp,
                    isSelected = isCustomSelected,
                    enabled = enabled,
                    modifier = Modifier.clickable(enabled = enabled) {
                        showCustomPicker = true
                        onColorSelected(customColor)
                    }
                )
            }
        }
        if (allowCustomColor && savedCustomHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.custom_color_recent_label),
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) LocalContentColor.current else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                savedCustomHistory.forEach { colorArgb ->
                    val color = Color(colorArgb)
                    val isSelected = colorArgb == selectedColor.toArgb()
                    CustomHistorySwatch(
                        color = color,
                        size = 30.dp,
                        isSelected = isSelected,
                        enabled = enabled,
                        modifier = Modifier.clickable(enabled = enabled) {
                            customColor = color
                            showCustomPicker = false
                            onColorSelected(color)
                        }
                    )
                }
            }
        }
        if (allowCustomColor && showCustomPicker) {
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
                SeriesColorSwatch(
                    color = customColor,
                    size = 36.dp,
                    enabled = enabled
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
    size: Dp = 14.dp
) {
    SeriesColorSwatch(
        color = color,
        size = size,
        modifier = modifier
    )
}

@Composable
private fun SeriesColorSwatch(
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    enabled: Boolean = true
) {
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val surfaceRing = MaterialTheme.colorScheme.surfaceVariant
    val swatchAlpha = if (enabled) 1f else 0.5f

    Box(
        modifier = modifier
            .size(size)
            .background(surfaceRing.copy(alpha = swatchAlpha), CircleShape)
            .border(borderWidth, borderColor.copy(alpha = swatchAlpha), CircleShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
                .background(color.copy(alpha = swatchAlpha), CircleShape)
        )
        if (color.alpha == 0f) {
            Text(
                text = "Ø",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = swatchAlpha),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun CustomColorSwatch(
    size: Dp,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    enabled: Boolean = true
) {
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val surfaceRing = MaterialTheme.colorScheme.surfaceVariant
    val swatchAlpha = if (enabled) 1f else 0.5f
    val spectrumBrush = Brush.sweepGradient(
        listOf(
            Color.Red,
            Color.Yellow,
            Color.Green,
            Color.Cyan,
            Color.Blue,
            Color.Magenta,
            Color.Red
        )
    )

    Box(
        modifier = modifier
            .size(size)
            .background(surfaceRing.copy(alpha = swatchAlpha), CircleShape)
            .border(borderWidth, borderColor.copy(alpha = swatchAlpha), CircleShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
                .background(spectrumBrush, CircleShape)
        )
    }
}

@Composable
private fun CustomHistorySwatch(
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    enabled: Boolean = true
) {
    Box(modifier = modifier.size(size)) {
        SeriesColorSwatch(
            color = color,
            size = size,
            isSelected = isSelected,
            enabled = enabled,
            modifier = Modifier.align(Alignment.Center)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(14.dp)
                .background(
                    MaterialTheme.colorScheme.secondary.copy(alpha = if (enabled) 1f else 0.5f),
                    CircleShape
                )
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            Text(
                text = stringResource(R.string.custom_color_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
