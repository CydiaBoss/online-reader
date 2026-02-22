package com.wang.twkanviewer.ui.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.wang.twkanviewer.models.Chapter

@Composable
fun ChapterListView(chapters: List<Chapter>) {
    LazyColumn {
        items(chapters) { chapter ->
            Text(text = chapter.title)
        }
    }
}
