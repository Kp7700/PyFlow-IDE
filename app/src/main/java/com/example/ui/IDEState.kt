package com.example.ui

import androidx.compose.ui.text.input.TextFieldValue
import com.example.data.PyFileModel

enum class AppScreen {
    HOME,
    FILES,
    EDITOR,
    SETTINGS
}

enum class FileSortOrder {
    DATE_DESC,
    DATE_ASC,
    NAME_ASC,
    NAME_DESC
}

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

enum class ConsoleTab {
    OUTPUT,
    ERRORS,
    INTERACTIVE
}

enum class ExecutionStatus {
    IDLE,
    RUNNING,
    WAITING_FOR_INPUT,
    SUCCESS,
    ERROR,
    STOPPED
}

data class ConsoleLogItem(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isError: Boolean = false,
    val isInput: Boolean = false,
    val isSystem: Boolean = false
)

sealed interface DialogState {
    data object NewFile : DialogState
    data class RenameFile(val currentName: String) : DialogState
    data class DeleteFile(val fileName: String) : DialogState
    data object Templates : DialogState
    data object About : DialogState
}

data class IDEUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val files: List<PyFileModel> = emptyList(),
    val activeFileName: String = "main.py",
    val openTabs: List<String> = listOf("main.py"),
    val editorValue: TextFieldValue = TextFieldValue(""),
    val isModified: Boolean = false,
    val selectedConsoleTab: ConsoleTab = ConsoleTab.OUTPUT,
    val executionStatus: ExecutionStatus = ExecutionStatus.IDLE,
    val standardOutput: String = "",
    val standardError: String = "",
    val consoleLogs: List<ConsoleLogItem> = emptyList(),
    val isWaitingForInput: Boolean = false,
    val currentInputPrompt: String = "",
    val executionTimeMs: Long = 0L,
    val isDarkTheme: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val editorFontSize: Float = 14f,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
    val autoSave: Boolean = true,
    val fileSearchQuery: String = "",
    val fileSortOrder: FileSortOrder = FileSortOrder.DATE_DESC,
    val isGridView: Boolean = false,
    val isSearchBarVisible: Boolean = false,
    val searchQuery: String = "",
    val replaceQuery: String = "",
    val searchMatchesCount: Int = 0,
    val currentSearchMatchIndex: Int = 0,
    val isConsoleOpen: Boolean = false,
    val consoleHeightRatio: Float = 0.42f,
    val dialogState: DialogState? = null,
    val snackbarMessage: String? = null
)
