package com.blissless.stream

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blissless.stream.ContentItem
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

@Composable
fun ScheduleScreen(
    onContentClick: (ContentItem) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val isOled = false
    val upcoming by viewModel.upcoming.collectAsState()
    val upcomingTVShows by viewModel.upcomingTVShows.collectAsState()
    val airingToday by viewModel.airingToday.collectAsState()

    val upcomingFiltered = upcoming.sortedBy {
        try { LocalDate.parse(it.releaseDate) } catch (e: Exception) { LocalDate.MAX }
    }
    val upcomingTVFiltered = upcomingTVShows.sortedBy {
        try { LocalDate.parse(it.airDate) } catch (e: Exception) { LocalDate.MAX }
    }

    fun onTVClick(show: AiringShow) {
        onContentClick(
            ContentItem(
                id = show.id,
                name = show.name,
                type = "tv",
                posterUrl = show.posterUrl,
                voteAverage = show.voteAverage
            )
        )
    }

    LaunchedEffect(Unit) {
        viewModel.loadUpcoming()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isOled) Color.Black else Color(0xFF121212))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AndroidView(
                factory = { ctx ->
                    android.widget.ImageView(ctx).apply {
                        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    }
                },
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                update = { imageView ->
                    Glide.with(context)
                        .load(R.mipmap.ic_launcher_round)
                        .circleCrop()
                        .into(imageView)
                }
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Schedule",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        if (upcoming.isEmpty() && upcomingTVShows.isEmpty() && airingToday.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                val today = LocalDate.now()
                val displayDate = today.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))

                item {
                    Text(
                        text = "Today - $displayDate",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (airingToday.isNotEmpty()) {
                    item {
                        AiringSection(
                            shows = airingToday,
                            onShowClick = { show ->
                                onContentClick(
                                    ContentItem(
                                        id = show.id,
                                        name = show.name,
                                        type = "tv",
                                        posterUrl = show.posterUrl,
                                        voteAverage = show.voteAverage
                                    )
                                )
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Coming Soon",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (upcomingTVFiltered.isNotEmpty()) {
                    item {
                        Text(
                            text = "TV Shows",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(upcomingTVFiltered) { item ->
                                UpcomingCard(
                                    item = item,
                                    onClick = { onTVClick(item) },
                                    isTV = true
                                )
                            }
                        }
                    }
                }

                if (upcomingFiltered.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Movies",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(upcomingFiltered) { item ->
                                UpcomingCard(
                                    item = item,
                                    onClick = { onContentClick(item) },
                                    isTV = false
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                if (airingToday.isEmpty() && upcomingFiltered.isEmpty() && upcomingTVFiltered.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nothing scheduled",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiringSection(
    shows: List<AiringShow>,
    onShowClick: (AiringShow) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(shows) { show ->
                AiringShowCard(show = show, onClick = { onShowClick(show) })
            }
        }
    }
}

@Composable
fun AiringShowCard(
    show: AiringShow,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.width(140.dp)) {
        Box(
            modifier = Modifier
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
        ) {
            AndroidView(
                factory = { ctx ->
                    android.widget.ImageView(ctx).apply {
                        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { imageView ->
                    Glide.with(context)
                        .load(show.posterUrl)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(imageView)
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "TV",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            val displayScore = show.voteAverage.takeIf { it > 0 }
            displayScore?.let { score ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "★ ${String.format(Locale.US, "%.1f", score)}",
                        color = Color(0xFFFFD700),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = "S${show.seasonNumber} E${show.episodeNumber}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (show.airTime.isNotEmpty()) {
                        Text(
                            text = show.airTime,
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        Text(
            text = show.name,
            modifier = Modifier
                .padding(top = 8.dp)
                .height(36.dp),
            maxLines = 2,
            style = MaterialTheme.typography.labelMedium,
            overflow = TextOverflow.Ellipsis,
            color = Color.White
        )
    }
}

@Composable
fun UpcomingCard(
    item: AiringShow,
    onClick: () -> Unit,
    isTV: Boolean
) {
    val context = LocalContext.current

    Column(modifier = Modifier.width(160.dp)) {
        Box(
            modifier = Modifier
                .height(220.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
        ) {
            AndroidView(
                factory = { ctx ->
                    android.widget.ImageView(ctx).apply {
                        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { imageView ->
                    Glide.with(context)
                        .load(item.posterUrl)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(imageView)
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "TV",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            val displayScore = item.voteAverage.takeIf { it > 0 }
            displayScore?.let { score ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "★ ${String.format(Locale.US, "%.1f", score)}",
                        color = Color(0xFFFFD700),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text = item.name,
            modifier = Modifier
                .padding(top = 8.dp)
                .height(36.dp),
            maxLines = 2,
            style = MaterialTheme.typography.labelMedium,
            overflow = TextOverflow.Ellipsis,
            color = Color.White
        )

        if (item.airDate.isNotEmpty()) {
            val formattedDate = try {
                LocalDate.parse(item.airDate).format(DateTimeFormatter.ofPattern("MMM d"))
            } catch (e: DateTimeParseException) {
                item.airDate
            }
            Text(
                text = "S${item.seasonNumber}E${item.episodeNumber} - $formattedDate",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun UpcomingCard(
    item: ContentItem,
    onClick: () -> Unit,
    isTV: Boolean
) {
    val context = LocalContext.current

    Column(modifier = Modifier.width(160.dp)) {
        Box(
            modifier = Modifier
                .height(220.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
        ) {
            AndroidView(
                factory = { ctx ->
                    android.widget.ImageView(ctx).apply {
                        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { imageView ->
                    Glide.with(context)
                        .load(item.backdropUrl ?: item.posterUrl)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(imageView)
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isTV) "TV" else "Movie",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            val displayScore = item.voteAverage.takeIf { it > 0 }
            displayScore?.let { score ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "★ ${String.format(Locale.US, "%.1f", score)}",
                        color = Color(0xFFFFD700),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text = item.name,
            modifier = Modifier
                .padding(top = 8.dp)
                .height(36.dp),
            maxLines = 2,
            style = MaterialTheme.typography.labelMedium,
            overflow = TextOverflow.Ellipsis,
            color = Color.White
        )

        if (!isTV && item.releaseDate.isNotEmpty()) {
            val formattedDate = try {
                LocalDate.parse(item.releaseDate).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
            } catch (e: DateTimeParseException) {
                item.releaseDate
            }
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}