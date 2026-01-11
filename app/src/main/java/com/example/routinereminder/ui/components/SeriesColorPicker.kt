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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.routinereminder.data.DEFAULT_SERIES_COLOR_ARGB
import com.example.routinereminder.data.SERIES_COLOR_OPTIONS

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
    showLabel: Boolean = true
) {
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
