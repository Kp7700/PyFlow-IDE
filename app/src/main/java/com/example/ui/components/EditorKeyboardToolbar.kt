package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditorKeyboardToolbar(
    onInsertText: (String) -> Unit,
    onEnterKey: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val quickSymbols = listOf(
        "TAB" to "    ",
        ":" to ":",
        "(" to "(",
        ")" to ")",
        "[" to "[",
        "]" to "]",
        "{" to "{",
        "}" to "}",
        "\"" to "\"",
        "'" to "'",
        "=" to "=",
        "==" to " == ",
        "!=" to " != ",
        "+" to "+",
        "-" to "-",
        "*" to "*",
        "/" to "/",
        "%" to "%",
        "<" to "<",
        ">" to ">",
        "," to ", ",
        "." to ".",
        "#" to "# ",
        "_" to "_",
        "->" to " -> "
    )

    val quickKeywords = listOf(
        "def" to "def ",
        "class" to "class ",
        "if" to "if ",
        "elif" to "elif ",
        "else" to "else:\n    ",
        "for" to "for ",
        "while" to "while ",
        "in" to " in ",
        "return" to "return ",
        "import" to "import ",
        "print()" to "print()",
        "input()" to "input(\"\")",
        "len()" to "len()",
        "range()" to "range()"
    )

    val bgColor = if (isDarkTheme) Color(0xFF181926) else Color(0xFFE2E8F0)
    val btnBgColor = if (isDarkTheme) Color(0xFF282A36) else Color(0xFFFFFFFF)
    val textColor = if (isDarkTheme) Color(0xFFECEFF4) else Color(0xFF1E293B)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(bgColor)
            .horizontalScroll(scrollState)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Undo / Redo
        IconButton(
            onClick = onUndo,
            modifier = Modifier
                .size(34.dp)
                .testTag("undo_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Undo",
                tint = textColor,
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(
            onClick = onRedo,
            modifier = Modifier
                .size(34.dp)
                .testTag("redo_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Redo,
                contentDescription = "Redo",
                tint = textColor,
                modifier = Modifier.size(18.dp)
            )
        }

        VerticalDivider(
            modifier = Modifier
                .height(22.dp)
                .padding(horizontal = 2.dp),
            color = if (isDarkTheme) Color(0xFF4C4F69) else Color(0xFFCBD5E1)
        )

        // Symbols
        for ((label, text) in quickSymbols) {
            Surface(
                onClick = { onInsertText(text) },
                shape = RoundedCornerShape(6.dp),
                color = if (label == "TAB") MaterialTheme.colorScheme.primaryContainer else btnBgColor,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .height(32.dp)
                    .defaultMinSize(minWidth = 32.dp)
                    .testTag("key_$label")
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (label == "TAB") MaterialTheme.colorScheme.onPrimaryContainer else textColor,
                        fontSize = if (label.length > 2) 11.sp else 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        VerticalDivider(
            modifier = Modifier
                .height(22.dp)
                .padding(horizontal = 2.dp),
            color = if (isDarkTheme) Color(0xFF4C4F69) else Color(0xFFCBD5E1)
        )

        // Keywords
        for ((label, text) in quickKeywords) {
            Surface(
                onClick = { onInsertText(text) },
                shape = RoundedCornerShape(6.dp),
                color = btnBgColor,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .height(32.dp)
                    .testTag("kw_$label")
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
