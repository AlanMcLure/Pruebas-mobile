package com.alanmclure.glassnav

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Deliberately loud scrolling content (saturated gradients, text, stripes) so blur, saturation
 * boost and edge refraction are easy to judge by eye while scrolling under the bar.
 */
@Composable
fun DemoContent(modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(Brush.verticalGradient(listOf(Color(0xFF0B1026), Color(0xFF1B0B2E))))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 150.dp, bottom = 160.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
        ) {
            items((0 until 40).toList()) { i -> DemoCard(i) }
        }
    }
}

@Composable
private fun DemoCard(index: Int) {
    val hue = (index * 37f) % 360f
    val a = Color.hsv(hue, 0.85f, 1f)
    val b = Color.hsv((hue + 60f) % 360f, 0.9f, 0.8f)
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(shape)
            .background(Brush.linearGradient(listOf(a, b)))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val step = 28.dp.toPx()
            var x = -size.height
            while (x < size.width) {
                drawLine(
                    Color.White.copy(alpha = 0.35f),
                    Offset(x, size.height),
                    Offset(x + size.height, 0f),
                    strokeWidth = 4.dp.toPx(),
                )
                x += step
            }
        }
        Text(
            "Tarjeta ${index + 1}",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        )
    }
}
