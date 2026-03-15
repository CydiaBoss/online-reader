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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wang.twkanviewer.R
import com.wang.twkanviewer.models.Chapter
import com.wang.twkanviewer.models.ChapterLocale
import kotlinx.coroutines.launch

@Composable
fun ChapterListView(
    chapters: List<Chapter>,
    bookmarkedChapterId: Int? = null,
    showTranslate: Boolean = false,
    translatedChapters: List<ChapterLocale>,
    onBackToStoryClick: () -> Unit,
    onClickChapter: (Chapter) -> Unit,
    onVisibleIdsChange: (Set<Int>) -> Unit = {},
    listState: LazyListState = rememberLazyListState()
) {
    var searchQuery by remember { mutableStateOf("") }
    var isDescending by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    
    // Memoize the translation map to avoid O(N*M) lookups during recomposition
    val translationMap by remember(translatedChapters.size, showTranslate) {
        derivedStateOf {
            if (showTranslate) {
                translatedChapters.associateBy { it.chapterId }
            } else emptyMap()
        }
    }

    val processedList by remember(chapters, translationMap, searchQuery, isDescending) {
        derivedStateOf {
            val mapped = chapters.map { chapter ->
                chapter to translationMap[chapter.id]?.title
            }

            val filtered = if (searchQuery.isEmpty()) mapped else {
                mapped.filter { (chapter, translated) ->
                    chapter.title.contains(searchQuery, ignoreCase = true) ||
                            (translated?.contains(searchQuery, ignoreCase = true) == true)
                }
            }

            val sorted = if (isDescending) {
                filtered.sortedByDescending { it.first.order }
            } else {
                filtered.sortedBy { it.first.order }
            }

            // Deduplicate by chapter id as a final safety net
            val seenIds = mutableSetOf<Int>()
            sorted.filter { (chapter, _) -> seenIds.add(chapter.id) }
        }
    }

    val visibleChapterIds by remember(listState, processedList) {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.mapNotNull { item ->
                processedList.getOrNull(item.index)?.first?.id
            }.toSet()
        }
    }

    LaunchedEffect(visibleChapterIds) {
        onVisibleIdsChange(visibleChapterIds)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(
                items = processedList,
                key = { it.first.id } // Adding key for better list performance
            ) { (chapter, translatedTitle) ->
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
                                        contentDescription = stringResource(R.string.bookmarked_content_description),
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_content_description))
            }
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                placeholder = { Text(stringResource(R.string.search_chapters_placeholder)) },
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

            if (bookmarkedChapterId != null) {
                IconButton(onClick = {
                    val index = processedList.indexOfFirst { it.first.id == bookmarkedChapterId }
                    if (index != -1) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(index)
                        }
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.filled_bookmark_24px),
                        contentDescription = stringResource(R.string.jump_to_bookmark_content_description),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = { isDescending = !isDescending }) {
                Icon(
                    painter = painterResource(id = R.drawable.sort_24px),
                    contentDescription = stringResource(R.string.toggle_order_content_description),
                    tint = if (isDescending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
