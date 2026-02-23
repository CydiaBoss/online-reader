package com.wang.twkanviewer.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.wang.twkanviewer.R

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReaderView(
    url: String,
    onScrap: (Boolean) -> Unit,
    onSave: () -> Unit,
    onTranslate: (Boolean) -> Unit,
    onWebViewCreated: (WebView) -> Unit
) {
    var webViewUrl by remember { mutableStateOf(url) }
    val isBookOrTxt = remember(webViewUrl) {
        val regex = Regex("(/book/)|(/txt/)")
        regex.containsMatchIn(webViewUrl)
    }

    Column {
        TopAppBar(
            title = { Text(text = stringResource(id = R.string.app_name)) },
            actions = {
                IconToggleButton(checked = false, onCheckedChange = onScrap, enabled = isBookOrTxt) {
                    Text(text = stringResource(id = R.string.scrap_button_label))
                }
                Spacer(Modifier.width(8.dp))
                IconToggleButton(
                    checked = false,
                    onCheckedChange = onTranslate,
                    enabled = isBookOrTxt
                ) {
                    Text(text = stringResource(id = R.string.translate_button_label))
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onSave, enabled = isBookOrTxt) {
                    Text(text = stringResource(id = R.string.save_button_label))
                }
            }
        )
        AndroidView(factory = {
            WebView(it).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        webViewUrl = url ?: ""
                    }
                }
                webChromeClient = WebChromeClient() // Add this

                // Set a common user-agent string
                settings.userAgentString =
                    "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Mobile Safari/537.36"

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true

                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                loadUrl(url)
                onWebViewCreated(this)
            }
        }, update = {
            CookieManager.getInstance().setAcceptThirdPartyCookies(it, true)
            // Only load a new URL if it's different from the one currently in the WebView
            if (url != it.url) {
                it.loadUrl(url)
            }
        })
    }
}
