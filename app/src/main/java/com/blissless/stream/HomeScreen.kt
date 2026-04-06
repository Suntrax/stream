package com.blissless.stream

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.blissless.stream.ContentItem
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.coroutines.delay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween

data class ListSection(
    val key: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val getItems: () -> List<ContentItem>
)

@Composable
fun HomeScreen(
    getContinueWatchingList: () -> List<ContentItem>,
    getPlanningToWatchList: () -> List<ContentItem>,
    getCompletedList: () -> List<ContentItem>,
    getOnHoldList: () -> List<ContentItem>,
    getDroppedList: () -> List<ContentItem>,
    refreshKey: Int = 0,
    onContentClick: (ContentItem) -> Unit,
    onSearchClick: () -> Unit
) {
    val context = LocalContext.current
    val continueWatching = getContinueWatchingList()
    val planningToWatch = getPlanningToWatchList()
    val completed = getCompletedList()
    val onHold = getOnHoldList()
    val dropped = getDroppedList()

    LaunchedEffect(refreshKey) {
        // This will trigger recomposition when refreshKey changes
    }

    val sections = listOf(
        ListSection("continue", "Watching", Icons.Default.PlayArrow, Color(0xFFf472a1)) { continueWatching },
        ListSection("planning", "Planning to Watch", Icons.Default.Bookmark, Color(0xFFf472a1)) { planningToWatch },
        ListSection("completed", "Completed", Icons.Default.Check, Color(0xFFf472a1)) { completed },
        ListSection("onhold", "On Hold", Icons.Default.Pause, Color(0xFFf472a1)) { onHold },
        ListSection("dropped", "Dropped", Icons.Default.Delete, Color(0xFFf472a1)) { dropped }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AndroidView(
                    factory = { ctx ->
                        android.widget.ImageView(ctx).apply {
                            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    update = { imageView: android.widget.ImageView ->
                        Glide.with(context)
                            .load(R.mipmap.ic_launcher_round)
                            .circleCrop()
                            .into(imageView)
                    }
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Stream",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White
                    )
                }
            }
        }

        sections.forEach { section ->
            val items = section.getItems()
            if (items.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = section.title,
                        icon = section.icon,
                        color = section.color,
                        count = items.size
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items, key = { "${section.key}_${it.id}_${it.type}" }) { item ->
                            AnimatedListCard(
                                item = item,
                                listType = section.key,
                                onClick = { onContentClick(item) }
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (continueWatching.isEmpty() && planningToWatch.isEmpty() && completed.isEmpty() && onHold.isEmpty() && dropped.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Your list is empty",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add content from Explore tab!",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "($count)",
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun AnimatedListCard(
    item: ContentItem,
    listType: String,
    onClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        delay(50)
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = tween(300),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "alpha"
    )

    Card(
        modifier = Modifier
            .width(140.dp)
            .height(220.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    android.widget.ImageView(ctx).apply {
                        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                update = { imageView ->
                    Glide.with(context)
                        .load(item.posterUrl ?: item.backdropUrl)
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
                                Color.Black.copy(alpha = 0.8f)
                            ),
                            startY = 100f
                        )
                    )
            )

            if (listType == "continue" && item.progressPosition > 0 && item.progressDuration > 0) {
                val progress = (item.progressPosition.toFloat() / item.progressDuration.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = Color(0xfff472a1),
                    trackColor = Color.Gray.copy(alpha = 0.3f)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (listType == "continue" && item.progressPosition > 0 && item.progressDuration > 0) {
                    val progress = (item.progressPosition.toFloat() / item.progressDuration.toFloat()).coerceIn(0f, 1f)
                    Text(
                        text = "${(progress * 100).toInt()}% watched",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
                if (listType == "continue" && item.type == "tv") {
                    Text(
                        text = "S${item.progressSeason}:E${item.progressEpisode}",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentCardHome(
    item: ContentItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        AndroidView(
            factory = { ctx ->
                android.widget.ImageView(ctx).apply {
                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                }
            },
            modifier = Modifier
                .width(120.dp)
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray),
            update = { imageView ->
                Glide.with(context)
                    .load(item.posterUrl ?: item.backdropUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageView)
            }
        )
        Text(
            text = item.name,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
