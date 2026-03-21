package com.wang.twkanviewer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wang.twkanviewer.R
import com.wang.twkanviewer.models.Story
import com.wang.twkanviewer.models.StoryLocale
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun StoryView(
    story: Story,
    isSaved: Boolean = false,
    showTranslate: Boolean = false,
    translatedStory: StoryLocale? = null,
    onChapterClick: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Determine display values based on translation state
    val displayTitle = if (showTranslate && translatedStory != null) translatedStory.title else story.title
    val displayGenre = if (showTranslate && translatedStory != null) translatedStory.genre else story.genre
    val displayDescription = if (showTranslate && translatedStory != null) translatedStory.description else story.description
    val displayTags = if (showTranslate && translatedStory != null) translatedStory.tags else story.tags.joinToString(", ")

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_story_title)) },
            text = { Text(stringResource(R.string.delete_story_confirmation, story.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(R.string.delete_button), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScrollbar(
                    state = scrollState,
                    extraTopInset = 8.dp,
                    extraBottomInset = 8.dp
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = displayTitle,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(text = story.author)
                Spacer(modifier = Modifier.height(10.dp))
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(story.imgUrl)
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Mobile Safari/537.36")
                        .build(),
                    contentDescription = displayTitle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = if (story.completed) stringResource(R.string.status_completed) else stringResource(R.string.status_ongoing))
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = stringResource(R.string.word_count_format, story.wordCount))
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = stringResource(R.string.last_updated_format, dateFormatter.format(story.lastUpdated)))
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = stringResource(R.string.genre_format, displayGenre))
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = displayDescription)
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = stringResource(R.string.tags_format, displayTags))
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
        BottomAppBar(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if (isSaved) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_content_description),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                IconButton(onClick = onSave) {
                    Icon(
                        painter = painterResource(id = R.drawable.add_24px),
                        contentDescription = stringResource(R.string.save_content_description)
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onChapterClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = stringResource(R.string.chapters_content_description)
                )
            }
        }
    }
}
