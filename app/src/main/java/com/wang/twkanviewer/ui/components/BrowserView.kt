package com.wang.twkanviewer.ui.components

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun BrowserView(
    webView: WebView,
) {
    AndroidView(factory = {
        webView
    })
}
