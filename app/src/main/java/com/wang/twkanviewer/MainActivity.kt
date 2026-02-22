package com.wang.twkanviewer

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.wang.twkanviewer.ui.components.ReaderView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // A surface container using the 'background' color from the theme
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                var url by remember { mutableStateOf("https://twkan.com") }
                var webView by remember { mutableStateOf<WebView?>(null) }

                ReaderView(
                    url = url,
                    onScrap = {
                        // TODO: Edit the javascript to extract the content you want.
                        webView?.evaluateJavascript("(function() { return document.body.innerText; })();") { content ->
                            // The 'content' variable now holds the extracted text.
                            // You can process it further here.
                            println("Extracted Text: $content")
                        }
                    },
                    onSave = { /*TODO: Implement save logic*/ },
                    onWebViewCreated = { webView = it }
                )
            }
        }
    }
}