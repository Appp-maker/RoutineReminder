package com.example.routinereminder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RichTextEditor(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    singleLine: Boolean = false,
    outlined: Boolean = false
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    var isFocused by remember { mutableStateOf(false) }
    var colorMenuExpanded by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme
    val colorOptions = listOf(
        ColorOption("Primary", colorScheme.primary),
        ColorOption("Secondary", colorScheme.secondary)
    )
    val richTextVisualTransformation = remember { RichTextVisualTransformation() }
    val cursorIndex = fieldValue.selection.min
    val cursorState = remember(fieldValue.text, fieldValue.selection) {
        computeCursorState(fieldValue.text, cursorIndex)
    }
    val activeBold = cursorState.bold
    val activeItalic = cursorState.italic
    val activeColor = fieldValue.findEnclosingColorTag(cursorIndex)
    val activeActionColor = MaterialTheme.colorScheme.secondary
    val inactiveActionColor = MaterialTheme.colorScheme.onSurfaceVariant

    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            val selection = fieldValue.selection
            val clampedSelection = TextRange(
                selection.start.coerceAtMost(value.length),
                selection.end.coerceAtMost(value.length)
            )
            fieldValue = TextFieldValue(value, selection = clampedSelection)
        }
    }

    val textFieldModifier = modifier
        .fillMaxWidth()
        .onFocusChanged { focusState ->
            isFocused = focusState.isFocused
        }

    Box(modifier = modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Column {
            if (isFocused) {
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 2.dp,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                fieldValue = fieldValue.applyCheckboxToggle()
                                    .also { onValueChange(it.text) }
                            },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = inactiveActionColor)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckBox,
                                contentDescription = "Insert checkbox"
                            )
                        }
                        IconButton(
                            onClick = {
                                fieldValue = fieldValue.toggleInlineFormatting("__", "__")
                                    .also { onValueChange(it.text) }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = if (activeBold) activeActionColor else inactiveActionColor
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FormatBold,
                                contentDescription = "Bold"
                            )
                        }
                        IconButton(
                            onClick = {
                                fieldValue = fieldValue.toggleInlineFormatting("*", "*")
                                    .also { onValueChange(it.text) }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = if (activeItalic) activeActionColor else inactiveActionColor
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FormatItalic,
                                contentDescription = "Italic"
                            )
                        }
                        IconButton(
                            onClick = { colorMenuExpanded = true },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = if (activeColor != null) activeActionColor else inactiveActionColor
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FormatColorText,
                                contentDescription = "Text color"
                            )
                        }
                        DropdownMenu(
                            expanded = colorMenuExpanded,
                            onDismissRequest = { colorMenuExpanded = false }
                        ) {
                            colorOptions.forEach { option ->
                                val isActiveOption = activeColor?.hex?.equals(option.hex, ignoreCase = true) == true
                                DropdownMenuItem(
                                    text = {
                                        Row {
                                            Text(
                                                option.name,
                                                color = if (isActiveOption) activeActionColor else MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "●",
                                                color = option.color,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    },
                                    onClick = {
                                        fieldValue = if (isActiveOption && activeColor != null) {
                                            fieldValue.removeEnclosingColorTag(activeColor)
                                        } else {
                                            fieldValue.wrapSelection(
                                                "[color=#${option.hex}]",
                                                "[/color]"
                                            )
                                        }.also { onValueChange(it.text) }
                                        colorMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (outlined) {
                OutlinedTextField(
                    value = fieldValue,
                    onValueChange = {
                        val normalized = it.normalizeColorTagsForNewlines()
                            .sanitizeColorTagEdits()
                            .stripInlinePlaceholders()
                        fieldValue = normalized
                        onValueChange(normalized.text)
                    },
                    label = { Text(label) },
                    modifier = textFieldModifier,
                    minLines = minLines,
                    singleLine = singleLine,
                    visualTransformation = richTextVisualTransformation
                )
            } else {
                TextField(
                    value = fieldValue,
                    onValueChange = {
                        val normalized = it.normalizeColorTagsForNewlines()
                            .sanitizeColorTagEdits()
                            .stripInlinePlaceholders()
                        fieldValue = normalized
                        onValueChange(normalized.text)
                    },
                    label = { Text(label) },
                    modifier = textFieldModifier,
                    minLines = minLines,
                    singleLine = singleLine,
                    visualTransformation = richTextVisualTransformation
                )
            }
        }
    }
}

private fun TextFieldValue.wrapSelection(prefix: String, suffix: String): TextFieldValue {
    val selection = selection
    val start = selection.min
    val end = selection.max
    val text = text
    return if (selection.collapsed) {
        val newText = text.substring(0, start) + prefix + suffix + text.substring(end)
        val newCursor = start + prefix.length
        copy(text = newText, selection = TextRange(newCursor))
    } else {
        val selectedText = text.substring(start, end)
        val newText = text.substring(0, start) + prefix + selectedText + suffix + text.substring(end)
        copy(text = newText, selection = TextRange(start + prefix.length, end + prefix.length))
    }
}

private fun TextFieldValue.applyCheckboxToggle(): TextFieldValue {
    val selectionIndex = selection.min
    val text = text
    val lineStart = text.lastIndexOf('\n', startIndex = (selectionIndex - 1).coerceAtLeast(0))
        .let { if (it == -1) 0 else it + 1 }
    val lineEnd = text.indexOf('\n', startIndex = selectionIndex).let { if (it == -1) text.length else it }
    val lineText = text.substring(lineStart, lineEnd)
    val hasUnchecked = lineText.startsWith("[ ]")
    val hasChecked = lineText.startsWith("[x]") || lineText.startsWith("[X]")

    return when {
        hasUnchecked -> {
            val updatedLine = lineText.replaceFirst("[ ]", "[x]")
            val newText = text.substring(0, lineStart) + updatedLine + text.substring(lineEnd)
            copy(text = newText, selection = TextRange(selectionIndex))
        }
        hasChecked -> {
            val updatedLine = lineText.replaceFirst("[x]", "[ ]").replaceFirst("[X]", "[ ]")
            val newText = text.substring(0, lineStart) + updatedLine + text.substring(lineEnd)
            copy(text = newText, selection = TextRange(selectionIndex))
        }
        else -> {
            val newText = text.substring(0, lineStart) + "[ ] " + lineText + text.substring(lineEnd)
            val newCursor = (selectionIndex + 4).coerceAtMost(newText.length)
            copy(text = newText, selection = TextRange(newCursor))
        }
    }
}

private data class InlineTagRange(
    val start: Int,
    val end: Int
)

private data class ColorOption(
    val name: String,
    val color: Color
) {
    val hex: String = color.toHexString()
}

private data class ColorTagRange(
    val hex: String,
    val start: Int,
    val startEnd: Int,
    val end: Int
)

private fun Color.toHexString(): String {
    val rgb = toArgb() and 0xFFFFFF
    return String.format("%06X", rgb)
}

private fun TextFieldValue.toggleInlineFormatting(prefix: String, suffix: String): TextFieldValue {
    val text = text
    val selectionStart = selection.min
    val selectionEnd = selection.max
    if (!selection.collapsed) {
        val selectedText = text.substring(selectionStart, selectionEnd)
        val hasWrapper = selectedText.startsWith(prefix) && selectedText.endsWith(suffix)
        return if (hasWrapper) {
            val unwrapped = selectedText.substring(prefix.length, selectedText.length - suffix.length)
            val newText = text.substring(0, selectionStart) + unwrapped + text.substring(selectionEnd)
            copy(
                text = newText,
                selection = TextRange(selectionStart, selectionStart + unwrapped.length)
            )
        } else {
            wrapSelection(prefix, suffix)
        }
    }

    val enclosing = findEnclosingInlineTag(text, selectionStart, prefix, suffix)
    return if (enclosing != null) {
        removeEnclosingInlineTag(enclosing, prefix, suffix)
    } else {
        insertInlineTagWithCursor(prefix, suffix)
    }
}

private fun TextFieldValue.removeEnclosingInlineTag(
    enclosing: InlineTagRange,
    prefix: String,
    suffix: String
): TextFieldValue {
    val start = enclosing.start
    val end = enclosing.end
    val text = text
    val before = text.substring(0, start)
    val inner = text.substring(start + prefix.length, end).takeIf { it != INLINE_PLACEHOLDER.toString() } ?: ""
    val after = text.substring(end + suffix.length)
    val newText = before + inner + after
    val newCursor = (selection.min - prefix.length).coerceIn(0, newText.length)
    return copy(text = newText, selection = TextRange(newCursor))
}

private fun TextFieldValue.insertInlineTagWithCursor(prefix: String, suffix: String): TextFieldValue {
    val selectionStart = selection.min
    val selectionEnd = selection.max
    val text = text
    val shouldUsePlaceholder = prefix.length == 1 && prefix == suffix
    val middle = if (shouldUsePlaceholder) INLINE_PLACEHOLDER.toString() else ""
    val newText = text.substring(0, selectionStart) + prefix + middle + suffix + text.substring(selectionEnd)
    val newCursor = selectionStart + prefix.length + middle.length
    return copy(text = newText, selection = TextRange(newCursor))
}

private fun TextFieldValue.removeEnclosingColorTag(colorTag: ColorTagRange): TextFieldValue {
    val text = text
    val colorStart = parseColorTagStart(text, colorTag.start) ?: return this
    val (_, openEnd) = colorStart
    val closeStart = colorTag.end
    val closeEnd = closeStart + COLOR_TAG_CLOSE.length
    if (openEnd > text.length || closeEnd > text.length) return this
    val newText = text.removeRange(closeStart, closeEnd).removeRange(colorTag.start, openEnd)
    fun adjustOffset(offset: Int): Int {
        var updated = offset
        if (offset > closeStart) updated -= COLOR_TAG_CLOSE.length
        if (offset > openEnd) updated -= (openEnd - colorTag.start)
        return updated.coerceIn(0, newText.length)
    }
    return copy(
        text = newText,
        selection = TextRange(
            adjustOffset(selection.min),
            adjustOffset(selection.max)
        )
    )
}

private fun findEnclosingInlineTag(
    text: String,
    cursor: Int,
    prefix: String,
    suffix: String
): InlineTagRange? {
    if (cursor < 0 || cursor > text.length) return null
    val openIndex = text.lastIndexOf(prefix, startIndex = (cursor - 1).coerceAtLeast(0))
    if (openIndex == -1) return null
    if (prefix == "*" && suffix == "*") {
        if ((openIndex > 0 && text[openIndex - 1] == '*') || (openIndex + 1 < text.length && text[openIndex + 1] == '*')) {
            return null
        }
    }
    val closeIndex = text.indexOf(suffix, startIndex = cursor)
    if (closeIndex == -1) return null
    if (prefix == "*" && suffix == "*") {
        if ((closeIndex > 0 && text[closeIndex - 1] == '*') || (closeIndex + 1 < text.length && text[closeIndex + 1] == '*')) {
            return null
        }
    }
    if (openIndex + prefix.length > cursor) return null
    return InlineTagRange(openIndex, closeIndex)
}

private fun TextFieldValue.findEnclosingColorTag(cursor: Int): ColorTagRange? {
    var index = 0
    var openHex: String? = null
    var openIndex = 0
    var openEndIndex = 0
    val text = text
    while (index < text.length && index < cursor) {
        val colorStart = parseColorTagStart(text, index)
        if (colorStart != null) {
            val (hex, nextIndex) = colorStart
            openHex = hex
            openIndex = index
            openEndIndex = nextIndex
            index = nextIndex
            continue
        }
        if (text.startsWith(COLOR_TAG_CLOSE, index)) {
            openHex = null
            index += COLOR_TAG_CLOSE.length
            continue
        }
        index += 1
    }

    if (openHex == null) return null
    val closeIndex = text.indexOf(COLOR_TAG_CLOSE, startIndex = cursor)
    if (closeIndex == -1) return null
    if (cursor < openEndIndex) return null
    return ColorTagRange(openHex, openIndex, openEndIndex, closeIndex)
}

private fun TextFieldValue.normalizeColorTagsForNewlines(): TextFieldValue {
    val text = text
    var openColorHex: String? = null
    val builder = StringBuilder(text.length)
    val selectionStart = selection.min
    val selectionEnd = selection.max
    var newSelectionStart = selectionStart
    var newSelectionEnd = selectionEnd
    var index = 0

    while (index < text.length) {
        val colorTagStart = parseColorTagStart(text, index)
        if (colorTagStart != null) {
            val (hex, nextIndex) = colorTagStart
            openColorHex = hex
            builder.append(text.substring(index, nextIndex))
            index = nextIndex
            continue
        }

        if (text.startsWith(COLOR_TAG_CLOSE, index)) {
            openColorHex = null
            builder.append(COLOR_TAG_CLOSE)
            index += COLOR_TAG_CLOSE.length
            continue
        }

        val currentChar = text[index]
        if (currentChar == '\n' && openColorHex != null) {
            builder.append(COLOR_TAG_CLOSE)
            if (index <= selectionStart) {
                newSelectionStart += COLOR_TAG_CLOSE.length
            }
            if (index <= selectionEnd) {
                newSelectionEnd += COLOR_TAG_CLOSE.length
            }
            builder.append('\n')
            val reopenTag = "[color=#${openColorHex}]"
            builder.append(reopenTag)
            if (index < selectionStart) {
                newSelectionStart += reopenTag.length
            }
            if (index < selectionEnd) {
                newSelectionEnd += reopenTag.length
            }
            index += 1
            continue
        }

        builder.append(currentChar)
        index += 1
    }

    val normalizedText = builder.toString()
    if (normalizedText == text) return this
    return copy(
        text = normalizedText,
        selection = TextRange(
            newSelectionStart.coerceIn(0, normalizedText.length),
            newSelectionEnd.coerceIn(0, normalizedText.length)
        )
    )
}

private fun TextFieldValue.sanitizeColorTagEdits(): TextFieldValue {
    val text = text
    if (!text.contains("[color=") && !text.contains(COLOR_TAG_CLOSE)) return this
    val selectionStart = selection.min
    val selectionEnd = selection.max
    val ranges = mutableListOf<IntRange>()
    var index = 0
    while (index < text.length) {
        val colorStart = parseColorTagStart(text, index)
        if (colorStart != null) {
            val (_, nextIndex) = colorStart
            ranges.add(index until nextIndex)
            index = nextIndex
            continue
        }
        if (text.startsWith(COLOR_TAG_CLOSE, index)) {
            ranges.add(index until index + COLOR_TAG_CLOSE.length)
            index += COLOR_TAG_CLOSE.length
            continue
        }
        index += 1
    }
    if (ranges.isEmpty()) return this
    val rangesToRemove = ranges.filter { range ->
        (selectionStart in range) || (selectionEnd in range)
    }
    if (rangesToRemove.isEmpty()) return this
    var newText = text
    var removedBeforeStart = 0
    var removedBeforeEnd = 0
    rangesToRemove.sortedByDescending { it.first }.forEach { range ->
        newText = newText.removeRange(range)
        if (range.first < selectionStart) {
            removedBeforeStart += range.last - range.first + 1
        }
        if (range.first < selectionEnd) {
            removedBeforeEnd += range.last - range.first + 1
        }
    }
    return copy(
        text = newText,
        selection = TextRange(
            (selectionStart - removedBeforeStart).coerceIn(0, newText.length),
            (selectionEnd - removedBeforeEnd).coerceIn(0, newText.length)
        )
    )
}

private fun TextFieldValue.stripInlinePlaceholders(): TextFieldValue {
    val text = text
    if (!text.contains(INLINE_PLACEHOLDER)) return this
    val starResult = stripInlinePlaceholderForMarker(text, '*', selection)
    val underscoreResult = stripInlinePlaceholderForMarker(
        starResult.text,
        '_',
        starResult.selection
    )
    if (underscoreResult.text == text) return this
    return copy(text = underscoreResult.text, selection = underscoreResult.selection)
}

private data class PlaceholderStripResult(
    val text: String,
    val selection: TextRange
)

private fun stripInlinePlaceholderForMarker(
    text: String,
    marker: Char,
    selection: TextRange
): PlaceholderStripResult {
    val placeholderChar = INLINE_PLACEHOLDER
    val builder = StringBuilder(text.length)
    var index = 0
    var removedBeforeStart = 0
    var removedBeforeEnd = 0
    while (index < text.length) {
        if (text[index] != marker || (index + 1 < text.length && text[index + 1] == marker)) {
            builder.append(text[index])
            index += 1
            continue
        }
        val closeIndex = text.indexOf(marker, startIndex = index + 1)
        if (closeIndex == -1) {
            builder.append(text[index])
            index += 1
            continue
        }
        val segment = text.substring(index + 1, closeIndex)
        val hasPlaceholder = segment.contains(placeholderChar)
        val hasContent = segment.any { it != placeholderChar }
        if (hasPlaceholder && hasContent) {
            builder.append(marker)
            segment.forEachIndexed { offset, char ->
                val originalIndex = index + 1 + offset
                if (char == placeholderChar) {
                    if (originalIndex < selection.min) removedBeforeStart += 1
                    if (originalIndex < selection.max) removedBeforeEnd += 1
                } else {
                    builder.append(char)
                }
            }
            builder.append(marker)
        } else {
            builder.append(text.substring(index, closeIndex + 1))
        }
        index = closeIndex + 1
    }
    val newText = builder.toString()
    val newSelection = TextRange(
        (selection.min - removedBeforeStart).coerceIn(0, newText.length),
        (selection.max - removedBeforeEnd).coerceIn(0, newText.length)
    )
    return PlaceholderStripResult(newText, newSelection)
}

private data class CursorFormatState(
    val bold: Boolean,
    val italic: Boolean
)

private fun computeCursorState(text: String, cursor: Int): CursorFormatState {
    var index = 0
    var bold = false
    var italic = false
    var code = false
    while (index < text.length && index < cursor) {
        if (code) {
            if (text[index] == '`') {
                code = false
                index += 1
            } else {
                index += 1
            }
            continue
        }
        val colorStart = parseColorTagStart(text, index)
        if (colorStart != null) {
            index = colorStart.second
            continue
        }
        if (text.startsWith(COLOR_TAG_CLOSE, index)) {
            index += COLOR_TAG_CLOSE.length
            continue
        }
        if (text.startsWith("**", index) || text.startsWith("__", index)) {
            bold = !bold
            index += 2
            continue
        }
        if (text.startsWith("~~", index)) {
            index += 2
            continue
        }
        if (text[index] == '`') {
            code = true
            index += 1
            continue
        }
        if (text[index] == '*' || text[index] == '_') {
            italic = !italic
            index += 1
            continue
        }
        index += 1
    }
    return CursorFormatState(bold = bold, italic = italic)
}

private class RichTextVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val result = buildRichAnnotatedString(text.text)
        return TransformedText(result.annotatedString, result.offsetMapping)
    }
}

private const val INLINE_PLACEHOLDER = '\u2060'
