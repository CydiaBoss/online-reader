package com.wang.twkanviewer

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.wang.twkanviewer.ui.components.ReaderView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // A surface container using the 'background' color from the theme
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                var url by remember { mutableStateOf("https://twkan.com") }
                var webView by remember { mutableStateOf<WebView?>(null) }
                var translatedText by remember { mutableStateOf<String?>(null) }

                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.CHINESE)
                    .setTargetLanguage(TranslateLanguage.ENGLISH)
                    .build()
                val translator = Translation.getClient(options)

                var showDialog by remember { mutableStateOf(false) }

                val onTranslate = {
                    webView?.evaluateJavascript("(function() { return document.body.innerText; })();") { content ->
                        translator.downloadModelIfNeeded(DownloadConditions.Builder().build())
                            .addOnSuccessListener {
                                translator.translate(content)
                                    .addOnSuccessListener { text ->
                                        translatedText = text
                                        showDialog = true
                                    }
                                    .addOnFailureListener { exception ->
                                        // Handle translation failure
                                    }
                            }
                            .addOnFailureListener { exception ->
                                // Handle model download failure
                            }
                    }
                }

                ReaderView(
                    url = url,
                    onScrap = {
                        // TODO: Edit the javascript to extract the content you want.
                        webView?.evaluateJavascript("(function() { return document.body.innerText; })();") { content ->
                            // The 'content' variable now holds the extracted text.
                            // You can process it further here.
                        }
                    },
                    onSave = { /*TODO: Implement save logic*/ },
                    onTranslate = onTranslate, // Pass the translate function
                    onWebViewCreated = { webView = it }
                )

                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = {
                            Text("Translated Text")
                        },
                        text = {
                            Text(translatedText ?: "")
                        },
                        confirmButton = {
                            Button(
                                onClick = { showDialog = false }
                            ) {
                                Text("OK")
                            }
                        }
                    )
                }
            }
        }
    }
}