package com.wang.twkanviewer

import android.os.Bundle
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

                ReaderView(
                    url = url,
                    onUrlChange = { url = it },
                    onScrap = { /*TODO: Implement scraping logic*/ },
                    onSave = { /*TODO: Implement save logic*/ }
                )
            }
        }
    }
}