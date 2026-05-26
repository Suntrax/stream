package com.blissless.stream

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

private val CardColor = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF9CA3AF)
private val Accent = Color(0xFFf472a1)
private val StarColor = Color(0xFFF59E0B)
private val GreenDot = Color(0xFF4CAF50)

data class ListStatus(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val color: Color
)

val listStatuses = listOf(
    ListStatus("continue", "Watching", Icons.Default.PlayArrow, Color(0xFF4CAF50)),
    ListStatus("planning", "Planning", Icons.Default.Bookmark, Color(0xFF2196F3)),
    ListStatus("completed", "Completed", Icons.Default.Check, Color(0xFF9C27B0)),
    ListStatus("onhold", "On Hold", Icons.Default.Pause, Color(0xFFFF9800)),
    ListStatus("dropped", "Dropped", Icons.Default.Delete, Color(0xFFF44336))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    content: ContentItem,
    details: ContentDetails?,
    onPlayClick: (season: Int, episode: Int) -> Unit,
    onBackClick: () -> Unit,
    onAddToList: (String) -> Unit,
    onRemoveFromList: ((String) -> Unit)? = null,
    getContinueWatchingList: () -> List<ContentItem>,
    getPlanningToWatchList: () -> List<ContentItem>,
    getCompletedList: () -> List<ContentItem>,
    getOnHoldList: () -> List<ContentItem>,
    getDroppedList: () -> List<ContentItem>,
    refreshKey: Int = 0,
    onContentClick: ((ContentItem) -> Unit)? = null
) {
    val context = LocalContext.current
    var showEpisodeSelector by remember { mutableStateOf(false) }
    var selectedSeason by remember { mutableIntStateOf(content.progressSeason.takeIf { it > 0 } ?: 1) }
    var selectedEpisode by remember { mutableIntStateOf(content.progressEpisode.takeIf { it > 0 } ?: 1) }

    LaunchedEffect(refreshKey) {}

    val isSeries = content.type == "tv"
    val totalSeasons = details?.seasons?.size ?: 1
    val episodesPerSeason = details?.seasons?.find { it.seasonNumber == selectedSeason }?.episodeCount ?: 12
    val trailer = details?.trailers?.firstOrNull()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val continueList = getContinueWatchingList()
    val planningList = getPlanningToWatchList()
    val completedList = getCompletedList()
    val onHoldList = getOnHoldList()
    val droppedList = getDroppedList()

    val isInContinue = continueList.any { it.id == content.id && it.type == content.type }
    val isInPlanning = planningList.any { it.id == content.id && it.type == content.type }
    val isInCompleted = completedList.any { it.id == content.id && it.type == content.type }
    val isInOnHold = onHoldList.any { it.id == content.id && it.type == content.type }
    val isInDropped = droppedList.any { it.id == content.id && it.type == content.type }

    val currentStatus = when {
        isInContinue -> listStatuses[0]
        isInPlanning -> listStatuses[1]
        isInCompleted -> listStatuses[2]
        isInOnHold -> listStatuses[3]
        isInDropped -> listStatuses[4]
        else -> null
    }

    if (showEpisodeSelector) {
        ModalBottomSheet(
            onDismissRequest = { showEpisodeSelector = false },
            sheetState = sheetState,
            containerColor = Color(0xFF1A1A1A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Episodes",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showEpisodeSelector = false }) {
                        Icon(Icons.Filled.Close, "Close", tint = Color.White)
                    }
                }

                if (isSeries && totalSeasons > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items((1..totalSeasons).toList()) { season ->
                            val isSel = season == selectedSeason
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSel) Accent else CardColor)
                                    .clickable { selectedSeason = season }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Season $season",
                                    color = if (isSel) Color.White else TextSecondary,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(episodesPerSeason.coerceAtLeast(1)) { index ->
                        val ep = index + 1
                        val isCurrent = ep == content.progressEpisode && selectedSeason == (content.progressSeason.takeIf { it > 0 } ?: 1)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCurrent) Accent.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable {
                                    selectedEpisode = ep
                                    showEpisodeSelector = false
                                    onPlayClick(selectedSeason, selectedEpisode)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isCurrent) Accent else CardColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$ep",
                                    color = if (isCurrent) Color.White else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "Episode $ep",
                                color = if (isCurrent) Accent else Color.White,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )

                            if (isCurrent) {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = "Playing",
                                    tint = Accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
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
                            .load(content.backdropUrl ?: content.posterUrl)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(imageView)
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                )

                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .statusBarsPadding()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Accent.copy(alpha = 0.9f))
                        .clickable {
                            if (isSeries) showEpisodeSelector = true
                            else onPlayClick(1, 1)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = if (isSeries) "Episodes" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                if (isSeries) {
                    val savedSeason = content.progressSeason.takeIf { it > 0 } ?: 1
                    val savedEpisode = content.progressEpisode.takeIf { it > 0 } ?: 1
                    val epCount = details?.seasons?.find { it.seasonNumber == savedSeason }?.episodeCount ?: 12
                    Text(
                        text = "S$savedSeason:E$savedEpisode${if (epCount > 0) " / $epCount" else ""}",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (details != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        if (details.posterUrl != null) {
                            AndroidView(
                                factory = { ctx ->
                                    android.widget.ImageView(ctx).apply {
                                        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                                    }
                                },
                                modifier = Modifier
                                    .size(width = 90.dp, height = 135.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                update = { iv ->
                                    Glide.with(context)
                                        .load(details.posterUrl)
                                        .transition(DrawableTransitionOptions.withCrossFade())
                                        .into(iv)
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = content.name,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val year = details.releaseDate.takeIf { it.length >= 4 }?.take(4)
                                if (!year.isNullOrEmpty()) {
                                    Text(year, color = TextSecondary, fontSize = 13.sp)
                                    Text("•", color = TextSecondary, fontSize = 13.sp)
                                }

                                if (details.runtime > 0) {
                                    val hours = details.runtime / 60
                                    val mins = details.runtime % 60
                                    Text(
                                        text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m",
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                    Text("•", color = TextSecondary, fontSize = 13.sp)
                                }

                                if (details.voteAverage > 0) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = StarColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = String.format("%.1f", details.voteAverage),
                                        color = StarColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (details.genres.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    details.genres.take(3).forEach { genre ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Accent.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = genre,
                                                color = Accent,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            if (details.status.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (details.status == "Released" || details.status == "Returning Series") GreenDot
                                                else TextSecondary
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = details.status,
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    if (details.tagline.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = details.tagline,
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 20.sp
                        )
                    }

                    if (details.overview.isNotEmpty() && details.overview != "No description available.") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = details.overview,
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (isSeries) showEpisodeSelector = true
                            else onPlayClick(1, 1)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSeries) "Episodes" else "Play",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    if (trailer != null) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:${trailer.key}"))
                                    )
                                } catch (_: Exception) {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://www.youtube.com/watch?v=${trailer.key}")
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "Trailer",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Trailer", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }

                if (details != null && details.cast.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = "Cast",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(details.cast.take(15), key = { it.id }) { castMember ->
                            CastCard(castMember)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "My List",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listStatuses) { status ->
                        val isSelected = when (status.key) {
                            "continue" -> isInContinue
                            "planning" -> isInPlanning
                            "completed" -> isInCompleted
                            "onhold" -> isInOnHold
                            "dropped" -> isInDropped
                            else -> false
                        }

                        StatusButton(
                            status = status,
                            isSelected = isSelected,
                            onClick = {
                                if (isSelected && onRemoveFromList != null) {
                                    onRemoveFromList(status.key)
                                } else {
                                    onAddToList(status.key)
                                }
                            }
                        )
                    }
                }

                if (currentStatus != null && content.progressPosition > 0 && content.progressDuration > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val progress = (content.progressPosition.toFloat() / content.progressDuration.toFloat()).coerceIn(0f, 1f)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Progress",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                color = currentStatus.color,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = currentStatus.color,
                            trackColor = Color.Gray.copy(alpha = 0.2f)
                        )
                    }
                }

                if (details != null && details.recommendations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = "More Like This",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(details.recommendations, key = { it.id }) { rec ->
                            RecommendationCard(
                                item = rec,
                                onClick = { onContentClick?.invoke(rec) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun CastCard(cast: CastMember) {
    Column(
        modifier = Modifier.width(76.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(CardColor),
            contentAlignment = Alignment.Center
        ) {
            if (cast.profileUrl != null) {
                AndroidView(
                    factory = { ctx ->
                        android.widget.ImageView(ctx).apply {
                            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                        }
                    },
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    update = { iv ->
                        Glide.with(iv.context).load(cast.profileUrl).circleCrop().into(iv)
                    }
                )
            } else {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = cast.name,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Text(
            text = cast.character,
            color = TextSecondary,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RecommendationCard(item: ContentItem, onClick: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(110.dp)
                .height(165.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CardColor),
            contentAlignment = Alignment.Center
        ) {
            if (item.posterUrl != null) {
                AndroidView(
                    factory = { ctx ->
                        android.widget.ImageView(ctx).apply {
                            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { iv ->
                        Glide.with(context)
                            .load(item.posterUrl)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(iv)
                    }
                )
            } else {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.name,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (item.voteAverage > 0) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = StarColor,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = String.format("%.1f", item.voteAverage),
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun StatusButton(
    status: ListStatus,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isSelected) status.color.copy(alpha = 0.2f) else CardColor
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, status.color)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = status.icon,
                contentDescription = status.label,
                tint = if (isSelected) status.color else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = status.label,
                color = if (isSelected) status.color else Color.Gray,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
