package com.example.routinereminder.data

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromName(name: String?): ThemeMode {
            return entries.find { it.name == name } ?: SYSTEM
        }
    }
}
