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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

private val colorOptions = listOf(
    ColorOption("Red", "D32F2F"),
    ColorOption("Orange", "F57C00"),
    ColorOption("Green", "388E3C"),
    ColorOption("Blue", "1976D2"),
    ColorOption("Purple", "7B1FA2")
)

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
                                fieldValue = fieldValue.applyCheckboxToggle().also {
                                    onValueChange(it.text)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckBox,
                                contentDescription = "Insert checkbox"
                            )
                        }
                        IconButton(
                            onClick = {
                                fieldValue = fieldValue.wrapSelection("**", "**").also {
                                    onValueChange(it.text)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FormatBold,
                                contentDescription = "Bold"
                            )
                        }
                        IconButton(
                            onClick = {
                                fieldValue = fieldValue.wrapSelection("*", "*").also {
                                    onValueChange(it.text)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FormatItalic,
                                contentDescription = "Italic"
                            )
                        }
                        IconButton(
                            onClick = { colorMenuExpanded = true }
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
                                DropdownMenuItem(
                                    text = {
                                        Row {
                                            Text(option.name)
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "●",
                                                color = option.color,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    },
                                    onClick = {
                                        fieldValue = fieldValue.wrapSelection(
                                            "[color=#${option.hex}]",
                                            "[/color]"
                                        ).also {
                                            onValueChange(it.text)
                                        }
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
                        fieldValue = it
                        onValueChange(it.text)
                    },
                    label = { Text(label) },
                    modifier = textFieldModifier,
                    minLines = minLines,
                    singleLine = singleLine
                )
            } else {
                TextField(
                    value = fieldValue,
                    onValueChange = {
                        fieldValue = it
                        onValueChange(it.text)
                    },
                    label = { Text(label) },
                    modifier = textFieldModifier,
                    minLines = minLines,
                    singleLine = singleLine
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

private data class ColorOption(
    val name: String,
    val hex: String
) {
    val color = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#$hex"))
}
