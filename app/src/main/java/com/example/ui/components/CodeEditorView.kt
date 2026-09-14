package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.editor.PythonVisualTransformation
import com.example.ui.theme.DarkEditorBackground
import com.example.ui.theme.DarkLineNumber
import com.example.ui.theme.LightEditorBackground
import com.example.ui.theme.LightLineNumber

@Composable
fun CodeEditorView(
    editorValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    searchQuery: String = "",
    currentMatchIndex: Int = -1,
    isDarkTheme: Boolean = true,
    fontSize: Float = 14f,
    showLineNumbers: Boolean = true,
    wordWrap: Boolean = false,
    modifier: Modifier = Modifier
) {
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    val linesCount by remember(editorValue.text) {
        derivedStateOf {
            val count = editorValue.text.count { it == '\n' } + 1
            maxOf(count, 1)
        }
    }

    val visualTransformation = remember(searchQuery, currentMatchIndex) {
        PythonVisualTransformation(searchQuery, currentMatchIndex)
    }

    val editorBg = if (isDarkTheme) DarkEditorBackground else LightEditorBackground
    val lineNumberColor = if (isDarkTheme) DarkLineNumber else LightLineNumber
    val textColor = if (isDarkTheme) Color(0xFFABB2BF) else Color(0xFF24292F)
    val cursorColor = MaterialTheme.colorScheme.primary
    val effectiveLineHeight = (fontSize * 1.45f).sp

    val lineDigits = linesCount.toString().length
    val lineGutterWidth = (lineDigits * 9 + 20).dp

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(editorBg)
            .verticalScroll(verticalScroll)
    ) {
        // Line Numbers Gutter
        if (showLineNumbers) {
            Column(
                modifier = Modifier
                    .width(lineGutterWidth)
                    .background(if (isDarkTheme) Color(0xFF14151E) else Color(0xFFE2E8F0))
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..linesCount) {
                    Text(
                        text = i.toString(),
                        color = lineNumberColor,
                        fontSize = (fontSize - 1f).sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        lineHeight = effectiveLineHeight,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth().padding(end = 4.dp)
                    )
                }
            }
        }

        // Code Editor Text Field
        val textContainerModifier = if (wordWrap) {
            Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 24.dp)
        } else {
            Modifier
                .weight(1f)
                .horizontalScroll(horizontalScroll)
                .padding(start = 12.dp, end = 24.dp, top = 12.dp, bottom = 24.dp)
        }

        Box(modifier = textContainerModifier) {
            BasicTextField(
                value = editorValue,
                onValueChange = onValueChange,
                visualTransformation = visualTransformation,
                textStyle = TextStyle(
                    color = textColor,
                    fontSize = fontSize.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Normal,
                    lineHeight = effectiveLineHeight
                ),
                cursorBrush = SolidColor(cursorColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("code_editor_field")
            )
        }
    }
}
