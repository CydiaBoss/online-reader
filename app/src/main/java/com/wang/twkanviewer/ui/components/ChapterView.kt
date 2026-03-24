package com.wang.twkanviewer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wang.twkanviewer.R
import com.wang.twkanviewer.models.Chapter
import com.wang.twkanviewer.models.ChapterLocale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChapterView(
    chapters: List<Chapter>,
    initialIndex: Int,
    bookmarkedChapterId: Int?,
    showTranslate: Boolean = false,
    translatedChapters: List<ChapterLocale> = emptyList(),
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    fontFamily: String,
    onBookmarkClick: () -> Unit,
    onRefreshClick: (Chapter) -> Unit,
    onNavigateToChapter: (Chapter) -> Unit,
    onBackClick: () -> Unit,
    showBars: Boolean = true, // Default value to fix the missing parameter error
    onToggleBars: (Boolean) -> Unit = {}
) {
    val pageCount = chapters.size + 2
    val pagerState = rememberPagerState(initialPage = initialIndex + 1) { pageCount }
    val scope = rememberCoroutineScope()
    var showAppBar by remember(showBars) { mutableStateOf(showBars) }

    // Auto-hide after idle
    LaunchedEffect(showAppBar) {
        if (showAppBar) {
            delay(3000)
            showAppBar = false
            onToggleBars(false)
        }
    }

    LaunchedEffect(initialIndex) {
        val targetPage = initialIndex + 1
        if (pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        when (pagerState.currentPage) {
            0 -> onBackClick()
            pageCount - 1 -> onBackClick()
            else -> {
                onNavigateToChapter(chapters[pagerState.currentPage - 1])
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            when (page) {
                0, pageCount - 1 -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    val chapter = chapters[page - 1]
                    val translatedChapter = translatedChapters.find { it.chapterId == chapter.id }

                    ChapterPageContent(
                        chapter = chapter,
                        translatedChapter = translatedChapter,
                        showTranslate = showTranslate,
                        fontSize = fontSize,
                        fontFamily = fontFamily,
                        showBars = showAppBar,
                        onToggleAppBar = { 
                            showAppBar = !showAppBar
                            onToggleBars(showAppBar)
                        },
                        onScrollInProgress = { 
                            if (it && showAppBar) {
                                showAppBar = false
                                onToggleBars(false)
                            }
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showAppBar,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column {
                LinearProgressIndicator(
                    progress = {
                        if (chapters.isEmpty()) 0f
                        else {
                            val realPage = (pagerState.currentPage - 1).coerceIn(0, chapters.size - 1)
                            val totalSteps = if (chapters.size > 1) chapters.size - 1 else 1
                            (realPage.toFloat() / totalSteps).coerceIn(0f, 1f)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    gapSize = 0.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Butt,
                    drawStopIndicator = {} // Removes the dot at the end of the track by providing an empty draw lambda
                )
                BottomAppBar {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.chapter_list_content_description))
                        }
                        
                        IconButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous_chapter_content_description))
                        }

                        val currentChapterIndex = (pagerState.currentPage - 1).coerceIn(0, chapters.size - 1)
                        val isBookmarked = chapters[currentChapterIndex].id == bookmarkedChapterId
                        
                        IconButton(onClick = onBookmarkClick) {
                            Icon(
                                painter = painterResource(id = if (isBookmarked) R.drawable.filled_bookmark_24px else R.drawable.bookmark_24px),
                                contentDescription = stringResource(R.string.bookmark_content_description),
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(onClick = { onRefreshClick(chapters[currentChapterIndex]) }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh_button_content_description))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onFontSizeChange(fontSize - 1f) }) {
                                Text(stringResource(R.string.font_decrease), style = MaterialTheme.typography.labelLarge)
                            }
                            Text(
                                text = fontSize.toInt().toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            IconButton(onClick = { onFontSizeChange(fontSize + 1f) }) {
                                Text(stringResource(R.string.font_increase), style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        IconButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next_chapter_content_description))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterPageContent(
    chapter: Chapter,
    translatedChapter: ChapterLocale?,
    showTranslate: Boolean,
    fontSize: Float,
    fontFamily: String,
    showBars: Boolean,
    onToggleAppBar: () -> Unit,
    onScrollInProgress: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollState.isScrollInProgress) {
        onScrollInProgress(scrollState.isScrollInProgress)
    }

    val isTranslatedAvailable = showTranslate && translatedChapter != null
    val displayTitle = if (isTranslatedAvailable) translatedChapter.title else chapter.title
    val paragraphs = if (isTranslatedAvailable && translatedChapter.content.isNotEmpty())
        translatedChapter.content
    else 
        chapter.content

    val composeFontFamily = when (fontFamily) {
        "Serif" -> FontFamily.Serif
        "Sans Serif" -> FontFamily.SansSerif
        "Monospace" -> FontFamily.Monospace
        else -> FontFamily.Default
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScrollbar(
                state = scrollState,
                extraTopInset = 8.dp,
                extraBottomInset = if (showBars) 80.dp else 8.dp
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleAppBar() }
                )
            }
    ) {
        if (paragraphs.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = displayTitle,
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = composeFontFamily),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            chapter.uploadedAt?.let {
                Text(
                    text = it.toString(),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = composeFontFamily),
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))
            
            paragraphs.forEach { paragraph ->
                if (paragraph.isNotBlank()) {
                    Text(
                        text = paragraph.trim(),
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.5).sp,
                        fontFamily = composeFontFamily,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Left
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
