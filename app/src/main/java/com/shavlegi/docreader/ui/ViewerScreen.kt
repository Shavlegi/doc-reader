package com.shavlegi.docreader.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shavlegi.docreader.R
import com.shavlegi.docreader.data.DocContent
import com.shavlegi.docreader.data.PickedFile
import com.shavlegi.docreader.model.DocType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    file: PickedFile,
    type: DocType,
    content: DocContent?,
    isLoading: Boolean,
    error: DocError?,
    onClose: () -> Unit,
    onSaveText: (String, (Boolean) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf(false) }
    var editedText by remember { mutableStateOf("") }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val originalText = (content as? DocContent.PlainText)?.text.orEmpty()
    val isDirty = editing && editedText != originalText

    // Entering edit mode loads the current text into the editor.
    LaunchedEffect(editing, content) {
        if (editing) editedText = originalText
    }

    BackHandler {
        when {
            showDiscardDialog -> showDiscardDialog = false
            isDirty -> showDiscardDialog = true
            editing -> editing = false
            else -> onClose()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = file.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    when {
                        isDirty -> showDiscardDialog = true
                        editing -> editing = false
                        else -> onClose()
                    }
                }) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            },
            actions = {
                if (type.isEditable && content is DocContent.PlainText) {
                    if (editing) {
                        IconButton(
                            onClick = {
                                onSaveText(editedText) { ok ->
                                    if (ok) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.saved),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        editing = false
                                    } else {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.save_failed),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            enabled = isDirty
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = stringResource(R.string.save))
                        }
                        IconButton(onClick = {
                            if (isDirty) showDiscardDialog = true else editing = false
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.done))
                        }
                    } else {
                        IconButton(onClick = { editing = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit))
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (type == DocType.PDF) 0.dp else 8.dp)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(
                                when (error) {
                                    DocError.UNSUPPORTED -> R.string.unsupported_type
                                    DocError.TOO_LARGE -> R.string.file_too_large
                                    DocError.LOAD_FAILED -> R.string.error_load_failed
                                    DocError.OPEN_FAILED -> R.string.error_open_file
                                }
                            ),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
                type == DocType.PDF -> {
                    PdfViewer(uri = file.uri)
                }
                content is DocContent.OfficeDoc -> {
                    OfficeDocView(blocks = content.blocks)
                }
                content is DocContent.SheetDoc -> {
                    SheetView(sheets = content.sheets)
                }
                content is DocContent.SlidesDoc -> {
                    SlidesView(slides = content.slides)
                }
                content is DocContent.PlainText -> {
                    TextViewer(
                        text = if (editing) editedText else content.text,
                        editing = editing,
                        onTextChange = { editedText = it }
                    )
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.discard_changes)) },
            text = { Text(stringResource(R.string.discard_changes_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    editing = false
                    editedText = originalText
                }) {
                    Text(stringResource(R.string.discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.keep_editing))
                }
            }
        )
    }
}
