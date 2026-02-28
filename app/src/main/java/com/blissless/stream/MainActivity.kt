package com.blissless.stream

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    // UI Components
    private lateinit var rootContainer: ConstraintLayout
    private lateinit var searchContainer: LinearLayout
    private lateinit var searchEditText: AppCompatEditText
    private lateinit var searchWrapper: FrameLayout
    private lateinit var clearButton: ImageButton
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var contentAdapter: ContentAdapter

    // Player components
    private lateinit var playerContainer: FrameLayout
    private lateinit var webView: WebView
    private lateinit var playerControls: LinearLayout
    private lateinit var btnBack: ImageButton
    private lateinit var btnPrevious: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnFullscreen: ImageButton
    private lateinit var titleTextView: TextView

    // State
    private var isFullscreen = false
    private var currentContent: ContentItem? = null
    private var currentSeason = 1
    private var currentEpisode = 1
    private var totalSeasons = 1
    private var totalEpisodes = 1
    private var isPlayerVisible = false
    private var seasonEpisodeCounts: Map<Int, Int> = emptyMap()

    // Rotation animation for loading indicator
    private var rotationAnimator: ValueAnimator? = null

    // Ad blocking
    private val adDomains = listOf(
        "googlesyndication.com",
        "doubleclick.net",
        "googleadservices.com",
        "googleads.g.doubleclick.net",
        "ads.google.com",
        "pagead2.googlesyndication.com",
        "adservice.google.com",
        "taboola.com",
        "outbrain.com",
        "popads.net",
        "popmyads.com",
        "propellerads.com",
        "adcash.com",
        "realsrv.com",
        "exosrv.com",
        "JuicyAds.com",
        "adtng.com",
        "dtghtv.com",
        "btrll.com",
        "criteo.com",
        "advertising.com",
        "adnxs.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "casalemedia.com",
        "moatads.com"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createUI()
        setupFullscreenMode()
        setupObservers()
        setupBackHandler()
    }

    private fun setupFullscreenMode() {
        window.apply {
            statusBarColor = Color.BLACK
            navigationBarColor = Color.BLACK
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insetsController?.apply {
                    systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                )
            }
        }
    }

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        // Fallback if we can't get the status bar height
        if (result == 0) {
            result = (24 * resources.displayMetrics.density).toInt()
        }
        return result
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun createUI() {
        val statusBarHeight = getStatusBarHeight()

        // Root container
        rootContainer = ConstraintLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Search container with status bar padding
        searchContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(), 16.dp() + statusBarHeight, 16.dp(), 16.dp())
            setBackgroundColor(Color.parseColor("#000000"))
        }

        // Search wrapper (FrameLayout to overlay clear button)
        searchWrapper = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Search EditText
        searchEditText = AppCompatEditText(this).apply {
            hint = "Search movies & TV shows..."
            setHintTextColor(Color.parseColor("#666666"))
            setTextColor(Color.WHITE)
            setBackgroundResource(R.drawable.search_background)
            setPadding(16.dp(), 16.dp(), 48.dp(), 16.dp()) // Extra padding on right for clear button
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        searchWrapper.addView(searchEditText)

        // Clear button / Loading indicator container
        val clearButtonContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                40.dp(),
                40.dp()
            ).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.END
                marginEnd = 8.dp()
            }
        }

        // Clear button (X)
        clearButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.parseColor("#888888"))
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(8.dp(), 8.dp(), 8.dp(), 8.dp())
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
            setOnClickListener {
                searchEditText.setText("")
                searchEditText.requestFocus()
            }
        }
        clearButtonContainer.addView(clearButton)

        // Loading indicator (ProgressBar)
        loadingIndicator = ProgressBar(this).apply {
            indeterminateDrawable.setTint(Color.parseColor("#BB86FC"))
            layoutParams = FrameLayout.LayoutParams(
                28.dp(),
                28.dp()
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
            visibility = View.GONE
        }
        clearButtonContainer.addView(loadingIndicator)

        searchWrapper.addView(clearButtonContainer)
        searchContainer.addView(searchWrapper)

        // Results RecyclerView
        resultsRecyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            contentAdapter = ContentAdapter { item -> onContentClicked(item) }
            adapter = contentAdapter
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        searchContainer.addView(resultsRecyclerView)

        // Player container (initially hidden)
        playerContainer = FrameLayout(this).apply {
            visibility = View.GONE
            setBackgroundColor(Color.BLACK)
        }

        // WebView for video playback
        webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadsImagesAutomatically = true
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess = true
                allowContentAccess = true
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            }
            webViewClient = AdBlockWebViewClient()
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    if (newProgress == 100) {
                        injectPlayerEnhancements()
                    }
                }
            }
            setBackgroundColor(Color.BLACK)
        }

        // Enable cookies
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        playerContainer.addView(webView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // Player controls overlay
        playerControls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#CC000000"))
            setPadding(8.dp(), 8.dp(), 8.dp(), 8.dp())
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.BOTTOM
            }
        }

        // Back button
        btnBack = createControlButton(android.R.drawable.ic_menu_revert) { closePlayer() }
        playerControls.addView(btnBack)

        // Previous episode button
        btnPrevious = createControlButton(R.drawable.ic_skip_previous) { playPreviousEpisode() }
        playerControls.addView(btnPrevious)

        // Next episode button
        btnNext = createControlButton(R.drawable.ic_skip_next) { playNextEpisode() }
        playerControls.addView(btnNext)

        // Title text
        titleTextView = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(16.dp(), 0, 16.dp(), 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        playerControls.addView(titleTextView)

        // Fullscreen button
        btnFullscreen = createControlButton(R.drawable.ic_fullscreen) { toggleFullscreen() }
        playerControls.addView(btnFullscreen)

        playerContainer.addView(playerControls)

        // Add views to root
        rootContainer.addView(searchContainer, ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        ))
        rootContainer.addView(playerContainer, ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        ))

        setContentView(rootContainer)

        // Setup debounced search
        setupDebouncedSearch()
    }

    private fun createControlButton(iconRes: Int, onClick: () -> Unit): ImageButton {
        return ImageButton(this).apply {
            setImageResource(iconRes)
            setColorFilter(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(12.dp(), 12.dp(), 12.dp(), 12.dp())
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(48.dp(), 48.dp())
        }
    }

    @OptIn(FlowPreview::class)
    private fun setupDebouncedSearch() {
        var searchJob: Job? = null

        searchEditText.doAfterTextChanged { text ->
            // Show/hide clear button based on text
            clearButton.visibility = if (!text.isNullOrEmpty()) View.VISIBLE else View.GONE

            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                text?.toString()?.let { query ->
                    if (query.length >= 2) {
                        kotlinx.coroutines.delay(500) // 500ms debounce
                        viewModel.searchContent(query)
                    }
                }
            }
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.searchResults.collect { results ->
                contentAdapter.submitList(results)
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                if (isLoading) {
                    clearButton.visibility = View.GONE
                    loadingIndicator.visibility = View.VISIBLE
                } else {
                    loadingIndicator.visibility = View.GONE
                    // Show clear button again if there's text
                    if (searchEditText.text?.isNotEmpty() == true) {
                        clearButton.visibility = View.VISIBLE
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.seasonInfo.collect { info ->
                info?.let { (seasons, episodes) ->
                    totalSeasons = seasons
                    totalEpisodes = episodes
                    updateNavigationButtons()
                }
            }
        }
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isPlayerVisible) {
                    closePlayer()
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                        finish()
                    } else {
                        @Suppress("DEPRECATION")
                        finish()
                    }
                }
            }
        })
    }

    private fun onContentClicked(item: ContentItem) {
        currentContent = item
        when (item.type) {
            "movie" -> {
                currentSeason = 1
                currentEpisode = 1
                totalSeasons = 1
                totalEpisodes = 1
                playContent(item)
            }
            "tv" -> showEpisodeSelector(item)
        }
    }

    private fun showEpisodeSelector(item: ContentItem) {
        lifecycleScope.launch {
            loadingIndicator.visibility = View.VISIBLE
            val seasonData = viewModel.getSeasonInfoWithEpisodes(item.id)
            loadingIndicator.visibility = View.GONE

            if (seasonData.isEmpty()) {
                Toast.makeText(this@MainActivity, "Could not load episode info", Toast.LENGTH_SHORT).show()
                return@launch
            }

            seasonEpisodeCounts = seasonData
            totalSeasons = seasonData.keys.maxOrNull() ?: 1
            showSeasonEpisodeDialog(item, seasonData)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showSeasonEpisodeDialog(item: ContentItem, seasonData: Map<Int, Int>) {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp(), 24.dp(), 24.dp(), 24.dp())
        }

        // Season selector
        val seasonLabel = TextView(this).apply {
            text = "Season"
            setTextColor(Color.WHITE)
            textSize = 16f
        }
        dialogView.addView(seasonLabel)

        var selectedSeason = 1
        var selectedEpisode = 1

        val seasonItems = mutableMapOf<Int, MaterialCardView>()

        // Grid layout for episodes - defined BEFORE season click listener so it can be referenced
        val episodeGridLayout = GridLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Horizontal scroll container for seasons
        val seasonScrollView = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = true
            setPadding(0, 8.dp(), 0, 16.dp())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val seasonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        seasonData.keys.sorted().forEachIndexed { index, seasonNum ->
            val seasonCard = MaterialCardView(this).apply {
                radius = 8.dp().toFloat()
                setCardBackgroundColor(if (index == 0) Color.parseColor("#BB86FC") else Color.parseColor("#1A1A1A"))
                layoutParams = LinearLayout.LayoutParams(48.dp(), 48.dp()).apply {
                    marginEnd = 8.dp()
                }
                tag = seasonNum
                setOnClickListener { clickedView ->
                    val newSeason = (clickedView as MaterialCardView).tag as Int
                    if (newSeason != selectedSeason) {
                        selectedSeason = newSeason
                        seasonItems.forEach { (num, card) ->
                            card.setCardBackgroundColor(Color.parseColor("#1A1A1A"))
                        }
                        setCardBackgroundColor(Color.parseColor("#BB86FC"))

                        // Update episode selector for this season
                        val epCount = seasonData[selectedSeason] ?: 0
                        totalEpisodes = epCount
                        selectedEpisode = 1
                        updateEpisodeSelector(episodeGridLayout, epCount, selectedEpisode) { ep ->
                            selectedEpisode = ep
                        }
                    }
                }
                addView(TextView(this@MainActivity).apply {
                    text = "$seasonNum"
                    setTextColor(Color.WHITE)
                    textSize = 16f
                    gravity = android.view.Gravity.CENTER
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                })
            }
            seasonItems[seasonNum] = seasonCard
            seasonContainer.addView(seasonCard)
        }
        seasonScrollView.addView(seasonContainer)
        dialogView.addView(seasonScrollView)

        // Episode selector label
        val episodeLabel = TextView(this).apply {
            text = "Episode"
            setTextColor(Color.WHITE)
            textSize = 16f
        }
        dialogView.addView(episodeLabel)

        // Vertical scroll container for episodes
        val episodeScrollView = ScrollView(this).apply {
            isVerticalScrollBarEnabled = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                200.dp() // Fixed height for scrolling
            )
            setPadding(0, 8.dp(), 0, 8.dp())
        }

        // Initial episode count for season 1
        totalEpisodes = seasonData[1] ?: 0
        updateEpisodeSelector(episodeGridLayout, totalEpisodes, selectedEpisode) { ep ->
            selectedEpisode = ep
        }
        episodeScrollView.addView(episodeGridLayout)
        dialogView.addView(episodeScrollView)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(item.name)
            .setView(dialogView)
            .setPositiveButton("Play") { _, _ ->
                currentSeason = selectedSeason
                currentEpisode = selectedEpisode
                totalEpisodes = seasonData[selectedSeason] ?: 1
                playContent(item)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.black)
        }

        dialog.show()
    }

    private fun updateEpisodeSelector(
        container: GridLayout,
        episodeCount: Int,
        currentSelection: Int,
        onSelect: (Int) -> Unit
    ) {
        container.removeAllViews()

        if (episodeCount == 0) {
            val noEpisodesText = TextView(this).apply {
                text = "No episodes available yet"
                setTextColor(Color.parseColor("#888888"))
                textSize = 14f
                setPadding(0, 8.dp(), 0, 8.dp())
            }
            container.addView(noEpisodesText)
            return
        }

        container.columnCount = 5 // 5 episodes per row

        val episodeCards = mutableMapOf<Int, MaterialCardView>()

        for (i in 1..episodeCount) {
            val episodeCard = MaterialCardView(this).apply {
                radius = 8.dp().toFloat()
                setCardBackgroundColor(if (i == currentSelection) Color.parseColor("#BB86FC") else Color.parseColor("#1A1A1A"))
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 48.dp()
                    height = 48.dp()
                    setMargins(0, 0, 8.dp(), 8.dp())
                }
                setOnClickListener {
                    onSelect(i)
                    episodeCards.forEach { (_, card) ->
                        card.setCardBackgroundColor(Color.parseColor("#1A1A1A"))
                    }
                    setCardBackgroundColor(Color.parseColor("#BB86FC"))
                }
                addView(TextView(this@MainActivity).apply {
                    text = "$i"
                    setTextColor(Color.WHITE)
                    textSize = 16f
                    gravity = android.view.Gravity.CENTER
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                })
            }
            episodeCards[i] = episodeCard
            container.addView(episodeCard)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun playContent(item: ContentItem) {
        isPlayerVisible = true
        playerContainer.visibility = View.VISIBLE
        searchContainer.visibility = View.GONE

        currentContent = item

        val streamUrl = if (item.type == "tv") {
            "https://vidlink.pro/tv/${item.id}/$currentSeason/$currentEpisode?primaryColor=bb86fc&secondaryColor=03dac6&iconColor=ffffff&autoplay=true"
        } else {
            "https://vidlink.pro/movie/${item.id}?primaryColor=bb86fc&secondaryColor=03dac6&iconColor=ffffff&autoplay=true"
        }

        titleTextView.text = if (item.type == "tv") {
            "${item.name} - S${currentSeason}E$currentEpisode"
        } else {
            item.name
        }

        updateNavigationButtons()

        // Load URL with ad blocking enabled
        webView.loadUrl(streamUrl)

        hideSystemUI()
    }

    private fun updateNavigationButtons() {
        val isTV = currentContent?.type == "tv"
        val canGoPrevious = isTV && (currentSeason > 1 || currentEpisode > 1)
        val canGoNext = isTV && let {
            val currentSeasonMax = seasonEpisodeCounts[currentSeason] ?: totalEpisodes
            if (currentEpisode < currentSeasonMax) {
                true
            } else if (currentSeason < totalSeasons) {
                seasonEpisodeCounts[currentSeason + 1]?.let { it > 0 } ?: false
            } else {
                false
            }
        }

        btnPrevious.visibility = if (canGoPrevious) View.VISIBLE else View.GONE
        btnNext.visibility = if (canGoNext) View.VISIBLE else View.GONE
    }

    private fun playPreviousEpisode() {
        if (currentEpisode > 1) {
            currentEpisode--
            continuePlayback()
        } else if (currentSeason > 1) {
            currentSeason--
            // Get episode count from cache or use cached data
            val prevSeasonEpisodes = seasonEpisodeCounts[currentSeason] ?: totalEpisodes
            currentEpisode = maxOf(1, prevSeasonEpisodes)
            continuePlayback()
        }
    }

    private fun playNextEpisode() {
        val currentSeasonMax = seasonEpisodeCounts[currentSeason] ?: totalEpisodes
        if (currentEpisode < currentSeasonMax) {
            currentEpisode++
        } else if (currentSeason < totalSeasons) {
            currentSeason++
            currentEpisode = 1
        }
        continuePlayback()
    }

    @SuppressLint("SetTextI18n")
    private fun continuePlayback() {
        currentContent?.let { item ->
            val streamUrl = "https://vidlink.pro/tv/${item.id}/$currentSeason/$currentEpisode?primaryColor=bb86fc&secondaryColor=03dac6&iconColor=ffffff&autoplay=true"
            titleTextView.text = "${item.name} - S${currentSeason}E$currentEpisode"
            updateNavigationButtons()
            webView.loadUrl(streamUrl)
        }
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            playerControls.visibility = View.GONE
            hideSystemUI()
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            playerControls.visibility = View.VISIBLE
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.systemBars())
            window.insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    )
        }
    }

    private fun showSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    private fun closePlayer() {
        isPlayerVisible = false
        isFullscreen = false
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        playerContainer.visibility = View.GONE
        searchContainer.visibility = View.VISIBLE
        webView.loadUrl("about:blank")
        showSystemUI()
    }

    @SuppressLint("SetTextI18n")
    private fun injectPlayerEnhancements() {
        val js = """
            (function() {
                // Remove ads and popups
                var adSelectors = [
                    '[class*="ad"]', '[id*="ad"]', '[class*="popup"]', 
                    '[class*="banner"]', '[class*="sponsor"]', 'iframe',
                    '.ad-container', '.ad-wrapper', '.ads', '#ads'
                ];
                
                adSelectors.forEach(function(selector) {
                    try {
                        var elements = document.querySelectorAll(selector);
                        elements.forEach(function(el) { 
                            if (!el.className.includes('player') && !el.className.includes('video')) {
                                el.remove(); 
                            }
                        });
                    } catch(e) {}
                });
                
                // Prevent popups
                window.open = function() { return null; };
                document.addEventListener('click', function(e) {
                    if (e.target.tagName === 'A' && !e.target.href.includes('vidlink')) {
                        e.preventDefault();
                    }
                }, true);
            })();
        """.trimIndent()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(js, null)
        } else {
            @Suppress("DEPRECATION")
            webView.loadUrl("javascript:$js")
        }
    }

    // Helper extension function
    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    // WebView Client with Ad Blocking
    inner class AdBlockWebViewClient : WebViewClient() {
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)

            // Block ad domains
            for (adDomain in adDomains) {
                if (url.contains(adDomain, ignoreCase = true)) {
                    return WebResourceResponse(
                        "text/plain",
                        "utf-8",
                        java.io.ByteArrayInputStream("".toByteArray())
                    )
                }
            }

            // Block common ad patterns
            if (url.contains("/ad/", ignoreCase = true) ||
                url.contains("/ads/", ignoreCase = true) ||
                url.contains("/banner/", ignoreCase = true) ||
                url.contains("/popup/", ignoreCase = true) ||
                url.contains("googleads", ignoreCase = true) ||
                url.contains("analytics", ignoreCase = true) ||
                url.contains("tracking", ignoreCase = true) ||
                url.contains("/click/", ignoreCase = true) ||
                url.endsWith(".gif", ignoreCase = true) && url.contains("ad", ignoreCase = true)
            ) {
                return WebResourceResponse(
                    "text/plain",
                    "utf-8",
                    java.io.ByteArrayInputStream("".toByteArray())
                )
            }

            return super.shouldInterceptRequest(view, request)
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url?.toString() ?: return false

            // Block redirects to ad domains
            for (adDomain in adDomains) {
                if (url.contains(adDomain, ignoreCase = true)) {
                    return true
                }
            }

            return false
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            injectPlayerEnhancements()
        }
    }

    // Content Adapter
    inner class ContentAdapter(private val onItemClick: (ContentItem) -> Unit) :
        RecyclerView.Adapter<ContentViewHolder>() {

        private var items = listOf<ContentItem>()

        fun submitList(newItems: List<ContentItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ContentViewHolder {
            val card = MaterialCardView(parent.context).apply {
                radius = 12.dp().toFloat()
                setCardBackgroundColor(Color.parseColor("#1A1A1A"))
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8.dp(), 0, 8.dp())
                }
                useCompatPadding = true
            }

            val container = LinearLayout(parent.context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(8.dp(), 8.dp(), 8.dp(), 8.dp())
            }

            // Poster image
            val poster = AppCompatImageView(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(80.dp(), 120.dp())
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(Color.parseColor("#2A2A2A"))
            }
            container.addView(poster)

            // Info container
            val infoContainer = LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16.dp(), 0, 16.dp(), 0)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            // Title
            val title = TextView(parent.context).apply {
                setTextColor(Color.WHITE)
                textSize = 16f
                setLines(2)
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            infoContainer.addView(title)

            // Type badge
            val typeBadge = TextView(parent.context).apply {
                setTextColor(Color.parseColor("#BB86FC"))
                textSize = 12f
                setPadding(0, 8.dp(), 0, 0)
            }
            infoContainer.addView(typeBadge)

            container.addView(infoContainer)
            card.addView(container)

            card.setOnClickListener {
                val position = parent.indexOfChild(card)
                if (position != -1 && position < items.size) {
                    onItemClick(items[position])
                }
            }

            return ContentViewHolder(card, poster, title, typeBadge)
        }

        override fun onBindViewHolder(holder: ContentViewHolder, position: Int) {
            val item = items[position]

            holder.title.text = item.name
            holder.typeBadge.text = item.type.uppercase()

            // Load poster with Glide
            if (!item.posterUrl.isNullOrEmpty()) {
                Glide.with(holder.poster.context)
                    .load(item.posterUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.placeholder_poster)
                    .error(R.drawable.placeholder_poster)
                    .centerCrop()
                    .into(holder.poster)
            } else {
                holder.poster.setImageResource(R.drawable.placeholder_poster)
            }
        }

        override fun getItemCount() = items.size
    }

    inner class ContentViewHolder(
        val card: MaterialCardView,
        val poster: AppCompatImageView,
        val title: TextView,
        val typeBadge: TextView
    ) : RecyclerView.ViewHolder(card)

    override fun onDestroy() {
        rotationAnimator?.cancel()
        webView.destroy()
        super.onDestroy()
    }
}
