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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.wang.twkanviewer.ui.components.SettingsView
import com.wang.twkanviewer.ui.components.StoryListView
import com.wang.twkanviewer.ui.components.StoryView
import com.wang.twkanviewer.ui.theme.TWKANViewerTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
        BROWSER, STORY, CHAPTER_LIST, CHAPTER, STORY_LIST, SETTINGS
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
            .baseUrl(getString(R.string.translation_api_base_url))
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
                    var visibleChapterIds by remember { mutableStateOf(emptySet<Int>()) }
                    var currentStory by remember { mutableStateOf<Story?>(null) }
                    var currentChapter by remember { mutableStateOf<Chapter?>(null) }
                    val allStories = remember { mutableStateListOf<Story>() }

                    // Scroll State
                    val chapterListState = rememberLazyListState()
                    var lastScrolledStoryId by remember { mutableStateOf<Int?>(null) }

                    // Translation
                    var showTranslate by remember { mutableStateOf(false) }
                    var useExternalTranslator by remember { mutableStateOf(false) }
                    var translatorApiKey by remember { mutableStateOf("") }
                    var userAgent by remember { mutableStateOf("") }
                    var translatedStory by remember { mutableStateOf<StoryLocale?>(null) }
                    val translatedChapters = remember { mutableStateListOf<ChapterLocale>() }
                    val allTranslatedStories = remember { mutableStateListOf<StoryLocale>() }
                    var lastTranslatedStoryId by remember { mutableStateOf<Int?>(null) }
                    
                    // Tracking state to avoid duplicate concurrent translations
                    val translatingIds = remember { mutableStateListOf<Int>() }
                    var isStoryTranslating by remember { mutableStateOf(false) }
                    var isListTranslating by remember { mutableStateOf(false) }
                    var isLibraryTranslating by remember { mutableStateOf(false) }
                    var isModelDownloading by remember { mutableStateOf(false) }
                    var listTranslationJob by remember { mutableStateOf<Job?>(null) }
                    var libraryTranslationJob by remember { mutableStateOf<Job?>(null) }

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
                    val defaultUrl = stringResource(id = R.string.default_url)
                    val defaultUserAgent = stringResource(id = R.string.default_user_agent)
                    var currentUrl by remember { mutableStateOf(defaultUrl) }
                    
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
                                    val loadedUrl = url ?: return@launch

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
                                        val storyId = matchBookUrl.groupValues[1].toInt()
                                        
                                        // Preserve existing bookmark if we already have this story in DB or state
                                        val existingStory = storyDao.getById(storyId)
                                        val existingBookmark = existingStory?.bookmarkedChapterId 
                                            ?: currentStory?.takeIf { it.id == storyId }?.bookmarkedChapterId

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

                                        val scrapedStory = Story(
                                            id = storyId,
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
                                            bookmarkedChapterId = existingBookmark
                                        )
                                        currentStory = scrapedStory
                                        
                                        // Update database if story is in library
                                        if (existingStory != null) {
                                            storyDao.insert(scrapedStory)
                                        }
                                        return@launch
                                    }

                                    // Process if chapters URL
                                    val matchChaptersUrl = regexChapters.find(loadedUrl)
                                    if (matchChaptersUrl != null) {
                                        val storyId = matchChaptersUrl.groupValues[1].toInt()
                                        val elements = doc.select("div#allchapter ul li a")

                                        // If it's the same story, check for new chapters
                                        if (currentChapters.isNotEmpty() && currentChapters.first().storyId == storyId) {
                                            if (elements.size > currentChapters.size) {
                                                // Append new chapters
                                                val newChapters = elements.drop(currentChapters.size).mapIndexedNotNull { index, cLink ->
                                                    val tokens = regexTxt.find(cLink.attr("href")) ?: return@mapIndexedNotNull null
                                                    Chapter(
                                                        id = tokens.groupValues[2].toInt(),
                                                        storyId = storyId,
                                                        order = currentChapters.size + index,
                                                        title = cLink.text().trim(),
                                                        url = cLink.attr("href"),
                                                        uploadedAt = null,
                                                        content = emptyList()
                                                    )
                                                }
                                                
                                                if (newChapters.isNotEmpty()) {
                                                    currentChapters.addAll(newChapters)
                                                    if (storyDao.getById(storyId) != null) {
                                                        chapterDao.insertAll(newChapters)
                                                    }
                                                }
                                            }
                                            return@launch
                                        }

                                        // Otherwise full rebuild or first load
                                        val allChapters = elements.mapIndexedNotNull { index, cLink ->
                                            val tokens = regexTxt.find(cLink.attr("href")) ?: return@mapIndexedNotNull null
                                            Chapter(
                                                id = tokens.groupValues[2].toInt(),
                                                storyId = storyId,
                                                order = index,
                                                title = cLink.text().trim(),
                                                url = cLink.attr("href"),
                                                uploadedAt = null,
                                                content = emptyList()
                                            )
                                        }
                                        
                                        // Avoid UI flicker: only update if data changed
                                        if (allChapters.size != currentChapters.size || (allChapters.isNotEmpty() && allChapters.first().id != currentChapters.first().id)) {
                                            currentChapters.clear()
                                            currentChapters.addAll(allChapters)
                                        }
                                        
                                        // Persist if story is in library
                                        if (storyDao.getById(storyId) != null) {
                                            chapterDao.insertAll(allChapters)
                                        }
                                        return@launch
                                    }

                                    // Process if txt URL
                                    if (regexTxt.containsMatchIn(loadedUrl)) {
                                        // Parse chapter page here
                                        val chapterDateStr = doc.selectFirst("div.txtinfo > span")?.text()
                                        val chapterDate = if (chapterDateStr != null) {
                                            try { dateTimeFormat.parse(chapterDateStr) } catch (_: Exception) { null }
                                        } else null

                                        // Preserve newlines from <p> and <br> tags
                                        val contentElement = doc.selectFirst("div#txtcontent0")?.text()
                                        val chapterContent = contentElement?.split(Regex("\u2003+")) ?: emptyList()

                                        // Update the currentChapter state with a new object to trigger recomposition
                                        currentChapter?.let {
                                            if (it.url == loadedUrl) {
                                                val updatedChapter = it.copy(
                                                    uploadedAt = chapterDate,
                                                    content = chapterContent
                                                )
                                                currentChapter = updatedChapter
                                                
                                                // Also update in the list to cache it
                                                val index = currentChapters.indexOfFirst { c -> c.url == loadedUrl }
                                                if (index != -1) {
                                                    currentChapters[index] = updatedChapter
                                                }
                                                
                                                // Persist if story is in library
                                                currentStory?.let { story ->
                                                    if (storyDao.getById(story.id) != null) {
                                                        chapterDao.insertAll(listOf(updatedChapter))
                                                    }
                                                }
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
                            settings.userAgentString = userAgent.ifEmpty { defaultUserAgent }

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

                        // Using larger chunks for titles to reduce overhead
                        val chunkSize = if (useExternalTranslator) 100 else 250
                        val results = mutableListOf<String>()

                        for (chunk in texts.chunked(chunkSize)) {
                            val chunkResults = if (useExternalTranslator) {
                                val payload = listOf(
                                    listOf(chunk, "zh", targetLanguage),
                                    "wt_lib"
                                )
                                try {
                                    val response = translationService.translateHtml(
                                        apiKey = translatorApiKey,
                                        userAgent = userAgent.ifEmpty { webView.settings.userAgentString },
                                        body = payload
                                    )
                                    val jsonResponse = JSONArray(response)
                                    val translations = jsonResponse.getJSONArray(0)
                                    List(translations.length()) { i ->
                                        val translatedText = translations.getString(i)
                                        HtmlCompat.fromHtml(translatedText, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
                                    }
                                } catch (e: Exception) {
                                    Log.e("Translate", "Error in external translation chunk", e)
                                    if (e is retrofit2.HttpException && e.code() == 429) {
                                        delay(5000) // Back off on rate limit
                                    }
                                    chunk
                                }
                            } else {
                                try {
                                    val delimiter = " [|] "
                                    val combinedText = chunk.joinToString(delimiter)
                                    val translatedCombined = translator.translate(combinedText).await()
                                    translatedCombined.split(delimiter).map { it.trim() }
                                } catch (e: Exception) {
                                    Log.e("Translate", "Error in internal translation chunk", e)
                                    chunk
                                }
                            }
                            
                            // Align sizes
                            val adjustedResults = if (chunkResults.size < chunk.size) {
                                chunkResults + chunk.drop(chunkResults.size)
                            } else {
                                chunkResults.take(chunk.size)
                            }
                            results.addAll(adjustedResults)
                            
                            if (texts.size > chunkSize) {
                                delay(1500) // Delay between batches
                            }
                        }
                        return results
                    }

                    // State for Story in DB
                    var isCurrentStorySaved by remember { mutableStateOf(false) }
                    LaunchedEffect(currentStory) {
                        isCurrentStorySaved = currentStory?.let { storyDao.getById(it.id) != null } ?: false
                    }
                    
                    // Preload data from DB when story or language changes
                    LaunchedEffect(currentStory?.id, targetLanguage) {
                        val storyId = currentStory?.id ?: return@LaunchedEffect
                        
                        // Load chapters from DB immediately
                        val savedChapters = chapterDao.getChaptersForStory(storyId)
                        if (savedChapters.isNotEmpty()) {
                            if (currentChapters.isEmpty() || currentChapters.first().storyId != storyId) {
                                currentChapters.clear()
                                currentChapters.addAll(savedChapters)
                            }
                        }

                        // Load existing story locale
                        val existingStoryLocale = storyDao.getLocaleByStoryId(storyId, targetLanguage)
                        if (existingStoryLocale != null) {
                            translatedStory = existingStoryLocale
                        }
                        
                        // Load existing chapter locales
                        val existingLocales = chapterDao.getLocalesForStory(storyId, targetLanguage)
                        if (existingLocales.isNotEmpty()) {
                            lastTranslatedStoryId = storyId
                            translatedChapters.clear()
                            translatedChapters.addAll(existingLocales)
                        } else if (lastTranslatedStoryId != storyId) {
                            translatedChapters.clear()
                        }
                    }

                    // Dialog state
                    var showExitPrompt by remember { mutableStateOf(false) }
                    var nextViewStateAfterPrompt by remember { mutableStateOf<ViewState?>(null) }
                    
                    val onSave: () -> Unit = {
                        scope.launch {
                            currentStory?.let { story ->
                                storyDao.insert(story)
                                isCurrentStorySaved = true
                                translatedStory?.let { locale ->
                                    if (locale.storyId == story.id) {
                                        storyDao.insertLocale(locale)
                                    }
                                }

                                currentChapters.forEach { chapter ->
                                    chapter.storyId = story.id
                                }
                                chapterDao.insertAll(currentChapters)
                                chapterDao.insertAllLocale(translatedChapters.filter { currentChapters.find { c -> c.id == it.chapterId } != null })
                            }
                        }
                    }

                    // Function
                    val onShowScrapped: (Boolean) -> Unit = {
                        if (it && isScrappableUrl) {
                            if (regexBook.find(currentUrl) != null)
                                navigateTo(ViewState.STORY)
                            else if (regexChapters.find(currentUrl) != null)
                                navigateTo(ViewState.CHAPTER_LIST)
                            else if (regexTxt.find(currentUrl) != null) {
                                // Ensure currentChapter is set correctly
                                if (currentChapter?.url != currentUrl) {
                                    currentChapter = currentChapters.find { c -> c.url == currentUrl }
                                }
                                navigateTo(ViewState.CHAPTER)
                            }
                        }else{
                            if (!isCurrentStorySaved && currentStory != null) {
                                nextViewStateAfterPrompt = ViewState.BROWSER
                                showExitPrompt = true
                            } else {
                                navigateTo(ViewState.BROWSER)
                            }
                        }
                    }
                    val onShowChapters: () -> Unit = {
                        if (currentStory != null) {
                            val indexUrl = currentStory!!.url.replace(".html", "/index.html")
                            // Always sync WebView URL when entering chapter list to check for updates
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
                                                val storyId = currentStory?.id ?: return@launch
                                                if (isListTranslating) return@launch

                                                listTranslationJob?.cancel()
                                                listTranslationJob = scope.launch {
                                                    if (lastTranslatedStoryId != storyId) {
                                                        translatedChapters.clear()
                                                        translatingIds.clear()

                                                        val existingLocales = chapterDao.getLocalesForStory(storyId, targetLanguage)
                                                        val seenIds = mutableSetOf<Int>()
                                                        val dedupedExisting = existingLocales.filter { seenIds.add(it.chapterId) }
                                                        translatedChapters.addAll(dedupedExisting)
                                                        lastTranslatedStoryId = storyId
                                                    }

                                                    isListTranslating = true
                                                    try {
                                                        val translatedIds = translatedChapters.map { it.chapterId }.toSet()
                                                        val untranslated = currentChapters.filter { chapter ->
                                                            chapter.id !in translatedIds && !translatingIds.contains(chapter.id)
                                                        }

                                                        if (untranslated.isEmpty()) return@launch

                                                        val (visibleFirst, remainder) = untranslated.partition {
                                                            it.id in visibleChapterIds
                                                        }
                                                        val prioritized = visibleFirst + remainder

                                                        val chunkSize = if (useExternalTranslator) 50 else 100
                                                        for (chunk in prioritized.chunked(chunkSize)) {
                                                            if (!isActive) break

                                                            val titles = chunk.map { it.title }
                                                            val translatedTitles = performTranslation(titles)

                                                            val newLocales = chunk.mapIndexed { index, chapter ->
                                                                ChapterLocale(
                                                                    chapterId = chapter.id,
                                                                    language = targetLanguage,
                                                                    title = translatedTitles.getOrElse(index) { chapter.title },
                                                                    content = emptyList()
                                                                )
                                                            }

                                                            translatedChapters.addAll(newLocales)

                                                            // Only persist if job is still active
                                                            if (coroutineContext.isActive) {
                                                                scope.launch {
                                                                    newLocales.forEach { locale ->
                                                                        try {
                                                                            if (chapterDao.getById(locale.chapterId) != null) {
                                                                                chapterDao.insertLocale(locale)
                                                                            }
                                                                        } catch (e: android.database.sqlite.SQLiteConstraintException) {
                                                                            Log.w("Translate", "Skipping locale insert, parent chapter gone: ${locale.chapterId}", e)
                                                                        }
                                                                    }
                                                                }
                                                            }

                                                            if (prioritized.size > chunkSize) {
                                                                delay(300)
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e("Translate", "Error translating chapter titles", e)
                                                    } finally {
                                                        isListTranslating = false
                                                    }
                                                }
                                            }
                                            ViewState.CHAPTER -> {
                                                val chapter = currentChapter ?: return@launch
                                                if (chapter.content.isEmpty()) return@launch

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
                                                        val existingLocale = chapterDao.getLocaleByChapterId(chapter.id, targetLanguage)
                                                        if (existingLocale != null && existingLocale.content.isNotEmpty() && existingLocale.content.size == chapter.content.size) {
                                                            val idx = translatedChapters.indexOfFirst { it.chapterId == chapter.id }
                                                            if (idx == -1) translatedChapters.add(existingLocale)
                                                            else translatedChapters[idx] = existingLocale
                                                            return@launch
                                                        }

                                                        val originalContent = chapter.content
                                                        val idx = translatedChapters.indexOfFirst { it.chapterId == chapter.id }
                                                        if (idx == -1) {
                                                            translatedChapters.add(ChapterLocale(chapterId = chapter.id, language = targetLanguage, title = chapter.title, content = originalContent))
                                                        }

                                                        val contentToTranslate = listOf(chapter.title) + originalContent
                                                        val translatedResults = performTranslation(contentToTranslate)
                                                        
                                                        val tTitle = translatedResults.getOrElse(0) { chapter.title }
                                                        val tContent = if (translatedResults.size > 1) translatedResults.drop(1) else originalContent

                                                        val updateIdx = translatedChapters.indexOfFirst { it.chapterId == chapter.id }
                                                        if (updateIdx != -1) {
                                                            val updatedLocale = translatedChapters[updateIdx].copy(title = tTitle, content = tContent)
                                                            translatedChapters[updateIdx] = updatedLocale
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
                                                libraryTranslationJob?.cancel()
                                                libraryTranslationJob = scope.launch {
                                                    // Quick pass for database items
                                                    for (story in allStories) {
                                                        if (allTranslatedStories.none { it.storyId == story.id }) {
                                                            val existing = storyDao.getLocaleByStoryId(story.id, targetLanguage)
                                                            if (existing != null) {
                                                                allTranslatedStories.add(existing)
                                                            }
                                                        }
                                                    }
                                                    
                                                    try {
                                                        for (story in allStories) {
                                                            if (allTranslatedStories.none { it.storyId == story.id }) {
                                                                try {
                                                                    // Translate story details (network)
                                                                    val storyDetails = listOf(story.title, story.genre, story.description, if (story.tags.isEmpty()) "" else story.tags.joinToString(", "))
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
                                                                    delay(2000)
                                                                } catch (e: Exception) {
                                                                    Log.e("Translate", "Error translating story in library", e)
                                                                }
                                                            }
                                                        }
                                                    } finally {
                                                        isLibraryTranslating = false
                                                    }
                                                }
                                            }
                                            ViewState.BROWSER -> {}
                                            ViewState.SETTINGS -> {}
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

                    LaunchedEffect(currentViewState) {
                        if (currentViewState != ViewState.CHAPTER_LIST) {
                            listTranslationJob?.cancel()
                            isListTranslating = false
                        }
                        if (currentViewState != ViewState.STORY_LIST) {
                            libraryTranslationJob?.cancel()
                            isLibraryTranslating = false
                        }
                    }

                    LaunchedEffect(currentViewState, showTranslate, currentStory, currentChapters.size,
                        currentChapter, useExternalTranslator, translatedChapters.size,
                        visibleChapterIds) {
                        if (showTranslate) {
                            onTranslate(true)
                        }
                    }

                    LaunchedEffect(currentViewState, currentStory, currentChapters.size) {
                        if (currentViewState == ViewState.CHAPTER_LIST && currentStory != null && currentChapters.isNotEmpty()) {
                            if (currentStory!!.id != lastScrolledStoryId) {
                                val bookmarkId = currentStory!!.bookmarkedChapterId
                                if (bookmarkId != null) {
                                    val index = currentChapters.indexOfFirst { it.id == bookmarkId }
                                    if (index != -1) {
                                        chapterListState.scrollToItem(index)
                                    }
                                } else {
                                    chapterListState.scrollToItem(0)
                                }
                                lastScrolledStoryId = currentStory!!.id
                            }
                        }
                    }

                    BackHandler(enabled = true) {
                        showTopBar = true
                        val targetState = when(currentViewState) {
                            ViewState.BROWSER -> null
                            ViewState.STORY -> ViewState.BROWSER
                            ViewState.CHAPTER_LIST -> ViewState.STORY
                            ViewState.CHAPTER -> ViewState.CHAPTER_LIST
                            ViewState.STORY_LIST -> ViewState.BROWSER
                            ViewState.SETTINGS -> ViewState.BROWSER
                        }

                        if (targetState == ViewState.BROWSER && !isCurrentStorySaved && currentStory != null) {
                            nextViewStateAfterPrompt = ViewState.BROWSER
                            showExitPrompt = true
                        } else if (targetState != null) {
                            if (currentViewState == ViewState.BROWSER && webView.canGoBack()) {
                                webView.goBack()
                            } else if (currentViewState != ViewState.BROWSER) {
                                if (targetState == ViewState.STORY || targetState == ViewState.CHAPTER_LIST) {
                                    webView.goBack()
                                }
                                navigateTo(targetState)
                            }
                        } else {
                            if (webView.canGoBack()) webView.goBack()
                            else finish()
                        }
                    }

                    if (showExitPrompt) {
                        AlertDialog(
                            onDismissRequest = { showExitPrompt = false },
                            title = { Text(stringResource(R.string.unsaved_changes_title)) },
                            text = { Text(stringResource(R.string.unsaved_changes_message)) },
                            confirmButton = {
                                Button(onClick = {
                                    onSave()
                                    showExitPrompt = false
                                    nextViewStateAfterPrompt?.let { navigateTo(it) }
                                }) {
                                    Text(stringResource(R.string.save_and_exit_button))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    showExitPrompt = false
                                    nextViewStateAfterPrompt?.let { navigateTo(it) }
                                }) {
                                    Text(stringResource(R.string.discard_and_exit_button))
                                }
                            }
                        )
                    }

                    Column {
                        if (currentViewState != ViewState.CHAPTER || showTopBar) {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.app_name)
                                    )
                                },
                                actions = {
                                    val isScrappedView = currentViewState != ViewState.BROWSER && currentViewState != ViewState.STORY_LIST && currentViewState != ViewState.SETTINGS
                                    IconToggleButton(
                                        checked = isScrappedView,
                                        onCheckedChange = onShowScrapped,
                                        enabled = isScrappableUrl && currentStory != null
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.scan_24px),
                                            contentDescription = stringResource(id = R.string.scrapper_content_description)
                                        )
                                    }
                                    IconToggleButton(
                                        checked = showTranslate,
                                        onCheckedChange = onTranslate,
                                        enabled = currentViewState != ViewState.BROWSER && (currentViewState == ViewState.STORY_LIST || (isScrappableUrl && currentStory != null))
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.translate_24px),
                                            contentDescription = stringResource(id = R.string.translator_content_description)
                                        )
                                    }
                                    IconButton(onClick = onShowLibrary) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.library_books_24px),
                                            contentDescription = stringResource(id = R.string.library_content_description),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    IconButton(onClick = { navigateTo(ViewState.SETTINGS) }) {
                                        Icon(Icons.Default.Settings, contentDescription = stringResource(id = R.string.settings_content_description))
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

                        Box(modifier = Modifier.weight(1f)) {
                            BrowserView(
                                webView = webView,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (currentViewState != ViewState.BROWSER) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.background
                                ) {
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
                                                onBackToStoryClick = onBackToStory,
                                                onVisibleIdsChange = { visibleChapterIds = it },
                                                listState = chapterListState
                                            )
                                        ViewState.CHAPTER -> 
                                            currentChapter?.let { chapter ->
                                                val chapterIndex = currentChapters.indexOfFirst { it.url == chapter.url }
                                                if (chapterIndex != -1) {
                                                    ChapterView(
                                                        chapters = currentChapters,
                                                        initialIndex = chapterIndex,
                                                        bookmarkedChapterId = currentStory?.bookmarkedChapterId,
                                                        showTranslate = showTranslate,
                                                        translatedChapters = translatedChapters,
                                                        fontSize = chapterFontSize,
                                                        onFontSizeChange = { newSize -> 
                                                            chapterFontSize = newSize
                                                            scope.launch { settingsManager.setChapterFontSize(newSize) }
                                                        },
                                                        onBookmarkClick = onToggleBookmark,
                                                        onNavigateToChapter = { newChapter ->
                                                            if (currentChapter?.url != newChapter.url) {
                                                                currentChapter = newChapter
                                                                webView.loadUrl(newChapter.url)
                                                                
                                                                // Auto-bookmark
                                                                scope.launch {
                                                                    val story = currentStory ?: return@launch
                                                                    val updatedStory = story.copy(bookmarkedChapterId = newChapter.id)
                                                                    currentStory = updatedStory
                                                                    if (storyDao.getById(story.id) != null) {
                                                                        storyDao.insert(updatedStory)
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        onBackClick = onShowChapters,
                                                        onToggleBars = { visible -> showTopBar = visible }
                                                    )
                                                }
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
                                        ViewState.SETTINGS ->
                                            SettingsView(
                                                useExternalTranslator = useExternalTranslator,
                                                onUseExternalTranslatorChange = { enabled ->
                                                    useExternalTranslator = enabled
                                                    scope.launch { settingsManager.setUseExternalTranslator(enabled) }
                                                },
                                                translatorApiKey = translatorApiKey,
                                                onTranslatorApiKeyChange = { key ->
                                                    translatorApiKey = key
                                                    scope.launch { settingsManager.setTranslatorApiKey(key) }
                                                },
                                                userAgent = userAgent,
                                                onUserAgentChange = { ua ->
                                                    userAgent = ua
                                                    scope.launch { settingsManager.setUserAgent(ua) }
                                                },
                                                chapterFontSize = chapterFontSize,
                                                onChapterFontSizeChange = { size ->
                                                    chapterFontSize = size
                                                    scope.launch { settingsManager.setChapterFontSize(size) }
                                                },
                                                onBackClick = { currentViewState = ViewState.BROWSER }
                                            )
                                        ViewState.BROWSER -> {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
