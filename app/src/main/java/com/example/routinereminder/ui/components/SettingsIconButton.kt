package com.example.routinereminder.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.routinereminder.ui.theme.AppPalette

@Composable
fun SettingsIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Settings"
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp).then(modifier)
    ) {
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = contentDescription,
            tint = AppPalette.TextPrimary
        )
    }
}
