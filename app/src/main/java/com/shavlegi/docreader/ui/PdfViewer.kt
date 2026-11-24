package com.shavlegi.docreader.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shavlegi.docreader.R
import com.shavlegi.docreader.data.PdfDoc
import androidx.compose.foundation.Image

/** PDF viewer: vertical pager, zoom buttons, "3 / 12" indicator. */
@Composable
fun PdfViewer(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    var pdfDoc by remember { mutableStateOf<PdfDoc?>(null) }
    var openError by remember { mutableStateOf(false) }
    var zoomIndex by remember { mutableStateOf(0) }
    val zoomLevels = remember { listOf(1.0f, 1.5f, 2.0f) }
    val listState = rememberLazyListState()

    DisposableEffect(uri) {
        var doc: PdfDoc? = null
        var pfd: ParcelFileDescriptor? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                doc = PdfDoc(PdfRenderer(pfd))
            } else {
                openError = true
            }
        } catch (_: Exception) {
            openError = true
        }
        pdfDoc = doc
        onDispose {
            doc?.close()
            runCatching { pfd?.close() }
        }
    }

    val doc = pdfDoc
    if (doc == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (openError) {
                Text(
                    text = stringResource(R.string.error_open_file),
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                CircularProgressIndicator()
            }
        }
        return
    }

    val widthPx = with(density) {
        (screenWidthDp.dp * zoomLevels[zoomIndex]).toPx().toInt()
    }.coerceAtMost(1600)
    val currentPage by remember {
        derivedStateOf { listState.firstVisibleItemIndex + 1 }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(doc.pageCount, key = { it }) { index ->
                PdfPageItem(doc = doc, index = index, widthPx = widthPx)
            }
        }
        // Bottom overlay: page indicator + zoom buttons.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ) {
                Text(
                    text = stringResource(R.string.page_of, currentPage, doc.pageCount),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            FilledIconButton(
                onClick = { if (zoomIndex > 0) zoomIndex-- },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.zoom_out))
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = { if (zoomIndex < zoomLevels.lastIndex) zoomIndex++ },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.zoom_in))
            }
        }
    }
}

@Composable
private fun PdfPageItem(doc: PdfDoc, index: Int, widthPx: Int) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(doc, index, widthPx) {
        bitmap = runCatching { doc.renderPage(index, widthPx) }.getOrNull()
    }
    // Recycle ONLY when this page leaves the list. Never recycle when the
    // bitmap is replaced (e.g. zoom change): the old bitmap may still be
    // drawn by the Image in the current frame, and recycling it crashes
    // with "Canvas: trying to use a recycled bitmap". A replaced bitmap is
    // left for the GC to free.
    val latestBitmap = rememberUpdatedState(bitmap)
    DisposableEffect(Unit) {
        onDispose { latestBitmap.value?.recycle() }
    }

    val density = LocalDensity.current
    val imageWidthDp = with(density) { widthPx.toDp() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        val current = bitmap
        if (current != null) {
            Image(
                bitmap = current.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.width(imageWidthDp)
            )
        } else {
            Box(
                modifier = Modifier
                    .width(imageWidthDp)
                    .aspectRatio(1f / 1.414f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
