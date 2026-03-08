package com.wang.twkanviewer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wang.twkanviewer.R
import com.wang.twkanviewer.models.Story
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun StoryView(
    story: Story,
    onChapterClick: () -> Unit,
    onSave: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = story.title,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
            Text(text = story.author)
            Spacer(modifier = Modifier.height(10.dp))
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(story.imgUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Mobile Safari/537.36")
                    .build(),
                contentDescription = story.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = if (story.completed) "Completed" else "Ongoing")
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Word Count: " + story.wordCount)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Last Updated: " + dateFormatter.format(story.lastUpdated))
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Genre: " + story.genre)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = story.description)
            Spacer(modifier = Modifier.height(10.dp))
            Row() {
                Text(text = "Tags: ")
                for (tag in story.tags) {
                    Text(text = "$tag ")
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
        BottomAppBar(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            IconButton(
                onClick = onSave
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.add_24px),
                    contentDescription = "save"
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onChapterClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "Chapters"
                )
            }
        }
    }
}
