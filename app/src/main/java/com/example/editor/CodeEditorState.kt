package com.example.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

data class SearchResult(
    val query: String = "",
    val matches: List<IntRange> = emptyList(),
    val currentIndex: Int = -1
)

class EditorHistoryManager(private val maxHistory: Int = 50) {
    private val undoStack = ArrayDeque<TextFieldValue>()
    private val redoStack = ArrayDeque<TextFieldValue>()
    private var lastRecordedText: String = ""

    fun recordChange(value: TextFieldValue) {
        if (value.text == lastRecordedText) return
        undoStack.add(value)
        if (undoStack.size > maxHistory) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        lastRecordedText = value.text
    }

    fun canUndo(): Boolean = undoStack.size > 1
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo(current: TextFieldValue): TextFieldValue? {
        if (!canUndo()) return null
        val popped = undoStack.removeLast()
        redoStack.add(popped)
        val previous = undoStack.last()
        lastRecordedText = previous.text
        return previous
    }

    fun redo(): TextFieldValue? {
        if (!canRedo()) return null
        val next = redoStack.removeLast()
        undoStack.add(next)
        lastRecordedText = next.text
        return next
    }
}

object CodeEditorHelper {

    fun handleEnterKey(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val selection = current.selection
        val cursor = selection.start.coerceIn(0, text.length)

        // Find current line before cursor
        val lineStart = text.lastIndexOf('\n', cursor - 1).let { if (it == -1) 0 else it + 1 }
        val currentLineBeforeCursor = text.substring(lineStart, cursor)

        // Count leading spaces
        val leadingSpaces = currentLineBeforeCursor.takeWhile { it == ' ' }.length
        val endsWithColon = currentLineBeforeCursor.trimEnd().endsWith(':')

        val indentCount = if (endsWithColon) leadingSpaces + 4 else leadingSpaces
        val indent = " ".repeat(indentCount)

        val newText = text.substring(0, cursor) + "\n" + indent + text.substring(selection.end.coerceIn(0, text.length))
        val newCursor = cursor + 1 + indentCount

        return TextFieldValue(
            text = newText,
            selection = TextRange(newCursor, newCursor)
        )
    }

    fun insertTextAtCursor(current: TextFieldValue, insert: String): TextFieldValue {
        val text = current.text
        val selection = current.selection
        val start = selection.min.coerceIn(0, text.length)
        val end = selection.max.coerceIn(0, text.length)

        // If wrapping with brackets or quotes
        if (start != end) {
            val selectedText = text.substring(start, end)
            val wrapped = when (insert) {
                "(" -> "($selectedText)"
                "[" -> "[$selectedText]"
                "{" -> "{$selectedText}"
                "\"" -> "\"$selectedText\""
                "'" -> "'$selectedText'"
                else -> insert + text.substring(start, end)
            }
            val newText = text.substring(0, start) + wrapped + text.substring(end)
            return TextFieldValue(
                text = newText,
                selection = TextRange(start, start + wrapped.length)
            )
        }

        val newText = text.substring(0, start) + insert + text.substring(end)
        val newPos = start + insert.length
        return TextFieldValue(
            text = newText,
            selection = TextRange(newPos, newPos)
        )
    }

    fun findMatches(text: String, query: String): List<IntRange> {
        if (query.isEmpty() || text.isEmpty()) return emptyList()
        val regex = Regex(Regex.escape(query), RegexOption.IGNORE_CASE)
        return regex.findAll(text).map { it.range }.toList()
    }

    fun replaceMatch(
        current: TextFieldValue,
        matchRange: IntRange,
        replacement: String
    ): TextFieldValue {
        val text = current.text
        if (matchRange.first < 0 || matchRange.last >= text.length) return current
        val newText = text.substring(0, matchRange.first) + replacement + text.substring(matchRange.last + 1)
        val newPos = matchRange.first + replacement.length
        return TextFieldValue(
            text = newText,
            selection = TextRange(newPos, newPos)
        )
    }

    fun replaceAll(
        current: TextFieldValue,
        query: String,
        replacement: String
    ): TextFieldValue {
        if (query.isEmpty()) return current
        val regex = Regex(Regex.escape(query), RegexOption.IGNORE_CASE)
        val newText = regex.replace(current.text, replacement)
        return TextFieldValue(
            text = newText,
            selection = TextRange(0, 0)
        )
    }
}
