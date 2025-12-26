package com.example.routinereminder.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable

sealed class Screen(
    val route: String,
    val title: String,
    val icon: (@Composable (() -> Unit))? = null
) {

    // MAIN TABS
    object RoutineReminder : Screen(
        "routine_reminder",
        "Routine",
        { Icon(Icons.Filled.Home, contentDescription = "Routine") }
    )

    object CalorieTracker : Screen(
        "calories",
        "Calories",
        { Icon(Icons.Filled.Restaurant, contentDescription = "Calories") }
    )

    object Map : Screen(
        "map",
        "Map",
        { Icon(Icons.Filled.LocationOn, contentDescription = "Map") }
    )

    // BARCODE SCANNER (MISSING ONE—THIS FIXES THE ERROR)
    object BarcodeScanner : Screen(
        "barcode_scanner",
        "Barcode Scanner",
        null
    )

    // SETTINGS WITH PARAMETER
    object Settings : Screen("settings/{from}", "Settings") {
        fun from(source: String) = "settings/$source"
    }

    // SHARE PREVIEW
    object SharePreview : Screen("share_preview/{sessionId}", "Share Preview") {
        fun route(sessionId: String) = "share_preview/$sessionId"
    }

    companion object {
        val bottomBarScreens = listOf(
            RoutineReminder,
            CalorieTracker,
            Map
        )

        val allScreens = listOf(
            RoutineReminder,
            CalorieTracker,
            Map,
            BarcodeScanner,   // <-- ADD THIS
            Settings,
            SharePreview
        )
    }
}
