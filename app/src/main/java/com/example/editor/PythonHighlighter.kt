package com.example.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

object PythonSyntaxTheme {
    val Keyword = Color(0xFFC678DD)       // Purple
    val Builtin = Color(0xFF61AFEF)       // Blue
    val StringColor = Color(0xFF98C379)   // Green
    val NumberColor = Color(0xFFD19A66)   // Amber/Orange
    val CommentColor = Color(0xFF7E8490)  // Slate Muted Gray
    val Definition = Color(0xFFE5C07B)    // Gold
    val Decorator = Color(0xFFE06C75)     // Red/Coral
    val Operator = Color(0xFF56B6C2)      // Cyan
    val Self = Color(0xFFE06C75)          // Coral
    val SearchMatch = Color(0xFF64501E)   // Gold highlight background
    val SearchMatchCurrent = Color(0xFFA16207) // Active match background
}

class PythonVisualTransformation(
    private val searchQuery: String = "",
    private val currentMatchIndex: Int = -1
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = highlightPythonCode(text.text, searchQuery, currentMatchIndex)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

fun highlightPythonCode(
    code: String,
    searchQuery: String = "",
    currentMatchIndex: Int = -1
): AnnotatedString {
    return buildAnnotatedString {
        append(code)

        if (code.isEmpty()) return@buildAnnotatedString

        // 1. Strings (multiline """ and ''' and regular " and ')
        val stringRegex = Regex("(\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''|f?\"(?:\\\\.|[^\"\\\\])*\"|f?'(?:\\\\.|[^'\\\\])*')")
        val stringMatches = stringRegex.findAll(code).toList()
        for (m in stringMatches) {
            addStyle(SpanStyle(color = PythonSyntaxTheme.StringColor), m.range.first, m.range.last + 1)
        }

        // 2. Comments (# to end of line, unless inside string)
        val commentRegex = Regex("#.*")
        for (m in commentRegex.findAll(code)) {
            val isInsideString = stringMatches.any { sm -> m.range.first >= sm.range.first && m.range.first <= sm.range.last }
            if (!isInsideString) {
                addStyle(
                    SpanStyle(
                        color = PythonSyntaxTheme.CommentColor,
                        fontStyle = FontStyle.Italic
                    ),
                    m.range.first,
                    m.range.last + 1
                )
            }
        }

        // 3. Keywords
        val keywords = setOf(
            "def", "class", "return", "if", "elif", "else", "while", "for", "in",
            "try", "except", "finally", "raise", "import", "from", "as", "pass",
            "break", "continue", "lambda", "global", "nonlocal", "and", "or", "not",
            "is", "with", "yield", "assert", "True", "False", "None"
        )
        val wordRegex = Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b")
        for (m in wordRegex.findAll(code)) {
            val word = m.value
            val isInsideString = stringMatches.any { sm -> m.range.first >= sm.range.first && m.range.first <= sm.range.last }
            if (!isInsideString) {
                if (word in keywords) {
                    addStyle(
                        SpanStyle(
                            color = PythonSyntaxTheme.Keyword,
                            fontWeight = FontWeight.Bold
                        ),
                        m.range.first,
                        m.range.last + 1
                    )
                } else if (word == "self" || word == "cls") {
                    addStyle(
                        SpanStyle(
                            color = PythonSyntaxTheme.Self,
                            fontStyle = FontStyle.Italic
                        ),
                        m.range.first,
                        m.range.last + 1
                    )
                } else if (isBuiltinFunction(word)) {
                    addStyle(
                        SpanStyle(
                            color = PythonSyntaxTheme.Builtin,
                            fontWeight = FontWeight.Medium
                        ),
                        m.range.first,
                        m.range.last + 1
                    )
                }
            }
        }

        // 4. Function and Class names (after def or class)
        val defRegex = Regex("\\b(def|class)\\s+([A-Za-z_][A-Za-z0-9_]*)")
        for (m in defRegex.findAll(code)) {
            val nameGroup = m.groups[2]
            if (nameGroup != null) {
                val isInsideString = stringMatches.any { sm -> nameGroup.range.first >= sm.range.first && nameGroup.range.first <= sm.range.last }
                if (!isInsideString) {
                    addStyle(
                        SpanStyle(
                            color = PythonSyntaxTheme.Definition,
                            fontWeight = FontWeight.Bold
                        ),
                        nameGroup.range.first,
                        nameGroup.range.last + 1
                    )
                }
            }
        }

        // 5. Decorators
        val decoratorRegex = Regex("@[A-Za-z_][A-Za-z0-9_.]*")
        for (m in decoratorRegex.findAll(code)) {
            val isInsideString = stringMatches.any { sm -> m.range.first >= sm.range.first && m.range.first <= sm.range.last }
            if (!isInsideString) {
                addStyle(SpanStyle(color = PythonSyntaxTheme.Decorator), m.range.first, m.range.last + 1)
            }
        }

        // 6. Numbers (integers, floats, hex, bin)
        val numberRegex = Regex("\\b(0[xX][0-9a-fA-F]+|0[bB][01]+|\\d+(\\.\\d+)?([eE][+-]?\\d+)?)\\b")
        for (m in numberRegex.findAll(code)) {
            val isInsideString = stringMatches.any { sm -> m.range.first >= sm.range.first && m.range.first <= sm.range.last }
            if (!isInsideString) {
                addStyle(SpanStyle(color = PythonSyntaxTheme.NumberColor), m.range.first, m.range.last + 1)
            }
        }

        // 7. Search matches highlight
        if (searchQuery.isNotEmpty()) {
            val searchRegex = Regex(Regex.escape(searchQuery), RegexOption.IGNORE_CASE)
            var matchIdx = 0
            for (m in searchRegex.findAll(code)) {
                val isCurrent = matchIdx == currentMatchIndex
                val bgColor = if (isCurrent) PythonSyntaxTheme.SearchMatchCurrent else PythonSyntaxTheme.SearchMatch
                addStyle(
                    SpanStyle(
                        background = bgColor,
                        color = Color.White,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                    ),
                    m.range.first,
                    m.range.last + 1
                )
                matchIdx++
            }
        }
    }
}

private fun isBuiltinFunction(name: String): Boolean {
    return name in setOf(
        "print", "input", "len", "range", "type", "isinstance", "issubclass",
        "str", "int", "float", "bool", "list", "dict", "set", "tuple",
        "abs", "round", "min", "max", "sum", "sorted", "reversed", "enumerate",
        "zip", "map", "filter", "any", "all", "chr", "ord", "bin", "hex", "oct",
        "open", "dir", "vars", "hasattr", "getattr", "setattr", "delattr",
        "callable", "pow", "divmod", "hash", "id"
    )
}
