package com.example.routinereminder.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

private val checkedBoxRegex = Regex("\\[[xX]]")
private val uncheckedBoxRegex = Regex("\\[\\s]")
private val markdownTokenRegex = Regex(
    "`[^`]+`|\\*\\*[^*]+\\*\\*|__[^_]+__|~~[^~]+~~|\\*[^*]+\\*|_[^_]+_",
    setOf(RegexOption.DOT_MATCHES_ALL)
)

fun formatChecklistText(text: String): AnnotatedString {
    val normalized = text
        .replace(checkedBoxRegex, "☑")
        .replace(uncheckedBoxRegex, "☐")
    return buildMarkdownAnnotatedString(normalized)
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
