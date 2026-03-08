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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.wang.twkanviewer.database.AppDatabase
import com.wang.twkanviewer.models.Chapter
import com.wang.twkanviewer.models.Story
import com.wang.twkanviewer.ui.components.BrowserView
import com.wang.twkanviewer.ui.components.ChapterListView
import com.wang.twkanviewer.ui.components.ChapterView
import com.wang.twkanviewer.ui.components.StoryView
import com.wang.twkanviewer.ui.theme.TWKANViewerTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    enum class ViewState {
        BROWSER, STORY, CHAPTER_LIST, CHAPTER
    }

    /**
     * Special Android Web Bridge
     */
    class WebAppInterface(private val onResult: (String) -> Unit) {
        @android.webkit.JavascriptInterface
        fun onContentScraped(html: String) {
            onResult(html)
        }
    }

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
                    val viewHistory = remember { mutableStateListOf<ViewState>() }
                    var currentViewState by remember { mutableStateOf(ViewState.BROWSER) }

                    val currentChapters = remember { mutableStateListOf<Chapter>() }
                    var currentStory: Story? by remember { mutableStateOf(null) }
                    var currentChapter: Chapter? by remember { mutableStateOf(null) }
                    var showTranslate by remember { mutableStateOf(false) }
                    var translatedText by remember { mutableStateOf<String?>(null) }
                    
                    var chapterFontSize by remember { mutableFloatStateOf(16f) }
                    var showTopBar by remember { mutableStateOf(true) }

                    // Database
                    val db = AppDatabase.getDatabase(this)
                    val storyDao = db.storyDao()
                    val chapterDao = db.chapterDao()

                    // Coroutine Scope
                    val scope = rememberCoroutineScope()

                    fun navigateTo(state: ViewState) {
                        if (currentViewState != state) {
                            viewHistory.add(currentViewState)
                            currentViewState = state
                        }
                    }

                    // WebView
                    var currentUrl by remember { mutableStateOf("https://twkan.com") }
                    @SuppressLint("SetJavaScriptEnabled")
                    val webView = remember {
                        WebView(this).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            // Link to Android Bridge
                            addJavascriptInterface(WebAppInterface({ content ->
                                // Help with bg js thread
                                scope.launch {
                                    // Validate url
                                    if (url == null) return@launch
                                    val loadedUrl = url!!

                                    // The 'content' variable is a JSON-encoded string, so it needs to be unescaped.
                                    val unescapedHtml =
                                        content.trim().removeSurrounding("\"")
                                            .replace("\\u003C", "<")
                                            .replace("\\n", "\n")
                                            .replace("\\t", "\t")
                                            .replace("\\\"", "\"")

                                    // Parse the HTML with Jsoup
                                    val doc = Ksoup.parse(unescapedHtml)

                                    // Process if book URL
                                    val matchBookUrl = regexBook.find(loadedUrl)
                                    if (matchBookUrl != null) {
                                        val title =
                                            doc.selectFirst("div.booknav2 h1 a")?.text()
                                                ?: ""
                                        val imgUrl =
                                            doc.selectFirst("div.bookimg2 img")?.attr("src")
                                                ?: ""
                                        val genre =
                                            doc.selectFirst("div.booknav2 a[href*=/novels/class/]")
                                                ?.text() ?: ""
                                        val author =
                                            doc.selectFirst("div.booknav2 a[href*=/author/]")
                                                ?.text() ?: ""
                                        val description =
                                            doc.selectFirst("div.navtxt > p")?.text() ?: ""
                                        val completed =
                                            doc.selectFirst("span.status1") != null
                                        var wordCount = 0.0F
                                        var lastUpdated = Date()
                                        doc.select("div.booknav2 > p").forEach { p ->
                                            val text = p.text()

                                            // Word Count
                                            val matchWordCounter =
                                                regexWordCount.matchEntire(text)
                                            if (matchWordCounter != null) {
                                                wordCount =
                                                    matchWordCounter.groupValues[1].toFloat()
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
                                            val matchUpdateAt =
                                                regexUpdateAt.matchEntire(text)
                                            if (matchUpdateAt != null) {
                                                lastUpdated =
                                                    dateFormat.parse(matchUpdateAt.groupValues[1])
                                                        ?: Date()
                                                return@forEach
                                            }
                                        }
                                        val tags =
                                            doc.select("div.tagul a").map { it.text() }

                                        currentStory = Story(
                                            id = matchBookUrl.groupValues[1].toInt(),
                                            url = loadedUrl,
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
                                        return@launch
                                    }

                                    // Process if chapters URL
                                    val matchChaptersUrl = regexChapters.find(currentUrl)
                                    if (matchChaptersUrl != null) {
                                        // Find Chapters
                                        val elements = doc.select("div#allchapter ul li a")

                                        // Validate if an update is needed for the list
                                        if (
                                            currentChapters.isNotEmpty() &&
                                            currentChapters.first().storyId == matchChaptersUrl.groupValues[1].toInt() &&
                                            currentChapters.size == elements.size
                                        ) {
                                            return@launch
                                        }

                                        // Clear
                                        currentChapters.clear()

                                        // Parse chapters
                                        var i = 0
                                        elements.forEach { cLink ->
                                            // Parse chapter url
                                            val tokens =
                                                regexTxt.find(cLink.attr("href"))
                                                    ?: return@forEach
                                            currentChapters.add(
                                                Chapter(
                                                    id = tokens.groupValues[2].toInt(),
                                                    storyId = currentStory!!.id,
                                                    order = i,
                                                    title = cLink.text().trim(),
                                                    url = cLink.attr("href"),
                                                    uploadedAt = null,
                                                    content = null
                                                )
                                            )
                                            i++;
                                        }
                                        return@launch
                                    }

                                    // Process if txt URL
                                    val matchTxtUrl = regexTxt.find(currentUrl)
                                    if (regexTxt.containsMatchIn(currentUrl)) {
                                        // Parse chapter page here
                                        val chapterDate = dateTimeFormat.parse(
                                            doc.selectFirst("div.txtinfo > span")?.text()!!
                                        )
                                        
                                        // Preserve newlines from <p> and <br> tags
                                        val contentElement = doc.selectFirst("div#txtcontent0")
                                        contentElement?.select("p")?.prepend("\n")
                                        contentElement?.select("br")?.prepend("\n")
                                        var chapterContent = contentElement?.text() ?: ""

                                        // Clean
                                        if (currentChapter != null)
                                            chapterContent = chapterContent.replaceFirst(currentChapter!!.title, "")
                                        chapterContent.replace(Regex("[\n ]+"), "\n")

                                        // Update the currentChapter state with a new object to trigger recomposition
                                        currentChapter?.let {
                                            if (it.url == currentUrl) {
                                                currentChapter = it.copy(
                                                    uploadedAt = chapterDate,
                                                    content = chapterContent.trim()
                                                )
                                            }
                                        }

                                        return@launch
                                    }
                                }
                            }), "AndroidBridge")

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    return request != null && request.url.host?.contains("twkan.com") == true && super.shouldOverrideUrlLoading(view, request)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    // Scrap website for story details
                                    if (url != null) {
                                        evaluateJavascript("""
                                            (async function() {
                                                let mainContainer = document.body.getElementsByClassName('container')[0];
                                                if (!mainContainer) return;
                                        
                                                async function scrape() {
                                                    if (typeof LoadMore === 'function') {
                                                        let loadMoreButton = document.body.getElementsByClassName('more-btn')[0];
                                                        if (loadMoreButton) {
                                                            await new Promise((resolve) => {
                                                                let observer = new MutationObserver((mutations, obs) => {
                                                                    for (const mutation of mutations) {
                                                                        if (mutation.type !== 'childList') continue;
                                                                        for (const removedNode of mutation.removedNodes) {
                                                                            if (removedNode === loadMoreButton || removedNode.contains(loadMoreButton)) {
                                                                                obs.disconnect();
                                                                                resolve();
                                                                                return;
                                                                            }
                                                                        }
                                                                    }
                                                                });
                                                                observer.observe(mainContainer, { childList: true, subtree: true });
                                                                LoadMore();
                                                                // Fallback timeout in case LoadMore fails
                                                                setTimeout(resolve, 5000); 
                                                            });
                                                        }
                                                    }
                                                    AndroidBridge.onContentScraped(mainContainer.outerHTML);
                                                }
                                                
                                                scrape();
                                            })();
                                        """.trimIndent(), null)
                                    }
                                    super.onPageFinished(view, url)
                                }

                                override fun doUpdateVisitedHistory(view: WebView?, urlLocal: String?, isReload: Boolean) {
                                    currentUrl = urlLocal!!
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

                            loadUrl(currentUrl)
                        }
                    }
                    val isScrappableUrl = remember(currentUrl) {
                        regexBook.containsMatchIn(currentUrl) ||
                        regexChapters.containsMatchIn(currentUrl) ||
                        regexTxt.containsMatchIn(currentUrl)
                    }

                    // Translator
                    val targetLanguage = remember {
                        TranslateLanguage.fromLanguageTag(Locale.getDefault().language) ?: TranslateLanguage.ENGLISH
                    }
                    val translator = remember(targetLanguage) {
                        val options = TranslatorOptions.Builder()
                            .setSourceLanguage(TranslateLanguage.CHINESE)
                            .setTargetLanguage(targetLanguage)
                            .build()
                        Translation.getClient(options)
                    }

                    // Function
                    val onShowScrapped: (Boolean) -> Unit = {
                        if (it && isScrappableUrl) {
                            if (regexBook.find(currentUrl) != null)
                                navigateTo(ViewState.STORY)
                            else if (regexChapters.find(currentUrl) != null)
                                navigateTo(ViewState.CHAPTER_LIST)
                            else if (regexTxt.find(currentUrl) != null)
                                navigateTo(ViewState.CHAPTER)
                        }else{
                            navigateTo(ViewState.BROWSER)
                        }
                    }
                    val onShowChapters: () -> Unit = {
                        if (currentStory != null) {
                            val indexUrl = currentStory!!.url.replace(".html", "/index.html")
                            if (currentChapters.isEmpty() || currentChapters.first().storyId != currentStory!!.id) {
                                // Clear
                                currentChapters.clear()
                            }
                            // Always sync WebView URL when entering chapter list
                            webView.loadUrl(indexUrl)
                            navigateTo(ViewState.CHAPTER_LIST)
                        }
                    }
                    val onBackToStory: () -> Unit = {
                        if (currentStory != null) {
                            webView.loadUrl(currentStory!!.url)
                        }
                        navigateTo(ViewState.STORY)
                    }
                    val onShowChapter: (Chapter) -> Unit = {
                        webView.loadUrl(it.url)
                        currentChapter = it
                        translatedText = null
                        navigateTo(ViewState.CHAPTER)
                    }
                    val onPreviousChapter: () -> Unit = {
                        currentChapter?.let { chapter ->
                            val index = currentChapters.indexOfFirst { it.url == chapter.url }
                            if (index > 0) {
                                onShowChapter(currentChapters[index - 1])
                            }
                        }
                    }
                    val onNextChapter: () -> Unit = {
                        currentChapter?.let { chapter ->
                            val index = currentChapters.indexOfFirst { it.url == chapter.url }
                            if (index != -1 && index < currentChapters.size - 1) {
                                onShowChapter(currentChapters[index + 1])
                            }
                        }
                    }
                    val onTranslate: (Boolean) -> Unit = {
                        showTranslate = it
                        if (it) {
                            val textToTranslate = when (currentViewState) {
                                ViewState.CHAPTER -> currentChapter?.content
                                ViewState.STORY -> currentStory?.description
                                else -> null
                            }

                            if (textToTranslate != null) {
                                translator.downloadModelIfNeeded(DownloadConditions.Builder().build())
                                    .addOnSuccessListener {
                                        translator.translate(textToTranslate)
                                            .addOnSuccessListener { translated ->
                                                translatedText = translated
                                            }
                                    }
                            }
                        } else {
                            translatedText = null
                        }
                    }
                    val onSave: () -> Unit = {
                        scope.launch {
                            currentStory?.let { story ->
                                storyDao.insert(story)
                                currentChapters.forEach { chapter ->
                                    chapter.storyId = story.id
                                }
                                chapterDao.insertAll(currentChapters)
                            }
                        }
                    };

                    // Back Handler
                    BackHandler(enabled = true) {
                        if (viewHistory.isNotEmpty()) {
                            val previousState = viewHistory.removeAt(viewHistory.size - 1)
                            
                            // SYNC WEBVIEW URL when returning to browser or other screens
                            if (previousState == ViewState.STORY && currentStory != null) {
                                webView.loadUrl(currentStory!!.url)
                            } else if (previousState == ViewState.CHAPTER_LIST && currentStory != null) {
                                webView.loadUrl(currentStory!!.url.replace(".html", "/index.html"))
                            } else if (previousState == ViewState.CHAPTER && currentChapter != null) {
                                webView.loadUrl(currentChapter!!.url)
                            }
                            
                            currentViewState = previousState
                            showTopBar = true // Reset top bar when going back
                        } else if (webView.canGoBack()) {
                            webView.goBack()
                        } else {
                            finish()
                        }
                    }

                    // Layout
                    Column {
                        if (showTopBar) {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.app_name)
                                    )
                                },
                                actions = {
                                    val isScrappedView = currentViewState != ViewState.BROWSER
                                    IconToggleButton(
                                        checked = isScrappedView,
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
                                        enabled = isScrappableUrl && currentStory != null && (currentViewState == ViewState.CHAPTER || currentViewState == ViewState.STORY)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.translate_24px),
                                            contentDescription = "translator"
                                        )
                                    }
                                }
                            )
                        }

                        // Check one of the views
                        when (currentViewState) {
                            ViewState.STORY -> 
                                currentStory?.let {
                                    StoryView(
                                        story = if (showTranslate && translatedText != null) it.copy(description = translatedText!!) else it,
                                        onChapterClick = onShowChapters,
                                        onSave = onSave
                                    )
                                }
                            ViewState.CHAPTER_LIST -> 
                                ChapterListView(
                                    chapters = currentChapters,
                                    onClickChapter = onShowChapter,
                                    onBackToStoryClick = onBackToStory
                                )
                            ViewState.CHAPTER -> 
                                currentChapter?.let {
                                    ChapterView(
                                        chapter = if (showTranslate && translatedText != null) it.copy(content = translatedText!!) else it,
                                        fontSize = chapterFontSize,
                                        onFontSizeChange = { newSize -> chapterFontSize = newSize },
                                        onPreviousClick = onPreviousChapter,
                                        onNextClick = onNextChapter,
                                        onBackClick = onShowChapters,
                                        onToggleBars = { visible -> showTopBar = visible }
                                    )
                                }
                            ViewState.BROWSER -> 
                                BrowserView(webView = webView)
                        }
                    }
                }
            }
        }
    }
}