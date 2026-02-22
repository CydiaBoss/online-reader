package com.wang.twkanviewer.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.wang.twkanviewer.R

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReaderView(
    url: String,
    onUrlChange: (String) -> Unit,
    onScrap: () -> Unit,
    onSave: () -> Unit
) {
    Column {
        TopAppBar(
            title = { Text(text = stringResource(id = R.string.app_name)) },
            actions = {
                Button(onClick = onScrap) {
                    Text(text = stringResource(id = R.string.scrap_button_label))
                }
                Button(onClick = onSave) {
                    Text(text = stringResource(id = R.string.save_button_label))
                }
            }
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = url,
                onValueChange = onUrlChange,
                label = { Text(text = stringResource(id = R.string.url_textfield_label)) },
                modifier = Modifier.weight(1f)
            )
        }
        AndroidView(factory = {
            WebView(it).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient() // Add this

                // Set a common user-agent string
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Mobile Safari/537.36"

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true

                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                loadUrl(url)
            }
        }, update = {
            CookieManager.getInstance().setAcceptThirdPartyCookies(it, true)
            it.loadUrl(url)
        })
    }
}
