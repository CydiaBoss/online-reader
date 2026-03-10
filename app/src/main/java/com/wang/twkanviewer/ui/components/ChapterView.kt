package com.wang.twkanviewer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wang.twkanviewer.models.Chapter
import com.wang.twkanviewer.models.ChapterLocale
import kotlinx.coroutines.delay

@Composable
fun ChapterView(
    chapter: Chapter,
    showTranslate: Boolean = false,
    translatedChapter: ChapterLocale? = null,
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    onToggleBars: (Boolean) -> Unit = {}
) {
    var showAppBar by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()
    val interactionSource = remember { MutableInteractionSource() }

    // Sync visibility with TopAppBar in MainActivity
    LaunchedEffect(showAppBar) {
        onToggleBars(showAppBar)
    }

    // Auto-hide logic when scrolling starts
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            showAppBar = false
        }
    }

    // Auto-hide after idle
    LaunchedEffect(showAppBar) {
        if (showAppBar) {
            delay(3000)
            showAppBar = false
        }
    }

    // Determine what text to display. Fallback to original text if translation content is missing or empty.
    val isTranslatedAvailable = showTranslate && translatedChapter != null
    val displayTitle = if (isTranslatedAvailable) translatedChapter!!.title else chapter.title
    val paragraphs = if (isTranslatedAvailable && translatedChapter!!.content.isNotEmpty()) 
        translatedChapter!!.content 
    else 
        chapter.content

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                showAppBar = !showAppBar
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Spacer for TopAppBar overlay
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = displayTitle,
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
            
            paragraphs?.forEach { paragraph ->
                if (paragraph.isNotBlank()) {
                    Text(
                        text = paragraph.trim(),
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.5).sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }
            }

            // Spacer for BottomAppBar overlay
            Spacer(modifier = Modifier.height(64.dp))
        }

        AnimatedVisibility(
            visible = showAppBar,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BottomAppBar() {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Chapter List")
                    }
                    
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
}
