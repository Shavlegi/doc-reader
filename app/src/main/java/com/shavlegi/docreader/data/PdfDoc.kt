package com.shavlegi.docreader.data

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Thin wrapper around android.graphics.pdf.PdfRenderer.
 * Rendering is serialised with a Mutex: PdfRenderer is not thread-safe.
 */
class PdfDoc(private val renderer: PdfRenderer) {

    private val mutex = Mutex()

    val pageCount: Int
        get() = renderer.pageCount

    suspend fun renderPage(index: Int, widthPx: Int): Bitmap =
        withContext(Dispatchers.Default) {
            mutex.withLock {
                renderer.openPage(index).use { page ->
                    val heightPx = (widthPx * page.height.toFloat() / page.width)
                        .toInt()
                        .coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }
        }

    fun close() {
        runCatching { renderer.close() }
    }
}
