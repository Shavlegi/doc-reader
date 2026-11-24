package com.shavlegi.docreader.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Plain-text view (.txt / .md / .csv). TXT and MD are editable in v1. */
@Composable
fun TextViewer(
    text: String,
    editing: Boolean,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (editing) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 17.sp,
                lineHeight = 26.sp
            )
        )
    } else {
        SelectionContainer {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 26.sp,
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
}
