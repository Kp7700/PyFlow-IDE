package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DefaultTemplates
import com.example.ui.DialogState
import com.example.ui.theme.PythonBlue
import com.example.ui.theme.PythonYellow

@Composable
fun IDEDialogContainer(
    dialogState: DialogState?,
    onDismiss: () -> Unit,
    onCreateFile: (String) -> Unit,
    onRenameFile: (String) -> Unit,
    onDeleteFile: (String) -> Unit,
    onLoadTemplate: (String, String) -> Unit
) {
    when (dialogState) {
        is DialogState.NewFile -> {
            var fileName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Create New Python File") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Enter script name (e.g. script.py):", fontSize = 13.sp)
                        OutlinedTextField(
                            value = fileName,
                            onValueChange = { fileName = it },
                            placeholder = { Text("my_script.py") },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_file_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (fileName.isNotBlank()) {
                                onCreateFile(fileName.trim())
                            }
                        },
                        enabled = fileName.isNotBlank(),
                        modifier = Modifier.testTag("confirm_create_file")
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )
        }

        is DialogState.RenameFile -> {
            var fileName by remember { mutableStateOf(dialogState.currentName) }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Rename File") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Enter new name for ${dialogState.currentName}:", fontSize = 13.sp)
                        OutlinedTextField(
                            value = fileName,
                            onValueChange = { fileName = it },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rename_file_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (fileName.isNotBlank()) {
                                onRenameFile(fileName.trim())
                            }
                        },
                        enabled = fileName.isNotBlank() && fileName != dialogState.currentName,
                        modifier = Modifier.testTag("confirm_rename_file")
                    ) {
                        Text("Rename")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )
        }

        is DialogState.DeleteFile -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Delete File?") },
                text = {
                    Text("Are you sure you want to delete '${dialogState.fileName}'? This action cannot be undone.")
                },
                confirmButton = {
                    Button(
                        onClick = { onDeleteFile(dialogState.fileName) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_delete_file")
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )
        }

        is DialogState.Templates -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AutoStories, contentDescription = null, tint = PythonYellow)
                        Text("Starter Python Samples")
                    }
                },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(DefaultTemplates.ALL_TEMPLATES) { (name, content) ->
                            val desc = when (name) {
                                "main.py" -> "Introduction: Math, loops, lists, and functions"
                                "calculator.py" -> "Interactive CLI Calculator with error handling"
                                "interactive_input.py" -> "Real-time user profile & quiz with input()"
                                "data_structures.py" -> "Lists, dicts, sets, tuples, and slicing"
                                "oop_classes.py" -> "Classes, inheritance, and OOP methods"
                                "algorithms.py" -> "Fibonacci, primes, and runtime benchmarking"
                                else -> "Python script"
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = name,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = desc,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { onLoadTemplate(name, content) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Load", fontSize = 11.5.sp)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            )
        }

        is DialogState.About -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PythonBlue,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Py",
                                    color = PythonYellow,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Text("About PyPocket")
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "PyPocket is a complete Python IDE and runtime engine built natively for Android in Kotlin.",
                            fontSize = 13.sp
                        )
                        HorizontalDivider()
                        Text(
                            text = "⚡ Key Capabilities:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                        val features = listOf(
                            "• Real-time local execution (no internet or server required)",
                            "• Interactive terminal with input() support",
                            "• Live syntax highlighting, line numbers, and search & replace",
                            "• Built-in standard modules: math, random, time, datetime, sys, json, re, os",
                            "• File system sandboxed I/O with open() reading & writing",
                            "• Full control flow: if/elif/else, for/while, try/except/finally, def/class/lambda",
                            "• Quick symbols & keywords accessory keyboard toolbar"
                        )
                        for (f in features) {
                            Text(text = f, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = onDismiss) { Text("Got it") }
                }
            )
        }

        null -> { /* no dialog */ }
    }
}
