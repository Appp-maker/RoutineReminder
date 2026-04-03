package com.example.routinereminder.data

data class EventCardDetailSettings(
    val advancedModeEnabled: Boolean = false,
    val showLocation: Boolean = true,
    val showRouteEtaAndDistance: Boolean = true,
    val showWeather: Boolean = true
)
