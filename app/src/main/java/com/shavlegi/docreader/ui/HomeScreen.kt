package com.shavlegi.docreader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shavlegi.docreader.R
import com.shavlegi.docreader.model.DocType
import com.shavlegi.docreader.model.RecentFile

private enum class HomeFilter { ALL, PDF, OFFICE, TEXT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    recents: List<RecentFile>,
    onOpenFileClick: () -> Unit,
    onRecentClick: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearRecents: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(HomeFilter.ALL) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val filtered = remember(recents, query, filter) {
        recents.filter { file ->
            val matchesQuery = query.isBlank() ||
                file.name.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                HomeFilter.ALL -> true
                HomeFilter.PDF -> file.docType == DocType.PDF
                HomeFilter.OFFICE -> file.docType.isOffice
                HomeFilter.TEXT -> file.docType.isText
            }
            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onOpenFileClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Filled.FolderOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = stringResource(R.string.open_file), fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.open_file_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = filter == HomeFilter.ALL,
                onClick = { filter = HomeFilter.ALL },
                label = { Text(stringResource(R.string.filter_all)) }
            )
            FilterChip(
                selected = filter == HomeFilter.PDF,
                onClick = { filter = HomeFilter.PDF },
                label = { Text(stringResource(R.string.filter_pdf)) }
            )
            FilterChip(
                selected = filter == HomeFilter.OFFICE,
                onClick = { filter = HomeFilter.OFFICE },
                label = { Text(stringResource(R.string.filter_office)) }
            )
            FilterChip(
                selected = filter == HomeFilter.TEXT,
                onClick = { filter = HomeFilter.TEXT },
                label = { Text(stringResource(R.string.filter_text)) }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.recent_files),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (recents.isNotEmpty()) {
                TextButton(onClick = { showClearConfirm = true }) {
                    Text(stringResource(R.string.clear_all))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (recents.isEmpty()) {
                        stringResource(R.string.no_recent_files)
                    } else {
                        stringResource(R.string.no_search_results)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.uri }) { file ->
                    RecentFileCard(
                        file = file,
                        onClick = { onRecentClick(file.uri) },
                        onDelete = { onRemoveRecent(file.uri) }
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            text = { Text(stringResource(R.string.clear_recents_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    onClearRecents()
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentFileCard(
    file: RecentFile,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FormatBadge(file.docType)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = file.docType.label + "  •  " + formatSize(file.size) +
                        "  •  " + formatDateLabel(file.lastOpened),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
