package com.alanmclure.glassnav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class NavDestination(val label: String, val icon: ImageVector)

private val DESTINATIONS = listOf(
    NavDestination("Inicio", Icons.Filled.Home),
    NavDestination("Buscar", Icons.Filled.Search),
    NavDestination("Favoritos", Icons.Filled.Favorite),
    NavDestination("Perfil", Icons.Filled.Person),
)

private val BAR_HEIGHT = 64.dp

/** Floating pill navigation bar whose background is [GlassStyle]. */
@Composable
fun GlassNavBar(
    backdrop: Backdrop,
    style: GlassStyle,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
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
}

@Composable
private fun RowScope.NavItem(item: NavDestination, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier = Modifier
            .weight(1f)
            .height(BAR_HEIGHT - 12.dp)
            .clip(shape)
            .background(if (selected) Color.White.copy(alpha = 0.24f) else Color.Transparent, shape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(item.icon, contentDescription = item.label, tint = Color.White, modifier = Modifier.padding(bottom = 2.dp))
        Text(
            item.label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.35f), blurRadius = 4f)),
        )
    }
}
