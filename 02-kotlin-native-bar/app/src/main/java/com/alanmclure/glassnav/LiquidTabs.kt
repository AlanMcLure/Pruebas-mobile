package com.alanmclure.glassnav

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Glass of the "lens" that lifts out of the bar while a tab is pressed. */
private val LENS_STYLE = GlassStyle(
    blur = 3.dp,
    tint = Color.White.copy(alpha = 0.12f),
    saturation = 1.4f,
    refraction = 16.dp,
)

/** Follows the finger with a bit of lag, so the lens feels like it has weight. */
private val FOLLOW_SPRING = spring<Float>(dampingRatio = 0.8f, stiffness = 600f)

/** Bouncy settle into a tab after release or a plain tap. */
private val SETTLE_SPRING = spring<Float>(dampingRatio = 0.7f, stiffness = 350f)

/**
 * The iOS 26 tab-bar gesture, independent of how the tabs themselves are drawn.
 *
 * - Idle: a flat capsule sits behind the selected tab.
 * - Press (anywhere in the bar): the capsule becomes a glass lens that scales up and refracts.
 * - Drag: the lens trails the finger; the tab under it is "hovered" live and a haptic tick plays
 *   each time it changes.
 * - Release: the lens springs into the nearest tab and that tab is selected.
 * - A plain tap is the same gesture without movement.
 *
 * [content] draws the tabs (equal width, filling this layer) and receives the hovered tab index
 * and the press amount (0..1, may overshoot) so it can recolour / magnify the hovered item.
 */
@Composable
fun LiquidTabLayer(
    backdrop: Backdrop,
    count: Int,
    selected: Int,
    onSelect: (Int) -> Unit,
    indicatorHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable (hovered: Int, press: Float) -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var widthPx by remember { mutableIntStateOf(0) }
    val itemW = if (count > 0) widthPx.toFloat() / count else 0f
    val indicatorX = remember { Animatable(0f) }
    var pressed by remember { mutableStateOf(false) }
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "press",
    )
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)

    // Keep the indicator on the selected tab when it changes from outside (or on first layout).
    LaunchedEffect(selected, itemW) {
        if (!pressed && itemW > 0f) indicatorX.animateTo(selected * itemW, SETTLE_SPRING)
    }

    val hovered by remember(itemW, count) {
        derivedStateOf {
            if (itemW <= 0f) currentSelected
            else ((indicatorX.value + itemW / 2f) / itemW).toInt().coerceIn(0, count - 1)
        }
    }

    LaunchedEffect(hovered) {
        if (pressed) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Box(
        modifier = modifier
            .onSizeChanged { widthPx = it.width }
            .pointerInput(itemW, count) {
                if (itemW <= 0f) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val maxX = itemW * (count - 1)
                    fun targetFor(px: Float) = (px - itemW / 2f).coerceIn(0f, maxX)

                    var lastX = down.position.x
                    pressed = true
                    scope.launch { indicatorX.animateTo(targetFor(lastX), FOLLOW_SPRING) }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        if (change.positionChanged()) {
                            change.consume()
                            lastX = change.position.x
                            scope.launch { indicatorX.animateTo(targetFor(lastX), FOLLOW_SPRING) }
                        }
                    }

                    pressed = false
                    val index = (lastX / itemW).toInt().coerceIn(0, count - 1)
                    if (index != currentSelected) currentOnSelect(index)
                    scope.launch { indicatorX.animateTo(index * itemW, SETTLE_SPRING) }
                }
            },
    ) {
        if (itemW > 0f) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset(indicatorX.value.roundToInt(), 0) }
                    .size(with(density) { itemW.toDp() }, indicatorHeight),
            ) {
                // Resting state: the flat capsule from the screenshot.
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = (1f - press).coerceIn(0f, 1f) }
                        .background(Color.White.copy(alpha = 0.20f), RoundedCornerShape(50))
                )
                // Pressed state: glass lens, slightly enlarged so it "lifts" and magnifies.
                if (press > 0.01f) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = press.coerceIn(0f, 1f)
                                scaleX = 1f + 0.10f * press
                                scaleY = 1f + 0.18f * press
                            }
                            .glassBackdrop(backdrop, cornerRadius = indicatorHeight / 2, style = LENS_STYLE)
                    )
                }
            }
        }
        content(hovered, press)
    }
}
