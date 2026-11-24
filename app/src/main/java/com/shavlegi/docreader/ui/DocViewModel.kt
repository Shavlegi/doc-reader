package com.shavlegi.docreader.ui

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shavlegi.docreader.data.DocContent
import com.shavlegi.docreader.data.PickedFile
import com.shavlegi.docreader.data.RecentFilesStore
import com.shavlegi.docreader.data.loadDocument
import com.shavlegi.docreader.data.queryFileInfo
import com.shavlegi.docreader.data.saveText
import com.shavlegi.docreader.model.DocType
import com.shavlegi.docreader.model.RecentFile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DocError {
    UNSUPPORTED, TOO_LARGE, LOAD_FAILED, OPEN_FAILED
}

class DocViewModel(application: Application) : AndroidViewModel(application) {

    private val store = RecentFilesStore(application)

    val recents: StateFlow<List<RecentFile>> = store.recents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var openedFile by mutableStateOf<PickedFile?>(null)
        private set
    var openedType by mutableStateOf(DocType.UNKNOWN)
        private set
    var content by mutableStateOf<DocContent?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<DocError?>(null)
        private set

    fun openFile(uri: Uri) {
        val resolver: ContentResolver = getApplication<Application>().contentResolver
        viewModelScope.launch {
            isLoading = true
            error = null
            content = null
            openedFile = null
            val picked = runCatching { queryFileInfo(resolver, uri) }.getOrNull()
            if (picked == null) {
                error = DocError.OPEN_FAILED
                isLoading = false
                return@launch
            }
            val type = DocType.of(picked.name)
            if (type == DocType.UNKNOWN) {
                error = DocError.UNSUPPORTED
                isLoading = false
                return@launch
            }
            openedFile = picked
            openedType = type
            store.add(
                RecentFile(
                    uri = uri.toString(),
                    name = picked.name,
                    type = type.name,
                    size = picked.size,
                    lastOpened = System.currentTimeMillis()
                )
            )
            if (type == DocType.PDF) {
                // The viewer renders PDF pages itself with PdfRenderer.
                isLoading = false
                return@launch
            }
            loadDocument(resolver, uri, type)
                .onSuccess { content = it }
                .onFailure { throwable ->
                    error = if (throwable.message == "File too large") {
                        DocError.TOO_LARGE
                    } else {
                        DocError.LOAD_FAILED
                    }
                }
            isLoading = false
        }
    }

    fun closeFile() {
        openedFile = null
        openedType = DocType.UNKNOWN
        content = null
        error = null
        isLoading = false
    }

    fun clearError() {
        error = null
    }

    fun saveText(uri: Uri, text: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = saveText(getApplication<Application>().contentResolver, uri, text).isSuccess
            if (ok && content is DocContent.PlainText) {
                content = (content as DocContent.PlainText).copy(text = text)
            }
            onResult(ok)
        }
    }

    fun removeRecent(uri: String) {
        viewModelScope.launch { store.remove(uri) }
    }

    fun clearRecents() {
        viewModelScope.launch { store.clear() }
    }
}
