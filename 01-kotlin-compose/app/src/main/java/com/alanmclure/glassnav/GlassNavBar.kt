package com.alanmclure.glassnav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class NavDestination(val label: String, val icon: ImageVector)

// Same four tabs as the iPhone reference screenshot.
private val DESTINATIONS = listOf(
    NavDestination("Dashboard", Icons.Filled.Home),
    NavDestination("Diary", Icons.AutoMirrored.Filled.List),
    NavDestination("Library", Icons.Filled.Star),
    NavDestination("Settings", Icons.Filled.Settings),
)

private val BAR_HEIGHT = 64.dp
private val SELECTED = Color(0xFF4C8DFF)

/**
 * iOS 26-style tab bar: a glass pill with the tabs plus a separate round glass action button.
 * Every glass surface samples the same [backdrop].
 */
@Composable
fun GlassNavBar(
    backdrop: Backdrop,
    style: GlassStyle,
    selected: Int,
    onSelect: (Int) -> Unit,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(BAR_HEIGHT)
                .glassBackdrop(backdrop, cornerRadius = BAR_HEIGHT / 2, style = style)
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DESTINATIONS.forEachIndexed { index, item ->
                NavItem(item, selected = index == selected, onClick = { onSelect(index) })
            }
        }
        Box(
            modifier = Modifier
                .size(BAR_HEIGHT)
                .glassBackdrop(backdrop, cornerRadius = BAR_HEIGHT / 2, style = style)
                .clip(CircleShape)
                .clickable(onClick = onAction),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Añadir", tint = Color.White, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun RowScope.NavItem(item: NavDestination, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    val color = if (selected) SELECTED else Color.White
    Column(
        modifier = Modifier
            .weight(1f)
            .height(BAR_HEIGHT - 12.dp)
            .clip(shape)
            .background(if (selected) Color.White.copy(alpha = 0.14f) else Color.Transparent, shape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(item.icon, contentDescription = item.label, tint = color, modifier = Modifier.size(24.dp))
        Text(
            item.label,
            color = color,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}
