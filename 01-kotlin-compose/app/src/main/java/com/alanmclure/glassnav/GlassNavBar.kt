package com.alanmclure.glassnav

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
        Box(
            modifier = Modifier
                .weight(1f)
                .height(BAR_HEIGHT)
                .glassBackdrop(backdrop, cornerRadius = BAR_HEIGHT / 2, style = style)
                .padding(6.dp),
        ) {
            LiquidTabLayer(
                backdrop = backdrop,
                count = DESTINATIONS.size,
                selected = selected,
                onSelect = onSelect,
                indicatorHeight = BAR_HEIGHT - 12.dp,
                modifier = Modifier.fillMaxSize(),
            ) { hovered, press ->
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    DESTINATIONS.forEachIndexed { index, item ->
                        NavItem(
                            item = item,
                            selected = index == selected,
                            hovered = index == hovered,
                            press = press,
                            onSelect = { onSelect(index) },
                        )
                    }
                }
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

/**
 * Pure visuals: touches are handled by [LiquidTabLayer]. [hovered] is the tab currently under the
 * lens (changes live while dragging); it is magnified a little while the bar is pressed.
 */
@Composable
private fun RowScope.NavItem(
    item: NavDestination,
    selected: Boolean,
    hovered: Boolean,
    press: Float,
    onSelect: () -> Unit,
) {
    val color = if (hovered) SELECTED else Color.White
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            // For TalkBack: the gesture layer owns touch, this keeps the tab actionable.
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                this.selected = selected
                onClick(label = item.label) { onSelect(); true }
            }
            .graphicsLayer {
                val scale = if (hovered) 1f + 0.08f * press else 1f
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(item.icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Text(
            item.label,
            color = color,
            fontSize = 11.sp,
            fontWeight = if (hovered) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}
