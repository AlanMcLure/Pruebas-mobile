package com.alanmclure.glassnav

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private data class NavDestination(val label: String, val icon: ImageVector)

private val DESTINATIONS = listOf(
    NavDestination("Dashboard", Icons.Filled.Home),
    NavDestination("Diary", Icons.AutoMirrored.Filled.List),
    NavDestination("Library", Icons.Filled.Star),
    NavDestination("Settings", Icons.Filled.Settings),
)

private val SELECTED = Color(0xFF4C8DFF)
private val ACTION_SIZE = 64.dp

/**
 * Same look as the hand-made bar in 01, but the tabs are the stock Material 3 [NavigationBar].
 * Its container is transparent with the glass behind it. Material has no press-and-drag
 * selection, so [LiquidTabLayer] adds that on top: its indicator replaces Material's, and the
 * stock items just follow the "hovered" tab. Sizing, ripple and a11y still come from Material.
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
        // The glass goes on this wrapper (not on NavigationBar) so it sits UNDER the lens indicator.
        Box(
            modifier = Modifier
                .weight(1f)
                .glassBackdrop(backdrop, cornerRadius = 40.dp, style = style),
        ) {
            LiquidTabLayer(
                backdrop = backdrop,
                count = DESTINATIONS.size,
                selected = selected,
                onSelect = onSelect,
                indicatorHeight = 64.dp,
                modifier = Modifier.fillMaxWidth(),
            ) { hovered, _ ->
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    // Insets are handled by the parent (navigationBarsPadding), not by the bar itself.
                    windowInsets = WindowInsets(0, 0, 0, 0),
                ) {
                    DESTINATIONS.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = index == hovered,
                            onClick = { onSelect(index) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SELECTED,
                                selectedTextColor = SELECTED,
                                // Material's own indicator is off: LiquidTabLayer draws ours.
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = Color.White,
                                unselectedTextColor = Color.White,
                            ),
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .size(ACTION_SIZE)
                .glassBackdrop(backdrop, cornerRadius = ACTION_SIZE / 2, style = style)
                .clip(CircleShape)
                .clickable(onClick = onAction),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Añadir", tint = Color.White, modifier = Modifier.size(30.dp))
        }
    }
}
