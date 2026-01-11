package com.example.routinereminder.ui.components

private val checkedBoxRegex = Regex("\\[[xX]]")
private val uncheckedBoxRegex = Regex("\\[\\s]")

fun formatChecklistText(text: String): String {
    return text
        .replace(checkedBoxRegex, "☑")
        .replace(uncheckedBoxRegex, "☐")
}
