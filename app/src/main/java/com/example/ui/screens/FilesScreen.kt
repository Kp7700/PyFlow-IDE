package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PyFileModel
import com.example.ui.AppScreen
import com.example.ui.DialogState
import com.example.ui.FileSortOrder
import com.example.ui.IDEUiState
import com.example.ui.IDEViewModel
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PythonBlue
import com.example.ui.theme.PythonYellow
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    viewModel: IDEViewModel,
    uiState: IDEUiState,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()) }

    // Filter and sort files
    val filteredFiles = remember(uiState.files, uiState.fileSearchQuery, uiState.fileSortOrder) {
        val query = uiState.fileSearchQuery.trim().lowercase()
        val list = if (query.isEmpty()) {
            uiState.files
        } else {
            uiState.files.filter { it.name.lowercase().contains(query) }
        }

        when (uiState.fileSortOrder) {
            FileSortOrder.DATE_DESC -> list.sortedByDescending { it.lastModified }
            FileSortOrder.DATE_ASC -> list.sortedBy { it.lastModified }
            FileSortOrder.NAME_ASC -> list.sortedBy { it.name.lowercase() }
            FileSortOrder.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "My Python Files",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(filteredFiles.size.toString(), fontSize = 11.sp)
                        }
                    }
                },
                actions = {
                    // Grid / List Toggle
                    IconButton(
                        onClick = { viewModel.toggleGridView() },
                        modifier = Modifier.testTag("toggle_grid_view_btn")
                    ) {
                        Icon(
                            imageVector = if (uiState.isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = if (uiState.isGridView) "List View" else "Grid View"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showDialog(DialogState.NewFile) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New File") },
                shape = RoundedCornerShape(16.dp),
                containerColor = if (uiState.isDarkTheme) PythonYellow else PythonBlue,
                contentColor = if (uiState.isDarkTheme) Color(0xFF1E1F2B) else Color.White,
                modifier = Modifier
                    .padding(bottom = 70.dp)
                    .testTag("files_fab_new_file")
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding()
                )
        ) {
            // Search Bar & Filter Chips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = uiState.fileSearchQuery,
                    onValueChange = { viewModel.setFileSearchQuery(it) },
                    placeholder = { Text("Search python files...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (uiState.fileSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setFileSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("files_search_field")
                )

                // Sort Options Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.fileSortOrder == FileSortOrder.DATE_DESC,
                        onClick = { viewModel.setFileSortOrder(FileSortOrder.DATE_DESC) },
                        label = { Text("Recent", fontSize = 12.sp) },
                        leadingIcon = if (uiState.fileSortOrder == FileSortOrder.DATE_DESC) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        shape = RoundedCornerShape(8.dp)
                    )
                    FilterChip(
                        selected = uiState.fileSortOrder == FileSortOrder.NAME_ASC,
                        onClick = { viewModel.setFileSortOrder(FileSortOrder.NAME_ASC) },
                        label = { Text("Name (A-Z)", fontSize = 12.sp) },
                        leadingIcon = if (uiState.fileSortOrder == FileSortOrder.NAME_ASC) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // File Listing
            if (filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Text(
                            text = if (uiState.fileSearchQuery.isNotEmpty()) "No files match \"${uiState.fileSearchQuery}\"" else "No Python files found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (uiState.isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredFiles, key = { it.name }) { file ->
                        FileGridItem(
                            file = file,
                            dateFormat = dateFormat,
                            isDarkTheme = uiState.isDarkTheme,
                            onOpen = { viewModel.openFileAndNavigate(file.name) },
                            onRun = {
                                viewModel.openFile(file.name)
                                viewModel.navigateTo(AppScreen.EDITOR)
                                viewModel.runCode()
                            },
                            onRename = { viewModel.showDialog(DialogState.RenameFile(file.name)) },
                            onDelete = { viewModel.showDialog(DialogState.DeleteFile(file.name)) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredFiles, key = { it.name }) { file ->
                        FileListItem(
                            file = file,
                            dateFormat = dateFormat,
                            isDarkTheme = uiState.isDarkTheme,
                            onOpen = { viewModel.openFileAndNavigate(file.name) },
                            onRun = {
                                viewModel.openFile(file.name)
                                viewModel.navigateTo(AppScreen.EDITOR)
                                viewModel.runCode()
                            },
                            onRename = { viewModel.showDialog(DialogState.RenameFile(file.name)) },
                            onDelete = { viewModel.showDialog(DialogState.DeleteFile(file.name)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileListItem(
    file: PyFileModel,
    dateFormat: SimpleDateFormat,
    isDarkTheme: Boolean,
    onOpen: () -> Unit,
    onRun: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    ElevatedCard(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isDarkTheme) Color(0xFF1E1F2B) else Color(0xFFFFFFFF)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("file_list_item_${file.name}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isDarkTheme) Color(0xFF282A36) else Color(0xFFEBF3FC),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = ".py",
                        color = if (isDarkTheme) PythonYellow else PythonBlue,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${file.sizeBytes} B • ${dateFormat.format(Date(file.lastModified))}",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Quick Run Button
            IconButton(
                onClick = onRun,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Run",
                    tint = AccentGreen,
                    modifier = Modifier.size(22.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Open") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onOpen()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileGridItem(
    file: PyFileModel,
    dateFormat: SimpleDateFormat,
    isDarkTheme: Boolean,
    onOpen: () -> Unit,
    onRun: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isDarkTheme) Color(0xFF1E1F2B) else Color(0xFFFFFFFF)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("file_grid_item_${file.name}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkTheme) Color(0xFF282A36) else Color(0xFFEBF3FC),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = ".py",
                            color = if (isDarkTheme) PythonYellow else PythonBlue,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Open") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpen()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onRename()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Text(
                text = file.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${file.sizeBytes} B",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalIconButton(
                    onClick = onRun,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = AccentGreen.copy(alpha = 0.15f),
                        contentColor = AccentGreen
                    ),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
