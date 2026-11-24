package com.shavlegi.docreader.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shavlegi.docreader.R
import com.shavlegi.docreader.data.DocBlock
import com.shavlegi.docreader.data.SheetContent
import com.shavlegi.docreader.data.SlideContent

// --- Word (.docx / .doc) ---------------------------------------------------

@Composable
fun OfficeDocView(blocks: List<DocBlock>, modifier: Modifier = Modifier) {
    if (blocks.isEmpty()) {
        EmptyContent(modifier)
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(blocks) { block ->
            when (block) {
                is DocBlock.Paragraph -> {
                    Text(
                        text = block.text,
                        style = when (block.headingLevel) {
                            1 -> MaterialTheme.typography.headlineSmall
                            2 -> MaterialTheme.typography.titleLarge
                            3 -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.bodyLarge
                        },
                        fontWeight = if (block.headingLevel > 0) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        },
                        lineHeight = if (block.headingLevel > 0) 30.sp else 26.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is DocBlock.Table -> DocTable(rows = block.rows)
            }
        }
    }
}

// --- Excel (.xlsx / .xls) --------------------------------------------------

@Composable
fun SheetView(sheets: List<SheetContent>, modifier: Modifier = Modifier) {
    if (sheets.isEmpty()) {
        EmptyContent(modifier)
        return
    }
    var selected by remember { mutableIntStateOf(0) }
    Column(modifier = modifier.fillMaxSize()) {
        if (sheets.size > 1) {
            ScrollableTabRow(selectedTabIndex = selected) {
                sheets.forEachIndexed { index, sheet ->
                    Tab(
                        selected = selected == index,
                        onClick = { selected = index },
                        text = { Text(sheet.name) }
                    )
                }
            }
        }
        val rows = sheets[selected.coerceIn(sheets.indices)].rows
        if (rows.isEmpty()) {
            EmptyContent(Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 12.dp)
            ) {
                item { DocTable(rows = rows) }
            }
        }
    }
}

// --- PowerPoint (.pptx) ----------------------------------------------------

@Composable
fun SlidesView(slides: List<SlideContent>, modifier: Modifier = Modifier) {
    if (slides.isEmpty()) {
        EmptyContent(modifier)
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(slides) { slide ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.slide) + " " + slide.number,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = slide.text.ifBlank {
                            stringResource(R.string.no_text_in_document)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 26.sp,
                        color = if (slide.text.isBlank()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

// --- Shared bits ------------------------------------------------------------

@Composable
fun DocTable(rows: List<List<String>>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Column {
                rows.forEachIndexed { rowIndex, row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { cell ->
                            Box(
                                modifier = Modifier
                                    .width(140.dp)
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = cell,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (rowIndex == 0) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    }
                                )
                            }
                        }
                    }
                    if (rowIndex < rows.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.empty_document),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp)
        )
    }
}
