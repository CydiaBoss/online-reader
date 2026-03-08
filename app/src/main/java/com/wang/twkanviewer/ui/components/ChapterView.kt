package com.wang.twkanviewer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wang.twkanviewer.models.Chapter

@Composable
fun ChapterView(
    chapter: Chapter,
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            chapter.uploadedAt?.let {
                Text(
                    text = it.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            val contentChunks = chapter.content?.split(Regex("[\n ]+")) ?: emptyList()
            println("chunks: " + contentChunks.size)
            println("chunks pieces: $contentChunks")

            if (contentChunks.isNotEmpty())
                contentChunks.filter{
                    it.isNotBlank()
                }.forEach {
                    Text(
                        text = it,
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.5).sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }
            else
                Text(
                    text = "Loading...",
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.5).sp,
                    modifier = Modifier.fillMaxWidth()
                )
        }

        BottomAppBar {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Chapter List")
            }
            Spacer( modifier = Modifier.weight(1f) )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousClick) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Chapter")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onFontSizeChange(fontSize - 1f) }) {
                        Text("A-", style = MaterialTheme.typography.labelLarge)
                    }
                    Text(
                        text = fontSize.toInt().toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(onClick = { onFontSizeChange(fontSize + 1f) }) {
                        Text("A+", style = MaterialTheme.typography.labelLarge)
                    }
                }

                IconButton(onClick = onNextClick) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Chapter")
                }
            }
        }
    }
}
