package com.wang.twkanviewer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.wang.twkanviewer.R
import com.wang.twkanviewer.models.Story
import com.wang.twkanviewer.models.StoryLocale

@Composable
fun StoryListView(
    stories: List<Story>,
    showTranslate: Boolean = false,
    translatedStories: List<StoryLocale> = emptyList(),
    onBackClick: () -> Unit,
    onStoryClick: (Story) -> Unit,
    onDeleteStory: (Story) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var storyToDelete by remember { mutableStateOf<Story?>(null) }

    val filteredList by remember(stories, showTranslate, translatedStories, searchQuery) {
        derivedStateOf {
            val translationMap = if (showTranslate) {
                translatedStories.associateBy { it.storyId }
            } else emptyMap()

            stories.map { story ->
                story to translationMap[story.id]
            }.filter { (story, locale) ->
                val searchTarget = if (showTranslate && locale != null) {
                    "${locale.title} ${story.author} ${locale.description}"
                } else {
                    "${story.title} ${story.author} ${story.description}"
                }
                searchQuery.isEmpty() || searchTarget.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    if (storyToDelete != null) {
        AlertDialog(
            onDismissRequest = { storyToDelete = null },
            title = { Text(stringResource(R.string.delete_story_title)) },
            text = { Text(stringResource(R.string.delete_story_confirmation, storyToDelete!!.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteStory(storyToDelete!!)
                        storyToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete_button), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { storyToDelete = null }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(filteredList) { (story, locale) ->
                val displayTitle = if (showTranslate && locale != null) locale.title else story.title
                val displayDescription = if (showTranslate && locale != null) locale.description else story.description

                ListItem(
                    modifier = Modifier.clickable { onStoryClick(story) },
                    leadingContent = {
                        AsyncImage(
                            model = story.imgUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(60.dp, 80.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                    },
                    headlineContent = {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    supportingContent = {
                        Column {
                            Text(
                                text = story.author,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1
                            )
                            Text(
                                text = displayDescription,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    },
                    trailingContent = {
                        IconButton(onClick = { storyToDelete = story }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_button),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }

        BottomAppBar {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_content_description))
            }
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                placeholder = { Text(stringResource(R.string.search_stories_placeholder)) },
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
    }
}
