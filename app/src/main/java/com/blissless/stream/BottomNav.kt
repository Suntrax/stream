package com.blissless.stream

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class NavTab(
    val index: Int,
    val icon: ImageVector,
    val label: String
)

@Composable
fun StreamBottomNav(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Color(0xfff472a1)

    val tabs = listOf(
        NavTab(0, Icons.Filled.DateRange, "Schedule"),
        NavTab(1, Icons.Filled.Explore, "Explore"),
        NavTab(2, Icons.Filled.Home, "Home")
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .height(48.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.5f), spotColor = Color.Black.copy(alpha = 0.5f))
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF141414).copy(alpha = 0.88f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = tab.index == selectedTab
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) accent else Color.Transparent,
                    animationSpec = spring(dampingRatio = 0.68f, stiffness = 280f),
                    label = "tabBg"
                )
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color(0xFF6B6B6B),
                    animationSpec = spring(dampingRatio = 0.68f, stiffness = 280f),
                    label = "tabIcon"
                )
                val tabWeight by animateFloatAsState(
                    targetValue = if (isSelected) 1.35f else 1f,
                    animationSpec = spring(dampingRatio = 0.68f, stiffness = 280f),
                    label = "tabWeight"
                )

                Box(
                    modifier = Modifier
                        .weight(tabWeight)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(bgColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(tab.index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = tab.label,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
