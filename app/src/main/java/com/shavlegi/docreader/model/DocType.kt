package com.shavlegi.docreader.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

/** Document format, detected by file extension. */
enum class DocType {
    PDF, DOCX, DOC, XLSX, XLS, PPTX, TXT, MD, CSV, UNKNOWN;

    val label: String
        get() = when (this) {
            PDF -> "PDF"
            DOCX -> "DOCX"
            DOC -> "DOC"
            XLSX -> "XLSX"
            XLS -> "XLS"
            PPTX -> "PPTX"
            TXT -> "TXT"
            MD -> "MD"
            CSV -> "CSV"
            UNKNOWN -> "?"
        }

    /** Distinct brand colour per format, shown on the file badge. */
    val color: Color
        get() = when (this) {
            PDF -> Color(0xFFE5484D)
            DOCX, DOC -> Color(0xFF4C8DFF)
            XLSX, XLS -> Color(0xFF34C77B)
            PPTX -> Color(0xFFFF7A45)
            TXT -> Color(0xFF8A94A6)
            MD -> Color(0xFF9D6BFF)
            CSV -> Color(0xFF2BB3A3)
            UNKNOWN -> Color(0xFF8A94A6)
        }

    val isOffice: Boolean
        get() = this == DOCX || this == DOC || this == XLSX || this == XLS || this == PPTX

    val isText: Boolean
        get() = this == TXT || this == MD || this == CSV

    /** TXT and MD are editable in v1; CSV is read-only. */
    val isEditable: Boolean
        get() = this == TXT || this == MD

    companion object {
        fun of(fileName: String): DocType {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return when (ext) {
                "pdf" -> PDF
                "docx" -> DOCX
                "doc" -> DOC
                "xlsx" -> XLSX
                "xls" -> XLS
                "pptx" -> PPTX
                "txt" -> TXT
                "md", "markdown" -> MD
                "csv" -> CSV
                else -> UNKNOWN
            }
        }
    }
}

/** One entry in the "recent files" list, persisted with DataStore. */
@Serializable
data class RecentFile(
    val uri: String,
    val name: String,
    val type: String, // DocType name
    val size: Long = 0L,
    val lastOpened: Long = 0L,
) {
    val docType: DocType
        get() = runCatching { DocType.valueOf(type) }.getOrDefault(DocType.UNKNOWN)
}
