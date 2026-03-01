package com.wang.twkanviewer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fleeksoft.ksoup.Ksoup
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.wang.twkanviewer.models.Chapter
import com.wang.twkanviewer.models.Story
import com.wang.twkanviewer.ui.components.BrowserView
import com.wang.twkanviewer.ui.components.ReaderView
import com.wang.twkanviewer.ui.components.StoryView
import com.wang.twkanviewer.ui.theme.TWKANViewerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // A surface container using the 'background' color from the theme
            TWKANViewerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // Regex
                    val regexBook = Regex("/book/")
                    val regexTxt = Regex("/txt/")
                    val regexWordCount = Regex("((?:\\d+\\.)?\\d+)(\\w)字.+")
                    val regexUpdateAt = Regex("更新：(\\d{4}-\\d{2}-\\d{2}).+")
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                    // WebView
                    var url by remember { mutableStateOf("https://twkan.com") }

                    @SuppressLint("SetJavaScriptEnabled")
                    val webView = remember {
                        WebView(this).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webViewClient = object : WebViewClient() {
                                override fun doUpdateVisitedHistory(
                                    view: WebView?,
                                    urlLocal: String?,
                                    isReload: Boolean
                                ) {
                                    url = urlLocal!!
                                    super.doUpdateVisitedHistory(view, urlLocal, isReload)
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
                        }
                    }
                    val isBookOrTxt = remember(url) {
                        regexBook.containsMatchIn(url) || regexTxt.containsMatchIn(url)
                    }

                    // Translator
                    val options = TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.CHINESE)
                        .setTargetLanguage(TranslateLanguage.ENGLISH)
                        .build()
                    val translator = Translation.getClient(options)

                    // State
                    var showDialog by remember { mutableStateOf(false) }
                    var showStory: Story? by remember { mutableStateOf(null) }
                    var showTranslate by remember { mutableStateOf(false) }

                    // Function
                    val onScrap: (Boolean) -> Unit = {
                        if (it && isBookOrTxt)
                        // Scrap website for story details
                            webView.evaluateJavascript("document.body.getElementsByClassName('container')[0].outerHTML;") { content ->
                                // The 'content' variable is a JSON-encoded string, so it needs to be unescaped.
                                val unescapedHtml = content.trim().removeSurrounding("\"")
                                    .replace("\\u003C", "<")
                                    .replace("\\n", "\n")
                                    .replace("\\t", "\t")
                                    .replace("\\\"", "\"")

                                // Parse the HTML with Jsoup
                                val doc = Ksoup.parse(unescapedHtml)

                                // Process as needed
                                if (regexBook.containsMatchIn(url)) {
                                    val title = doc.selectFirst("div.booknav2 h1 a")?.text() ?: ""
                                    val imgUrl = doc.selectFirst("div.bookimg2 img")?.attr("src") ?: ""
                                    val genre = doc.selectFirst("div.booknav2 a[href*=/novels/class/]")?.text() ?: ""
                                    val author = doc.selectFirst("div.booknav2 a[href*=/author/]")?.text() ?: ""
                                    val description = doc.selectFirst("div.navtxt > p")?.text() ?: ""
                                    val completed = doc.selectFirst("span.status1") != null
                                    var wordCount = 0.0F
                                    var lastUpdated = Date()
                                    doc.select("div.booknav2 > p").forEach { p ->
                                        val text = p.text()
                                        println("ptag: $text")

                                        // Word Count
                                        val matchWordCounter = regexWordCount.matchEntire(text)
                                        if (matchWordCounter != null) {
                                            wordCount = matchWordCounter.groupValues[1].toFloat()
                                            // Multiplier
                                            when (matchWordCounter.groupValues[2]) {
                                                "百" -> wordCount *= 100
                                                "千" -> wordCount *= 1000
                                                "萬", "万" -> wordCount *= 10000
                                                "億", "亿" -> wordCount *= 100000000
                                            }
                                            return@forEach
                                        }

                                        // Updated At
                                        val matchUpdateAt = regexUpdateAt.matchEntire(text)
                                        if (matchUpdateAt != null) {
                                            lastUpdated =
                                                dateFormat.parse(matchUpdateAt.groupValues[1])
                                                    ?: Date()
                                            return@forEach
                                        }
                                    }
                                    val tags = doc.select("div.tagul a").map { it.text() }

                                    showStory = Story(
                                        title = title,
                                        imgUrl = imgUrl,
                                        genre = genre,
                                        author = author,
                                        description = description,
                                        completed = completed,
                                        wordCount = wordCount.toInt(),
                                        lastUpdated = lastUpdated,
                                        tags = tags,
                                        chapters = mutableListOf()
                                    )
                                } else if (regexTxt.containsMatchIn(url)) {
                                    // Parse chapter page here
                                }
                            }
                        else
                            showStory = null
                    }
                    val onTranslate: (Boolean) -> Unit = {
                        showTranslate = it
                        if (it)
                            webView.evaluateJavascript("(function() { return document.body.innerText; })();") { content ->
                                translator.downloadModelIfNeeded(
                                    DownloadConditions.Builder().build()
                                )
                                    .addOnSuccessListener {
                                        translator.translate(content)
                                            .addOnSuccessListener { text ->
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
                    val onSave: () -> Unit = {};

                    // Back Handler
                    BackHandler(enabled = true) {
                        if (showStory != null) {
                            showStory = null
                        } else if (webView.canGoBack()) {
                            webView.goBack()
                        } else {
                            finish()
                        }
                    }

                    // Layout
                    Column {
                        TopAppBar(
                            title = {
                                Text(
                                    //                            fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                    text = stringResource(id = R.string.app_name)
                                )
                            },
                            actions = {
                                IconToggleButton(
                                    checked = showStory != null,
                                    onCheckedChange = onScrap,
                                    enabled = isBookOrTxt
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.scan_24px),
                                        contentDescription = "scrapper"
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                IconToggleButton(
                                    checked = false,
                                    onCheckedChange = onTranslate,
                                    enabled = isBookOrTxt
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.translate_24px),
                                        contentDescription = "translator"
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                IconButton(onClick = onSave, enabled = isBookOrTxt) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.add_24px),
                                        contentDescription = "save"
                                    )
                                }
                            }
                        )
                        if (showStory == null)
                            BrowserView(webView = webView)
                        else
                            StoryView(
                                story = showStory!!,
                                onChapterClick = { /*TODO*/ }
                            )
                    }
                }
            }
        }
    }
}