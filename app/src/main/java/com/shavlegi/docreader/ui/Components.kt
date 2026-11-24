package com.shavlegi.docreader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shavlegi.docreader.R
import com.shavlegi.docreader.model.DocType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Coloured format badge — e.g. a red "PDF" square. */
@Composable
fun FormatBadge(type: DocType, size: Dp = 52.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(type.color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = type.label,
            color = androidx.compose.ui.graphics.Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = if (type.label.length > 3) 11.sp else 13.sp
        )
    }
}

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val kb = bytes / 1024.0
    if (kb < 1) return "$bytes B"
    val mb = kb / 1024.0
    return if (mb < 1) {
        "%.0f KB".format(kb)
    } else {
        "%.1f MB".format(mb)
    }
}

/** "Today" / "Yesterday" / "12 Nov" style label for a timestamp. */
@Composable
fun formatDateLabel(timestamp: Long): String {
    val today = stringResource(R.string.today)
    val yesterday = stringResource(R.string.yesterday)
    val cal = Calendar.getInstance()
    val now = Calendar.getInstance()
    cal.timeInMillis = timestamp
    val isToday = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    if (isToday) return today
    now.add(Calendar.DAY_OF_YEAR, -1)
    val isYesterday = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    if (isYesterday) return yesterday
    return SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(timestamp))
}
