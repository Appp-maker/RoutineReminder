package com.example.routinereminder.ui.components

import androidx.compose.ui.text.AnnotatedString

private val checkedBoxRegex = Regex("\\[[xX]]")
private val uncheckedBoxRegex = Regex("\\[\\s]")
private val checklistLineRegex = Regex("^\\s*\\[(x|X|\\s)]\\s*(.*)$")

data class ChecklistLine(
    val text: String,
    val hasCheckbox: Boolean,
    val isChecked: Boolean
)

data class ChecklistCompletion(
    val hasCheckboxes: Boolean,
    val allChecked: Boolean
)

fun formatChecklistText(text: String): AnnotatedString {
    val normalized = text
        .replace(checkedBoxRegex, "☑")
        .replace(uncheckedBoxRegex, "☐")
    return buildMarkdownAnnotatedString(normalized)
}

fun formatChecklistLineText(text: String): AnnotatedString {
    return buildMarkdownAnnotatedString(text)
}

fun parseChecklistLines(text: String): List<ChecklistLine> {
    return text.split("\n").map { line ->
        val match = checklistLineRegex.find(line)
        if (match != null) {
            val isChecked = match.groupValues[1].equals("x", ignoreCase = true)
            ChecklistLine(
                text = match.groupValues[2],
                hasCheckbox = true,
                isChecked = isChecked
            )
        } else {
            ChecklistLine(
                text = line,
                hasCheckbox = false,
                isChecked = false
            )
        }
    }
}

fun toggleChecklistLine(text: String, lineIndex: Int): String {
    val lines = text.split("\n").toMutableList()
    val target = lines.getOrNull(lineIndex) ?: return text
    val match = checklistLineRegex.find(target) ?: return text
    val isChecked = match.groupValues[1].equals("x", ignoreCase = true)
    val content = match.groupValues[2]
    val updatedMarker = if (isChecked) " " else "x"
    lines[lineIndex] = "[$updatedMarker] $content"
    return lines.joinToString("\n")
}

fun checklistCompletionState(text: String): ChecklistCompletion {
    val lines = parseChecklistLines(text)
    val checkboxLines = lines.filter { it.hasCheckbox }
    val hasCheckboxes = checkboxLines.isNotEmpty()
    val allChecked = hasCheckboxes && checkboxLines.all { it.isChecked }
    return ChecklistCompletion(hasCheckboxes = hasCheckboxes, allChecked = allChecked)
}

private fun buildMarkdownAnnotatedString(text: String): AnnotatedString {
    return buildRichAnnotatedString(text).annotatedString
}
