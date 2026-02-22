package com.wang.twkanviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wang.twkanviewer.models.Chapter
import com.wang.twkanviewer.ui.components.ChapterListView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // A surface container using the 'background' color from the theme
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column {
                    Text(text = stringResource(id = R.string.saved_chapters_title))
                    ChapterListView(chapters = listOf(
                        Chapter(title = "Chapter 1", url = "https://example.com/chapter1"),
                        Chapter(title = "Chapter 2", url = "https://example.com/chapter2"),
                        Chapter(title = "Chapter 3", url = "https://example.com/chapter3"),
                    ))
                }
            }
        }
    }
}