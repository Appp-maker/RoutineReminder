package com.example.routinereminder.data

data class AppThemeColors(
    val primary: Int,
    val secondary: Int,
    val tertiary: Int
) {
    companion object {
        val Default = AppThemeColors(
            primary = DEFAULT_PRIMARY_COLOR_ARGB,
            secondary = DEFAULT_SECONDARY_COLOR_ARGB,
            tertiary = DEFAULT_TERTIARY_COLOR_ARGB
        )
    }
}

const val DEFAULT_PRIMARY_COLOR_ARGB = 0xFF2196F3.toInt()
const val DEFAULT_SECONDARY_COLOR_ARGB = 0xFF4CAF50.toInt()
const val DEFAULT_TERTIARY_COLOR_ARGB = 0xFFFFC107.toInt()
