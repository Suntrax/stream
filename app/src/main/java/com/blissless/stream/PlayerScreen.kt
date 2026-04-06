package com.blissless.stream

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind

import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    videoUrl: String,
    currentSeason: Int = 1,
    currentEpisode: Int = 1,
    totalEpisodes: Int = 0,
    maxSeason: Int = 1,
    animeName: String = "",
    episodeLength: Int = 1440,
    isLoadingStream: Boolean = false,
    currentCategory: String = "sub",
    forwardSkipSeconds: Int = 10,
    backwardSkipSeconds: Int = 10,
    autoPlayNextEpisode: Boolean = true,
    savedPosition: Long = 0L,
    onSavePosition: ((position: Long, duration: Long) -> Unit)? = null,
    onPositionSaved: ((Long) -> Unit)? = null,
    onProgressUpdate: (percentage: Int) -> Unit = {},
    onPreviousEpisode: (() -> Unit)? = null,
    onNextEpisode: (() -> Unit)? = null,
    onPlaybackError: (() -> Unit)? = null,
    onClose: () -> Unit,
    isSeries: Boolean = false
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    var hasTriggeredProgressUpdate by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var hasError by remember { mutableStateOf(false) }
    var hasPlaybackStarted by remember { mutableStateOf(false) }

    var resizeModeIndex by remember { mutableIntStateOf(0) }
    val resizeModes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit",
        AspectRatioFrameLayout.RESIZE_MODE_FILL to "Stretch",
        AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH to "16:9"
    )

    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var bufferedPosition by remember { mutableLongStateOf(0L) }
    var maxBufferedPosition by remember { mutableLongStateOf(0L) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    var sliderValue by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var wasPlayingBeforeScrub by remember { mutableStateOf(false) }

    var showSkipIndicator by remember { mutableStateOf(false) }
    var skipIndicatorText by remember { mutableStateOf("") }
    var skipIsForward by remember { mutableStateOf(true) }

    val scope = remember { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main) }
    
    var hasRestoredPosition by remember { mutableStateOf(false) }
    
    var currentSpeed by remember { mutableFloatStateOf(1f) }
    val speedOptions = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

    LaunchedEffect(Unit) {
        activity?.window?.let { window ->
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    val exoPlayer = remember(context) {
        val bufferAheadMs = 30000
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                bufferAheadMs,
                bufferAheadMs + 60000,
                1500,
                3000
            )
            .build()

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(20000)
            .setReadTimeoutMs(20000)
            .setDefaultRequestProperties(mapOf(
                "Accept" to "*/*",
                "Accept-Language" to "en-US,en;q=0.9",
                "Origin" to "https://vidlink.pro",
                "Referer" to "https://vidlink.pro/"
            ))
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory)
            )
            .setLoadControl(loadControl)
            .build()
            .also { player ->
                player.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                        if (playing) {
                            hasPlaybackStarted = true
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        isBuffering = playbackState == Player.STATE_BUFFERING
                        if (playbackState == Player.STATE_READY) {
                            hasError = false
                            playbackError = null
                            isBuffering = false
                        }
                        if (playbackState == Player.STATE_ENDED) {
                            if (autoPlayNextEpisode && onNextEpisode != null) {
                                onNextEpisode.invoke()
                            }
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        hasError = true
                        playbackError = error.message ?: "Unknown playback error"
                        showControls = true
                    }
                })
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            if (dur > 0 && pos > 0) {
                onSavePosition?.invoke(pos, dur)
            }
            activity?.window?.let { window ->
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_STOP -> {
                    exoPlayer.pause()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(videoUrl) {
        hasError = false
        playbackError = null
        hasRestoredPosition = false
        isBuffering = true
        hasPlaybackStarted = false
        bufferedPosition = 0L
        maxBufferedPosition = 0L

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(videoUrl)
            .setMimeType(MimeTypes.APPLICATION_M3U8)

        exoPlayer.setMediaItem(mediaItemBuilder.build())
        exoPlayer.prepare()
        exoPlayer.playWhenReady = savedPosition == 0L

        hasTriggeredProgressUpdate = false
        currentPosition = 0L
        sliderValue = 0f
    }

    LaunchedEffect(exoPlayer.playbackState, savedPosition, hasRestoredPosition) {
        if (exoPlayer.playbackState == Player.STATE_READY && savedPosition > 0 && !hasRestoredPosition) {
            exoPlayer.seekTo(savedPosition)
            hasRestoredPosition = true
            exoPlayer.playWhenReady = true
        }
    }

    fun seekBy(milliseconds: Long, isForward: Boolean) {
        val newPosition = (exoPlayer.currentPosition + milliseconds).coerceIn(0, exoPlayer.duration)
        exoPlayer.seekTo(newPosition)
        currentPosition = newPosition
        sliderValue = newPosition.toFloat()
        val seconds = abs(milliseconds / 1000)
        skipIndicatorText = if (milliseconds > 0) "+${seconds}s" else "-${seconds}s"
        skipIsForward = isForward
        showSkipIndicator = true
        scope.launch {
            delay(500)
            showSkipIndicator = false
        }
    }

    LaunchedEffect(exoPlayer, videoUrl) {
        while (true) {
            delay(500)
            if (!isDragging) {
                currentPosition = exoPlayer.currentPosition
                duration = exoPlayer.duration
                bufferedPosition = exoPlayer.bufferedPosition
                if (bufferedPosition > maxBufferedPosition) {
                    maxBufferedPosition = bufferedPosition
                }
                if (duration > 0) {
                    sliderValue = currentPosition.toFloat()
                }
            }
        }
    }

    LaunchedEffect(showControls, isPlaying, isDragging, hasError, showSpeedMenu) {
        if (showControls && isPlaying && !isDragging && !hasError && !showSpeedMenu) {
            delay(3000)
            if (!isDragging && !hasError && isPlaying && !showSpeedMenu) {
                showControls = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { 
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            if (dur > 0) {
                onSavePosition?.invoke(pos, dur)
            }
            exoPlayer.release() 
        }
    }

    fun formatTime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = ms / (1000 * 60 * 60)
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    resizeMode = resizeModes[resizeModeIndex].first
                    useController = false
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    controllerShowTimeoutMs = 3000
                    controllerAutoShow = false

                    val style = CaptionStyleCompat(
                        android.graphics.Color.WHITE,
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                        CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                        android.graphics.Color.BLACK,
                        null
                    )
                    subtitleView?.apply {
                        setStyle(style)
                        setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 22f)
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (hasPlaybackStarted) 1f else 0f),
            update = { view -> 
                view.resizeMode = resizeModes[resizeModeIndex].first
            }
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.3f)
                .padding(start = 40.dp)
                .align(Alignment.CenterStart)
                .pointerInput(backwardSkipSeconds) {
                    detectTapGestures(
                        onTap = { if (!hasError) showControls = !showControls },
                        onDoubleTap = { if (!hasError) seekBy(-(backwardSkipSeconds * 1000L), false) }
                    )
                }
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.4f)
                .align(Alignment.Center)
                .pointerInput(Unit) { detectTapGestures(onTap = { if (!hasError) showControls = !showControls }) }
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.3f)
                .padding(end = 40.dp)
                .align(Alignment.CenterEnd)
                .pointerInput(forwardSkipSeconds) {
                    detectTapGestures(
                        onTap = { if (!hasError) showControls = !showControls },
                        onDoubleTap = { if (!hasError) seekBy(forwardSkipSeconds * 1000L, true) }
                    )
                }
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(40.dp)
                .align(Alignment.CenterStart)
                .pointerInput(Unit) { detectTapGestures(onTap = {}) }
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(40.dp)
                .align(Alignment.CenterEnd)
                .pointerInput(Unit) { detectTapGestures(onTap = {}) }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .align(Alignment.TopCenter)
                .pointerInput(Unit) { detectTapGestures(onTap = {}) }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .align(Alignment.BottomCenter)
                .pointerInput(Unit) { detectTapGestures(onTap = {}) }
        )

        AnimatedVisibility(
            visible = showControls || hasError || showSkipIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                        .statusBarsPadding()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Column {
                                Text(
                                    text = animeName,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isSeries) {
                                    Text(
                                        text = "S$currentSeason:E$currentEpisode${if (totalEpisodes > 0) " / $totalEpisodes" else ""}",
                                        color = Color.White.copy(alpha = 0.8f),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
                                IconButton(
                                    onClick = { showSpeedMenu = true },
                                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                                ) {
                                    Icon(Icons.Default.Speed, "Speed", tint = Color.White)
                                }

                                DropdownMenu(
                                    expanded = showSpeedMenu,
                                    onDismissRequest = { showSpeedMenu = false },
                                    modifier = Modifier.background(Color(0xFF1A1A1A))
                                ) {
                                    speedOptions.forEach { speed ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${speed}x",
                                                    color = if (currentSpeed == speed) MaterialTheme.colorScheme.primary else Color.White
                                                )
                                            },
                                            onClick = {
                                                currentSpeed = speed
                                                exoPlayer.setPlaybackSpeed(speed)
                                                showSpeedMenu = false
                                            },
                                            leadingIcon = if (currentSpeed == speed) {
                                                { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
                                            } else null
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
                                IconButton(
                                    onClick = { resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size },
                                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                                ) {
                                    Icon(Icons.Default.AspectRatio, "Aspect Ratio", tint = Color.White)
                                }
                            }

                            Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
                                IconButton(
                                    onClick = { onClose() },
                                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close", tint = Color.White)
                                }
                            }
                        }
                    }
                }

                Box(modifier = Modifier.align(Alignment.Center)) {
                    val canGoPrevious = isSeries && (currentSeason > 1 || currentEpisode > 1)
                    val canGoNext = isSeries && (currentSeason < maxSeason || currentEpisode < totalEpisodes)

                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (canGoPrevious) onPreviousEpisode?.invoke() },
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .alpha(if (canGoPrevious && !isLoadingStream) 1f else 0.3f),
                            enabled = canGoPrevious && !isLoadingStream
                        ) {
                            Icon(Icons.Default.SkipPrevious, "Previous Episode", tint = Color.White, modifier = Modifier.size(32.dp))
                        }

                        IconButton(
                            onClick = {
                                if (hasError) {
                                    onPlaybackError?.invoke()
                                    hasError = false
                                    playbackError = null
                                    exoPlayer.prepare()
                                    exoPlayer.playWhenReady = true
                                } else {
                                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                }
                            },
                            modifier = Modifier.size(72.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            if (isBuffering) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(42.dp),
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (hasError) Icons.Default.Refresh else if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (hasError) "Retry" else if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { if (canGoNext) onNextEpisode?.invoke() },
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .alpha(if (canGoNext && !isLoadingStream) 1f else 0.3f),
                            enabled = canGoNext && !isLoadingStream
                        ) {
                            Icon(Icons.Default.SkipNext, "Next Episode", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }

                    AnimatedVisibility(
                        visible = showSkipIndicator && !skipIsForward,
                        enter = fadeIn() + scaleIn(initialScale = 0.8f),
                        exit = fadeOut() + scaleOut(targetScale = 0.8f),
                        modifier = Modifier.align(Alignment.CenterStart).offset(x = (-120).dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(56.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FastRewind, "Rewind", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(skipIndicatorText, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    AnimatedVisibility(
                        visible = showSkipIndicator && skipIsForward,
                        enter = fadeIn() + scaleIn(initialScale = 0.8f),
                        exit = fadeOut() + scaleOut(targetScale = 0.8f),
                        modifier = Modifier.align(Alignment.CenterEnd).offset(x = 120.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(56.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FastForward, "Forward", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(skipIndicatorText, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                    }

                if (hasError && playbackError != null) {
                    Card(
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Error, null, tint = Color.Red, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Stream Error", color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Text(playbackError ?: "Unknown error", color = Color.Gray, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    onPlaybackError?.invoke()
                                    hasError = false
                                    playbackError = null
                                    exoPlayer.prepare()
                                    exoPlayer.playWhenReady = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragStart = { offset ->
                                        isDragging = true
                                        wasPlayingBeforeScrub = isPlaying
                                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                                        sliderValue = ratio * (if (duration > 0) duration.toFloat() else 1000f)
                                        currentPosition = sliderValue.toLong()
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        exoPlayer.seekTo(sliderValue.toLong())
                                        if (wasPlayingBeforeScrub) {
                                            exoPlayer.play()
                                        }
                                    },
                                    onHorizontalDrag = { _, dragAmount ->
                                        val currentRatio = sliderValue / (if (duration > 0) duration.toFloat() else 1000f)
                                        val newRatio = (currentRatio + dragAmount / size.width).coerceIn(0f, 1f)
                                        sliderValue = newRatio * (if (duration > 0) duration.toFloat() else 1000f)
                                        currentPosition = sliderValue.toLong()
                                    }
                                )
                            }
                            .pointerInput(duration) {
                                detectTapGestures { offset ->
                                    if (duration > 0) {
                                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                                        val targetPosition = (ratio * duration).toLong()
                                        exoPlayer.seekTo(targetPosition)
                                        currentPosition = targetPosition
                                        sliderValue = targetPosition.toFloat()
                                    }
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val sliderWidth = size.width
                            val trackHeight = 8.dp.toPx()
                            val trackTop = (size.height - trackHeight) / 2f
                            val cornerRadius = 4.dp.toPx()
                            val thumbRadiusPx = 8.dp.toPx()

                            if (duration > 0) {
                                val progressRatio = currentPosition.toFloat() / duration
                                val bufferedRatio = maxBufferedPosition.toFloat() / duration

                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.3f),
                                    topLeft = Offset(0f, trackTop),
                                    size = Size(sliderWidth, trackHeight),
                                    cornerRadius = CornerRadius(cornerRadius)
                                )

                                if (maxBufferedPosition > currentPosition) {
                                    val bufferStartX = progressRatio * sliderWidth
                                    val bufferEndX = bufferedRatio * sliderWidth
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = 0.5f),
                                        topLeft = Offset(bufferStartX, trackTop),
                                        size = Size(bufferEndX - bufferStartX, trackHeight),
                                        cornerRadius = CornerRadius(2.dp.toPx())
                                    )
                                }

                                val progressX = progressRatio * sliderWidth
                                drawRoundRect(
                                    color = Color.White,
                                    topLeft = Offset(0f, trackTop),
                                    size = Size(progressX.coerceAtLeast(thumbRadiusPx), trackHeight),
                                    cornerRadius = CornerRadius(cornerRadius)
                                )

                                drawCircle(
                                    color = Color.White,
                                    radius = thumbRadiusPx,
                                    center = Offset(progressX, size.height / 2)
                                )
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(currentPosition), color = Color.White, style = MaterialTheme.typography.labelSmall)
                        Text(if (duration > 0) formatTime(duration) else "--:--", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
