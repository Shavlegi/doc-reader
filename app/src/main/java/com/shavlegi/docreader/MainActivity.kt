package com.shavlegi.docreader

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shavlegi.docreader.data.clearCrashLog
import com.shavlegi.docreader.data.installCrashHandler
import com.shavlegi.docreader.data.readCrashLog
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
        installCrashHandler(applicationContext)
        setContent {
            DocReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    var crashLog by remember { mutableStateOf(readCrashLog(context)) }
                    if (crashLog != null) {
                        AlertDialog(
                            onDismissRequest = {
                                crashLog = null
                                clearCrashLog(context)
                            },
                            title = { Text(stringResource(R.string.crash_title)) },
                            text = {
                                Column(
                                    modifier = Modifier
                                        .heightIn(max = 420.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(stringResource(R.string.crash_hint))
                                    Text(crashLog ?: "")
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    val clipboard =
                                        context.getSystemService(Context.CLIPBOARD_SERVICE)
                                            as ClipboardManager
                                    clipboard.setPrimaryClip(
                                        ClipData.newPlainText("crash", crashLog ?: "")
                                    )
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.copied),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }) {
                                    Text(stringResource(R.string.copy))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    crashLog = null
                                    clearCrashLog(context)
                                }) {
                                    Text(stringResource(R.string.done))
                                }
                            }
                        )
                    }
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
