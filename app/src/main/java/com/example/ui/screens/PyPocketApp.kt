package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.IDEUiState
import com.example.ui.IDEViewModel
import com.example.ui.components.IDEDialogContainer

@Composable
fun PyPocketApp(
    viewModel: IDEViewModel,
    uiState: IDEUiState,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = if (uiState.currentScreen == AppScreen.EDITOR) 40.dp else 80.dp)
            )
        },
        bottomBar = {
            // Show standard Material 3 NavigationBar only on top-level tabs (Home, Files, Settings)
            if (uiState.currentScreen != AppScreen.EDITOR) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier.testTag("app_bottom_navigation_bar")
                ) {
                    NavigationBarItem(
                        selected = uiState.currentScreen == AppScreen.HOME,
                        onClick = { viewModel.navigateTo(AppScreen.HOME) },
                        icon = {
                            Icon(
                                imageVector = if (uiState.currentScreen == AppScreen.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = { Text("Home", fontSize = 12.sp) },
                        modifier = Modifier.testTag("nav_item_home")
                    )

                    NavigationBarItem(
                        selected = uiState.currentScreen == AppScreen.FILES,
                        onClick = { viewModel.navigateTo(AppScreen.FILES) },
                        icon = {
                            Icon(
                                imageVector = if (uiState.currentScreen == AppScreen.FILES) Icons.Filled.Folder else Icons.Outlined.Folder,
                                contentDescription = "Files"
                            )
                        },
                        label = { Text("Files", fontSize = 12.sp) },
                        modifier = Modifier.testTag("nav_item_files")
                    )

                    NavigationBarItem(
                        selected = uiState.currentScreen == AppScreen.SETTINGS,
                        onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        icon = {
                            Icon(
                                imageVector = if (uiState.currentScreen == AppScreen.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Settings"
                            )
                        },
                        label = { Text("Settings", fontSize = 12.sp) },
                        modifier = Modifier.testTag("nav_item_settings")
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (uiState.currentScreen != AppScreen.EDITOR) innerPadding.calculateBottomPadding() else 0.dp)
        ) {
            AnimatedContent(
                targetState = uiState.currentScreen,
                transitionSpec = {
                    if (targetState == AppScreen.EDITOR) {
                        (slideInVertically { it / 4 } + fadeIn()).togetherWith(fadeOut())
                    } else if (initialState == AppScreen.EDITOR) {
                        fadeIn().togetherWith(slideOutVertically { it / 4 } + fadeOut())
                    } else {
                        fadeIn().togetherWith(fadeOut())
                    }
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    AppScreen.HOME -> HomeScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        modifier = Modifier.fillMaxSize()
                    )

                    AppScreen.FILES -> FilesScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        modifier = Modifier.fillMaxSize()
                    )

                    AppScreen.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        modifier = Modifier.fillMaxSize()
                    )

                    AppScreen.EDITOR -> EditorScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        onBack = { viewModel.navigateTo(AppScreen.HOME) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Centralized Material 3 Dialog Container
    IDEDialogContainer(
        dialogState = uiState.dialogState,
        onDismiss = { viewModel.dismissDialog() },
        onCreateFile = { name ->
            viewModel.createNewFile(name)
            viewModel.navigateTo(AppScreen.EDITOR)
        },
        onRenameFile = { newName -> viewModel.renameCurrentFile(newName) },
        onDeleteFile = { name -> viewModel.deleteFile(name) },
        onLoadTemplate = { name, content ->
            viewModel.loadTemplate(name, content)
            viewModel.navigateTo(AppScreen.EDITOR)
        }
    )
}
