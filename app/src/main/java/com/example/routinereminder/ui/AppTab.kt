package com.example.routinereminder.ui

import androidx.annotation.StringRes
import com.example.routinereminder.R

enum class AppTab(
    val id: String,
    @StringRes val labelRes: Int,
    val screen: Screen
) {
    Routine("routine", R.string.tab_events, Screen.RoutineReminder),
    Calories("calories", R.string.tab_calories, Screen.CalorieTracker),
    Map("map", R.string.tab_running, Screen.Map),
    Workout("workout", R.string.tab_workout, Screen.Workout);

    companion object {
        val defaultTabs: Set<AppTab> = entries.toSet()

        fun fromIds(ids: Set<String>): Set<AppTab> {
            return entries.filter { it.id in ids }.toSet()
        }
    }
}
