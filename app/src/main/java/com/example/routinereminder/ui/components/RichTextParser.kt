package com.example.routinereminder.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.style.TextDecoration

internal const val COLOR_TAG_CLOSE = "[/color]"
private val COLOR_HEX_REGEX = Regex("^[0-9a-fA-F]{6}$")

internal data class RichTextParseResult(
    val annotatedString: AnnotatedString,
    val offsetMapping: OffsetMapping
)

internal fun parseColorTagStart(text: String, index: Int): Pair<String, Int>? {
    if (!text.startsWith("[color=#", index)) return null
    val colorEnd = text.indexOf(']', startIndex = index + 8)
    if (colorEnd == -1) return null
    val colorHex = text.substring(index + 8, colorEnd)
    if (!COLOR_HEX_REGEX.matches(colorHex)) return null
    return colorHex to (colorEnd + 1)
}

internal fun buildRichAnnotatedString(text: String): RichTextParseResult {
    val originalLength = text.length
    val originalToTransformed = IntArray(originalLength + 1)
    val transformedToOriginal = mutableListOf(0)
    val builder = AnnotatedString.Builder()
    var outputLength = 0
    var index = 0

    var bold = false
    var italic = false
    var strike = false
    var code = false
    val colorStack = ArrayDeque<Color>()

    var currentStyle = SpanStyle()
    var currentStyleStart = 0

    fun computeStyle(): SpanStyle {
        var style = SpanStyle()
        if (bold) style = style.copy(fontWeight = FontWeight.Bold)
        if (italic) style = style.copy(fontStyle = FontStyle.Italic)
        if (strike) style = style.copy(textDecoration = TextDecoration.LineThrough)
        if (code) style = style.copy(fontFamily = FontFamily.Monospace)
        if (colorStack.isNotEmpty()) style = style.copy(color = colorStack.last())
        return style
    }

    fun updateStyle() {
        val newStyle = computeStyle()
        if (newStyle == currentStyle) return
        if (currentStyle != SpanStyle() && outputLength > currentStyleStart) {
            builder.addStyle(currentStyle, currentStyleStart, outputLength)
        }
        currentStyle = newStyle
        currentStyleStart = outputLength
    }

    fun appendChar(char: Char, originalIndex: Int) {
        originalToTransformed[originalIndex] = outputLength
        builder.append(char)
        outputLength += 1
        transformedToOriginal.add(originalIndex + 1)
    }

    while (index < text.length) {
        if (code) {
            if (text[index] == '`') {
                originalToTransformed[index] = outputLength
                code = false
                index += 1
                updateStyle()
                continue
            }
            appendChar(text[index], index)
            index += 1
            continue
        }

        val colorStart = parseColorTagStart(text, index)
        if (colorStart != null) {
            val (hex, nextIndex) = colorStart
            val color = Color(android.graphics.Color.parseColor("#$hex"))
            for (i in index until nextIndex) {
                originalToTransformed[i] = outputLength
            }
            colorStack.addLast(color)
            index = nextIndex
            updateStyle()
            continue
        }

        if (text.startsWith(COLOR_TAG_CLOSE, index) && colorStack.isNotEmpty()) {
            for (i in index until index + COLOR_TAG_CLOSE.length) {
                originalToTransformed[i] = outputLength
            }
            colorStack.removeLast()
            index += COLOR_TAG_CLOSE.length
            updateStyle()
            continue
        }

        if (text.startsWith("**", index)) {
            for (i in index until index + 2) {
                originalToTransformed[i] = outputLength
            }
            bold = !bold
            index += 2
            updateStyle()
            continue
        }

        if (text.startsWith("__", index)) {
            for (i in index until index + 2) {
                originalToTransformed[i] = outputLength
            }
            bold = !bold
            index += 2
            updateStyle()
            continue
        }

        if (text.startsWith("~~", index)) {
            for (i in index until index + 2) {
                originalToTransformed[i] = outputLength
            }
            strike = !strike
            index += 2
            updateStyle()
            continue
        }

        if (text[index] == '`') {
            originalToTransformed[index] = outputLength
            code = true
            index += 1
            updateStyle()
            continue
        }

        if (text[index] == '*' || text[index] == '_') {
            originalToTransformed[index] = outputLength
            italic = !italic
            index += 1
            updateStyle()
            continue
        }

        appendChar(text[index], index)
        index += 1
    }

    if (currentStyle != SpanStyle() && outputLength > currentStyleStart) {
        builder.addStyle(currentStyle, currentStyleStart, outputLength)
    }

    originalToTransformed[originalLength] = outputLength
    if (transformedToOriginal.size <= outputLength) {
        transformedToOriginal.add(originalLength)
    } else {
        transformedToOriginal[outputLength] = originalLength
    }

    val offsetMapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            val safeOffset = offset.coerceIn(0, originalLength)
            return originalToTransformed[safeOffset]
        }

        override fun transformedToOriginal(offset: Int): Int {
            val safeOffset = offset.coerceIn(0, outputLength)
            return transformedToOriginal.getOrElse(safeOffset) { originalLength }
        }
    }

    return RichTextParseResult(builder.toAnnotatedString(), offsetMapping)
}
