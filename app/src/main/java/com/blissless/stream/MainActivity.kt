package com.blissless.stream

import android.content.Context
import android.content.SharedPreferences
import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blissless.stream.MainViewModel
import com.blissless.stream.ContentItem
import com.blissless.stream.ContentDetails
import com.blissless.stream.ExploreScreen
import com.blissless.stream.HomeScreen
import com.blissless.stream.ScheduleScreen
import com.blissless.stream.SearchScreen
import com.blissless.stream.DetailScreen
import com.blissless.stream.StreamBottomNav
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers

object StreamTheme {
    @Composable
    operator fun invoke(content: @Composable () -> Unit) {
        content()
    }
}

class MainActivity : ComponentActivity() {

    private var userDataPrefs: SharedPreferences? = null

    private fun getUserDataStorage(): SharedPreferences {
        if (userDataPrefs == null) {
            userDataPrefs = getSharedPreferences("user_data", Context.MODE_PRIVATE)
        }
        return userDataPrefs!!
    }

    internal fun getContinueWatchingList(): List<ContentItem> {
        val json = getUserDataStorage().getString("continue_watching", null) ?: return emptyList()
        return try {
            parseStoredContentList(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    internal fun saveContinueWatchingList(items: List<ContentItem>) {
        val json = serializeContentList(items)
        getUserDataStorage().edit().putString("continue_watching", json).apply()
    }

    internal fun getPlanningToWatchList(): List<ContentItem> {
        val json = getUserDataStorage().getString("planning_to_watch", null) ?: return emptyList()
        return try {
            parseStoredContentList(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    internal fun savePlanningToWatchList(items: List<ContentItem>) {
        val json = serializeContentList(items)
        getUserDataStorage().edit().putString("planning_to_watch", json).apply()
    }

    internal fun getCompletedList(): List<ContentItem> {
        val json = getUserDataStorage().getString("completed", null) ?: return emptyList()
        return try {
            parseStoredContentList(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    internal fun saveCompletedList(items: List<ContentItem>) {
        val json = serializeContentList(items)
        getUserDataStorage().edit().putString("completed", json).apply()
    }

    fun getOnHoldList(): List<ContentItem> {
        val json = getUserDataStorage().getString("onhold", null) ?: return emptyList()
        return try {
            parseStoredContentList(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveOnHoldList(items: List<ContentItem>) {
        val json = serializeContentList(items)
        getUserDataStorage().edit().putString("onhold", json).apply()
    }

    fun getDroppedList(): List<ContentItem> {
        val json = getUserDataStorage().getString("dropped", null) ?: return emptyList()
        return try {
            parseStoredContentList(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveDroppedList(items: List<ContentItem>) {
        val json = serializeContentList(items)
        getUserDataStorage().edit().putString("dropped", json).apply()
    }

    private fun serializeContentList(items: List<ContentItem>): String {
        return items.joinToString("|||") {
            "${it.id}|${it.name}|${it.type}|${it.posterUrl ?: ""}|${it.backdropUrl ?: ""}|${it.voteAverage}|${it.genreIds.joinToString(",")}|${it.progressPosition}|${it.progressDuration}|${it.progressSeason}|${it.progressEpisode}"
        }
    }

    private fun parseStoredContentList(json: String): List<ContentItem> {
        if (json.isEmpty()) return emptyList()
        return json.split("|||").mapNotNull { itemStr ->
            val parts = itemStr.split("|")
            if (parts.size >= 7) {
                ContentItem(
                    id = parts[0].toIntOrNull() ?: return@mapNotNull null,
                    name = parts[1],
                    type = parts[2],
                    posterUrl = parts[3].ifEmpty { null },
                    backdropUrl = parts[4].ifEmpty { null },
                    voteAverage = parts[5].toDoubleOrNull() ?: 0.0,
                    genreIds = parts.getOrNull(6)?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList(),
                    progressPosition = parts.getOrNull(7)?.toLongOrNull() ?: 0L,
                    progressDuration = parts.getOrNull(8)?.toLongOrNull() ?: 0L,
                    progressSeason = parts.getOrNull(9)?.toIntOrNull() ?: 1,
                    progressEpisode = parts.getOrNull(10)?.toIntOrNull() ?: 1
                )
            } else null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()
            StreamApp(viewModel, onScrapeUrl = { url, callback ->
                scrapeM3u8Url(url, callback)
            })
        }
    }

    private val adDomains = listOf(
        "googlesyndication.com", "doubleclick.net", "googleadservices.com",
        "googleads.g.doubleclick.net", "ads.google.com", "pagead2.googlesyndication.com",
        "adservice.google.com", "taboola.com", "outbrain.com", "popads.net",
        "popmyads.com", "propellerads.com", "adcash.com", "realsrv.com",
        "exosrv.com", "JuicyAds.com", "adtng.com", "dtghtv.com",
        "btrll.com", "criteo.com", "advertising.com", "adnxs.com",
        "rubiconproject.com", "pubmatic.com", "openx.net",
        "casalemedia.com", "moatads.com"
    )

    @SuppressLint("SetJavaScriptEnabled")
    fun scrapeM3u8Url(url: String, onResult: (String?) -> Unit) {
        android.util.Log.d("StreamApp", "scrapeM3u8Url: Loading URL in WebView: $url")

        val foundM3u8Url = java.util.concurrent.atomic.AtomicReference<String?>(null)
        val scraperLock = java.util.concurrent.atomic.AtomicBoolean(false)

        val webView = WebView(this)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadsImagesAutomatically = false
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: android.webkit.WebResourceRequest?
            ): android.webkit.WebResourceResponse? {
                val requestUrl = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)

                for (adDomain in adDomains) {
                    if (requestUrl.contains(adDomain, ignoreCase = true)) {
                        return android.webkit.WebResourceResponse(
                            "text/plain",
                            "utf-8",
                            java.io.ByteArrayInputStream("".toByteArray())
                        )
                    }
                }

                if (requestUrl.contains(".m3u8", ignoreCase = true)) {
                    android.util.Log.d("StreamApp", "scrapeM3u8Url: Intercepted m3u8 request: $requestUrl")

                    val isHighQuality = requestUrl.contains("MTA4MA", ignoreCase = true) ||
                            requestUrl.contains("1080", ignoreCase = true) ||
                            requestUrl.contains("master", ignoreCase = true) ||
                            requestUrl.contains("720", ignoreCase = true)

                    if (isHighQuality && foundM3u8Url.get() == null && scraperLock.compareAndSet(false, true)) {
                        foundM3u8Url.set(requestUrl)
                        android.util.Log.d("StreamApp", "scrapeM3u8Url: Found valid m3u8 URL: $requestUrl")
                        onResult(requestUrl)
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            try { webView.destroy() } catch (e: Exception) { }
                        }
                    }
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                val requestUrl = request?.url?.toString() ?: return false
                for (adDomain in adDomains) {
                    if (requestUrl.contains(adDomain, ignoreCase = true)) {
                        return true
                    }
                }
                return false
            }
        }

        val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            android.util.Log.w("StreamApp", "scrapeM3u8Url: Timeout, fallback URL: ${foundM3u8Url.get() ?: url}")
            if (foundM3u8Url.get() == null) {
                onResult(url)
            }
            try {
                webView.destroy()
            } catch (e: Exception) { }
        }
        timeoutHandler.postDelayed(timeoutRunnable, 30000)

        webView.loadUrl(url)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StreamApp(viewModel: MainViewModel, onScrapeUrl: (String, (String?) -> Unit) -> Unit) {
    var selectedTab by remember { mutableIntStateOf(1) }
    var listRefreshKey by remember { mutableIntStateOf(0) }
    var detailRefreshKey by remember { mutableIntStateOf(0) }
    var isPlayerActive by remember { mutableStateOf(false) }
    var showDetailScreen by remember { mutableStateOf(false) }
    var showSearchScreen by remember { mutableStateOf(false) }
    var searchFocus by remember { mutableStateOf(false) }
    var currentVideoUrl by remember { mutableStateOf<String?>(null) }
    var currentContent by remember { mutableStateOf<ContentItem?>(null) }
    var currentSeason by remember { mutableIntStateOf(1) }
    var currentEpisode by remember { mutableIntStateOf(1) }
    var totalEpisodes by remember { mutableIntStateOf(0) }
    var savedPosition by remember { mutableLongStateOf(0L) }
    var contentDetails by remember { mutableStateOf<ContentDetails?>(null) }

    val context = LocalContext.current
    val activity = context as? MainActivity
    val focusManager = LocalFocusManager.current

    fun getContinueWatchingList(): List<ContentItem> {
        return activity?.getContinueWatchingList() ?: emptyList()
    }

    fun saveContinueWatchingList(items: List<ContentItem>) {
        activity?.saveContinueWatchingList(items)
    }

    fun getPlanningToWatchList(): List<ContentItem> {
        return activity?.getPlanningToWatchList() ?: emptyList()
    }

    fun savePlanningToWatchList(items: List<ContentItem>) {
        activity?.savePlanningToWatchList(items)
    }

    fun getCompletedList(): List<ContentItem> {
        return activity?.getCompletedList() ?: emptyList()
    }

    fun saveCompletedList(items: List<ContentItem>) {
        activity?.saveCompletedList(items)
    }

    fun getOnHoldList(): List<ContentItem> {
        return activity?.getOnHoldList() ?: emptyList()
    }

    fun saveOnHoldList(items: List<ContentItem>) {
        activity?.saveOnHoldList(items)
    }

    fun getDroppedList(): List<ContentItem> {
        return activity?.getDroppedList() ?: emptyList()
    }

    fun saveDroppedList(items: List<ContentItem>) {
        activity?.saveDroppedList(items)
    }

    fun onContentClick(item: ContentItem) {
        currentContent = item
        currentSeason = item.progressSeason
        currentEpisode = item.progressEpisode
        savedPosition = item.progressPosition
        totalEpisodes = 0
        showDetailScreen = true
        contentDetails = null
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            contentDetails = viewModel.getContentDetails(item)
        }
    }

    fun onPlayContent() {
        if (currentContent == null) return
        showDetailScreen = false
        currentVideoUrl = null
        isPlayerActive = true
        val item = currentContent!!
        val vidlinkUrl = if (item.type == "tv") {
            "https://vidlink.pro/tv/${item.id}/$currentSeason/$currentEpisode"
        } else {
            "https://vidlink.pro/movie/${item.id}"
        }
        android.util.Log.d("StreamApp", "onPlayContent: Playing ${item.name} (${item.type}), URL: $vidlinkUrl")
        onScrapeUrl(vidlinkUrl) { url ->
            android.util.Log.d("StreamApp", "onPlayContent: Received URL: $url")
            currentVideoUrl = url
        }
    }

    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        selectedTab = pagerState.currentPage
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        pagerState.animateScrollToPage(1)
    }

    BackHandler(enabled = true) {
        when {
            isPlayerActive -> {
                isPlayerActive = false
                currentVideoUrl = null
            }
            showSearchScreen -> {
                showSearchScreen = false
                searchFocus = false
                viewModel.clearSearchResults()
            }
            showDetailScreen -> {
                showDetailScreen = false
                contentDetails = null
            }
            else -> {
                // On any main tab (Schedule, Explore, Home), close the app
                activity?.finish()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> ScheduleScreen(onContentClick = { item: ContentItem ->
                        onContentClick(item)
                    })
                    1 -> ExploreScreen(
                        viewModel = viewModel, 
                        onContentClick = { item: ContentItem ->
                            onContentClick(item)
                        },
                        onSearchClick = {
                            showSearchScreen = true
                            searchFocus = true
                        }
                    )
                    2 -> HomeScreen(
                        getContinueWatchingList = { getContinueWatchingList() },
                        getPlanningToWatchList = { getPlanningToWatchList() },
                        getCompletedList = { getCompletedList() },
                        getOnHoldList = { getOnHoldList() },
                        getDroppedList = { getDroppedList() },
                        refreshKey = listRefreshKey,
                        onContentClick = { item: ContentItem ->
                            onContentClick(item)
                        },
                        onSearchClick = {
                            showSearchScreen = true
                            searchFocus = true
                        }
                    )
                }
            }
        }

        StreamBottomNav(
            selectedTab = selectedTab,
            onTabSelected = { tab: Int ->
                selectedTab = tab
                coroutineScope.launch {
                    pagerState.animateScrollToPage(tab)
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (isPlayerActive && currentContent != null) {
            val isSeries = currentContent?.type == "tv"
            val seasons = contentDetails?.seasons ?: emptyList()
            val maxSeason = seasons.maxOfOrNull { it.seasonNumber } ?: 1
            val currentSeasonInfo = seasons.find { it.seasonNumber == currentSeason }
            val episodesInCurrentSeason = currentSeasonInfo?.episodeCount ?: totalEpisodes.coerceAtLeast(1)

            PlayerScreen(
                videoUrl = currentVideoUrl ?: "",
                currentSeason = currentSeason,
                currentEpisode = currentEpisode,
                totalEpisodes = episodesInCurrentSeason,
                maxSeason = maxSeason,
                animeName = currentContent?.name ?: "",
                episodeLength = 1440,
                isLoadingStream = currentVideoUrl == null,
                currentCategory = "sub",
                forwardSkipSeconds = 15,
                backwardSkipSeconds = 5,
                autoPlayNextEpisode = true,
                savedPosition = savedPosition,
                isSeries = isSeries,
                onSavePosition = { position, dur ->
                    currentContent?.let { item ->
                        val continueList = getContinueWatchingList().toMutableList()
                        val index = continueList.indexOfFirst { it.id == item.id && it.type == item.type }
                        val existingItem = continueList.getOrNull(index)
                        val existingDuration = existingItem?.progressDuration ?: 0L
                        val actualDuration = if (dur > 0) dur else existingDuration
                        
                        if (index != -1) {
                            // Update existing item
                            if (position > 0) {
                                continueList[index] = continueList[index].copy(
                                    progressPosition = position,
                                    progressDuration = actualDuration,
                                    progressSeason = currentSeason,
                                    progressEpisode = currentEpisode
                                )
                            }
                        } else if (position > 0) {
                            // Add to continue watching list if not already there
                            continueList.add(0, item.copy(
                                progressPosition = position,
                                progressDuration = actualDuration,
                                progressSeason = currentSeason,
                                progressEpisode = currentEpisode
                            ))
                        }
                        saveContinueWatchingList(continueList)
                        listRefreshKey++
                    }
                },
                onProgressUpdate = { },
                onPreviousEpisode = {
                    val seasons = contentDetails?.seasons ?: emptyList()
                    val currentSeasonInfo = seasons.find { it.seasonNumber == currentSeason }
                    val episodesInCurrentSeason = currentSeasonInfo?.episodeCount ?: 24
                    val maxSeason = seasons.maxOfOrNull { it.seasonNumber } ?: 1

                    if (currentEpisode > 1) {
                        currentEpisode--
                    } else if (currentSeason > 1) {
                        currentSeason--
                        val prevSeasonInfo = seasons.find { it.seasonNumber == currentSeason }
                        currentEpisode = prevSeasonInfo?.episodeCount ?: 24
                    } else if (currentSeason == 1 && currentEpisode == 1) {
                        currentEpisode = 1
                    }
                    currentVideoUrl = null
                    val item = currentContent ?: return@PlayerScreen
                    val vidlinkUrl = if (item.type == "tv") {
                        "https://vidlink.pro/tv/${item.id}/$currentSeason/$currentEpisode"
                    } else {
                        "https://vidlink.pro/movie/${item.id}"
                    }
                    onScrapeUrl(vidlinkUrl) { url ->
                        currentVideoUrl = url
                    }
                },
                onNextEpisode = {
                    val seasons = contentDetails?.seasons ?: emptyList()
                    val currentSeasonInfo = seasons.find { it.seasonNumber == currentSeason }
                    val episodesInCurrentSeason = currentSeasonInfo?.episodeCount ?: 24
                    val maxSeason = seasons.maxOfOrNull { it.seasonNumber } ?: 1

                    if (currentEpisode < episodesInCurrentSeason) {
                        currentEpisode++
                    } else if (currentSeason < maxSeason) {
                        currentSeason++
                        currentEpisode = 1
                    }
                    currentVideoUrl = null
                    val item = currentContent ?: return@PlayerScreen
                    val vidlinkUrl = if (item.type == "tv") {
                        "https://vidlink.pro/tv/${item.id}/$currentSeason/$currentEpisode"
                    } else {
                        "https://vidlink.pro/movie/${item.id}"
                    }
                    onScrapeUrl(vidlinkUrl) { url ->
                        currentVideoUrl = url
                    }
                },
                onPlaybackError = {
                    currentVideoUrl = null
                    val item = currentContent ?: return@PlayerScreen
                    val vidlinkUrl = if (item.type == "tv") {
                        "https://vidlink.pro/tv/${item.id}/$currentSeason/$currentEpisode"
                    } else {
                        "https://vidlink.pro/movie/${item.id}"
                    }
                    onScrapeUrl(vidlinkUrl) { url ->
                        currentVideoUrl = url
                    }
                },
                onClose = {
                    isPlayerActive = false
                    currentVideoUrl = null
                }
            )
        }

        if (showSearchScreen) {
            SearchScreen(
                viewModel = viewModel,
                onContentClick = { item ->
                    showSearchScreen = false
                    searchFocus = false
                    onContentClick(item)
                },
                shouldFocus = searchFocus
            )
        }

        if (showDetailScreen && currentContent != null) {
            DetailScreen(
                content = currentContent!!,
                details = contentDetails,
                onPlayClick = { season: Int, episode: Int ->
                    currentSeason = season
                    currentEpisode = episode
                    onPlayContent()
                },
                onBackClick = {
                    showDetailScreen = false
                    contentDetails = null
                },
                onAddToList = { listType ->
                    val item = currentContent!!
                    // Remove from all lists first (only one list entry at a time)
                    var continueList = getContinueWatchingList().toMutableList()
                    var planningList = getPlanningToWatchList().toMutableList()
                    var completedList = getCompletedList().toMutableList()
                    var onHoldList = getOnHoldList().toMutableList()
                    var droppedList = getDroppedList().toMutableList()
                    
                    continueList.removeAll { it.id == item.id && it.type == item.type }
                    planningList.removeAll { it.id == item.id && it.type == item.type }
                    completedList.removeAll { it.id == item.id && it.type == item.type }
                    onHoldList.removeAll { it.id == item.id && it.type == item.type }
                    droppedList.removeAll { it.id == item.id && it.type == item.type }
                    
                    when (listType) {
                        "continue" -> {
                            continueList.add(0, item)
                            saveContinueWatchingList(continueList)
                            savePlanningToWatchList(planningList)
                            saveCompletedList(completedList)
                            saveOnHoldList(onHoldList)
                            saveDroppedList(droppedList)
                        }
                        "planning" -> {
                            planningList.add(item)
                            saveContinueWatchingList(continueList)
                            savePlanningToWatchList(planningList)
                            saveCompletedList(completedList)
                            saveOnHoldList(onHoldList)
                            saveDroppedList(droppedList)
                        }
                        "completed" -> {
                            completedList.add(item)
                            saveContinueWatchingList(continueList)
                            savePlanningToWatchList(planningList)
                            saveCompletedList(completedList)
                            saveOnHoldList(onHoldList)
                            saveDroppedList(droppedList)
                        }
                        "onhold" -> {
                            onHoldList.add(item)
                            saveContinueWatchingList(continueList)
                            savePlanningToWatchList(planningList)
                            saveCompletedList(completedList)
                            saveOnHoldList(onHoldList)
                            saveDroppedList(droppedList)
                        }
                        "dropped" -> {
                            droppedList.add(item)
                            saveContinueWatchingList(continueList)
                            savePlanningToWatchList(planningList)
                            saveCompletedList(completedList)
                            saveOnHoldList(onHoldList)
                            saveDroppedList(droppedList)
                        }
                    }
                    listRefreshKey++
                    detailRefreshKey++
                },
                onRemoveFromList = { listType ->
                    val item = currentContent!!
                    when (listType) {
                        "continue" -> {
                            val list = getContinueWatchingList().toMutableList()
                            list.removeAll { it.id == item.id && it.type == item.type }
                            saveContinueWatchingList(list)
                        }
                        "planning" -> {
                            val list = getPlanningToWatchList().toMutableList()
                            list.removeAll { it.id == item.id && it.type == item.type }
                            savePlanningToWatchList(list)
                        }
                        "completed" -> {
                            val list = getCompletedList().toMutableList()
                            list.removeAll { it.id == item.id && it.type == item.type }
                            saveCompletedList(list)
                        }
                        "onhold" -> {
                            val list = getOnHoldList().toMutableList()
                            list.removeAll { it.id == item.id && it.type == item.type }
                            saveOnHoldList(list)
                        }
                        "dropped" -> {
                            val list = getDroppedList().toMutableList()
                            list.removeAll { it.id == item.id && it.type == item.type }
                            saveDroppedList(list)
                        }
                    }
                    listRefreshKey++
                    detailRefreshKey++
                },
                getContinueWatchingList = { getContinueWatchingList() },
                getPlanningToWatchList = { getPlanningToWatchList() },
                getCompletedList = { getCompletedList() },
                getOnHoldList = { getOnHoldList() },
                getDroppedList = { getDroppedList() },
                refreshKey = detailRefreshKey
            )
        }
    }
}
