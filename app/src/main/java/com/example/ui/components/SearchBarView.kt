package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SearchBarView(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    replaceQuery: String,
    onReplaceQueryChange: (String) -> Unit,
    matchesCount: Int,
    currentMatchIndex: Int,
    onNextMatch: () -> Unit,
    onPrevMatch: () -> Unit,
    onReplaceCurrent: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isDarkTheme) Color(0xFF1E1F2B) else Color(0xFFF1F5F9)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = bgColor,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Search row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Find in file...", fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("search_input_field"),
                    shape = RoundedCornerShape(8.dp)
                )

                // Match count indicator
                if (searchQuery.isNotEmpty()) {
                    Text(
                        text = if (matchesCount > 0) "${currentMatchIndex + 1}/$matchesCount" else "0/0",
                        fontSize = 12.sp,
                        color = if (matchesCount > 0) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                IconButton(
                    onClick = onPrevMatch,
                    enabled = matchesCount > 0,
                    modifier = Modifier.size(32.dp).testTag("prev_match_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Previous Match",
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onNextMatch,
                    enabled = matchesCount > 0,
                    modifier = Modifier.size(32.dp).testTag("next_match_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Next Match",
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp).testTag("close_search_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Search",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Replace row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = replaceQuery,
                    onValueChange = onReplaceQueryChange,
                    placeholder = { Text("Replace with...", fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FindReplace,
                            contentDescription = "Replace",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("replace_input_field"),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedButton(
                    onClick = onReplaceCurrent,
                    enabled = matchesCount > 0,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp).testTag("replace_btn")
                ) {
                    Text("Replace", fontSize = 11.sp)
                }

                Button(
                    onClick = onReplaceAll,
                    enabled = matchesCount > 0,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp).testTag("replace_all_btn")
                ) {
                    Text("All", fontSize = 11.sp)
                }
            }
        }
    }
}
