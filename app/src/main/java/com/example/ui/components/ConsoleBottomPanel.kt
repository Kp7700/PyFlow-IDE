package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ConsoleLogItem
import com.example.ui.ConsoleTab
import com.example.ui.ExecutionStatus
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.PythonYellow

@Composable
fun ConsoleBottomPanel(
    selectedTab: ConsoleTab,
    onTabSelected: (ConsoleTab) -> Unit,
    status: ExecutionStatus,
    outputLogs: List<ConsoleLogItem>,
    standardOutput: String,
    standardError: String,
    isWaitingForInput: Boolean,
    currentInputPrompt: String,
    onSendInput: (String) -> Unit,
    onClear: () -> Unit,
    onStop: () -> Unit,
    isOpen: Boolean,
    onToggleOpen: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current

    // Auto-scroll to bottom when logs update
    LaunchedEffect(outputLogs.size, standardOutput.length, standardError.length) {
        if (outputLogs.isNotEmpty()) {
            listState.animateScrollToItem(outputLogs.size - 1)
        }
    }

    val panelBg = if (isDarkTheme) Color(0xFF13141C) else Color(0xFFF8FAFC)
    val headerBg = if (isDarkTheme) Color(0xFF1E1F2B) else Color(0xFFE2E8F0)
    val borderColor = if (isDarkTheme) Color(0xFF282A36) else Color(0xFFCBD5E1)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(panelBg)
            .border(width = 1.dp, color = borderColor)
    ) {
        // Console Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(headerBg)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Tabs Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ConsoleTabChip(
                    label = "Console",
                    icon = Icons.Default.Terminal,
                    isSelected = selectedTab == ConsoleTab.OUTPUT,
                    onClick = { onTabSelected(ConsoleTab.OUTPUT) },
                    isDarkTheme = isDarkTheme
                )

                ConsoleTabChip(
                    label = "Errors",
                    icon = Icons.Default.ErrorOutline,
                    isSelected = selectedTab == ConsoleTab.ERRORS,
                    badgeCount = if (standardError.isNotEmpty()) 1 else null,
                    badgeColor = AccentRed,
                    onClick = { onTabSelected(ConsoleTab.ERRORS) },
                    isDarkTheme = isDarkTheme
                )

                ConsoleTabChip(
                    label = "Interactive",
                    icon = Icons.Default.Keyboard,
                    isSelected = selectedTab == ConsoleTab.INTERACTIVE,
                    badgeCount = if (isWaitingForInput) 1 else null,
                    badgeColor = PythonYellow,
                    onClick = { onTabSelected(ConsoleTab.INTERACTIVE) },
                    isDarkTheme = isDarkTheme
                )
            }

            // Right side status & action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Status indicator chip
                StatusBadge(status = status, isWaitingForInput = isWaitingForInput)

                // Stop Execution Button (visible when running)
                if (status == ExecutionStatus.RUNNING || isWaitingForInput) {
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("stop_execution_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.StopCircle,
                            contentDescription = "Stop",
                            tint = AccentRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Copy Output
                IconButton(
                    onClick = {
                        val textToCopy = when (selectedTab) {
                            ConsoleTab.OUTPUT -> standardOutput
                            ConsoleTab.ERRORS -> standardError
                            ConsoleTab.INTERACTIVE -> outputLogs.joinToString("") { it.text }
                        }
                        if (textToCopy.isNotEmpty()) {
                            clipboardManager.setText(AnnotatedString(textToCopy))
                        }
                    },
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("copy_console_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Console Output",
                        tint = if (isDarkTheme) Color(0xFFABB2BF) else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Clear Output
                IconButton(
                    onClick = onClear,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("clear_console_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear Console",
                        tint = if (isDarkTheme) Color(0xFFABB2BF) else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Expand / Collapse Toggle
                IconButton(
                    onClick = onToggleOpen,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("toggle_console_btn")
                ) {
                    Icon(
                        imageVector = if (isOpen) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = if (isOpen) "Collapse Console" else "Expand Console",
                        tint = if (isDarkTheme) Color(0xFFECEFF4) else Color(0xFF1E293B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (isOpen) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    ConsoleTab.OUTPUT, ConsoleTab.INTERACTIVE -> {
                        SelectionContainer {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("console_logs_list")
                            ) {
                                if (outputLogs.isEmpty()) {
                                    item {
                                        Text(
                                            text = "Ready. Tap ▶ Run to execute current script.",
                                            color = if (isDarkTheme) Color(0xFF5C6370) else Color(0xFF94A3B8),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.5.sp,
                                            modifier = Modifier.padding(top = 16.dp)
                                        )
                                    }
                                } else {
                                    items(outputLogs) { log ->
                                        val color = when {
                                            log.isError -> AccentRed
                                            log.isInput -> PythonYellow
                                            log.isSystem -> if (isDarkTheme) Color(0xFF61AFEF) else Color(0xFF2563EB)
                                            else -> if (isDarkTheme) Color(0xFFABB2BF) else Color(0xFF1E293B)
                                        }
                                        Text(
                                            text = log.text,
                                            color = color,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            fontWeight = if (log.isInput || log.isSystem) FontWeight.Medium else FontWeight.Normal,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    ConsoleTab.ERRORS -> {
                        SelectionContainer {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                                    .testTag("error_logs_list")
                            ) {
                                if (standardError.isEmpty()) {
                                    item {
                                        Text(
                                            text = "No errors detected. Python syntax and runtime execution are clean! ✨",
                                            color = AccentGreen,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.5.sp,
                                            modifier = Modifier.padding(top = 16.dp)
                                        )
                                    }
                                } else {
                                    item {
                                        Text(
                                            text = standardError,
                                            color = AccentRed,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Input Bar (Visible always when interactive or waiting for input)
            if (isWaitingForInput || selectedTab == ConsoleTab.INTERACTIVE) {
                Surface(
                    color = if (isDarkTheme) Color(0xFF181926) else Color(0xFFE2E8F0),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (currentInputPrompt.isNotEmpty()) {
                            Text(
                                text = currentInputPrompt,
                                color = PythonYellow,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        } else {
                            Text(
                                text = ">>>",
                                color = if (isDarkTheme) Color(0xFF61AFEF) else Color(0xFF2563EB),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    if (isWaitingForInput) "Provide input and tap Send..." else "Enter Python interactive input...",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isWaitingForInput) PythonYellow else MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = if (isDarkTheme) Color(0xFF383A4C) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("console_input_field"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Button(
                            onClick = {
                                val toSend = inputText
                                inputText = ""
                                onSendInput(toSend)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isWaitingForInput) PythonYellow else MaterialTheme.colorScheme.primaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("submit_input_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Input",
                                tint = if (isWaitingForInput) Color.Black else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsoleTabChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    badgeCount: Int? = null,
    badgeColor: Color = AccentRed,
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) {
            if (isDarkTheme) Color(0xFF282A36) else Color.White
        } else Color.Transparent,
        modifier = Modifier.height(30.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) {
                    if (isDarkTheme) Color.White else Color.Black
                } else Color.Gray
            )

            if (badgeCount != null) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(badgeColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeCount.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ExecutionStatus, isWaitingForInput: Boolean) {
    val (text, color) = when {
        isWaitingForInput -> "Input Needed" to PythonYellow
        status == ExecutionStatus.RUNNING -> "Running" to AccentGreen
        status == ExecutionStatus.SUCCESS -> "Done" to AccentGreen
        status == ExecutionStatus.ERROR -> "Failed" to AccentRed
        status == ExecutionStatus.STOPPED -> "Stopped" to Color.Gray
        else -> "Idle" to Color.Gray
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.height(22.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
            Text(
                text = text,
                color = color,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
