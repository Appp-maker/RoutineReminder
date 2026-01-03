package com.example.routinereminder.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.navigation.navArgument

sealed class Screen(
    val route: String,
    val title: String = "",
    val icon: (@Composable (() -> Unit))? = null
) {

    // Bottom tabs
    object RoutineReminder : Screen(
        route = "routine_reminder",
        title = "Routine",
        icon = { Icon(Icons.Filled.Home, contentDescription = "Routine") }
    )

    object CalorieTracker : Screen(
        route = "calories",
        title = "Calories",
        icon = { Icon(Icons.Filled.Restaurant, contentDescription = "Calories") }
    )

    object Map : Screen(
        route = "map",
        title = "Map",
        icon = { Icon(Icons.Filled.LocationOn, contentDescription = "Map") }
    )

    object Workout : Screen(
        route = "workout",
        title = "Workout",
        icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = "Workout") }
    )

    // Other routes
    object BarcodeScanner : Screen("barcode_scanner")
    object FoodSearch : Screen("calories?mode=search")
    object CustomFood : Screen("calories?mode=custom")


    object BundleList : Screen("bundle")
    object CreateBundle : Screen("bundle/create")

    object BundleDetail : Screen("bundle/{id}") {
        fun route(id: Long) = "bundle/$id"
    }

    object Settings : Screen("settings/{from}") {
        fun from(source: String) = "settings/$source"
    }

    object SharePreview : Screen("share_preview/{sessionId}") {
        fun route(sessionId: String) = "share_preview/$sessionId"
    }

    companion object {
        val bottomBarScreens = listOf(
            RoutineReminder,
            CalorieTracker,
            Map,
            Workout
        )

        val allScreens = listOf(
            RoutineReminder,
            CalorieTracker,
            Map,
            Workout,
            BarcodeScanner,
            FoodSearch,
            CustomFood,
            BundleList,
            CreateBundle,
            Settings,
            SharePreview
        )

    }
}
