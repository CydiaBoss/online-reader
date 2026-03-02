package com.wang.twkanviewer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fleeksoft.ksoup.Ksoup
import com.google.android.gms.common.internal.ShowFirstParty
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.wang.twkanviewer.database.AppDatabase
import com.wang.twkanviewer.models.Chapter
import com.wang.twkanviewer.models.Story
import com.wang.twkanviewer.ui.components.BrowserView
import com.wang.twkanviewer.ui.components.ChapterListView
import com.wang.twkanviewer.ui.components.StoryView
import com.wang.twkanviewer.ui.theme.TWKANViewerTheme
import kotlinx.coroutines.launch
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
                Surface( modifier = Modifier.fillMaxSize()) {
                    // Regex
                    val regexBook = Regex("/book/(\\d+)\\.html")
                    val regexChapters = Regex("/book/(\\d+)/index\\.html")
                    val regexTxt = Regex("/txt/(\\d+)/(\\d+)")
                    val regexWordCount = Regex("((?:\\d+\\.)?\\d+)(\\p{InCJK_UNIFIED_IDEOGRAPHS})字.+")
                    val regexUpdateAt = Regex("更新：(\\d{4}-\\d{2}-\\d{2}).+")
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                    // State
                    var currentStory: Story? by remember { mutableStateOf(null) }
                    var currentChapters by remember { mutableListOf<Chapter>() }
                    var currentChapter: Chapter? by remember { mutableStateOf(null) }
                    var showStories by remember { mutableStateOf(false) }
                    var showStory by remember { mutableStateOf(false) }
                    var showChapters by remember { mutableStateOf(false) }
                    var showChapter by remember { mutableStateOf(false) }
                    var showTranslate by remember { mutableStateOf(false) }

                    // Database
                    val db = AppDatabase.getDatabase(this)
                    val storyDao = db.storyDao()
                    val chapterDao = db.chapterDao()

                    // Coroutine Scope
                    val scope = rememberCoroutineScope()

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
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    return request?.url?.host?.contains("twkan.com")!! && super.shouldOverrideUrlLoading(view, request)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    // Scrap website for story details
                                    if (url != null)
                                        evaluateJavascript(
                                            "(" +
                                                "function() { " +
                                                    "if (typeof LoadMore === 'function') LoadMore(); " +
                                                    "return document.body.getElementsByClassName('container')[0].outerHTML;" +
                                                "}" +
                                            ")();"
                                        ) { content ->
                                            // The 'content' variable is a JSON-encoded string, so it needs to be unescaped.
                                            val unescapedHtml = content.trim().removeSurrounding("\"")
                                                .replace("\\u003C", "<")
                                                .replace("\\n", "\n")
                                                .replace("\\t", "\t")
                                                .replace("\\\"", "\"")

                                            // Parse the HTML with Jsoup
                                            val doc = Ksoup.parse(unescapedHtml)

                                            // Process if book URL
                                            val matchBookUrl = regexBook.find(url)
                                            if (matchBookUrl != null) {
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

                                                currentStory = Story(
                                                    id = matchBookUrl.groupValues[1].toInt(),
                                                    url = url,
                                                    title = title,
                                                    imgUrl = imgUrl,
                                                    genre = genre,
                                                    author = author,
                                                    description = description,
                                                    completed = completed,
                                                    wordCount = wordCount.toInt(),
                                                    lastUpdated = lastUpdated,
                                                    tags = tags,
                                                )
                                                return@evaluateJavascript
                                            }

                                            // Process if chapters URL
                                            val matchChaptersUrl = regexChapters.find(url)
                                            if (matchChaptersUrl != null) {
                                                // Parse chapters
                                                var i = 1
                                                doc.select("div#allchapter ul li a").forEach { cLink ->
                                                    // Parse chapter url
                                                    val tokens = regexTxt.find(cLink.attr("href"))
                                                    if (tokens == null) return@forEach

                                                    currentStory?.chapters?.add(Chapter(
                                                        id = tokens.groupValues[2].toInt()
                                                        order = i,
                                                        title = cLink.text().trim(),
                                                        url = cLink.attr("href"),
                                                        uploadedAt = null,
                                                        content = null
                                                    ))
                                                    i++;
                                                }
                                                return@evaluateJavascript
                                            }

                                            // Process if txt URL
                                            val matchTxtUrl = regexTxt.find(url)
                                            if (regexTxt.containsMatchIn(url)) {
                                                // Parse chapter page here
                                                val chapterDate = dateTimeFormat.parse(doc.selectFirst("div.txtinfo > span")?.text()!!)
                                                val chapterContent = doc.selectFirst("div.txtcontent0")?.text()!!
                                                val currentChapter = currentStory?.chapters?.find { it.url == url }!!
                                                currentChapter.uploadedAt = chapterDate
                                                currentChapter.content = chapterContent

                                                return@evaluateJavascript
                                            }
                                        }
                                    super.onPageFinished(view, url)
                                }

                                override fun doUpdateVisitedHistory(view: WebView?, urlLocal: String?, isReload: Boolean) {
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
                    val isScrappableUrl = remember(url) {
                        regexBook.containsMatchIn(url) ||
                        regexChapters.containsMatchIn(url) ||
                        regexTxt.containsMatchIn(url)
                    }

                    // Translator
                    val options = TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.CHINESE)
                        .setTargetLanguage(TranslateLanguage.ENGLISH)
                        .build()
                    val translator = Translation.getClient(options)

                    // Function
                    val onShowScrapped: (Boolean) -> Unit = {
                        if (it && isScrappableUrl) {
                            if (regexBook.find(url) != null)
                                showStory = true
                            else if (regexChapters.find(url) != null)
                                showChapters = true
                            else if (regexTxt.find(url) != null)
                                showChapter = true
                        }else{
                            showStories = false
                            showStory = false
                            showChapters = false
                            showTranslate = false
                            showChapter = false
                        }
                    }
                    val onShowChapters: () -> Unit = {
                        if (currentStory != null) {
                            if (currentStory!!.chapters.isEmpty()) {
                                // Load Chapters Page
                                webView.loadUrl(currentStory!!.url.replace(".html", "/index.html"))
                            }
                            showChapters = true
                            showStory = false
                            showStories = false
                        }
                    }
                    val onShowChapter: (Chapter) -> Unit = {}
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
                                                // Translate
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
                    val onSave: () -> Unit = {
                        scope.launch {
                            currentStory?.let { story ->
                                storyDao.insert(story)
                                story.chapters.forEach { chapter ->
                                    chapter.storyId = story.id
                                }
                                chapterDao.insertAll(story.chapters)
                            }
                        }
                    };

                    // Back Handler
                    BackHandler(enabled = true) {
                        if (showStories) {
                            showStories = false
                        } else if (showStory) {
                            showStory = false
                        } else if (showChapters) {
                            showChapters = false
                        } else if (showChapter) {
                            showChapter = false
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
                                    text = stringResource(id = R.string.app_name)
                                )
                            },
                            actions = {
                                IconToggleButton(
                                    checked = showStory || showChapters || showChapter,
                                    onCheckedChange = onShowScrapped,
                                    enabled = isScrappableUrl && currentStory != null
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.scan_24px),
                                        contentDescription = "scrapper"
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                IconToggleButton(
                                    checked = showTranslate,
                                    onCheckedChange = onTranslate,
                                    enabled = isScrappableUrl && currentStory != null
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.translate_24px),
                                        contentDescription = "translator"
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                IconButton(
                                    onClick = onSave,
                                    enabled = isScrappableUrl && currentStory != null
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.add_24px),
                                        contentDescription = "save"
                                    )
                                }
                            }
                        )

                        // Check one of the views
                        if (showStory)
                            StoryView(
                                story = currentStory!!,
                                onChapterClick = onShowChapters
                            )
                        else if (showChapters)
                            ChapterListView(
                                chapters = currentChapters,
                                onClickChapter = onShowChapter
                            )
                        else if (showChapter)
                            ChapterView(
                                chapter = currentChapter!!
                            )
                        else
                            BrowserView(webView = webView)
                    }
                }
            }
        }
    }
}