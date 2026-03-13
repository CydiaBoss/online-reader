package com.wang.twkanviewer

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.core.text.HtmlCompat
import com.fleeksoft.ksoup.Ksoup
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.wang.twkanviewer.database.AppDatabase
import com.wang.twkanviewer.models.Chapter
import com.wang.twkanviewer.models.ChapterLocale
import com.wang.twkanviewer.models.Story
import com.wang.twkanviewer.models.StoryLocale
import com.wang.twkanviewer.ui.components.BrowserView
import com.wang.twkanviewer.ui.components.ChapterListView
import com.wang.twkanviewer.ui.components.ChapterView
import com.wang.twkanviewer.ui.components.StoryListView
import com.wang.twkanviewer.ui.components.StoryView
import com.wang.twkanviewer.ui.theme.TWKANViewerTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    enum class ViewState {
        BROWSER, STORY, CHAPTER_LIST, CHAPTER, STORY_LIST
    }

    /**
     * Special Android Web Bridge
     */
    class WebAppInterface(private val onResult: (String) -> Unit) {
        @android.webkit.JavascriptInterface
        fun onContentScraped(html: String) { onResult(html) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://translate-pa.googleapis.com/")
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val translationService = retrofit.create(TranslationService::class.java)

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

                    // Settings
                    val settingsManager = remember { SettingsManager(this) }
                    val savedShowTranslate by settingsManager.showTranslate.collectAsState(initial = false)
                    val savedChapterFontSize by settingsManager.chapterFontSize.collectAsState(initial = 16f)
                    val savedUseExternalTranslator by settingsManager.useExternalTranslator.collectAsState(initial = false)
                    val savedTranslatorApiKey by settingsManager.translatorApiKey.collectAsState(initial = "")
                    val savedUserAgent by settingsManager.userAgent.collectAsState(initial = "")

                    // State
                    var showTopBar by remember { mutableStateOf(true) }
                    var currentViewState by remember { mutableStateOf(ViewState.BROWSER) }
                    val currentChapters = remember { mutableStateListOf<Chapter>() }
                    var currentStory by remember { mutableStateOf<Story?>(null) }
                    var currentChapter by remember { mutableStateOf<Chapter?>(null) }
                    val allStories = remember { mutableStateListOf<Story>() }

                    // Translation
                    var showTranslate by remember { mutableStateOf(false) }
                    var useExternalTranslator by remember { mutableStateOf(false) }
                    var translatorApiKey by remember { mutableStateOf("") }
                    var userAgent by remember { mutableStateOf("") }
                    var translatedStory by remember { mutableStateOf<StoryLocale?>(null) }
                    val translatedChapters = remember { mutableStateListOf<ChapterLocale>() }
                    val allTranslatedStories = remember { mutableStateListOf<StoryLocale>() }
                    
                    // Tracking state to avoid duplicate concurrent translations
                    val translatingIds = remember { mutableStateListOf<Int>() }
                    var isStoryTranslating by remember { mutableStateOf(false) }
                    var isListTranslating by remember { mutableStateOf(false) }
                    var isLibraryTranslating by remember { mutableStateOf(false) }
                    var isModelDownloading by remember { mutableStateOf(false) }

                    val isAnyTranslationActive = isStoryTranslating || isListTranslating || isLibraryTranslating || translatingIds.isNotEmpty() || isModelDownloading

                    // Settings
                    var chapterFontSize by remember { mutableFloatStateOf(16f) }

                    // Sync settings
                    LaunchedEffect(savedShowTranslate) {
                        showTranslate = savedShowTranslate
                    }
                    LaunchedEffect(savedChapterFontSize) {
                        chapterFontSize = savedChapterFontSize
                    }
                    LaunchedEffect(savedUseExternalTranslator) {
                        useExternalTranslator = savedUseExternalTranslator
                    }
                    LaunchedEffect(savedTranslatorApiKey) {
                        translatorApiKey = savedTranslatorApiKey
                    }
                    LaunchedEffect(savedUserAgent) {
                        userAgent = savedUserAgent
                    }

                    // Database
                    val db = AppDatabase.getDatabase(this)
                    val storyDao = db.storyDao()
                    val chapterDao = db.chapterDao()

                    // Coroutine Scope
                    val scope = rememberCoroutineScope()

                    fun navigateTo(state: ViewState) {
                        if (currentViewState != state) {
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
                            addJavascriptInterface(WebAppInterface { content ->
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
                                                    content = emptyList()
                                                )
                                            )
                                            i++
                                        }
                                        return@launch
                                    }

                                    // Process if txt URL
                                    if (regexTxt.containsMatchIn(currentUrl)) {
                                        // Parse chapter page here
                                        val chapterDate = dateTimeFormat.parse(
                                            doc.selectFirst("div.txtinfo > span")?.text()!!
                                        )

                                        // Preserve newlines from <p> and <br> tags
                                        val contentElement = doc.selectFirst("div#txtcontent0")?.text()
                                        val chapterContent = contentElement?.split(Regex("\u2003+")) ?: emptyList()

                                        // Update the currentChapter state with a new object to trigger recomposition
                                        currentChapter?.let {
                                            if (it.url == currentUrl) {
                                                currentChapter = it.copy(
                                                    uploadedAt = chapterDate,
                                                    content = chapterContent
                                                )
                                            }
                                        }

                                        return@launch
                                    }
                                }
                            }, "AndroidBridge")

                            webViewClient = object : WebViewClient() {
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

                            // Set user-agent string from settings
                            settings.userAgentString = userAgent.ifEmpty {
                                "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Mobile Safari/537.36"
                            }

                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.javaScriptCanOpenWindowsAutomatically = true

                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                            loadUrl(currentUrl)
                        }
                    }
                    
                    // Sync userAgent to webView
                    LaunchedEffect(userAgent) {
                        if (userAgent.isNotEmpty()) {
                            webView.settings.userAgentString = userAgent
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

                    suspend fun performTranslation(texts: List<String>): List<String> {
                        if (texts.isEmpty()) return emptyList()

                        return if (useExternalTranslator) {
                            val payload = listOf(
                                listOf(
                                    texts,
                                    "zh",
                                    targetLanguage
                                ),
                                "wt_lib"
                            )
                            val response = translationService.translateHtml(
                                apiKey = translatorApiKey,
                                userAgent = userAgent.ifEmpty { webView.settings.userAgentString },
                                body = payload
                            )
                            try {
                                val jsonResponse = JSONArray(response)
                                val translations = jsonResponse.getJSONArray(0)
                                List(translations.length()) { i ->
                                    val translatedText = translations.getString(i)
                                    HtmlCompat.fromHtml(translatedText, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
                                }
                            } catch (e: Exception) {
                                Log.e("Translate", "Error parsing translation response", e)
                                texts
                            }
                        } else {
                            val delimiter = " [|] "
                            val combinedText = texts.joinToString(delimiter)
                            val translatedCombined = translator.translate(combinedText).await()
                            translatedCombined.split(delimiter).map { it.trim() }
                        }
                    }

                    // State for Story in DB
                    var isCurrentStorySaved by remember { mutableStateOf(false) }
                    LaunchedEffect(currentStory) {
                        isCurrentStorySaved = currentStory?.let { storyDao.getById(it.id) != null } ?: false
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

                        // Toggle Bookmark to current
                        scope.launch {
                            val story = currentStory ?: return@launch
                            val updatedStory = story.copy(bookmarkedChapterId = it.id)
                            currentStory = updatedStory
                            if (storyDao.getById(story.id) != null) {
                                storyDao.insert(updatedStory)
                            }
                        }

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
                    val onShowLibrary: () -> Unit = {
                        scope.launch {
                            allStories.clear()
                            allStories.addAll(storyDao.getAll())
                            allTranslatedStories.clear()
                            allStories.forEach { story ->
                                storyDao.getLocaleByStoryId(story.id, targetLanguage)?.let {
                                    allTranslatedStories.add(it)
                                }
                            }
                            navigateTo(ViewState.STORY_LIST)
                        }
                    }
                    val onDeleteStory: (Story) -> Unit = { story ->
                        scope.launch {
                            storyDao.delete(story)
                            allStories.remove(story)
                            allTranslatedStories.removeAll { it.storyId == story.id }
                            if (currentStory?.id == story.id) isCurrentStorySaved = false
                        }
                    }
                    val onToggleBookmark: () -> Unit = {
                        scope.launch {
                            val story = currentStory ?: return@launch
                            val chapter = currentChapter ?: return@launch
                            
                            val updatedStory = if (story.bookmarkedChapterId == chapter.id) {
                                story.copy(bookmarkedChapterId = null)
                            } else {
                                story.copy(bookmarkedChapterId = chapter.id)
                            }
                            
                            currentStory = updatedStory
                            if (storyDao.getById(story.id) != null) {
                                storyDao.insert(updatedStory)
                            }
                        }
                    }

                    val onToggleExternalTranslator: (Boolean) -> Unit = { enabled ->
                        useExternalTranslator = enabled
                        scope.launch { settingsManager.setUseExternalTranslator(enabled) }
                    }

                    val onTranslate: (Boolean) -> Unit = { enabled ->
                        showTranslate = enabled
                        scope.launch { settingsManager.setShowTranslate(enabled) }
                        if (enabled) {
                            val downloadConditions = DownloadConditions.Builder().build()
                            isModelDownloading = !useExternalTranslator
                            val modelTask = if (!useExternalTranslator) {
                                translator.downloadModelIfNeeded(downloadConditions)
                            } else {
                                null
                            }
                            
                            val startTranslation = {
                                isModelDownloading = false
                                scope.launch {
                                    try {
                                        // Check what to translate
                                        when (currentViewState) {
                                            ViewState.STORY -> {
                                                val story = currentStory ?: return@launch
                                                if (isStoryTranslating) return@launch

                                                // Check memory first
                                                if (translatedStory?.storyId == story.id) return@launch

                                                isStoryTranslating = true
                                                try {
                                                    // Check if we already have it in DB
                                                    val existingLocale = storyDao.getLocaleByStoryId(story.id, targetLanguage)
                                                    if (existingLocale != null) {
                                                        translatedStory = existingLocale
                                                        return@launch
                                                    }

                                                    val storyDetails = listOf(
                                                        story.title,
                                                        story.genre,
                                                        story.description,
                                                        if (story.tags.isEmpty()) "" else story.tags.joinToString(", ")
                                                    )
                                                    val results = performTranslation(storyDetails)

                                                    val newLocale = StoryLocale(
                                                        storyId = story.id,
                                                        language = targetLanguage,
                                                        title = results.getOrElse(0) { story.title },
                                                        genre = results.getOrElse(1) { story.genre },
                                                        description = results.getOrElse(2) { story.description },
                                                        tags = results.getOrElse(3) { "" }
                                                    )
                                                    translatedStory = newLocale

                                                    // Only save to DB if story exists in DB
                                                    if (storyDao.getById(story.id) != null) {
                                                        storyDao.insertLocale(newLocale)
                                                    }
                                                } finally {
                                                    isStoryTranslating = false
                                                }
                                            }
                                            ViewState.CHAPTER_LIST -> {
                                                if (currentChapters.isEmpty()) return@launch
                                                if (isListTranslating) return@launch

                                                isListTranslating = true
                                                try {
                                                    // Ensure memory matches current story
                                                    val currentStoryId = currentStory?.id
                                                    if (currentStoryId != null && translatedChapters.isNotEmpty()) {
                                                        val firstTranslated = translatedChapters.first()
                                                        val matchesCurrent = currentChapters.any { it.id == firstTranslated.chapterId }
                                                        if (!matchesCurrent) {
                                                            translatedChapters.clear()
                                                            translatingIds.clear()
                                                        }
                                                    }

                                                    // Batch translation for chapter titles
                                                    val potentialChapters = currentChapters.filter { chapter ->
                                                        translatedChapters.none { it.chapterId == chapter.id } && !translatingIds.contains(chapter.id)
                                                    }

                                                    if (potentialChapters.isNotEmpty()) {
                                                        scope.launch {
                                                            // Filter out those that already exist in the Database
                                                            val chaptersToTranslate = potentialChapters.filter { chapter ->
                                                                val existing = chapterDao.getLocaleByChapterId(chapter.id, targetLanguage)
                                                                if (existing != null) {
                                                                    translatedChapters.add(existing)
                                                                    false
                                                                } else {
                                                                    true
                                                                }
                                                            }

                                                            if (chaptersToTranslate.isNotEmpty()) {
                                                                chaptersToTranslate.forEach { translatingIds.add(it.id) }
                                                                try {
                                                                    val titles = chaptersToTranslate.map { it.title }
                                                                    val translatedTitles = performTranslation(titles)

                                                                    chaptersToTranslate.forEachIndexed { index, chapter ->
                                                                        val tTitle = translatedTitles.getOrElse(index) { chapter.title }
                                                                        val newLocale = ChapterLocale(
                                                                            chapterId = chapter.id,
                                                                            language = targetLanguage,
                                                                            title = tTitle,
                                                                            content = emptyList()
                                                                        )
                                                                        translatedChapters.add(newLocale)

                                                                        // Only save to DB if chapter exists in DB
                                                                        if (chapterDao.getById(chapter.id) != null) {
                                                                            chapterDao.insertLocale(newLocale)
                                                                        }
                                                                    }
                                                                } catch (e: Exception) {
                                                                    Log.e("Translate", "Error translating chapter titles", e)
                                                                } finally {
                                                                    chaptersToTranslate.forEach { translatingIds.remove(it.id) }
                                                                }
                                                            }
                                                        }
                                                    }
                                                } finally {
                                                    isListTranslating = false
                                                }
                                            }
                                            ViewState.CHAPTER -> {
                                                val chapter = currentChapter ?: return@launch
                                                if (chapter.content.isEmpty()) return@launch // Wait for content

                                                // Check memory first
                                                val memoryLocale = translatedChapters.find { it.chapterId == chapter.id }
                                                val isFullyTranslated = memoryLocale != null &&
                                                                        memoryLocale.content.isNotEmpty() &&
                                                                        memoryLocale.content.size == chapter.content.size &&
                                                                        memoryLocale.content != chapter.content

                                                if (isFullyTranslated) return@launch
                                                if (translatingIds.contains(chapter.id)) return@launch

                                                translatingIds.add(chapter.id)
                                                scope.launch {
                                                    try {
                                                        // 2. Check DB first
                                                        val existingLocale = chapterDao.getLocaleByChapterId(chapter.id, targetLanguage)
                                                        if (existingLocale != null && existingLocale.content.isNotEmpty() && existingLocale.content.size == chapter.content.size) {
                                                            val idx = translatedChapters.indexOfFirst { it.chapterId == chapter.id }
                                                            if (idx == -1) translatedChapters.add(existingLocale)
                                                            else translatedChapters[idx] = existingLocale
                                                            return@launch
                                                        }

                                                        // 3. Translate
                                                        val originalContent = chapter.content
                                                        // Ensure we have an entry in memory for fallback
                                                        val idx = translatedChapters.indexOfFirst { it.chapterId == chapter.id }
                                                        if (idx == -1) {
                                                            translatedChapters.add(ChapterLocale(
                                                                chapterId = chapter.id,
                                                                language = targetLanguage,
                                                                title = chapter.title,
                                                                content = originalContent
                                                            ))
                                                        } else if (translatedChapters[idx].content.isEmpty()) {
                                                            translatedChapters[idx] = translatedChapters[idx].copy(content = originalContent)
                                                        }

                                                        // Batch translate title and content
                                                        val contentToTranslate = listOf(chapter.title) + originalContent
                                                        val translatedResults = performTranslation(contentToTranslate)
                                                        
                                                        val tTitle = translatedResults.getOrElse(0) { chapter.title }
                                                        val tContent = if (translatedResults.size > 1) translatedResults.drop(1) else originalContent

                                                        val updateIdx = translatedChapters.indexOfFirst { it.chapterId == chapter.id }
                                                        if (updateIdx != -1) {
                                                            val updatedLocale = translatedChapters[updateIdx].copy(
                                                                title = tTitle,
                                                                content = tContent
                                                            )
                                                            translatedChapters[updateIdx] = updatedLocale

                                                            // Save to DB
                                                            if (chapterDao.getById(chapter.id) != null) {
                                                                chapterDao.insertLocale(updatedLocale)
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e("Translate", "Chapter translation error", e)
                                                    } finally {
                                                        translatingIds.remove(chapter.id)
                                                    }
                                                }
                                            }
                                            ViewState.STORY_LIST -> {
                                                if (isLibraryTranslating) return@launch
                                                isLibraryTranslating = true
                                                try {
                                                    allStories.forEach { story ->
                                                        if (allTranslatedStories.none { it.storyId == story.id }) {
                                                            scope.launch {
                                                                try {
                                                                    // Check DB first
                                                                    val existing = storyDao.getLocaleByStoryId(story.id, targetLanguage)
                                                                    if (existing != null) {
                                                                        allTranslatedStories.add(existing)
                                                                    } else {
                                                                        // Translate story details
                                                                        val storyDetails = listOf(
                                                                            story.title,
                                                                            story.genre,
                                                                            story.description,
                                                                            if (story.tags.isEmpty()) "" else story.tags.joinToString(", ")
                                                                        )
                                                                        val results = performTranslation(storyDetails)

                                                                        val newLocale = StoryLocale(
                                                                            storyId = story.id,
                                                                            language = targetLanguage,
                                                                            title = results.getOrElse(0) { story.title },
                                                                            genre = results.getOrElse(1) { story.genre },
                                                                            description = results.getOrElse(2) { story.description },
                                                                            tags = results.getOrElse(3) { "" }
                                                                        )
                                                                        allTranslatedStories.add(newLocale)
                                                                        storyDao.insertLocale(newLocale)
                                                                    }
                                                                } catch (e: Exception) {
                                                                    Log.e("Translate", "Error translating story in library", e)
                                                                }
                                                            }
                                                        }
                                                    }
                                                } finally {
                                                    isLibraryTranslating = false
                                                }
                                            }
                                            ViewState.BROWSER -> {}
                                        }
                                    } catch (e: Exception) {
                                        Log.e("Translate", "Error in onTranslate", e)
                                    }
                                }
                            }

                            if (modelTask != null) {
                                modelTask.addOnSuccessListener { startTranslation() }
                                    .addOnFailureListener {
                                        isModelDownloading = false
                                        Log.e("Translate", "Model download failed", it)
                                    }
                            } else {
                                startTranslation()
                            }
                        }
                    }
                    val onSave: () -> Unit = {
                        scope.launch {
                            currentStory?.let { story ->
                                storyDao.insert(story)
                                isCurrentStorySaved = true
                                // Also save the translated story if it exists
                                translatedStory?.let { locale ->
                                    if (locale.storyId == story.id) {
                                        storyDao.insertLocale(locale)
                                    }
                                }

                                currentChapters.forEach { chapter ->
                                    chapter.storyId = story.id
                                    chapterDao.insertAll(listOf(chapter))
                                    
                                    // Also save any existing translation for this chapter
                                    translatedChapters.find { it.chapterId == chapter.id }?.let { locale ->
                                        chapterDao.insertLocale(locale)
                                    }
                                }
                            }
                        }
                    }

                    // This effect will run whenever the view changes. If translation is enabled,
                    // it will automatically trigger the translation for the new view.
                    LaunchedEffect(currentViewState, showTranslate, currentStory, currentChapters.size, currentChapter, useExternalTranslator) {
                        if (showTranslate) {
                            onTranslate(true)
                        }
                    }

                    // Back Handler
                    BackHandler(enabled = true) {
                        showTopBar = true
                        when(currentViewState) {
                            ViewState.BROWSER -> {
                                if (webView.canGoBack()) webView.goBack()
                                else finish()
                            }
                            ViewState.STORY -> currentViewState = ViewState.BROWSER
                            ViewState.CHAPTER_LIST -> {
                                currentViewState = ViewState.STORY
                                webView.goBack()
                            }
                            ViewState.CHAPTER -> {
                                currentViewState = ViewState.CHAPTER_LIST
                                webView.goBack()
                            }
                            ViewState.STORY_LIST -> currentViewState = ViewState.BROWSER
                        }
                    }

                    // Layout
                    Column {
                        if (currentViewState != ViewState.CHAPTER || showTopBar) {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.app_name)
                                    )
                                },
                                actions = {
                                    IconButton(onClick = onShowLibrary) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.twkan_icon_foreground),
                                            contentDescription = "Library",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    val isScrappedView = currentViewState != ViewState.BROWSER && currentViewState != ViewState.STORY_LIST
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
                                        checked = useExternalTranslator,
                                        onCheckedChange = onToggleExternalTranslator,
                                        enabled = showTranslate
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.sort_24px),
                                            contentDescription = "External Translator",
                                            tint = if (useExternalTranslator) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    IconToggleButton(
                                        checked = showTranslate,
                                        onCheckedChange = onTranslate,
                                        enabled = currentViewState != ViewState.BROWSER && (currentViewState == ViewState.STORY_LIST || (isScrappableUrl && currentStory != null))
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.translate_24px),
                                            contentDescription = "translator"
                                        )
                                    }
                                }
                            )
                        }

                        if (isAnyTranslationActive) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        // Check one of the views
                        when (currentViewState) {
                            ViewState.STORY -> 
                                currentStory?.let {
                                    StoryView(
                                        story = it,
                                        isSaved = isCurrentStorySaved,
                                        showTranslate = showTranslate,
                                        translatedStory = translatedStory,
                                        onChapterClick = onShowChapters,
                                        onSave = onSave,
                                        onDelete = { onDeleteStory(it) }
                                    )
                                }
                            ViewState.CHAPTER_LIST -> 
                                ChapterListView(
                                    chapters = currentChapters,
                                    bookmarkedChapterId = currentStory?.bookmarkedChapterId,
                                    showTranslate = showTranslate,
                                    translatedChapters = translatedChapters,
                                    onClickChapter = onShowChapter,
                                    onBackToStoryClick = onBackToStory
                                )
                            ViewState.CHAPTER -> 
                                currentChapter?.let {
                                    ChapterView(
                                        chapter = it,
                                        isBookmarked = it.id == currentStory?.bookmarkedChapterId,
                                        showTranslate = showTranslate,
                                        translatedChapter = translatedChapters.find { tChap -> tChap.chapterId == it.id },
                                        fontSize = chapterFontSize,
                                        onFontSizeChange = { newSize -> 
                                            chapterFontSize = newSize
                                            scope.launch { settingsManager.setChapterFontSize(newSize) }
                                        },
                                        onBookmarkClick = onToggleBookmark,
                                        onPreviousClick = onPreviousChapter,
                                        onNextClick = onNextChapter,
                                        onBackClick = onShowChapters,
                                        onToggleBars = { visible -> showTopBar = visible }
                                    )
                                }
                            ViewState.STORY_LIST ->
                                StoryListView(
                                    stories = allStories,
                                    showTranslate = showTranslate,
                                    translatedStories = allTranslatedStories,
                                    onBackClick = { currentViewState = ViewState.BROWSER },
                                    onStoryClick = { story ->
                                        currentStory = story
                                        webView.loadUrl(story.url)
                                        currentViewState = ViewState.STORY
                                    },
                                    onDeleteStory = onDeleteStory
                                )
                            ViewState.BROWSER -> 
                                BrowserView(webView = webView)
                        }
                    }
                }
            }
        }
    }
}
