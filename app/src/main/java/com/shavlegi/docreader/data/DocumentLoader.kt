package com.shavlegi.docreader.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.shavlegi.docreader.model.DocType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFTextShape
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFTable
import java.io.InputStream

/** One rendered block of a Word document. */
sealed interface DocBlock {
    data class Paragraph(val text: String, val headingLevel: Int = 0) : DocBlock
    data class Table(val rows: List<List<String>>) : DocBlock
}

data class SheetContent(val name: String, val rows: List<List<String>>)
data class SlideContent(val number: Int, val text: String)

/** Parsed content of an opened file (PDF is rendered separately). */
sealed interface DocContent {
    data class OfficeDoc(val blocks: List<DocBlock>) : DocContent
    data class SheetDoc(val sheets: List<SheetContent>) : DocContent
    data class SlidesDoc(val slides: List<SlideContent>) : DocContent
    data class PlainText(val text: String) : DocContent
}

/** Safety cap: plain-text files bigger than this are refused in v1. */
const val MAX_TEXT_BYTES = 2 * 1024 * 1024L

/** Info about a file the user picked through the Storage Access Framework. */
data class PickedFile(val uri: Uri, val name: String, val size: Long)

fun queryFileInfo(resolver: ContentResolver, uri: Uri): PickedFile {
    var name = uri.lastPathSegment ?: "file"
    var size = 0L
    resolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
        null, null, null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            name = cursor.getString(0) ?: name
            size = cursor.getLong(1)
        }
    }
    return PickedFile(uri, name, size)
}

/**
 * Parses a document off the main thread. PDF is not parsed here — the
 * viewer renders its pages directly with PdfRenderer.
 */
suspend fun loadDocument(resolver: ContentResolver, uri: Uri, type: DocType): Result<DocContent> =
    withContext(Dispatchers.IO) {
        runCatching {
            when (type) {
                DocType.DOCX -> loadDocx(openInput(resolver, uri))
                DocType.DOC -> loadDoc(openInput(resolver, uri))
                DocType.XLSX, DocType.XLS -> loadWorkbook(openInput(resolver, uri))
                DocType.PPTX -> loadPptx(openInput(resolver, uri))
                DocType.TXT, DocType.MD, DocType.CSV -> loadText(resolver, uri)
                else -> throw IllegalArgumentException("Unsupported type: $type")
            }
        }
    }

/** Writes edited text back to the same file the user opened (SAF Uri). */
suspend fun saveText(resolver: ContentResolver, uri: Uri, text: String): Result<Unit> =
    withContext(Dispatchers.IO) {
        runCatching {
            resolver.openOutputStream(uri, "w")?.use { out ->
                out.write(text.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: throw IllegalStateException("Cannot open file for writing")
        }
    }

private fun openInput(resolver: ContentResolver, uri: Uri): InputStream =
    resolver.openInputStream(uri) ?: throw IllegalStateException("Cannot open file")

// --- Word (.docx) ---------------------------------------------------------

private fun loadDocx(stream: InputStream): DocContent.OfficeDoc {
    XWPFDocument(stream).use { doc ->
        val blocks = mutableListOf<DocBlock>()
        for (element in doc.bodyElements) {
            when (element) {
                is XWPFParagraph -> {
                    val text = element.text.trim()
                    if (text.isNotEmpty()) {
                        blocks += DocBlock.Paragraph(text, headingLevel(element.style))
                    }
                }
                is XWPFTable -> {
                    val rows = element.rows.map { row ->
                        row.tableCells.map { it.text.trim() }
                    }
                    if (rows.isNotEmpty()) blocks += DocBlock.Table(rows)
                }
            }
        }
        return DocContent.OfficeDoc(blocks)
    }
}

private fun headingLevel(style: String?): Int {
    if (style == null) return 0
    return when {
        style.startsWith("Heading1") || style == "Title" -> 1
        style.startsWith("Heading2") -> 2
        style.startsWith("Heading3") -> 3
        style.startsWith("Heading") -> 4
        else -> 0
    }
}

// --- Old Word (.doc) — HWPF, read-only -----------------------------------

private fun loadDoc(stream: InputStream): DocContent.OfficeDoc {
    HWPFDocument(stream).use { doc ->
        // HWPF has no paragraph/table structure API worth using here:
        // split the raw text into paragraphs. Formatting is not kept.
        val text = doc.documentText.orEmpty()
        val blocks = text.split('\r', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { DocBlock.Paragraph(it) }
        return DocContent.OfficeDoc(blocks)
    }
}

// --- Excel (.xlsx / .xls) --------------------------------------------------

private fun loadWorkbook(stream: InputStream): DocContent.SheetDoc {
    val formatter = DataFormatter()
    WorkbookFactory.create(stream).use { workbook ->
        val sheets = mutableListOf<SheetContent>()
        for (s in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(s)
            val rows = mutableListOf<List<String>>()
            for (row in sheet) {
                val last = row.lastCellNum.toInt()
                val cells = mutableListOf<String>()
                for (c in 0 until last) {
                    val cell = row.getCell(c)
                    cells += if (cell == null) "" else formatter.formatCellValue(cell)
                }
                if (cells.any { it.isNotBlank() }) rows += cells
            }
            sheets += SheetContent(sheet.sheetName, rows)
        }
        return DocContent.SheetDoc(sheets)
    }
}

// --- PowerPoint (.pptx) ----------------------------------------------------

private fun loadPptx(stream: InputStream): DocContent.SlidesDoc {
    XMLSlideShow(stream).use { show ->
        val slides = show.slides.mapIndexed { index, slide ->
            val text = slide.shapes
                .filterIsInstance<XSLFTextShape>()
                .mapNotNull { it.text?.trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n")
            SlideContent(index + 1, text)
        }
        return DocContent.SlidesDoc(slides)
    }
}

// --- Plain text (.txt / .md / .csv) ----------------------------------------

private fun loadText(resolver: ContentResolver, uri: Uri): DocContent.PlainText {
    resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val size = cursor.getLong(0)
            if (size > MAX_TEXT_BYTES) throw IllegalStateException("File too large")
        }
    }
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
        ?: throw IllegalStateException("Cannot open file")
    if (bytes.size > MAX_TEXT_BYTES) throw IllegalStateException("File too large")
    val text = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
    return DocContent.PlainText(text)
}
