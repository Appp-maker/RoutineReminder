package com.example.routinereminder.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

private val checkedBoxRegex = Regex("\\[[xX]]")
private val uncheckedBoxRegex = Regex("\\[\\s]")
private val checklistLineRegex = Regex("^\\s*\\[(x|X|\\s)]\\s*(.*)$")
private val markdownTokenRegex = Regex(
    "\\[color=#[0-9a-fA-F]{6}].+?\\[/color]|`[^`]+`|\\*\\*[^*]+\\*\\*|__[^_]+__|~~[^~]+~~|\\*[^*]+\\*|_[^_]+_",
    setOf(RegexOption.DOT_MATCHES_ALL)
)

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

private fun buildMarkdownAnnotatedString(text: String): AnnotatedString = buildAnnotatedString {
    var lastIndex = 0
    for (match in markdownTokenRegex.findAll(text)) {
        val startIndex = match.range.first
        val endIndex = match.range.last + 1
        if (startIndex > lastIndex) {
            append(text.substring(lastIndex, startIndex))
        }

        val token = match.value
        when {
            token.startsWith("**") && token.endsWith("**") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(token.substring(2, token.length - 2))
                }
            }
            token.startsWith("__") && token.endsWith("__") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(token.substring(2, token.length - 2))
                }
            }
            token.startsWith("[color=#") && token.endsWith("[/color]") -> {
                val colorEndIndex = token.indexOf(']')
                val colorHex = token.substring(8, colorEndIndex)
                val content = token.substring(colorEndIndex + 1, token.length - "[/color]".length)
                val parsed = android.graphics.Color.parseColor("#$colorHex")
                withStyle(SpanStyle(color = Color(parsed))) {
                    append(content)
                }
            }
            token.startsWith("~~") && token.endsWith("~~") -> {
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    append(token.substring(2, token.length - 2))
                }
            }
            token.startsWith("`") && token.endsWith("`") -> {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                    append(token.substring(1, token.length - 1))
                }
            }
            token.startsWith("*") && token.endsWith("*") -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(token.substring(1, token.length - 1))
                }
            }
            token.startsWith("_") && token.endsWith("_") -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(token.substring(1, token.length - 1))
                }
            }
            else -> append(token)
        }
        lastIndex = endIndex
    }

    if (lastIndex < text.length) {
        append(text.substring(lastIndex))
    }
}
