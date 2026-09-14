package com.example.ui

import android.app.Application
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DefaultTemplates
import com.example.data.PyFileManager
import com.example.editor.CodeEditorHelper
import com.example.editor.EditorHistoryManager
import com.example.pyengine.PyFileIO
import com.example.pyengine.PyInterpreter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class IDEViewModel(application: Application) : AndroidViewModel(application) {

    private val fileManager = PyFileManager(application)
    private val fileIO = PyFileIO(fileManager.scriptsDir)
    private val historyManager = EditorHistoryManager()

    private val _uiState = MutableStateFlow(IDEUiState())
    val uiState: StateFlow<IDEUiState> = _uiState.asStateFlow()

    private var executionJob: Job? = null
    private var inputDeferred: CompletableDeferred<String>? = null

    init {
        loadInitialFiles()
    }

    private fun loadInitialFiles() {
        val files = fileManager.listFiles()
        val defaultFile = files.firstOrNull { it.name == "main.py" } ?: files.firstOrNull()
        val activeName = defaultFile?.name ?: "main.py"
        val content = if (defaultFile != null) fileManager.readFile(activeName) else DefaultTemplates.MAIN_PY

        val initialValue = TextFieldValue(content, TextRange(0, 0))
        historyManager.recordChange(initialValue)

        _uiState.update {
            it.copy(
                files = files,
                activeFileName = activeName,
                openTabs = listOf(activeName),
                editorValue = initialValue,
                isModified = false
            )
        }
    }

    fun onEditorValueChanged(newValue: TextFieldValue) {
        val oldText = _uiState.value.editorValue.text
        if (newValue.text != oldText) {
            historyManager.recordChange(newValue)
            _uiState.update {
                it.copy(
                    editorValue = newValue,
                    isModified = true
                )
            }
            updateSearchMatches()
        } else {
            _uiState.update { it.copy(editorValue = newValue) }
        }
    }

    fun undo() {
        val current = _uiState.value.editorValue
        val undone = historyManager.undo(current)
        if (undone != null) {
            _uiState.update {
                it.copy(
                    editorValue = undone,
                    isModified = true
                )
            }
        }
    }

    fun redo() {
        val redone = historyManager.redo()
        if (redone != null) {
            _uiState.update {
                it.copy(
                    editorValue = redone,
                    isModified = true
                )
            }
        }
    }

    fun insertTextAtCursor(textToInsert: String) {
        val current = _uiState.value.editorValue
        val updated = CodeEditorHelper.insertTextAtCursor(current, textToInsert)
        onEditorValueChanged(updated)
    }

    fun handleEnterKey() {
        val current = _uiState.value.editorValue
        val updated = CodeEditorHelper.handleEnterKey(current)
        onEditorValueChanged(updated)
    }

    fun selectTab(tab: ConsoleTab) {
        _uiState.update { it.copy(selectedConsoleTab = tab) }
    }

    fun toggleConsole() {
        _uiState.update { it.copy(isConsoleOpen = !it.isConsoleOpen) }
    }

    fun clearOutput() {
        _uiState.update {
            it.copy(
                standardOutput = "",
                standardError = "",
                consoleLogs = emptyList(),
                executionStatus = ExecutionStatus.IDLE
            )
        }
    }

    fun toggleTheme() {
        _uiState.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    fun openFile(fileName: String) {
        // Save current file if modified
        if (_uiState.value.isModified) {
            saveCurrentFile()
        }

        val content = fileManager.readFile(fileName)
        val newValue = TextFieldValue(content, TextRange(0, 0))
        historyManager.recordChange(newValue)

        val currentTabs = _uiState.value.openTabs.toMutableList()
        if (!currentTabs.contains(fileName)) {
            currentTabs.add(fileName)
        }

        _uiState.update {
            it.copy(
                activeFileName = fileName,
                openTabs = currentTabs,
                editorValue = newValue,
                isModified = false
            )
        }
        updateSearchMatches()
    }

    fun closeTab(fileName: String) {
        val currentTabs = _uiState.value.openTabs.toMutableList()
        currentTabs.remove(fileName)

        if (currentTabs.isEmpty()) {
            val allFiles = fileManager.listFiles()
            val fallback = allFiles.firstOrNull()?.name ?: "main.py"
            currentTabs.add(fallback)
        }

        val newActive = if (_uiState.value.activeFileName == fileName) {
            currentTabs.last()
        } else {
            _uiState.value.activeFileName
        }

        val content = fileManager.readFile(newActive)
        val newValue = TextFieldValue(content, TextRange(0, 0))

        _uiState.update {
            it.copy(
                activeFileName = newActive,
                openTabs = currentTabs,
                editorValue = newValue,
                isModified = false
            )
        }
    }

    fun saveCurrentFile() {
        val state = _uiState.value
        val success = fileManager.saveFile(state.activeFileName, state.editorValue.text)
        if (success) {
            val updatedFiles = fileManager.listFiles()
            _uiState.update {
                it.copy(
                    files = updatedFiles,
                    isModified = false,
                    snackbarMessage = "Saved ${state.activeFileName}"
                )
            }
        }
    }

    fun createNewFile(fileName: String) {
        var cleanName = fileName.trim()
        if (!cleanName.endsWith(".py")) cleanName += ".py"

        val initialContent = "# $cleanName\n# Created with PyPocket\n\nprint(\"Hello from $cleanName!\")\n"
        val success = fileManager.createFile(cleanName, initialContent)
        if (success) {
            val updatedFiles = fileManager.listFiles()
            _uiState.update { it.copy(files = updatedFiles) }
            openFile(cleanName)
            dismissDialog()
        } else {
            _uiState.update { it.copy(snackbarMessage = "File $cleanName already exists") }
        }
    }

    fun renameCurrentFile(newName: String) {
        var cleanName = newName.trim()
        if (!cleanName.endsWith(".py")) cleanName += ".py"

        val oldName = _uiState.value.activeFileName
        val success = fileManager.renameFile(oldName, cleanName)
        if (success) {
            val updatedFiles = fileManager.listFiles()
            val updatedTabs = _uiState.value.openTabs.map { if (it == oldName) cleanName else it }
            _uiState.update {
                it.copy(
                    files = updatedFiles,
                    activeFileName = cleanName,
                    openTabs = updatedTabs,
                    snackbarMessage = "Renamed to $cleanName"
                )
            }
            dismissDialog()
        } else {
            _uiState.update { it.copy(snackbarMessage = "Cannot rename to $cleanName") }
        }
    }

    fun deleteFile(fileName: String) {
        val success = fileManager.deleteFile(fileName)
        if (success) {
            val updatedFiles = fileManager.listFiles()
            val updatedTabs = _uiState.value.openTabs.filter { it != fileName }
            val nextActive = if (_uiState.value.activeFileName == fileName) {
                updatedTabs.firstOrNull() ?: updatedFiles.firstOrNull()?.name ?: "main.py"
            } else {
                _uiState.value.activeFileName
            }

            if (!fileManager.scriptsDir.resolve(nextActive).exists()) {
                fileManager.createFile("main.py", DefaultTemplates.MAIN_PY)
            }

            val nextContent = fileManager.readFile(nextActive)
            val nextValue = TextFieldValue(nextContent, TextRange(0, 0))

            _uiState.update {
                it.copy(
                    files = fileManager.listFiles(),
                    activeFileName = nextActive,
                    openTabs = if (updatedTabs.isEmpty()) listOf(nextActive) else updatedTabs,
                    editorValue = nextValue,
                    isModified = false,
                    snackbarMessage = "Deleted $fileName"
                )
            }
            dismissDialog()
        }
    }

    fun loadTemplate(templateName: String, content: String) {
        val success = fileManager.saveFile(templateName, content)
        if (success) {
            val updatedFiles = fileManager.listFiles()
            _uiState.update { it.copy(files = updatedFiles) }
            openFile(templateName)
            dismissDialog()
        }
    }

    fun runCode() {
        if (_uiState.value.executionStatus == ExecutionStatus.RUNNING) {
            return
        }

        // Auto-save before run
        saveCurrentFile()

        val codeToRun = _uiState.value.editorValue.text
        val fileName = _uiState.value.activeFileName

        _uiState.update {
            it.copy(
                executionStatus = ExecutionStatus.RUNNING,
                standardOutput = "",
                standardError = "",
                consoleLogs = listOf(
                    ConsoleLogItem(
                        text = ">>> Running $fileName ...",
                        isSystem = true
                    )
                ),
                isWaitingForInput = false,
                currentInputPrompt = "",
                selectedConsoleTab = ConsoleTab.OUTPUT,
                isConsoleOpen = true
            )
        }

        val startTime = System.currentTimeMillis()

        executionJob = viewModelScope.launch(Dispatchers.Default) {
            val interpreter = PyInterpreter(
                fileIO = fileIO,
                stdout = { text ->
                    _uiState.update { current ->
                        val newOut = current.standardOutput + text
                        val newLogs = current.consoleLogs + ConsoleLogItem(text = text)
                        current.copy(standardOutput = newOut, consoleLogs = newLogs)
                    }
                },
                stderr = { errorText ->
                    _uiState.update { current ->
                        val newErr = if (current.standardError.isEmpty()) errorText else current.standardError + "\n" + errorText
                        val newLogs = current.consoleLogs + ConsoleLogItem(text = errorText, isError = true)
                        current.copy(
                            standardError = newErr,
                            consoleLogs = newLogs,
                            selectedConsoleTab = ConsoleTab.ERRORS
                        )
                    }
                },
                onInputPrompt = { prompt ->
                    _uiState.update {
                        it.copy(
                            isWaitingForInput = true,
                            currentInputPrompt = prompt,
                            executionStatus = ExecutionStatus.WAITING_FOR_INPUT,
                            selectedConsoleTab = ConsoleTab.INTERACTIVE
                        )
                    }

                    val deferred = CompletableDeferred<String>()
                    inputDeferred = deferred
                    val result = deferred.await()
                    inputDeferred = null

                    _uiState.update {
                        it.copy(
                            isWaitingForInput = false,
                            currentInputPrompt = "",
                            executionStatus = ExecutionStatus.RUNNING
                        )
                    }
                    result
                }
            )

            try {
                interpreter.execute(codeToRun, fileName)
                val duration = System.currentTimeMillis() - startTime
                _uiState.update {
                    it.copy(
                        executionStatus = ExecutionStatus.SUCCESS,
                        executionTimeMs = duration,
                        consoleLogs = it.consoleLogs + ConsoleLogItem(
                            text = "\n[Process finished in ${duration}ms]",
                            isSystem = true
                        )
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                _uiState.update {
                    it.copy(
                        executionStatus = ExecutionStatus.STOPPED,
                        consoleLogs = it.consoleLogs + ConsoleLogItem(
                            text = "\n[Execution stopped by user]",
                            isSystem = true
                        )
                    )
                }
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                _uiState.update {
                    it.copy(
                        executionStatus = ExecutionStatus.ERROR,
                        executionTimeMs = duration,
                        selectedConsoleTab = ConsoleTab.ERRORS
                    )
                }
            }
        }
    }

    fun submitInput(input: String) {
        _uiState.update {
            it.copy(
                consoleLogs = it.consoleLogs + ConsoleLogItem(
                    text = input + "\n",
                    isInput = true
                )
            )
        }
        inputDeferred?.complete(input)
    }

    fun stopExecution() {
        inputDeferred?.cancel()
        inputDeferred = null
        executionJob?.cancel()
        executionJob = null
        _uiState.update {
            it.copy(
                executionStatus = ExecutionStatus.STOPPED,
                isWaitingForInput = false,
                currentInputPrompt = ""
            )
        }
    }

    // Search & Replace
    fun toggleSearchBar() {
        _uiState.update {
            val nextVisible = !it.isSearchBarVisible
            it.copy(
                isSearchBarVisible = nextVisible,
                searchQuery = if (!nextVisible) "" else it.searchQuery,
                replaceQuery = if (!nextVisible) "" else it.replaceQuery
            )
        }
        updateSearchMatches()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        updateSearchMatches()
    }

    fun onReplaceQueryChanged(replace: String) {
        _uiState.update { it.copy(replaceQuery = replace) }
    }

    private fun updateSearchMatches() {
        val state = _uiState.value
        val matches = CodeEditorHelper.findMatches(state.editorValue.text, state.searchQuery)
        _uiState.update {
            it.copy(
                searchMatchesCount = matches.size,
                currentSearchMatchIndex = if (matches.isNotEmpty()) it.currentSearchMatchIndex.coerceIn(0, matches.size - 1) else 0
            )
        }
    }

    fun nextSearchMatch() {
        val state = _uiState.value
        if (state.searchMatchesCount > 0) {
            val nextIdx = (state.currentSearchMatchIndex + 1) % state.searchMatchesCount
            _uiState.update { it.copy(currentSearchMatchIndex = nextIdx) }
            jumpToMatch(nextIdx)
        }
    }

    fun prevSearchMatch() {
        val state = _uiState.value
        if (state.searchMatchesCount > 0) {
            val prevIdx = if (state.currentSearchMatchIndex - 1 < 0) state.searchMatchesCount - 1 else state.currentSearchMatchIndex - 1
            _uiState.update { it.copy(currentSearchMatchIndex = prevIdx) }
            jumpToMatch(prevIdx)
        }
    }

    private fun jumpToMatch(index: Int) {
        val state = _uiState.value
        val matches = CodeEditorHelper.findMatches(state.editorValue.text, state.searchQuery)
        if (index in matches.indices) {
            val range = matches[index]
            _uiState.update {
                it.copy(
                    editorValue = it.editorValue.copy(
                        selection = TextRange(range.first, range.last + 1)
                    )
                )
            }
        }
    }

    fun replaceCurrentMatch() {
        val state = _uiState.value
        val matches = CodeEditorHelper.findMatches(state.editorValue.text, state.searchQuery)
        if (state.currentSearchMatchIndex in matches.indices) {
            val targetRange = matches[state.currentSearchMatchIndex]
            val updated = CodeEditorHelper.replaceMatch(state.editorValue, targetRange, state.replaceQuery)
            onEditorValueChanged(updated)
        }
    }

    fun replaceAllMatches() {
        val state = _uiState.value
        val updated = CodeEditorHelper.replaceAll(state.editorValue, state.searchQuery, state.replaceQuery)
        onEditorValueChanged(updated)
    }

    fun showDialog(dialog: DialogState) {
        _uiState.update { it.copy(dialogState = dialog) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialogState = null) }
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun openFileAndNavigate(fileName: String) {
        openFile(fileName)
        navigateTo(AppScreen.EDITOR)
    }

    fun setConsoleOpen(open: Boolean) {
        _uiState.update { it.copy(isConsoleOpen = open) }
    }

    fun setThemeMode(mode: ThemeMode) {
        val isDark = when (mode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> true
        }
        _uiState.update { it.copy(themeMode = mode, isDarkTheme = isDark) }
    }

    fun setEditorFontSize(size: Float) {
        _uiState.update { it.copy(editorFontSize = size.coerceIn(10f, 24f)) }
    }

    fun toggleShowLineNumbers() {
        _uiState.update { it.copy(showLineNumbers = !it.showLineNumbers) }
    }

    fun toggleWordWrap() {
        _uiState.update { it.copy(wordWrap = !it.wordWrap) }
    }

    fun toggleAutoSave() {
        _uiState.update { it.copy(autoSave = !it.autoSave) }
    }

    fun setFileSearchQuery(query: String) {
        _uiState.update { it.copy(fileSearchQuery = query) }
    }

    fun setFileSortOrder(order: FileSortOrder) {
        _uiState.update { it.copy(fileSortOrder = order) }
    }

    fun toggleGridView() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
