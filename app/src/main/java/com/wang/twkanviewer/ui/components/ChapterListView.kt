package com.wang.twkanviewer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wang.twkanviewer.models.Chapter
import com.wang.twkanviewer.models.ChapterLocale

@Composable
fun ChapterListView(
    chapters: List<Chapter>,
    showTranslate: Boolean = false,
    translatedChapters: List<ChapterLocale>,
    onBackToStoryClick: () -> Unit,
    onClickChapter: (Chapter) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Create a list of pairs (Chapter, TranslatedTitle?)
    val displayList = remember(chapters, translatedChapters, showTranslate) {
        chapters.map { chapter ->
            val translatedTitle = if (showTranslate) {
                translatedChapters.find { it.chapterId == chapter.id }?.title
            } else null
            chapter to translatedTitle
        }
    }

    val filteredList = remember(displayList, searchQuery) {
        if (searchQuery.isEmpty()) {
            displayList
        } else {
            displayList.filter { (chapter, translated) ->
                chapter.title.contains(searchQuery, ignoreCase = true) ||
                (translated?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            itemsIndexed(filteredList) { _, (chapter, translatedTitle) ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = translatedTitle ?: chapter.title,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    },
                    modifier = Modifier
                        .clickable { onClickChapter(chapter) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }

        BottomAppBar(
            actions = {
                IconButton(onClick = onBackToStoryClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Story")
                }
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    placeholder = { Text("Search chapters...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    singleLine = true
                )
            }
        )
    }
}
