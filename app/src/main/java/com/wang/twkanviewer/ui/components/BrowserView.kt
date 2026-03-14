package com.wang.twkanviewer.ui.components

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun BrowserView(
    webView: WebView,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { webView },
        modifier = modifier
    )
}
