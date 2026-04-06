package com.blissless.stream

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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.blissless.stream.ContentItem
import com.blissless.stream.ContentDetails
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

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
    refreshKey: Int = 0
) {
    val context = LocalContext.current
    var showEpisodeSelector by remember { mutableStateOf(false) }
    var showListSelector by remember { mutableStateOf(false) }
    var selectedSeason by remember { mutableIntStateOf(content.progressSeason.takeIf { it > 0 } ?: 1) }
    var selectedEpisode by remember { mutableIntStateOf(content.progressEpisode.takeIf { it > 0 } ?: 1) }
    
    LaunchedEffect(refreshKey) {
        // Trigger recomposition when refreshKey changes
    }
    
    val isSeries = content.type == "tv"
    val totalSeasons = details?.seasons?.size ?: 1
    val episodesPerSeason = details?.seasons?.find { it.seasonNumber == selectedSeason }?.episodeCount ?: 24

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
                        text = "Select Episode",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showEpisodeSelector = false }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                if (isSeries && totalSeasons > 1) {
                    Text(
                        text = "Season",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items((1..totalSeasons).toList()) { season ->
                            val isSelected = season == selectedSeason
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xfff472a1) else Color(0xFF2A2A2A))
                                    .clickable { selectedSeason = season }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "S$season",
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Episode",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(episodesPerSeason.coerceAtLeast(1)) { index ->
                        val ep = index + 1
                        val isSelected = ep == selectedEpisode && selectedSeason == (content.progressSeason.takeIf { it > 0 } ?: 1)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xfff472a1).copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    selectedEpisode = ep
                                    showEpisodeSelector = false
                                    onPlayClick(selectedSeason, selectedEpisode)
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Episode $ep",
                                color = if (isSelected) Color(0xfff472a1) else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color(0xfff472a1)
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
                    .height(250.dp)
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
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.9f)
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

                if (isSeries) {
                    val savedSeason = content.progressSeason.takeIf { it > 0 } ?: 1
                    val savedEpisode = content.progressEpisode.takeIf { it > 0 } ?: 1
                    val episodeCount = details?.seasons?.find { it.seasonNumber == savedSeason }?.episodeCount ?: 24
                    Text(
                        text = "S$savedSeason:E$savedEpisode${if (episodeCount > 0) " / $episodeCount" else ""}",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = content.name,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                if (details != null) {
                    val d = details
                    Text(
                        text = d.genres.take(3).joinToString(" • "),
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (d.overview.isNotEmpty()) {
                        Text(
                            text = d.overview,
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (isSeries) {
                            showEpisodeSelector = true
                        } else {
                            onPlayClick(1, 1)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xfff472a1)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSeries) "Episodes" else "Play",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Add to List",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
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
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                color = currentStatus.color,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = currentStatus.color,
                            trackColor = Color.Gray.copy(alpha = 0.3f)
                        )
                    }
                }
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
    Card(
        modifier = Modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) status.color.copy(alpha = 0.2f) else Color(0xFF1A1A1A)
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, status.color)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
        }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
