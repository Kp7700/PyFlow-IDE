package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.ui.DialogState
import com.example.ui.IDEUiState
import com.example.ui.IDEViewModel
import com.example.ui.ThemeMode
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PythonBlue
import com.example.ui.theme.PythonYellow
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: IDEViewModel,
    uiState: IDEUiState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Appearance Section
            item {
                SettingsSection(title = "Appearance & Theme") {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Color Theme",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = uiState.themeMode == ThemeMode.DARK,
                                onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                                icon = {
                                    if (uiState.themeMode == ThemeMode.DARK) {
                                        Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            ) {
                                Text("Dark", fontSize = 12.sp)
                            }
                            SegmentedButton(
                                selected = uiState.themeMode == ThemeMode.LIGHT,
                                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                                icon = {
                                    if (uiState.themeMode == ThemeMode.LIGHT) {
                                        Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            ) {
                                Text("Light", fontSize = 12.sp)
                            }
                            SegmentedButton(
                                selected = uiState.themeMode == ThemeMode.SYSTEM,
                                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                                icon = {
                                    if (uiState.themeMode == ThemeMode.SYSTEM) {
                                        Icon(Icons.Default.SettingsBrightness, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            ) {
                                Text("System", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Editor Preferences Section
            item {
                SettingsSection(title = "Editor Preferences") {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        // Font Size Slider
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Font Size",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${uiState.editorFontSize.roundToInt()} sp",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp
                                )
                            }

                            Slider(
                                value = uiState.editorFontSize,
                                onValueChange = { viewModel.setEditorFontSize(it) },
                                valueRange = 11f..22f,
                                steps = 10,
                                modifier = Modifier.testTag("settings_font_size_slider")
                            )

                            // Live Preview Box
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (uiState.isDarkTheme) Color(0xFF14151E) else Color(0xFFF1F5F9),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "def hello():\n    print(\"Preview: Python 3\")",
                                    fontSize = uiState.editorFontSize.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (uiState.isDarkTheme) PythonYellow else PythonBlue,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))

                        // Show Line Numbers Switch
                        SettingsSwitchItem(
                            title = "Line Numbers",
                            description = "Display line count in the editor gutter",
                            icon = Icons.Default.FormatListNumbered,
                            checked = uiState.showLineNumbers,
                            onCheckedChange = { viewModel.toggleShowLineNumbers() }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))

                        // Word Wrap Switch
                        SettingsSwitchItem(
                            title = "Word Wrap",
                            description = "Wrap long code lines instead of scrolling horizontally",
                            icon = Icons.Default.WrapText,
                            checked = uiState.wordWrap,
                            onCheckedChange = { viewModel.toggleWordWrap() }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))

                        // Auto-Save Switch
                        SettingsSwitchItem(
                            title = "Auto-Save Before Run",
                            description = "Automatically save editor changes before running scripts",
                            icon = Icons.Default.Save,
                            checked = uiState.autoSave,
                            onCheckedChange = { viewModel.toggleAutoSave() }
                        )
                    }
                }
            }

            // Python Engine Details Section
            item {
                SettingsSection(title = "Python Runtime & Engine") {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AccentGreen.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AccentGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Native Offline Interpreter",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Embedded Kotlin AST Engine • Zero Network",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Supported Modules:",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "math, random, time, datetime, sys, json, re, os",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Starter Templates & Actions
            item {
                SettingsSection(title = "Resources") {
                    Column {
                        ListItem(
                            headlineContent = { Text("Code Templates & Examples") },
                            supportingContent = { Text("Explore calculator, algorithms & OOP samples") },
                            leadingContent = {
                                Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingContent = {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.testTag("settings_browse_templates")
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        ListItem(
                            headlineContent = { Text("About PyPocket") },
                            supportingContent = { Text("Version 1.0.0 • Material 3 Design") },
                            leadingContent = {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.testTag("settings_about_app")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )
        ElevatedCard(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(description, fontSize = 12.sp) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag("switch_$title")
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
