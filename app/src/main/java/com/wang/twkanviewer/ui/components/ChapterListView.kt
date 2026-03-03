package com.wang.twkanviewer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wang.twkanviewer.models.Chapter
import com.wang.twkanviewer.models.Story

@Composable
fun ChapterListView(
    chapters: List<Chapter>,
    onClickChapter: (Chapter) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(chapters) { chapter ->
            Text(
                text = chapter.title,
                modifier = Modifier
                    .clickable { onClickChapter(chapter) }
                    .fillMaxWidth()
            )
        }
    }
}
