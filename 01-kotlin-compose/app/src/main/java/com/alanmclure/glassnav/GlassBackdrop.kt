package com.alanmclure.glassnav

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlin.math.roundToInt

/** Blur needs RenderEffect (API 31). Below that we can only fake it with a translucent fill. */
val supportsBackdropBlur: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** Refraction uses an AGSL RuntimeShader (API 33). */
val supportsRefraction: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/**
 * Visual parameters of the glass.
 *
 * @param blur backdrop blur radius; 0 disables the blur entirely (translucent-only fallback).
 * @param tint colour painted over the blurred backdrop.
 * @param saturation 1 = unchanged. iOS glass boosts it (~1.5-1.8) so the blurred colours stay vivid.
 * @param refraction how far (in dp) the edges bend the backdrop; 0 = no lens effect.
 */
@Stable
data class GlassStyle(
    val blur: Dp,
    val tint: Color,
    val saturation: Float = 1f,
    val refraction: Dp = 0.dp,
)

/**
 * Holds a copy of the screen content that glass surfaces can sample.
 *
 * Android has no "blur what is behind me" primitive for views inside a window: `Modifier.blur`
 * blurs the node's own content, and `Window.setBackgroundBlurRadius` only works for whole windows.
 * So we record the content into a [GraphicsLayer] once and let each glass surface draw a (blurred)
 * piece of that layer behind itself.
 */
@Stable
class Backdrop(val layer: GraphicsLayer) {
    /** Position of the recorded content in the root, used to align the sampled region. */
    internal var origin: Offset = Offset.Zero
}

@Composable
fun rememberBackdrop(): Backdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { Backdrop(layer) }
}

/** Marks the composable whose pixels glass surfaces will see. Apply it to the full-screen content. */
fun Modifier.backdropSource(backdrop: Backdrop): Modifier = this
    .onGloballyPositioned { backdrop.origin = it.positionInRoot() }
    .drawWithContent {
        backdrop.layer.record { this@drawWithContent.drawContent() }
        drawLayer(backdrop.layer)
    }

/**
 * Draws a glass surface behind this node: blurred (and optionally refracted) backdrop, tint and a
 * specular rim. The node must be drawn AFTER the [backdropSource] (e.g. a later sibling in a Box).
 */
@Composable
fun Modifier.glassBackdrop(
    backdrop: Backdrop,
    cornerRadius: Dp,
    style: GlassStyle,
): Modifier {
    val blurLayer = rememberGraphicsLayer()
    val barOrigin = remember { floatArrayOf(0f, 0f) }
    val useRefraction = style.refraction > 0.dp && supportsRefraction
    val shader = remember(useRefraction) {
        if (useRefraction && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) LiquidGlassShader.create() else null
    }
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    return this
        .onGloballyPositioned {
            val p = it.positionInRoot()
            barOrigin[0] = p.x
            barOrigin[1] = p.y
        }
        .drawBehind {
            val w = size.width.roundToInt().coerceAtLeast(1)
            val h = size.height.roundToInt().coerceAtLeast(1)
            val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawBehind)) }

            if (supportsBackdropBlur && style.blur > 0.dp) {
                val blurPx = style.blur.toPx()
                val refractionPx = style.refraction.toPx()
                // Extra margin so the blur/refraction have real pixels to read at the edges
                // instead of smearing the clamped border.
                val pad = (blurPx * 2f + refractionPx).roundToInt()

                blurLayer.renderEffect = if (shader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    LiquidGlassShader.effect(
                        shader = shader,
                        blurPx = blurPx,
                        width = w.toFloat(),
                        height = h.toFloat(),
                        pad = pad.toFloat(),
                        cornerRadiusPx = cornerRadius.toPx(),
                        refractionPx = refractionPx,
                    )
                } else {
                    BlurEffect(blurPx, blurPx, TileMode.Clamp)
                }
                blurLayer.colorFilter = ColorFilter.colorMatrix(
                    ColorMatrix().apply { setToSaturation(style.saturation) }
                )

                val dx = barOrigin[0] - backdrop.origin.x
                val dy = barOrigin[1] - backdrop.origin.y
                blurLayer.record(IntSize(w + 2 * pad, h + 2 * pad)) {
                    translate(pad - dx, pad - dy) { drawLayer(backdrop.layer) }
                }
                clipPath(path) {
                    translate(-pad.toFloat(), -pad.toFloat()) { drawLayer(blurLayer) }
                }
            }

            drawPath(path, style.tint)
            // Specular rim: bright on top edge, fading toward the bottom, like light catching glass.
            drawPath(
                path,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.06f)),
                    startY = 0f,
                    endY = size.height,
                ),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
}
