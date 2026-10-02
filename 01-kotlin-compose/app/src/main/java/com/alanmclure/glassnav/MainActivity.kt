package com.alanmclure.glassnav

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class Variant(val label: String, val style: GlassStyle) {
    /** No blur at all: what you get on Android < 12, or if you just "fake" glass with alpha. */
    Translucent("1 Translúcido", GlassStyle(blur = 0.dp, tint = Color(0xFF14171C).copy(alpha = 0.6f))),

    /** Real backdrop blur + saturation boost (API 31+). */
    Blur("2 Blur real", GlassStyle(blur = 22.dp, tint = Color(0xFF0E1116).copy(alpha = 0.35f), saturation = 1.7f)),

    /** Blur + AGSL refraction and dispersion at the rim (API 33+). */
    Liquid(
        "3 Liquid",
        GlassStyle(blur = 6.dp, tint = Color(0xFF0E1116).copy(alpha = 0.22f), saturation = 1.5f, refraction = 22.dp),
    ),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) { GlassNavDemo() }
        }
    }
}

@Composable
private fun GlassNavDemo() {
    val backdrop = rememberBackdrop()
    var variantIndex by rememberSaveable { mutableIntStateOf(Variant.Liquid.ordinal) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val variant = Variant.entries[variantIndex]

    Box(Modifier.fillMaxSize()) {
        DemoContent(Modifier.fillMaxSize().backdropSource(backdrop))

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Variant.entries.forEach { v ->
                    val active = v == variant
                    Text(
                        v.label,
                        color = if (active) Color.Black else Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (active) Color.White else Color.Black.copy(alpha = 0.55f))
                            .clickable { variantIndex = v.ordinal }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            Text(
                "API ${Build.VERSION.SDK_INT} · blur ${if (supportsBackdropBlur) "sí" else "no"} · " +
                    "refracción ${if (supportsRefraction) "sí" else "no"}",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        GlassNavBar(
            backdrop = backdrop,
            style = variant.style,
            selected = tab,
            onSelect = { tab = it },
            onAction = {},
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
        )
    }
}
