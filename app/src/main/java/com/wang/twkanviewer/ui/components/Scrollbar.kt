package com.wang.twkanviewer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Vertical Scrollbar for LazyColumn (LazyListState)
 */
fun Modifier.verticalScrollbar(
    state: LazyListState,
    width: Dp = 4.dp,
    color: Color? = null,
    extraTopInset: Dp = 0.dp,
    extraBottomInset: Dp = 0.dp
): Modifier = composed {
    val scrollbarColor = color ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    var isScrolling by remember { mutableStateOf(false) }

    LaunchedEffect(state.isScrollInProgress) {
        if (state.isScrollInProgress) {
            isScrolling = true
        } else {
            delay(1500)
            isScrolling = false
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isScrolling) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "scrollbar_alpha"
    )

    drawWithContent {
        drawContent()

        val layoutInfo = state.layoutInfo
        val visibleItemsInfo = layoutInfo.visibleItemsInfo
        if (visibleItemsInfo.isEmpty()) return@drawWithContent

        val totalItemsCount = layoutInfo.totalItemsCount
        val viewportHeight = size.height
        val topPx = extraTopInset.toPx()
        val bottomPx = extraBottomInset.toPx()
        val trackHeight = viewportHeight - topPx - bottomPx
        
        if (trackHeight <= 0) return@drawWithContent

        // Estimate how many items fit in the viewport based on current average visible size
        val averageItemSize = visibleItemsInfo.map { it.size }.average().toFloat().coerceAtLeast(1f)
        val itemsInViewport = trackHeight / averageItemSize
        
        // No scrollbar needed if content fits on one screen
        if (totalItemsCount <= itemsInViewport) return@drawWithContent

        val minHeightPx = 32.dp.toPx()
        val maxHeightPx = trackHeight * 0.3f

        // Thumb height proportional to ratio of visible items
        val thumbHeight = (trackHeight * (itemsInViewport / totalItemsCount))
            .coerceIn(minHeightPx.coerceAtMost(trackHeight), maxHeightPx)

        // Seamless Progress calculation using pixel offsets
        val firstVisibleItem = visibleItemsInfo.first()
        val scrollOffset = state.firstVisibleItemScrollOffset.toFloat()
        val scrollProgressInsideItem = scrollOffset / averageItemSize
        
        // DIVISOR FIX: The scrollable range is the total items minus the ones already visible in the viewport
        val scrollableRange = (totalItemsCount - itemsInViewport).coerceAtLeast(1f)
        val totalProgress = (firstVisibleItem.index + scrollProgressInsideItem) / scrollableRange
        
        // MAPPING FIX: Map 0..1 progress to the available track space (top to trackBottom - thumb)
        val offset = topPx + (totalProgress.coerceIn(0f, 1f) * (trackHeight - thumbHeight))

        if (alpha > 0f) {
            drawRoundRect(
                color = scrollbarColor,
                topLeft = Offset(size.width - width.toPx() - 4.dp.toPx(), offset),
                size = Size(width.toPx(), thumbHeight),
                cornerRadius = CornerRadius(width.toPx() / 2, width.toPx() / 2),
                alpha = alpha
            )
        }
    }
}

/**
 * Vertical Scrollbar for regular Scrollable Column (ScrollState)
 */
fun Modifier.verticalScrollbar(
    state: ScrollState,
    width: Dp = 4.dp,
    color: Color? = null,
    extraTopInset: Dp = 0.dp,
    extraBottomInset: Dp = 0.dp
): Modifier = composed {
    val scrollbarColor = color ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    var isScrolling by remember { mutableStateOf(false) }

    LaunchedEffect(state.isScrollInProgress) {
        if (state.isScrollInProgress) {
            isScrolling = true
        } else {
            delay(1500)
            isScrolling = false
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isScrolling) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "scrollbar_alpha"
    )

    drawWithContent {
        drawContent()

        val viewportHeight = size.height
        val topPx = extraTopInset.toPx()
        val bottomPx = extraBottomInset.toPx()
        val trackHeight = viewportHeight - topPx - bottomPx
        
        if (trackHeight <= 0) return@drawWithContent

        val maxValue = state.maxValue.toFloat()
        if (maxValue <= 0) return@drawWithContent

        val totalContentHeight = trackHeight + maxValue
        val minHeightPx = 48.dp.toPx()
        val maxHeightPx = trackHeight * 0.3f

        val thumbHeight = (trackHeight * (trackHeight / totalContentHeight))
            .coerceIn(minHeightPx.coerceAtMost(trackHeight), maxHeightPx)

        val progress = state.value.toFloat() / maxValue
        val offset = topPx + (progress.coerceIn(0f, 1f) * (trackHeight - thumbHeight))

        if (alpha > 0f) {
            drawRoundRect(
                color = scrollbarColor,
                topLeft = Offset(size.width - width.toPx() - 4.dp.toPx(), offset),
                size = Size(width.toPx(), thumbHeight),
                cornerRadius = CornerRadius(width.toPx() / 2, width.toPx() / 2),
                alpha = alpha
            )
        }
    }
}
