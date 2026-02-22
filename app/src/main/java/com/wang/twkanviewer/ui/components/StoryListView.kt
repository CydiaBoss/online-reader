package com.wang.twkanviewer.ui.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.wang.twkanviewer.models.Story

@Composable
fun StoryListView(stories: List<Story>) {
    LazyColumn {
        items(stories) { story ->
            Text(text = story.title)
        }
    }
}
