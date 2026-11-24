package com.shavlegi.docreader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.shavlegi.docreader.ui.DocViewModel
import com.shavlegi.docreader.ui.HomeScreen
import com.shavlegi.docreader.ui.ViewerScreen
import com.shavlegi.docreader.ui.theme.DocReaderTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DocViewModel by viewModels()

    private val openDocument =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) handlePicked(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DocReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val opened = viewModel.openedFile
                    val recents by viewModel.recents.collectAsState()
                    if (opened == null) {
                        HomeScreen(
                            recents = recents,
                            onOpenFileClick = { openDocument.launch(arrayOf("*/*")) },
                            onRecentClick = { uriString ->
                                viewModel.openFile(Uri.parse(uriString))
                            },
                            onRemoveRecent = { viewModel.removeRecent(it) },
                            onClearRecents = { viewModel.clearRecents() },
                        )
                    } else {
                        ViewerScreen(
                            file = opened,
                            type = viewModel.openedType,
                            content = viewModel.content,
                            isLoading = viewModel.isLoading,
                            error = viewModel.error,
                            onClose = { viewModel.closeFile() },
                            onSaveText = { text, onResult ->
                                viewModel.saveText(opened.uri, text, onResult)
                            },
                        )
                    }
                }
            }
        }
    }

    private fun handlePicked(uri: Uri) {
        // Keep access to the file so TXT/MD edits can be saved back later,
        // even after the app restarts. Some providers only grant read
        // access — editing then fails gracefully with a message.
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        viewModel.openFile(uri)
    }
}
