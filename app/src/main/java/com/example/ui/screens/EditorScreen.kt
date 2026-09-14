package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.ui.components.CodeEditorView
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.PythonBlue
import com.example.ui.theme.PythonYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: IDEViewModel,
    uiState: IDEUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var moreMenuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showOutputSheet by remember { mutableStateOf(false) }

    // Automatically show output sheet when execution starts or waits for input
    LaunchedEffect(uiState.executionStatus) {
        if (uiState.executionStatus == ExecutionStatus.RUNNING || uiState.executionStatus == ExecutionStatus.WAITING_FOR_INPUT) {
            showOutputSheet = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("editor_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = uiState.activeFileName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (uiState.isModified) {
                            Surface(
                                shape = CircleShape,
                                color = if (uiState.isDarkTheme) PythonYellow else PythonBlue,
                                modifier = Modifier.size(8.dp)
                            ) {}
                        }
                    }
                },
                actions = {
                    // Undo & Redo
                    IconButton(
                        onClick = { viewModel.undo() },
                        modifier = Modifier.size(36.dp).testTag("editor_undo_btn")
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo", modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = { viewModel.redo() },
                        modifier = Modifier.size(36.dp).testTag("editor_redo_btn")
                    ) {
                        Icon(Icons.Default.Redo, contentDescription = "Redo", modifier = Modifier.size(20.dp))
                    }

                    // Search & Replace Toggle
                    IconButton(
                        onClick = { viewModel.toggleSearchBar() },
                        modifier = Modifier.size(36.dp).testTag("editor_search_btn")
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Find & Replace",
                            tint = if (uiState.isSearchBarVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Save Button
                    IconButton(
                        onClick = { viewModel.saveCurrentFile() },
                        modifier = Modifier.size(36.dp).testTag("editor_save_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(20.dp))
                    }

                    // Run / Stop Action Button
                    if (uiState.executionStatus == ExecutionStatus.RUNNING || uiState.executionStatus == ExecutionStatus.WAITING_FOR_INPUT) {
                        FilledTonalButton(
                            onClick = { viewModel.stopExecution() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp).testTag("editor_stop_btn")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                showOutputSheet = true
                                viewModel.runCode()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp).testTag("editor_run_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Run", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Overflow Menu
                    Box {
                        IconButton(
                            onClick = { moreMenuExpanded = true },
                            modifier = Modifier.size(36.dp).testTag("editor_more_btn")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }

                        DropdownMenu(
                            expanded = moreMenuExpanded,
                            onDismissRequest = { moreMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Output Console") },
                                leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null) },
                                onClick = {
                                    moreMenuExpanded = false
                                    showOutputSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Load Template") },
                                leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                                onClick = {
                                    moreMenuExpanded = false
                                    viewModel.showDialog(DialogState.Templates)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Console") },
                                leadingIcon = { Icon(Icons.Default.ClearAll, contentDescription = null) },
                                onClick = {
                                    moreMenuExpanded = false
                                    viewModel.clearOutput()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename File") },
                                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                                onClick = {
                                    moreMenuExpanded = false
                                    viewModel.showDialog(DialogState.RenameFile(uiState.activeFileName))
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Modern Bottom Action & Accessory Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Accessory Keyboard Symbols Toolbar
                EditorKeyboardToolbar(
                    onInsertSymbol = { symbol -> viewModel.insertTextAtCursor(symbol) },
                    onEnter = { viewModel.handleEnterKey() }
                )

                HorizontalDivider()

                // Bottom Output Trigger Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showOutputSheet = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("output_sheet_trigger_bar"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when (uiState.executionStatus) {
                                ExecutionStatus.RUNNING, ExecutionStatus.WAITING_FOR_INPUT -> PythonYellow
                                ExecutionStatus.SUCCESS -> AccentGreen
                                ExecutionStatus.ERROR -> AccentRed
                                else -> MaterialTheme.colorScheme.outline
                            },
                            modifier = Modifier.size(8.dp)
                        ) {}

                        Text(
                            text = when (uiState.executionStatus) {
                                ExecutionStatus.RUNNING -> "Running script..."
                                ExecutionStatus.WAITING_FOR_INPUT -> "Waiting for input..."
                                ExecutionStatus.SUCCESS -> "Process completed (${uiState.executionTimeMs}ms)"
                                ExecutionStatus.ERROR -> "Execution error"
                                ExecutionStatus.STOPPED -> "Execution stopped"
                                else -> "Output Console"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "View Output",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Expand Output",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search and Replace Expandable Bar
            AnimatedVisibility(visible = uiState.isSearchBarVisible) {
                EditorSearchBar(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                    replaceQuery = uiState.replaceQuery,
                    onReplaceQueryChange = { viewModel.onReplaceQueryChanged(it) },
                    matchesCount = uiState.searchMatchesCount,
                    currentIndex = uiState.currentSearchMatchIndex,
                    onNext = { viewModel.nextSearchMatch() },
                    onPrev = { viewModel.prevSearchMatch() },
                    onReplace = { viewModel.replaceCurrentMatch() },
                    onReplaceAll = { viewModel.replaceAllMatches() },
                    onClose = { viewModel.toggleSearchBar() }
                )
            }

            // Main Code Editor Workspace
            CodeEditorView(
                editorValue = uiState.editorValue,
                onValueChange = { viewModel.onEditorValueChanged(it) },
                searchQuery = if (uiState.isSearchBarVisible) uiState.searchQuery else "",
                currentMatchIndex = if (uiState.isSearchBarVisible) uiState.currentSearchMatchIndex else -1,
                isDarkTheme = uiState.isDarkTheme,
                fontSize = uiState.editorFontSize,
                showLineNumbers = uiState.showLineNumbers,
                wordWrap = uiState.wordWrap,
                modifier = Modifier.fillMaxSize().weight(1f)
            )
        }

        // Modern Material 3 Output Modal Bottom Sheet
        if (showOutputSheet) {
            ModalBottomSheet(
                onDismissRequest = { showOutputSheet = false },
                sheetState = bottomSheetState,
                containerColor = if (uiState.isDarkTheme) Color(0xFF181924) else Color(0xFFF8FAFC),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = {
                    BottomSheetDefaults.DragHandle(
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                },
                modifier = Modifier.testTag("output_modal_bottom_sheet")
            ) {
                OutputSheetContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    onCopyOutput = {
                        val textToCopy = uiState.consoleLogs.joinToString("\n") { it.text }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("PyPocket Output", textToCopy)
                        clipboard.setPrimaryClip(clip)
                    }
                )
            }
        }
    }
}

@Composable
private fun EditorKeyboardToolbar(
    onInsertSymbol: (String) -> Unit,
    onEnter: () -> Unit
) {
    val symbols = remember {
        listOf("    ", ":", "(", ")", "[", "]", "{", "}", "=", "+", "-", "*", "/", "\"", "'", "_", "def ", "return ", "print(", "input(")
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("editor_keyboard_accessory_bar")
    ) {
        items(symbols) { sym ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .height(34.dp)
                    .clickable { onInsertSymbol(sym) }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    Text(
                        text = if (sym == "    ") "Tab ⇥" else sym,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun OutputSheetContent(
    uiState: IDEUiState,
    viewModel: IDEViewModel,
    onCopyOutput: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.75f)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Output Sheet Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Output & Console",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Status Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (uiState.executionStatus) {
                        ExecutionStatus.RUNNING -> PythonYellow.copy(alpha = 0.2f)
                        ExecutionStatus.WAITING_FOR_INPUT -> PythonBlue.copy(alpha = 0.2f)
                        ExecutionStatus.SUCCESS -> AccentGreen.copy(alpha = 0.2f)
                        ExecutionStatus.ERROR -> AccentRed.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = when (uiState.executionStatus) {
                            ExecutionStatus.RUNNING -> "Running"
                            ExecutionStatus.WAITING_FOR_INPUT -> "Awaiting Input"
                            ExecutionStatus.SUCCESS -> "Finished (${uiState.executionTimeMs}ms)"
                            ExecutionStatus.ERROR -> "Error"
                            ExecutionStatus.STOPPED -> "Stopped"
                            else -> "Idle"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (uiState.executionStatus) {
                            ExecutionStatus.RUNNING -> PythonYellow
                            ExecutionStatus.WAITING_FOR_INPUT -> PythonBlue
                            ExecutionStatus.SUCCESS -> AccentGreen
                            ExecutionStatus.ERROR -> AccentRed
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Quick Actions: Copy & Clear
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onCopyOutput, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Output", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { viewModel.clearOutput() }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ClearAll, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                }
            }
        }

        // Running Indicator Bar
        if (uiState.executionStatus == ExecutionStatus.RUNNING) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Interactive Console Input Prompt (When isWaitingForInput)
        if (uiState.isWaitingForInput) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = uiState.currentInputPrompt.ifEmpty { "stdin >" },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Type input and submit...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("console_interactive_input_field")
                    )
                    Button(
                        onClick = {
                            viewModel.submitInput(inputText)
                            inputText = ""
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("console_submit_input_btn")
                    ) {
                        Text("Send", fontSize = 12.sp)
                    }
                }
            }
        }

        // Monospace Output Logs Console
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (uiState.isDarkTheme) Color(0xFF10111A) else Color(0xFFFFFFFF),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (uiState.isDarkTheme) Color(0xFF282A36) else Color(0xFFE2E8F0)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (uiState.consoleLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Run your code to see output here",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize().testTag("console_output_log_list")
                ) {
                    items(uiState.consoleLogs) { log ->
                        val logColor = when {
                            log.isError -> AccentRed
                            log.isSystem -> MaterialTheme.colorScheme.primary
                            log.isInput -> PythonYellow
                            else -> if (uiState.isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                        }

                        Text(
                            text = log.text,
                            color = logColor,
                            fontSize = 12.5.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    replaceQuery: String,
    onReplaceQueryChange: (String) -> Unit,
    matchesCount: Int,
    currentIndex: Int,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Find Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Find...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).height(46.dp)
                )

                Text(
                    text = if (matchesCount > 0) "${currentIndex + 1}/$matchesCount" else "0/0",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                IconButton(onClick = onPrev, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous")
                }
                IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next")
                }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Replace Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = replaceQuery,
                    onValueChange = onReplaceQueryChange,
                    placeholder = { Text("Replace with...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).height(46.dp)
                )

                OutlinedButton(
                    onClick = onReplace,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Replace", fontSize = 11.sp)
                }

                Button(
                    onClick = onReplaceAll,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("All", fontSize = 11.sp)
                }
            }
        }
    }
}
