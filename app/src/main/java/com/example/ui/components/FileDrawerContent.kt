package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.data.PyFileModel
import com.example.ui.theme.PythonBlue
import com.example.ui.theme.PythonYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FileDrawerContent(
    files: List<PyFileModel>,
    activeFileName: String,
    onFileSelected: (String) -> Unit,
    onNewFileClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onRenameClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onAboutClick: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isDarkTheme) Color(0xFF181926) else Color(0xFFF1F5F9)
    val cardBg = if (isDarkTheme) Color(0xFF202130) else Color.White
    val textColor = if (isDarkTheme) Color(0xFFECEFF4) else Color(0xFF1E293B)
    val subtitleColor = if (isDarkTheme) Color(0xFF8B92A2) else Color(0xFF64748B)

    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(bgColor)
            .padding(16.dp)
    ) {
        // App / Project Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PythonBlue,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Py",
                            color = PythonYellow,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Column {
                    Text(
                        text = "PyPocket Files",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "${files.size} script(s)",
                        fontSize = 11.sp,
                        color = subtitleColor
                    )
                }
            }

            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier.size(32.dp).testTag("drawer_theme_toggle")
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Theme",
                    tint = if (isDarkTheme) PythonYellow else Color(0xFF475569),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        HorizontalDivider(
            color = if (isDarkTheme) Color(0xFF383A4C) else Color(0xFFCBD5E1),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Action Buttons: New File & Starter Templates
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onNewFileClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("drawer_new_file_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "New File",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            OutlinedButton(
                onClick = onTemplatesClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("drawer_templates_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Samples", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // File List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(files, key = { it.name }) { file ->
                val isActive = file.name == activeFileName
                var menuExpanded by remember { mutableStateOf(false) }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else cardBg,
                    border = if (isActive) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                    tonalElevation = if (isActive) 2.dp else 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFileSelected(file.name) }
                        .testTag("file_item_${file.name}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = if (isActive) PythonYellow else PythonBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = file.name,
                                    fontSize = 13.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace,
                                    color = textColor,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${file.sizeBytes} B • ${dateFormat.format(Date(file.lastModified))}",
                                    fontSize = 10.sp,
                                    color = subtitleColor
                                )
                            }
                        }

                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("file_menu_${file.name}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "File Actions",
                                    tint = subtitleColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Open") },
                                    leadingIcon = { Icon(Icons.Default.FileOpen, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onFileSelected(file.name)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Rename") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onRenameClick(file.name)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        menuExpanded = false
                                        onDeleteClick(file.name)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            color = if (isDarkTheme) Color(0xFF383A4C) else Color(0xFFCBD5E1),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Bottom About PyPocket button
        OutlinedButton(
            onClick = onAboutClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .testTag("about_pypocket_btn"),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("About PyPocket Python", fontSize = 12.sp)
        }
    }
}
