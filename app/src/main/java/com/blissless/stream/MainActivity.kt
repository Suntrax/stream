package com.blissless.stream

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.inputmethod.InputMethodManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicBoolean
import androidx.core.graphics.toColorInt

@UnstableApi
@SuppressLint("SetTextI18n", "InternalInsetResource", "DiscouragedApi")
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    // UI Components - Main
    private lateinit var rootContainer: ConstraintLayout

    // Navigation
    private lateinit var bottomNav: LinearLayout
    private lateinit var btnExplore: LinearLayout
    private lateinit var btnSearch: LinearLayout
    private lateinit var ivExplore: ImageView
    private lateinit var ivSearch: ImageView
    private lateinit var tvExplore: TextView
    private lateinit var tvSearch: TextView

    // Explore Page
    private lateinit var exploreContainer: ScrollView
    private lateinit var exploreContent: LinearLayout
    private lateinit var carouselContainer: FrameLayout
    private lateinit var carouselViewPager: RecyclerView
    private lateinit var carouselIndicator: LinearLayout
    private lateinit var exploreLoadingIndicator: ProgressBar

    // Explore Sections
    private lateinit var popularMoviesRecyclerView: RecyclerView
    private lateinit var popularTVRecyclerView: RecyclerView
    private lateinit var topRatedRecyclerView: RecyclerView
    private lateinit var nowPlayingRecyclerView: RecyclerView
    private lateinit var actionMoviesRecyclerView: RecyclerView
    private lateinit var comedyMoviesRecyclerView: RecyclerView
    private lateinit var horrorMoviesRecyclerView: RecyclerView
    private lateinit var sciFiMoviesRecyclerView: RecyclerView
    private lateinit var animationMoviesRecyclerView: RecyclerView
    private lateinit var thrillerMoviesRecyclerView: RecyclerView
    private lateinit var fantasyMoviesRecyclerView: RecyclerView
    private lateinit var topRatedTVRecyclerView: RecyclerView
    private lateinit var crimeTVShowsRecyclerView: RecyclerView

    // Search Overlay
    private var searchOverlay: FrameLayout? = null
    private lateinit var searchOverlayEditText: AppCompatEditText
    private lateinit var searchOverlayLoading: ProgressBar
    private lateinit var searchOverlayResults: RecyclerView
    private lateinit var searchOverlayAdapter: SearchOverlayAdapter

    // Details Dialog
    private var detailsDialogView: FrameLayout? = null

    // Player components
    private lateinit var playerContainer: FrameLayout
    private lateinit var playerView: PlayerView
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var scraperWebView: WebView
    private lateinit var playerLoadingIndicator: ProgressBar
    private lateinit var overlayContainer: FrameLayout
    private lateinit var btnResize: ImageButton
    private lateinit var contentTitleText: TextView

    // Skip indicators
    private lateinit var skipIndicatorLeft: TextView
    private lateinit var skipIndicatorRight: TextView

    // Episode navigation buttons (Injected into PlayerView)
    private var btnPlayerPrevious: ImageButton? = null
    private var btnPlayerNext: ImageButton? = null

    // Gesture detection
    private lateinit var gestureDetector: GestureDetector

    // Resize mode
    private var currentResizeMode = 0
    private val resizeModes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit",
        AspectRatioFrameLayout.RESIZE_MODE_FILL to "Fill",
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Stretch"
    )

    // State
    private var currentNavPage = 0 // 0 = Explore, 1 = Search
    private var currentContent: ContentItem? = null
    private var currentContentDetails: ContentDetails? = null
    private var currentSeason = 1
    private var currentEpisode = 1
    private var totalSeasons = 1
    private var totalEpisodes = 1
    private var isPlayerVisible = false
    private var isDetailsVisible = false
    private var isSearchOverlayVisible = false

    // Season episode counts
    private var seasonEpisodeCounts: Map<Int, Int> = emptyMap()

    // M3U8 scraper state
    private var foundM3u8Url: String? = null
    private val scraperLock = AtomicBoolean(false)

    // Search debounce job
    private var searchOverlayJob: Job? = null

    // Flag to prevent search results from appearing on overlay open
    private var ignoreSearchResults = false

    // Carousel
    private var carouselTimer: Timer? = null
    private var currentCarouselPosition = 0
    private var carouselItems: List<ContentItem> = emptyList()
    private lateinit var carouselBackground: AppCompatImageView

    // Ad blocking domains
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

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createUI()
        setupObservers()
        setupBackHandler()
        initializePlayer()
        setupGestureDetector()
        setupNavigation()
        loadExploreContent()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (isPlayerVisible) {
            val location = IntArray(2)
            playerView.getLocationOnScreen(location)
            val x = ev.rawX
            val y = ev.rawY

            if (x >= location[0] && x <= location[0] + playerView.width &&
                y >= location[1] && y <= location[1] + playerView.height) {
                if (gestureDetector.onTouchEvent(ev)) {
                    return true
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
        playerView.player = exoPlayer
        playerView.useController = true
        playerView.controllerShowTimeoutMs = 3000

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    playNextEpisodeAuto()
                }
            }
        })

        playerView.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
            overlayContainer.visibility = visibility
            if (visibility == View.VISIBLE) {
                updatePlayerControlsVisibility()
            }
        })

        playerView.post {
            removeBackgroundFromPlayerControls(playerView)
            setupCustomPlayerControls(playerView)
        }
    }

    private fun setupCustomPlayerControls(playerView: PlayerView) {
        // 1. Hide the default Rewind (10s back) and Fast Forward (10s forward) buttons
        // We use the "Prev/Next" buttons for Episode navigation instead.
        playerView.findViewById<View>(androidx.media3.ui.R.id.exo_rew)?.visibility = View.GONE
        playerView.findViewById<View>(androidx.media3.ui.R.id.exo_ffwd)?.visibility = View.GONE

        // 2. Increase Play Button Size
        val playButton = playerView.findViewById<View>(androidx.media3.ui.R.id.exo_play_pause)
        playButton?.layoutParams = (playButton?.layoutParams as? android.widget.LinearLayout.LayoutParams)?.apply {
            width = 72.dp()
            height = 72.dp()
        }

        // 3. Find the Default "Prev" and "Next" buttons
        // These are normally used for 'Skip to Start' or 'Skip to End', but we will use them for Episodes.
        btnPlayerPrevious = playerView.findViewById(androidx.media3.ui.R.id.exo_prev)
        btnPlayerNext = playerView.findViewById(androidx.media3.ui.R.id.exo_next)

        // 4. Configure Previous Episode Button
        btnPlayerPrevious?.apply {
            // Remove default grey background/border
            background = null
            // Start hidden, will be shown by updatePlayerControlsVisibility() for TV shows
            visibility = View.GONE
            // Override default click with Episode logic
            setOnClickListener { playPreviousEpisode() }
        }

        // 5. Configure Next Episode Button
        btnPlayerNext?.apply {
            // Remove default grey background/border
            background = null
            // Start hidden
            visibility = View.GONE
            // Override default click with Episode logic
            setOnClickListener { playNextEpisode() }
        }
    }

    private fun updatePlayerControlsVisibility() {
        val content = currentContent
        if (content == null) {
            btnPlayerPrevious?.visibility = View.GONE
            btnPlayerNext?.visibility = View.GONE
            return
        }

        if (content.type == "tv") {
            val currentSeasonMax = if (seasonEpisodeCounts.isNotEmpty()) {
                seasonEpisodeCounts[currentSeason] ?: totalEpisodes
            } else {
                totalEpisodes
            }

            val canGoPrevious = currentSeason > 1 || currentEpisode > 1
            val canGoNext = if (seasonEpisodeCounts.isNotEmpty()) {
                currentEpisode < currentSeasonMax || currentSeason < (seasonEpisodeCounts.keys.maxOrNull() ?: totalSeasons)
            } else {
                true
            }

            btnPlayerPrevious?.apply {
                visibility = View.VISIBLE
                background = null // Remove grey background/border

                // Aggressively set visual state to Active
                imageAlpha = 255
                alpha = 1.0f

                // Clear theme tints and force White color
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    imageTintList = null
                }
                setColorFilter(Color.WHITE)

                isEnabled = canGoPrevious
                if (!canGoPrevious) {
                    imageAlpha = 100 // Visually dim if disabled
                    setColorFilter(Color.GRAY)
                }
            }

            btnPlayerNext?.apply {
                visibility = View.VISIBLE
                background = null // Remove grey background/border

                // Aggressively set visual state to Active
                imageAlpha = 255
                alpha = 1.0f

                // Clear theme tints and force White color
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    imageTintList = null
                }
                setColorFilter(Color.WHITE)

                isEnabled = canGoNext
                if (!canGoNext) {
                    imageAlpha = 100 // Visually dim if disabled
                    setColorFilter(Color.GRAY)
                }
            }
        } else {
            btnPlayerPrevious?.visibility = View.GONE
            btnPlayerNext?.visibility = View.GONE
        }
    }

    private fun removeBackgroundFromPlayerControls(view: View) {
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child is LinearLayout || child is FrameLayout) {
                    child.setBackgroundColor(Color.TRANSPARENT)
                }
                removeBackgroundFromPlayerControls(child)
            }
        }
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val location = IntArray(2)
                playerView.getLocationOnScreen(location)
                val playerCenterX = location[0] + playerView.width / 2
                val tapX = e.rawX

                if (tapX < playerCenterX) {
                    skipBackward(5000)
                } else {
                    skipForward(15000)
                }
                return true
            }
        })
    }

    private fun skipForward(ms: Long) {
        val newPosition = exoPlayer.currentPosition + ms
        exoPlayer.seekTo(newPosition)
        showSkipIndicator(right = true, ms)
    }

    private fun skipBackward(ms: Long) {
        val newPosition = maxOf(0, exoPlayer.currentPosition - ms)
        exoPlayer.seekTo(newPosition)
        showSkipIndicator(right = false, ms)
    }

    @SuppressLint("SetTextI18n")
    private fun showSkipIndicator(right: Boolean, ms: Long) {
        val seconds = ms / 1000
        if (right) {
            skipIndicatorRight.text = "+${seconds}s"
            skipIndicatorRight.visibility = View.VISIBLE
            Handler(Looper.getMainLooper()).postDelayed({
                skipIndicatorRight.visibility = View.GONE
            }, 500)
        } else {
            skipIndicatorLeft.text = "-${seconds}s"
            skipIndicatorLeft.visibility = View.VISIBLE
            Handler(Looper.getMainLooper()).postDelayed({
                skipIndicatorLeft.visibility = View.GONE
            }, 500)
        }
    }

    private fun playNextEpisodeAuto() {
        currentContent?.let { item ->
            if (item.type == "tv") {
                val currentSeasonMax = seasonEpisodeCounts[currentSeason] ?: totalEpisodes
                if (currentEpisode < currentSeasonMax) {
                    currentEpisode++
                    continuePlayback()
                } else if (currentSeason < (seasonEpisodeCounts.keys.maxOrNull() ?: totalSeasons)) {
                    currentSeason++
                    currentEpisode = 1
                    totalEpisodes = seasonEpisodeCounts[currentSeason] ?: 1
                    continuePlayback()
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun cycleResizeMode() {
        currentResizeMode = (currentResizeMode + 1) % resizeModes.size
        val (mode, name) = resizeModes[currentResizeMode]
        playerView.resizeMode = mode
        Toast.makeText(this, name, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        if (result == 0) {
            result = (24 * resources.displayMetrics.density).toInt()
        }
        return result
    }

    private fun setupNavigation() {
        btnExplore.setOnClickListener { switchToExplore() }
        btnSearch.setOnClickListener { showSearchOverlay() }
    }

    private fun switchToExplore() {
        currentNavPage = 0
        exploreContainer.visibility = View.VISIBLE
        updateNavSelection()
    }

    private fun updateNavSelection() {
        val selectedColor = "#f472a1".toColorInt()
        val unselectedColor = "#666666".toColorInt()

        if (currentNavPage == 0) {
            ivExplore.setColorFilter(selectedColor)
            tvExplore.setTextColor(selectedColor)
            ivSearch.setColorFilter(unselectedColor)
            tvSearch.setTextColor(unselectedColor)
        } else {
            ivExplore.setColorFilter(unselectedColor)
            tvExplore.setTextColor(unselectedColor)
            ivSearch.setColorFilter(selectedColor)
            tvSearch.setTextColor(selectedColor)
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun createUI() {
        val statusBarHeight = getStatusBarHeight()

        rootContainer = ConstraintLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // ========== EXPLORE PAGE ==========
        exploreContainer = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
        }

        exploreContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, statusBarHeight, 0, 80.dp())
        }

        exploreLoadingIndicator = ProgressBar(this).apply {
            indeterminateDrawable.setTint("#f472a1".toColorInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                topMargin = 100.dp()
            }
        }

        carouselContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                220.dp()
            )
        }

        carouselBackground = AppCompatImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor("#1A1A1A".toColorInt())
        }
        carouselContainer.addView(carouselBackground)

        val carouselGradient = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf("#60000000".toColorInt(), "#CC000000".toColorInt())
            )
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        carouselContainer.addView(carouselGradient)

        carouselViewPager = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            PagerSnapHelper().attachToRecyclerView(this)
        }
        carouselContainer.addView(carouselViewPager)

        carouselIndicator = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.BOTTOM
                bottomMargin = 12.dp()
            }
        }
        carouselContainer.addView(carouselIndicator)

        exploreContent.addView(carouselContainer)

        // ========== EXPLORE SECTIONS ==========
        val popularMoviesSection = createExploreSection("🔥 Popular Movies")
        exploreContent.addView(popularMoviesSection.first)
        popularMoviesRecyclerView = popularMoviesSection.second

        val popularTVSection = createExploreSection("📺 Popular TV Shows")
        exploreContent.addView(popularTVSection.first)
        popularTVRecyclerView = popularTVSection.second

        val topRatedSection = createExploreSection("⭐ Top Rated Movies")
        exploreContent.addView(topRatedSection.first)
        topRatedRecyclerView = topRatedSection.second

        val topRatedTVSection = createExploreSection("🏆 Top Rated Series")
        exploreContent.addView(topRatedTVSection.first)
        topRatedTVRecyclerView = topRatedTVSection.second

        val nowPlayingSection = createExploreSection("🎬 Now in Theaters")
        exploreContent.addView(nowPlayingSection.first)
        nowPlayingRecyclerView = nowPlayingSection.second

        val actionSection = createExploreSection("💥 Action & Adventure")
        exploreContent.addView(actionSection.first)
        actionMoviesRecyclerView = actionSection.second

        val comedySection = createExploreSection("😂 Comedy")
        exploreContent.addView(comedySection.first)
        comedyMoviesRecyclerView = comedySection.second

        val horrorSection = createExploreSection("👻 Horror")
        exploreContent.addView(horrorSection.first)
        horrorMoviesRecyclerView = horrorSection.second

        val sciFiSection = createExploreSection("🚀 Sci-Fi")
        exploreContent.addView(sciFiSection.first)
        sciFiMoviesRecyclerView = sciFiSection.second

        val animationSection = createExploreSection("🎨 Animation")
        exploreContent.addView(animationSection.first)
        animationMoviesRecyclerView = animationSection.second

        val thrillerSection = createExploreSection("🔪 Thriller")
        exploreContent.addView(thrillerSection.first)
        thrillerMoviesRecyclerView = thrillerSection.second

        val fantasySection = createExploreSection("🧙 Fantasy")
        exploreContent.addView(fantasySection.first)
        fantasyMoviesRecyclerView = fantasySection.second

        val crimeTVSection = createExploreSection("🕵️ Crime Dramas")
        exploreContent.addView(crimeTVSection.first)
        crimeTVShowsRecyclerView = crimeTVSection.second

        exploreContainer.addView(exploreContent)

        // ========== PLAYER CONTAINER ==========
        playerContainer = FrameLayout(this).apply {
            visibility = View.GONE
            setBackgroundColor(Color.BLACK)
        }

        playerView = PlayerView(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        playerContainer.addView(playerView)

        overlayContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        contentTitleText = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(12.dp(), 8.dp() + statusBarHeight, 12.dp(), 8.dp())
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
            }
        }
        overlayContainer.addView(contentTitleText)

        btnResize = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_crop) // Built-in icon
            setColorFilter(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(8.dp(), 8.dp(), 8.dp(), 8.dp())
            layoutParams = FrameLayout.LayoutParams(40.dp(), 40.dp()).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.END
                topMargin = statusBarHeight
                marginEnd = 8.dp()
            }
            setOnClickListener { cycleResizeMode() }
        }
        overlayContainer.addView(btnResize)

        // Skip indicators
        skipIndicatorLeft = TextView(this).apply {
            text = "-5s"
            setTextColor(Color.WHITE)
            textSize = 24f
            setPadding(32.dp(), 16.dp(), 32.dp(), 16.dp())
            setBackgroundColor(Color.parseColor("#80000000"))
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
                marginStart = 48.dp()
            }
        }
        overlayContainer.addView(skipIndicatorLeft)

        skipIndicatorRight = TextView(this).apply {
            text = "+15s"
            setTextColor(Color.WHITE)
            textSize = 24f
            setPadding(32.dp(), 16.dp(), 32.dp(), 16.dp())
            setBackgroundColor(Color.parseColor("#80000000"))
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.END
                marginEnd = 48.dp()
            }
        }
        overlayContainer.addView(skipIndicatorRight)

        playerContainer.addView(overlayContainer)

        playerLoadingIndicator = ProgressBar(this).apply {
            indeterminateDrawable.setTint("#f472a1".toColorInt())
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
            visibility = View.GONE
        }
        playerContainer.addView(playerLoadingIndicator)

        scraperWebView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadsImagesAutomatically = false
                mediaPlaybackRequiresUserGesture = true
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            }
            webViewClient = M3u8ScraperWebViewClient()
            setBackgroundColor(Color.TRANSPARENT)
            visibility = View.GONE
        }
        playerContainer.addView(scraperWebView)

        // ========== BOTTOM NAVIGATION ==========
        bottomNav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor("#121212".toColorInt())
            setPadding(0, 8.dp(), 0, 16.dp())
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            }
        }

        btnExplore = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(16.dp(), 8.dp(), 16.dp(), 8.dp())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        ivExplore = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_compass) // Built-in icon
            setColorFilter("#f472a1".toColorInt())
            layoutParams = LinearLayout.LayoutParams(28.dp(), 28.dp()).apply {
                gravity = android.view.Gravity.CENTER
            }
        }
        btnExplore.addView(ivExplore)

        tvExplore = TextView(this).apply {
            text = "Explore"
            textSize = 12f
            setTextColor("#f472a1".toColorInt())
            setPadding(0, 4.dp(), 0, 0)
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        btnExplore.addView(tvExplore)

        bottomNav.addView(btnExplore)

        btnSearch = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(16.dp(), 8.dp(), 16.dp(), 8.dp())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        ivSearch = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_search)
            setColorFilter("#666666".toColorInt())
            layoutParams = LinearLayout.LayoutParams(28.dp(), 28.dp()).apply {
                gravity = android.view.Gravity.CENTER
            }
        }
        btnSearch.addView(ivSearch)

        tvSearch = TextView(this).apply {
            text = "Search"
            textSize = 12f
            setTextColor("#666666".toColorInt())
            setPadding(0, 4.dp(), 0, 0)
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        btnSearch.addView(tvSearch)

        bottomNav.addView(btnSearch)

        rootContainer.addView(exploreContainer, ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        ).apply {
            bottomToTop = -1
        })

        rootContainer.addView(playerContainer, ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        ))

        rootContainer.addView(bottomNav)

        setContentView(rootContainer)

        createSearchOverlay()
    }

    @SuppressLint("SetTextI18n")
    private fun createSearchOverlay() {
        val statusBarHeight = getStatusBarHeight()

        searchOverlay = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#EB000000"))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
            isClickable = true
            isFocusable = true
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dp(), statusBarHeight + 20.dp(), 20.dp(), 20.dp())
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            isClickable = true
        }

        val searchBarCard = MaterialCardView(this).apply {
            radius = 12.dp().toFloat()
            setCardBackgroundColor("#1A1A1A".toColorInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isClickable = true
        }

        val searchBarContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(12.dp(), 8.dp(), 8.dp(), 8.dp())
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val searchIcon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_search)
            setColorFilter("#f472a1".toColorInt())
            layoutParams = LinearLayout.LayoutParams(24.dp(), 24.dp())
        }
        searchBarContainer.addView(searchIcon)

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(8.dp(), 1.dp())
        }
        searchBarContainer.addView(spacer)

        searchOverlayEditText = AppCompatEditText(this).apply {
            hint = "Search movies & TV shows..."
            setHintTextColor("#666666".toColorInt())
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        searchBarContainer.addView(searchOverlayEditText)

        searchOverlayLoading = ProgressBar(this).apply {
            indeterminateDrawable.setTint("#f472a1".toColorInt())
            layoutParams = LinearLayout.LayoutParams(20.dp(), 20.dp())
            visibility = View.GONE
        }
        searchBarContainer.addView(searchOverlayLoading)

        val closeBtn = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(4.dp(), 4.dp(), 4.dp(), 4.dp())
            layoutParams = LinearLayout.LayoutParams(28.dp(), 28.dp())
            setOnClickListener { hideSearchOverlay() }
        }
        searchBarContainer.addView(closeBtn)

        searchBarCard.addView(searchBarContainer)
        container.addView(searchBarCard)

        searchOverlayResults = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            searchOverlayAdapter = SearchOverlayAdapter { item ->
                onContentClicked(item)
            }
            adapter = searchOverlayAdapter
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                topMargin = 12.dp()
            }
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: android.graphics.Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.set(0, 0, 0, 8.dp())
                }
            })
        }
        container.addView(searchOverlayResults)

        searchOverlay?.addView(container)
        rootContainer.addView(searchOverlay)

        searchOverlayEditText.doAfterTextChanged { text ->
            searchOverlayJob?.cancel()
            searchOverlayJob = lifecycleScope.launch {
                if (text.isNullOrEmpty()) {
                    searchOverlayAdapter.submitList(emptyList())
                    searchOverlayLoading.visibility = View.GONE
                    ignoreSearchResults = true
                    viewModel.clearSearchResults()
                } else {
                    ignoreSearchResults = false
                    searchOverlayLoading.visibility = View.VISIBLE
                    delay(500)
                    viewModel.searchContent(text.toString())
                }
            }
        }
    }

    private fun showSearchOverlay() {
        isSearchOverlayVisible = true

        ignoreSearchResults = true

        searchOverlayAdapter.submitList(emptyList())
        viewModel.clearSearchResults()
        viewModel.clearSearchTrigger()
        searchOverlayEditText.text?.clear()
        searchOverlayLoading.visibility = View.GONE

        searchOverlay?.alpha = 0f
        searchOverlay?.translationY = -rootContainer.height.toFloat() * 0.1f
        searchOverlay?.visibility = View.VISIBLE

        searchOverlay?.animate()
            ?.translationY(0f)
            ?.alpha(1f)
            ?.setDuration(300)
            ?.setInterpolator(DecelerateInterpolator(1.5f))
            ?.start()

        searchOverlayEditText.requestFocus()

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(searchOverlayEditText, InputMethodManager.SHOW_IMPLICIT)

        currentNavPage = 1
        updateNavSelection()
    }

    private fun hideSearchOverlay() {
        isSearchOverlayVisible = false
        ignoreSearchResults = true

        viewModel.clearSearchResults()
        viewModel.clearSearchTrigger()

        searchOverlay?.animate()?.cancel()
        searchOverlay?.animate()
            ?.translationY(-rootContainer.height.toFloat() * 0.1f)
            ?.alpha(0f)
            ?.setDuration(250)
            ?.setInterpolator(AccelerateDecelerateInterpolator())
            ?.setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!isSearchOverlayVisible) {
                        searchOverlay?.visibility = View.GONE
                        searchOverlay?.translationY = 0f
                        searchOverlay?.alpha = 1f
                    }
                }
            })
            ?.start()

        searchOverlayEditText.text?.clear()
        searchOverlayEditText.clearFocus()

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchOverlayEditText.windowToken, 0)

        currentNavPage = 0
        updateNavSelection()
    }

    private fun createExploreSection(title: String): Pair<LinearLayout, RecyclerView> {
        val sectionContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16.dp(), 0, 16.dp())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleText = TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(16.dp(), 0, 16.dp(), 8.dp())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        sectionContainer.addView(titleText)

        val recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                220.dp()
            )
            setPadding(8.dp(), 0, 8.dp(), 0)
        }
        sectionContainer.addView(recyclerView)

        return Pair(sectionContainer, recyclerView)
    }

    private fun loadExploreContent() {
        lifecycleScope.launch {
            exploreLoadingIndicator.visibility = View.VISIBLE

            viewModel.loadTrending()
            viewModel.loadPopularMovies()
            viewModel.loadPopularTVShows()
            viewModel.loadTopRatedMovies()
            viewModel.loadTopRatedTVShows()
            viewModel.loadNowPlaying()
            viewModel.loadActionMovies()
            viewModel.loadComedyMovies()
            viewModel.loadHorrorMovies()
            viewModel.loadSciFiMovies()
            viewModel.loadAnimationMovies()
            viewModel.loadThrillerMovies()
            viewModel.loadFantasyMovies()
            viewModel.loadCrimeTVShows()

            exploreLoadingIndicator.visibility = View.GONE
        }
    }

    private fun playPreviousEpisode() {
        currentContent?.let { item ->
            if (item.type == "tv") {
                if (currentEpisode > 1) {
                    currentEpisode--
                    continuePlayback()
                } else if (currentSeason > 1) {
                    currentSeason--
                    currentEpisode = seasonEpisodeCounts[currentSeason] ?: 1
                    totalEpisodes = seasonEpisodeCounts[currentSeason] ?: 1
                    continuePlayback()
                }
            }
        }
    }

    private fun playNextEpisode() {
        currentContent?.let { item ->
            if (item.type == "tv") {
                val currentSeasonMax = seasonEpisodeCounts[currentSeason] ?: totalEpisodes
                val maxSeason = seasonEpisodeCounts.keys.maxOrNull() ?: totalSeasons

                if (currentEpisode < currentSeasonMax) {
                    currentEpisode++
                    continuePlayback()
                } else if (currentSeason < maxSeason) {
                    currentSeason++
                    currentEpisode = 1
                    totalEpisodes = seasonEpisodeCounts[currentSeason] ?: 1
                    continuePlayback()
                } else if (seasonEpisodeCounts.isEmpty()) {
                    currentEpisode++
                    continuePlayback()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateContentTitle() {
        currentContent?.let { item ->
            contentTitleText.text = if (item.type == "tv") {
                "${item.name} - S${currentSeason}E$currentEpisode"
            } else {
                item.name
            }
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.searchResults.collect { results ->
                if (!ignoreSearchResults) {
                    searchOverlayAdapter.submitList(results)
                }
                searchOverlayLoading.visibility = View.GONE
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
                info.let { (seasons, episodes) ->
                    totalSeasons = seasons
                    totalEpisodes = episodes
                }
            }
        }

        lifecycleScope.launch {
            viewModel.trending.collect { items ->
                if (items.isNotEmpty()) {
                    setupCarousel(items)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.popularMovies.collect { items ->
                if (items.isNotEmpty()) {
                    setupHorizontalSection(popularMoviesRecyclerView, items)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.popularTVShows.collect { items ->
                if (items.isNotEmpty()) {
                    setupHorizontalSection(popularTVRecyclerView, items)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.topRatedMovies.collect { items ->
                if (items.isNotEmpty()) {
                    setupHorizontalSection(topRatedRecyclerView, items)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.topRatedTVShows.collect { items ->
                if (items.isNotEmpty()) {
                    setupHorizontalSection(topRatedTVRecyclerView, items)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.nowPlaying.collect { items ->
                if (items.isNotEmpty()) {
                    setupHorizontalSection(nowPlayingRecyclerView, items)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.actionMovies.collect { items ->
                if (items.isNotEmpty()) {
                    setupHorizontalSection(actionMoviesRecyclerView, items)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.comedyMovies.collect { items ->
                if (items.isNotEmpty()) {
                    setupHorizontalSection(comedyMoviesRecyclerView, items)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.horrorMovies.collect { items ->
                if (items.isNotEmpty()) {
                    setupHorizontalSection(horrorMoviesRecyclerView, items)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.sciFiMovies.collect { items ->
                if (items.isNotEmpty()) {
                    setupHorizontalSection(sciFiMoviesRecyclerView, items)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.animationMovies.collect { items ->
                if (items.isNotEmpty()) {
                    setupHorizontalSection(animationMoviesRecyclerView, items)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.thrillerMovies.collect { items ->
                if (items.isNotEmpty()) {
                    setupHorizontalSection(thrillerMoviesRecyclerView, items)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.fantasyMovies.collect { items ->
                if (items.isNotEmpty()) {
                    setupHorizontalSection(fantasyMoviesRecyclerView, items)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.crimeTVShows.collect { items ->
                if (items.isNotEmpty()) {
                    setupHorizontalSection(crimeTVShowsRecyclerView, items)
                }
            }
        }
    }

    private fun setupCarousel(items: List<ContentItem>) {
        carouselItems = items
        val carouselAdapter = CarouselAdapter(items) { item ->
            onContentClicked(item)
        }
        carouselViewPager.adapter = carouselAdapter

        carouselIndicator.removeAllViews()
        items.take(10).forEachIndexed { index, _ ->
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(8.dp(), 8.dp()).apply {
                    marginEnd = 4.dp()
                }
                setBackgroundColor(if (index == 0) "#f472a1".toColorInt() else "#666666".toColorInt())
            }
            carouselIndicator.addView(dot)
        }

        updateCarouselBackground(0)

        carouselViewPager.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return

                val centerX = recyclerView.width / 2
                val centerChild = recyclerView.findChildViewUnder(centerX.toFloat(), recyclerView.height / 2f)

                if (centerChild != null) {
                    val newPosition = recyclerView.getChildAdapterPosition(centerChild)
                    if (newPosition != RecyclerView.NO_POSITION && newPosition != currentCarouselPosition) {
                        val clampedPosition = newPosition.coerceIn(0, minOf(carouselItems.size, 10) - 1)

                        currentCarouselPosition = clampedPosition
                        updateCarouselIndicator()
                        updateCarouselBackground(clampedPosition)
                    }
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                    layoutManager?.let {
                        val newPosition = it.findFirstCompletelyVisibleItemPosition()
                        if (newPosition != RecyclerView.NO_POSITION && newPosition != currentCarouselPosition) {
                            currentCarouselPosition = newPosition
                            updateCarouselIndicator()
                            updateCarouselBackground(newPosition)
                            resetCarouselTimer()
                        }
                    }
                }
            }
        })

        startCarouselTimer()
    }

    private fun updateCarouselBackground(position: Int) {
        if (position < 0 || position >= carouselItems.size) return
        val item = carouselItems[position]
        val backdropUrl = item.backdropUrl ?: item.posterUrl

        if (!backdropUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(backdropUrl)
                .transition(DrawableTransitionOptions.withCrossFade(300))
                .centerCrop()
                .into(carouselBackground)
        }
    }

    private fun startCarouselTimer() {
        carouselTimer?.cancel()
        carouselTimer = Timer()
        carouselTimer?.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    currentCarouselPosition = (currentCarouselPosition + 1) % minOf(carouselItems.size, 10)
                    carouselViewPager.smoothScrollToPosition(currentCarouselPosition)
                    updateCarouselIndicator()
                    updateCarouselBackground(currentCarouselPosition)
                }
            }
        }, 5000, 5000)
    }

    private fun resetCarouselTimer() {
        startCarouselTimer()
    }

    private fun updateCarouselIndicator() {
        for (i in 0 until carouselIndicator.childCount) {
            val dot = carouselIndicator.getChildAt(i)
            dot.setBackgroundColor(if (i == currentCarouselPosition) "#f472a1".toColorInt() else "#666666".toColorInt())
        }
    }

    private fun setupHorizontalSection(recyclerView: RecyclerView, items: List<ContentItem>) {
        val adapter = HorizontalContentAdapter(items) { item ->
            onContentClicked(item)
        }
        recyclerView.adapter = adapter
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    isPlayerVisible -> closePlayer()
                    isSearchOverlayVisible -> hideSearchOverlay()
                    isDetailsVisible -> hideDetailsDialog()
                    else -> finish()
                }
            }
        })
    }

    private fun onContentClicked(item: ContentItem) {
        if (isSearchOverlayVisible) {
            searchOverlay?.visibility = View.GONE
            searchOverlay?.alpha = 1f
            searchOverlay?.translationY = 0f
            isSearchOverlayVisible = false
            currentNavPage = 0
            updateNavSelection()
        }

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchOverlayEditText.windowToken, 0)
        searchOverlayEditText.clearFocus()

        currentContent = item
        showContentDetails(item)
    }

    // ========== CONTENT DETAILS DIALOG ==========
    @SuppressLint("SetTextI18n")
    private fun showContentDetails(item: ContentItem) {
        isDetailsVisible = true

        detailsDialogView = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val scrollView = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.TRANSPARENT)
        }

        val contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val backdropContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                250.dp()
            )
        }

        val backdropImageView = AppCompatImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor("#1A1A1A".toColorInt())
        }
        backdropContainer.addView(backdropImageView)

        val gradientView = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                80.dp()
            ).apply {
                gravity = android.view.Gravity.BOTTOM
            }
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.TRANSPARENT, Color.BLACK)
            )
        }
        backdropContainer.addView(gradientView)
        contentContainer.addView(backdropContainer)

        val infoSection = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16.dp(), 16.dp(), 16.dp(), 16.dp())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val posterImageView = AppCompatImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(100.dp(), 150.dp()).apply {
                marginEnd = 16.dp()
            }
            setBackgroundColor("#2A2A2A".toColorInt())
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, 12.dp().toFloat())
                }
            }
        }
        infoSection.addView(posterImageView)

        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleTextView = TextView(this).apply {
            text = item.name
            setTextColor(Color.WHITE)
            textSize = 20f
            setLines(2)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        textContainer.addView(titleTextView)

        val typeTextView = TextView(this).apply {
            text = if (item.type == "tv") "TV Series" else "Movie"
            setTextColor(Color.WHITE)
            textSize = 11f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 4.dp(), 8.dp(), 4.dp())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 6.dp().toFloat()
                setColor("#CC000000".toColorInt())
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 6.dp()
            }
        }
        textContainer.addView(typeTextView)

        val ratingTextView = TextView(this).apply {
            text = "Loading..."
            setTextColor("#888888".toColorInt())
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 4.dp()
            }
        }
        textContainer.addView(ratingTextView)

        val metaTextView = TextView(this).apply {
            text = ""
            setTextColor("#888888".toColorInt())
            textSize = 12f
            setPadding(0, 4.dp(), 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        textContainer.addView(metaTextView)

        infoSection.addView(textContainer)
        contentContainer.addView(infoSection)

        val genresContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16.dp(), 0, 16.dp(), 16.dp())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        contentContainer.addView(genresContainer)

        val taglineTextView = TextView(this).apply {
            setTextColor("#AAAAAA".toColorInt())
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.ITALIC)
            setPadding(16.dp(), 0, 16.dp(), 8.dp())
            visibility = View.GONE
        }
        contentContainer.addView(taglineTextView)

        val overviewTitle = TextView(this).apply {
            text = "Overview"
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(16.dp(), 16.dp(), 16.dp(), 8.dp())
        }
        contentContainer.addView(overviewTitle)

        val overviewTextView = TextView(this).apply {
            text = "Loading description..."
            setTextColor("#CCCCCC".toColorInt())
            textSize = 14f
            setPadding(16.dp(), 0, 16.dp(), 16.dp())
            lineHeight = 24.dp()
        }
        contentContainer.addView(overviewTextView)

        val additionalInfoContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(), 8.dp(), 16.dp(), 16.dp())
        }

        val statusTextView = TextView(this).apply {
            setTextColor("#888888".toColorInt())
            textSize = 12f
            visibility = View.GONE
        }
        additionalInfoContainer.addView(statusTextView)
        contentContainer.addView(additionalInfoContainer)

        val watchButton = MaterialCardView(this).apply {
            radius = 24.dp().toFloat()
            setCardBackgroundColor("#f472a1".toColorInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                48.dp()
            ).apply {
                setMargins(16.dp(), 16.dp(), 16.dp(), 80.dp())
            }
            useCompatPadding = false
            isClickable = true
            isFocusable = true
        }

        val watchButtonText = TextView(this).apply {
            text = "Watch Now"
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
        }
        watchButton.addView(watchButtonText)
        contentContainer.addView(watchButton)

        scrollView.addView(contentContainer)
        detailsDialogView?.addView(scrollView)

        val closeButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(16.dp(), 16.dp(), 16.dp(), 16.dp())
            layoutParams = FrameLayout.LayoutParams(48.dp(), 48.dp()).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.END
                topMargin = 12.dp() + getStatusBarHeight()
                marginEnd = 12.dp()
            }
            setOnClickListener { hideDetailsDialog() }
        }
        detailsDialogView?.addView(closeButton)

        detailsDialogView?.alpha = 0f
        detailsDialogView?.translationY = rootContainer.height.toFloat()
        rootContainer.addView(detailsDialogView)

        detailsDialogView?.animate()
            ?.translationY(0f)
            ?.alpha(1f)
            ?.setDuration(300)
            ?.setInterpolator(DecelerateInterpolator(1.5f))
            ?.start()

        var startY = 0f
        var isDragging = false
        val touchListener = View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.rawY - startY
                    if (deltaY > 10) {
                        isDragging = true
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                        detailsDialogView?.translationY = deltaY
                        val alphaProgress = ((deltaY - 300f) / 400f).coerceIn(0f, 1f)
                        detailsDialogView?.alpha = 1f - alphaProgress
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        val deltaY = event.rawY - startY
                        if (deltaY > 250) {
                            hideDetailsDialog()
                        } else {
                            detailsDialogView?.animate()
                                ?.translationY(0f)
                                ?.alpha(1f)
                                ?.setDuration(200)
                                ?.setInterpolator(DecelerateInterpolator())
                                ?.start()
                        }
                    }
                    isDragging = false
                    true
                }
                else -> false
            }
        }
        backdropContainer.setOnTouchListener(touchListener)
        detailsDialogView?.setOnTouchListener { v, event ->
            val touchY = event.rawY
            val sbHeight = getStatusBarHeight()
            val isNearTop = touchY < sbHeight + 100

            if (isNearTop) {
                touchListener.onTouch(v, event)
            } else {
                false
            }
        }

        lifecycleScope.launch {
            val details = viewModel.getContentDetails(item)
            details?.let { d ->
                currentContentDetails = d

                if (!d.backdropUrl.isNullOrEmpty()) {
                    Glide.with(this@MainActivity)
                        .load(d.backdropUrl)
                        .centerCrop()
                        .into(backdropImageView)
                } else if (!d.posterUrl.isNullOrEmpty()) {
                    Glide.with(this@MainActivity)
                        .load(d.posterUrl)
                        .centerCrop()
                        .into(backdropImageView)
                }

                if (!d.posterUrl.isNullOrEmpty()) {
                    Glide.with(this@MainActivity)
                        .load(d.posterUrl)
                        .centerCrop()
                        .into(posterImageView)
                }

                titleTextView.text = d.title
                typeTextView.text = if (d.type == "tv") "TV Series" else "Movie"

                val ratingText = if (d.voteAverage > 0) {
                    "★ %.1f".format(d.voteAverage)
                } else {
                    "No rating"
                }
                ratingTextView.text = ratingText
                when {
                    d.voteAverage >= 7 -> ratingTextView.setTextColor("#4CAF50".toColorInt())
                    d.voteAverage >= 5 -> ratingTextView.setTextColor("#FFC107".toColorInt())
                }

                val metaParts = mutableListOf<String>()
                if (d.releaseDate.isNotEmpty()) {
                    metaParts.add(d.releaseDate.take(4))
                }
                if (d.runtime > 0) {
                    val hours = d.runtime / 60
                    val mins = d.runtime % 60
                    metaParts.add("${hours}h ${mins}m")
                }
                if (d.type == "tv" && d.numberOfSeasons > 0) {
                    metaParts.add("${d.numberOfSeasons} Season${if (d.numberOfSeasons > 1) "s" else ""}")
                }
                metaTextView.text = metaParts.joinToString(" • ")

                genresContainer.removeAllViews()
                d.genres.take(4).forEach { genre ->
                    val chip = TextView(this@MainActivity).apply {
                        text = genre
                        setTextColor("#f472a1".toColorInt())
                        textSize = 12f
                        setPadding(12.dp(), 6.dp(), 12.dp(), 6.dp())
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = 12.dp().toFloat()
                            setColor("#1A1A1A".toColorInt())
                            setStroke(1.dp(), "#f472a1".toColorInt())
                        }
                    }
                    genresContainer.addView(chip)
                    if (genre != d.genres.take(4).last()) {
                        val spacer = View(this@MainActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(8.dp(), 1.dp())
                        }
                        genresContainer.addView(spacer)
                    }
                }

                if (d.tagline.isNotEmpty()) {
                    taglineTextView.text = "\"${d.tagline}\""
                    taglineTextView.visibility = View.VISIBLE
                }

                overviewTextView.text = d.overview.ifEmpty { "No description available." }

                if (d.status.isNotEmpty()) {
                    statusTextView.text = "Status: ${d.status}"
                    statusTextView.visibility = View.VISIBLE
                }
            }
        }

        watchButton.setOnClickListener {
            if (item.type == "tv") {
                showEpisodeSelectorFromDetails(item)
            } else {
                hideDetailsDialog()
                currentSeason = 1
                currentEpisode = 1
                totalSeasons = 1
                totalEpisodes = 1
                playContent(item)
            }
        }
    }

    private fun showEpisodeSelectorFromDetails(item: ContentItem) {
        lifecycleScope.launch {
            val seasonData = viewModel.getSeasonInfoWithEpisodes(item.id)

            if (seasonData.isEmpty()) {
                Toast.makeText(this@MainActivity, "Could not load episode info", Toast.LENGTH_SHORT).show()
                return@launch
            }

            seasonEpisodeCounts = seasonData
            totalSeasons = seasonData.keys.maxOrNull() ?: 1
            showSeasonEpisodeDialog(item, seasonData)
        }
    }

    private fun hideDetailsDialog() {
        isDetailsVisible = false
        detailsDialogView?.let { dialogView ->
            dialogView.animate()
                .translationY(rootContainer.height.toFloat())
                .alpha(0f)
                .setDuration(250)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        rootContainer.removeView(dialogView)
                        detailsDialogView = null
                    }
                })
                .start()
        } ?: run {
            detailsDialogView = null
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showSeasonEpisodeDialog(item: ContentItem, seasonData: Map<Int, Int>) {
        val dialogContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#CC000000"))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val dialogCard = MaterialCardView(this).apply {
            radius = 16.dp().toFloat()
            setCardBackgroundColor("#1A1A1A".toColorInt())
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
                marginEnd = 24.dp()
                marginStart = 24.dp()
            }
            cardElevation = 8.dp().toFloat()
        }

        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp(), 24.dp(), 24.dp(), 24.dp())
        }

        var selectedSeason = 1
        var selectedEpisode = 1
        val seasonItems = mutableMapOf<Int, MaterialCardView>()

        val episodeGridLayout = GridLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleText = TextView(this).apply {
            text = item.name
            setTextColor(Color.WHITE)
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16.dp())
        }
        dialogView.addView(titleText)

        val seasonLabel = TextView(this).apply {
            text = "Season"
            setTextColor(Color.WHITE)
            textSize = 16f
        }
        dialogView.addView(seasonLabel)

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
                setCardBackgroundColor(if (index == 0) "#f472a1".toColorInt() else "#1A1A1A".toColorInt())
                layoutParams = LinearLayout.LayoutParams(48.dp(), 48.dp()).apply {
                    marginEnd = 8.dp()
                }
                tag = seasonNum
                alpha = 0f
                translationY = 20f
                setOnClickListener { clickedView ->
                    val newSeason = (clickedView as MaterialCardView).tag as Int
                    if (newSeason != selectedSeason) {
                        selectedSeason = newSeason
                        seasonItems.forEach { (_, card) ->
                            card.setCardBackgroundColor("#1A1A1A".toColorInt())
                        }
                        setCardBackgroundColor("#f472a1".toColorInt())

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
            seasonCard.postDelayed({
                seasonCard.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(200)
                    .setInterpolator(DecelerateInterpolator(1.2f))
                    .start()
            }, index * 50L)
        }
        seasonScrollView.addView(seasonContainer)
        dialogView.addView(seasonScrollView)

        val episodeLabel = TextView(this).apply {
            text = "Episode"
            setTextColor(Color.WHITE)
            textSize = 16f
        }
        dialogView.addView(episodeLabel)

        val episodeScrollView = ScrollView(this).apply {
            isVerticalScrollBarEnabled = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                200.dp()
            )
            setPadding(0, 8.dp(), 0, 8.dp())
        }

        totalEpisodes = seasonData[1] ?: 0
        updateEpisodeSelector(episodeGridLayout, totalEpisodes, selectedEpisode) { ep ->
            selectedEpisode = ep
        }
        episodeScrollView.addView(episodeGridLayout)
        dialogView.addView(episodeScrollView)

        val buttonsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.END
            setPadding(0, 16.dp(), 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val cancelButton = TextView(this).apply {
            text = "Cancel"
            setTextColor("#f472a1".toColorInt())
            textSize = 14f
            setPadding(16.dp(), 12.dp(), 16.dp(), 12.dp())
            setOnClickListener {
                dialogCard.animate()
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .alpha(0f)
                    .setDuration(150)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
                dialogContainer.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            rootContainer.removeView(dialogContainer)
                        }
                    })
                    .start()
            }
        }
        buttonsContainer.addView(cancelButton)

        val playButton = TextView(this).apply {
            text = "Play"
            setTextColor("#f472a1".toColorInt())
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(16.dp(), 12.dp(), 16.dp(), 12.dp())
            setOnClickListener {
                dialogCard.animate()
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .alpha(0f)
                    .setDuration(150)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
                dialogContainer.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            rootContainer.removeView(dialogContainer)
                            currentSeason = selectedSeason
                            currentEpisode = selectedEpisode
                            totalEpisodes = seasonData[selectedSeason] ?: 1
                            hideDetailsDialog()
                            playContent(item)
                        }
                    })
                    .start()
            }
        }
        buttonsContainer.addView(playButton)
        dialogView.addView(buttonsContainer)

        dialogCard.addView(dialogView)
        dialogContainer.addView(dialogCard)

        dialogContainer.setOnClickListener {
            dialogCard.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .alpha(0f)
                .setDuration(150)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
            dialogContainer.animate()
                .alpha(0f)
                .setDuration(150)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        rootContainer.removeView(dialogContainer)
                    }
                })
                .start()
        }

        dialogCard.setOnClickListener { /* consume click */ }

        dialogContainer.alpha = 0f
        dialogCard.scaleX = 0.9f
        dialogCard.scaleY = 0.9f
        dialogCard.alpha = 0f
        rootContainer.addView(dialogContainer)

        dialogContainer.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
        dialogCard.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(250)
            .setInterpolator(OvershootInterpolator(1.1f))
            .start()
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
                setTextColor("#888888".toColorInt())
                textSize = 14f
                setPadding(0, 8.dp(), 0, 8.dp())
            }
            container.addView(noEpisodesText)
            return
        }

        container.columnCount = 5

        val episodeCards = mutableMapOf<Int, MaterialCardView>()

        for (i in 1..episodeCount) {
            val episodeCard = MaterialCardView(this).apply {
                radius = 8.dp().toFloat()
                setCardBackgroundColor(if (i == currentSelection) "#f472a1".toColorInt() else "#1A1A1A".toColorInt())
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 48.dp()
                    height = 48.dp()
                    setMargins(0, 0, 8.dp(), 8.dp())
                }
                alpha = 0f
                scaleX = 0.8f
                scaleY = 0.8f
                setOnClickListener {
                    onSelect(i)
                    episodeCards.forEach { (_, card) ->
                        card.setCardBackgroundColor("#1A1A1A".toColorInt())
                    }
                    setCardBackgroundColor("#f472a1".toColorInt())
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
            episodeCard.postDelayed({
                episodeCard.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .setInterpolator(OvershootInterpolator(1.2f))
                    .start()
            }, (i - 1) * 20L)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun playContent(item: ContentItem) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchOverlayEditText.windowToken, 0)
        searchOverlayEditText.clearFocus()

        isPlayerVisible = true
        playerContainer.visibility = View.VISIBLE
        bottomNav.visibility = View.GONE
        exploreContainer.visibility = View.GONE

        currentContent = item
        playerLoadingIndicator.visibility = View.VISIBLE

        if (item.type == "movie") {
            currentSeason = 1
            currentEpisode = 1
        }
        if (seasonEpisodeCounts.isEmpty()) {
            totalSeasons = 1
            totalEpisodes = 1
        }

        updateContentTitle()

        val vidlinkUrl = if (item.type == "tv") {
            "https://vidlink.pro/tv/${item.id}/$currentSeason/$currentEpisode"
        } else {
            "https://vidlink.pro/movie/${item.id}"
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR

        if (item.type == "tv") {
            lifecycleScope.launch {
                val seasonData = viewModel.getSeasonInfoWithEpisodes(item.id)
                if (seasonData.isNotEmpty()) {
                    seasonEpisodeCounts = seasonData
                    totalSeasons = seasonData.keys.maxOrNull() ?: 1
                    totalEpisodes = seasonData[currentSeason] ?: 1
                    runOnUiThread {
                        updatePlayerControlsVisibility()
                    }
                }
            }
        } else {
            updatePlayerControlsVisibility()
        }

        scrapeM3u8AndPlay(vidlinkUrl)
        hideSystemUI()
    }

    @SuppressLint("SetTextI18n")
    private fun continuePlayback() {
        currentContent?.let { item ->
            val vidlinkUrl = if (item.type == "tv") {
                "https://vidlink.pro/tv/${item.id}/$currentSeason/$currentEpisode"
            } else {
                "https://vidlink.pro/movie/${item.id}"
            }

            playerLoadingIndicator.visibility = View.VISIBLE
            exoPlayer.stop()
            updateContentTitle()
            updatePlayerControlsVisibility()
            scrapeM3u8AndPlay(vidlinkUrl)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun scrapeM3u8AndPlay(url: String) {
        foundM3u8Url = null
        scraperLock.set(false)

        scraperWebView.settings.javaScriptEnabled = true
        scraperWebView.settings.domStorageEnabled = true
        scraperWebView.settings.loadsImagesAutomatically = false

        lifecycleScope.launch {
            delay(30000)
            if (foundM3u8Url == null && scraperLock.compareAndSet(false, true)) {
                runOnUiThread {
                    playerLoadingIndicator.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Failed to load stream", Toast.LENGTH_SHORT).show()
                    closePlayer()
                }
            }
        }

        scraperWebView.loadUrl(url)
    }

    @OptIn(UnstableApi::class)
    private fun playM3u8WithExoPlayer(m3u8Url: String) {
        runOnUiThread {
            playerLoadingIndicator.visibility = View.GONE

            val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
                setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                setDefaultRequestProperties(mapOf(
                    "Accept" to "*/*",
                    "Accept-Language" to "en-US,en;q=0.9",
                    "Origin" to "https://vidlink.pro",
                    "Referer" to "https://vidlink.pro/"
                ))
            }

            val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(m3u8Url))

            exoPlayer.setMediaSource(hlsMediaSource)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
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
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        playerContainer.visibility = View.GONE
        bottomNav.visibility = View.VISIBLE
        exploreContainer.visibility = View.VISIBLE

        exoPlayer.stop()
        playerView.player = null
        playerView.player = exoPlayer

        scraperWebView.loadUrl("about:blank")
        showSystemUI()
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    inner class M3u8ScraperWebViewClient : WebViewClient() {
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)

            for (adDomain in adDomains) {
                if (url.contains(adDomain, ignoreCase = true)) {
                    return WebResourceResponse("text/plain", "utf-8", java.io.ByteArrayInputStream("".toByteArray()))
                }
            }

            if (url.contains(".m3u8", ignoreCase = true)) {
                if (url.contains("MTA4MA", ignoreCase = true) ||
                    url.contains("1080", ignoreCase = true) ||
                    url.contains("master", ignoreCase = true)) {

                    if (foundM3u8Url == null && scraperLock.compareAndSet(false, true)) {
                        foundM3u8Url = url
                        playM3u8WithExoPlayer(url)
                    }
                }
            }

            return super.shouldInterceptRequest(view, request)
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url?.toString() ?: return false

            for (adDomain in adDomains) {
                if (url.contains(adDomain, ignoreCase = true)) {
                    return true
                }
            }

            return false
        }
    }

    class ContentDiffCallback : DiffUtil.ItemCallback<ContentItem>() {
        override fun areItemsTheSame(oldItem: ContentItem, newItem: ContentItem): Boolean {
            return oldItem.id == newItem.id && oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: ContentItem, newItem: ContentItem): Boolean {
            return oldItem == newItem
        }
    }

    inner class SearchOverlayAdapter(private val onItemClick: (ContentItem) -> Unit) :
        ListAdapter<ContentItem, SearchOverlayViewHolder>(ContentDiffCallback()) {

        @SuppressLint("SetTextI18n")
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): SearchOverlayViewHolder {
            val card = MaterialCardView(parent.context).apply {
                radius = 12.dp().toFloat()
                setCardBackgroundColor("#1A1A1A".toColorInt())
                cardElevation = 0f
                rippleColor = ColorStateList.valueOf("#33f472a1".toColorInt())
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                )
            }

            val container = LinearLayout(parent.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(10.dp(), 10.dp(), 10.dp(), 10.dp())
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val poster = AppCompatImageView(parent.context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = LinearLayout.LayoutParams(50.dp(), 70.dp())
                setBackgroundColor("#2A2A2A".toColorInt())
                outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, 8.dp().toFloat())
                    }
                }
                clipToOutline = true
            }

            container.addView(poster)

            val infoContainer = LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(10.dp(), 0.dp(), 8.dp(), 0.dp())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val title = TextView(parent.context).apply {
                setTextColor(Color.WHITE)
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setLines(2)
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            infoContainer.addView(title)

            val ratingBadge = TextView(parent.context).apply {
                setTextColor("#FFD700".toColorInt())
                textSize = 10f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(6.dp(), 3.dp(), 6.dp(), 3.dp())
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 6.dp().toFloat()
                    setColor("#CC000000".toColorInt())
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 4.dp()
                }
            }
            infoContainer.addView(ratingBadge)

            val genres = TextView(parent.context).apply {
                setTextColor("#888888".toColorInt())
                textSize = 11f
                setLines(1)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 2.dp()
                }
            }
            infoContainer.addView(genres)

            val typeBadge = TextView(parent.context).apply {
                setTextColor("#f472a1".toColorInt())
                textSize = 10f
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 2.dp()
                }
            }
            infoContainer.addView(typeBadge)

            container.addView(infoContainer)

            val arrow = TextView(parent.context).apply {
                text = "›"
                setTextColor("#f472a1".toColorInt())
                textSize = 20f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            container.addView(arrow)

            card.addView(container)

            return SearchOverlayViewHolder(card, poster, title, ratingBadge, genres, typeBadge)
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: SearchOverlayViewHolder, position: Int) {
            val item = getItem(position)

            holder.title.text = item.name

            if (item.voteAverage > 0) {
                holder.ratingBadge.text = "★ ${String.format(java.util.Locale.US, "%.1f", item.voteAverage)}"
                holder.ratingBadge.visibility = View.VISIBLE
            } else {
                holder.ratingBadge.visibility = View.GONE
            }

            val genreNames = MainViewModel.getGenreNames(item.genreIds, item.type)
            if (genreNames.isNotEmpty()) {
                holder.genres.text = genreNames.joinToString(", ")
                holder.genres.visibility = View.VISIBLE
            } else {
                holder.genres.visibility = View.GONE
            }

            holder.typeBadge.text = if (item.type == "tv") "TV Show" else "Movie"

            holder.itemView.setOnClickListener { onItemClick(item) }

            if (!item.posterUrl.isNullOrEmpty()) {
                Glide.with(holder.poster.context)
                    .load(item.posterUrl)
                    .transition(DrawableTransitionOptions.withCrossFade(150))
                    .centerCrop()
                    .into(holder.poster)
            } else {
                holder.poster.setImageDrawable(null) // Clear if no URL
            }
        }
    }

    class SearchOverlayViewHolder(
        itemView: View,
        val poster: AppCompatImageView,
        val title: TextView,
        val ratingBadge: TextView,
        val genres: TextView,
        val typeBadge: TextView
    ) : RecyclerView.ViewHolder(itemView)

    inner class HorizontalContentAdapter(
        private val items: List<ContentItem>,
        private val onItemClick: (ContentItem) -> Unit
    ) : RecyclerView.Adapter<HorizontalContentViewHolder>() {

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): HorizontalContentViewHolder {
            val outerContainer = LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = RecyclerView.LayoutParams(130.dp(), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = 12.dp()
                }
            }

            val card = MaterialCardView(parent.context).apply {
                radius = 12.dp().toFloat()
                setCardBackgroundColor("#1A1A1A".toColorInt())
                cardElevation = 0f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    180.dp()
                )
            }

            val container = FrameLayout(parent.context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }

            val poster = AppCompatImageView(parent.context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor("#2A2A2A".toColorInt())
            }
            container.addView(poster)

            val ratingBadge = TextView(parent.context).apply {
                setTextColor("#FFD700".toColorInt())
                textSize = 10f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(8.dp(), 4.dp(), 8.dp(), 4.dp())
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 8.dp().toFloat()
                    setColor("#CC000000".toColorInt())
                }
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = android.view.Gravity.TOP or android.view.Gravity.END
                    topMargin = 6.dp()
                    marginEnd = 6.dp()
                }
                visibility = View.GONE
            }
            container.addView(ratingBadge)

            val typeBadge = TextView(parent.context).apply {
                setTextColor(Color.WHITE)
                textSize = 9f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(8.dp(), 4.dp(), 8.dp(), 4.dp())
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 8.dp().toFloat()
                    setColor("#CC000000".toColorInt())
                }
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = android.view.Gravity.TOP or android.view.Gravity.START
                    topMargin = 6.dp()
                    marginStart = 6.dp()
                }
            }
            container.addView(typeBadge)

            card.addView(container)
            outerContainer.addView(card)

            val titleText = TextView(parent.context).apply {
                setTextColor(Color.WHITE)
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setLines(2)
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, 6.dp(), 0, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            outerContainer.addView(titleText)

            return HorizontalContentViewHolder(outerContainer, poster, titleText, ratingBadge, typeBadge)
        }

        override fun onBindViewHolder(holder: HorizontalContentViewHolder, position: Int) {
            val item = items[position]
            holder.titleText.text = item.name

            holder.typeBadge.text = if (item.type == "tv") "TV" else "Movie"
            holder.typeBadge.visibility = View.VISIBLE

            if (item.voteAverage > 0) {
                holder.ratingBadge.text = "★ ${String.format(java.util.Locale.US, "%.1f", item.voteAverage)}"
                holder.ratingBadge.visibility = View.VISIBLE
            } else {
                holder.ratingBadge.visibility = View.GONE
            }

            if (!item.posterUrl.isNullOrEmpty()) {
                Glide.with(holder.poster.context)
                    .load(item.posterUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(holder.poster)
            } else {
                holder.poster.setImageDrawable(null)
            }

            holder.outerContainer.setOnClickListener { onItemClick(item) }
        }

        override fun getItemCount() = items.size
    }

    inner class CarouselAdapter(
        private val items: List<ContentItem>,
        private val onItemClick: (ContentItem) -> Unit
    ) : RecyclerView.Adapter<CarouselViewHolder>() {

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): CarouselViewHolder {
            val frameLayout = FrameLayout(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.MATCH_PARENT
                )
            }

            val imageView = AppCompatImageView(parent.context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            frameLayout.addView(imageView)

            val gradientOverlay = View(parent.context).apply {
                setBackgroundColor("#30000000".toColorInt())
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            frameLayout.addView(gradientOverlay)

            val bottomGradient = View(parent.context).apply {
                background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(Color.TRANSPARENT, "#CC000000".toColorInt())
                )
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    120.dp()
                ).apply {
                    gravity = android.view.Gravity.BOTTOM
                }
            }
            frameLayout.addView(bottomGradient)

            val titleContainer = LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = android.view.Gravity.BOTTOM
                }
            }

            val titleText = TextView(parent.context).apply {
                setTextColor(Color.WHITE)
                textSize = 18f
                setLines(2)
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(16.dp(), 12.dp(), 16.dp(), 4.dp())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            titleContainer.addView(titleText)

            val typeBadge = TextView(parent.context).apply {
                setTextColor(Color.WHITE)
                textSize = 10f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(10.dp(), 4.dp(), 10.dp(), 4.dp())
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 8.dp().toFloat()
                    setColor("#CC000000".toColorInt())
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = 16.dp()
                    bottomMargin = 36.dp()
                }
            }
            titleContainer.addView(typeBadge)

            frameLayout.addView(titleContainer)

            return CarouselViewHolder(frameLayout, imageView, titleText, typeBadge)
        }

        override fun onBindViewHolder(holder: CarouselViewHolder, position: Int) {
            val item = items[position]
            holder.titleText.text = item.name

            holder.typeBadge.text = if (item.type == "tv") "TV Series" else "Movie"

            if (!item.backdropUrl.isNullOrEmpty()) {
                Glide.with(holder.imageView.context)
                    .load(item.backdropUrl)
                    .centerCrop()
                    .into(holder.imageView)
            }

            holder.frameLayout.setOnClickListener { onItemClick(item) }
        }

        override fun getItemCount() = items.size
    }

    class CarouselViewHolder(
        val frameLayout: FrameLayout,
        val imageView: AppCompatImageView,
        val titleText: TextView,
        val typeBadge: TextView
    ) : RecyclerView.ViewHolder(frameLayout)

    class HorizontalContentViewHolder(
        val outerContainer: LinearLayout,
        val poster: AppCompatImageView,
        val titleText: TextView,
        val ratingBadge: TextView,
        val typeBadge: TextView
    ) : RecyclerView.ViewHolder(outerContainer)
}