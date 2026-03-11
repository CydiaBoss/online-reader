package com.wang.twkanviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.wang.twkanviewer.R
import com.wang.twkanviewer.models.Chapter
import com.wang.twkanviewer.models.ChapterLocale

@Composable
fun ChapterListView(
    chapters: List<Chapter>,
    bookmarkedChapterId: Int? = null,
    showTranslate: Boolean = false,
    translatedChapters: List<ChapterLocale>,
    onBackToStoryClick: () -> Unit,
    onClickChapter: (Chapter) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isDescending by remember { mutableStateOf(false) }
    
    val processedList by remember(chapters, bookmarkedChapterId, showTranslate, translatedChapters, searchQuery, isDescending) {
        derivedStateOf {
            val translationMap = if (showTranslate) {
                translatedChapters.associateBy { it.chapterId }
            } else emptyMap()
            
            // 1. Map to pairs with translation
            val mapped = chapters.map { chapter ->
                chapter to translationMap[chapter.id]?.title
            }

            // 2. Filter by search
            val filtered = mapped.filter { (chapter, translated) ->
                searchQuery.isEmpty() || 
                chapter.title.contains(searchQuery, ignoreCase = true) ||
                (translated?.contains(searchQuery, ignoreCase = true) == true)
            }

            // 3. Sort
            val sorted = if (isDescending) {
                filtered.sortedByDescending { it.first.order }
            } else {
                filtered.sortedBy { it.first.order }
            }

            // 4. Pin bookmark to top if it exists in the current filtered/sorted list
            val bookmarked = sorted.find { it.first.id == bookmarkedChapterId }
            if (bookmarked != null && searchQuery.isEmpty()) {
                listOf(bookmarked) + sorted.filter { it.first.id != bookmarkedChapterId }
            } else {
                sorted
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
            items(processedList) { (chapter, translatedTitle) ->
                val isBookmarked = chapter.id == bookmarkedChapterId
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isBookmarked) Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)) else Modifier)
                ) {
                    ListItem(
                        headlineContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isBookmarked) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Bookmarked",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp).padding(end = 8.dp)
                                    )
                                }
                                Text(
                                    text = translatedTitle ?: chapter.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = if (isBookmarked) MaterialTheme.colorScheme.primary else Color.Unspecified
                                )
                            }
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
        }

        BottomAppBar {
            IconButton(onClick = onBackToStoryClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            IconButton(onClick = { isDescending = !isDescending }) {
                Icon(
                    painter = painterResource(id = R.drawable.sort_24px),
                    contentDescription = "Toggle Order",
                    tint = if (isDescending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
