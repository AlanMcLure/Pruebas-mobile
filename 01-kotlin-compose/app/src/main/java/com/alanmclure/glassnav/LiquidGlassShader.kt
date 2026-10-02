package com.alanmclure.glassnav

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.asComposeRenderEffect

/**
 * AGSL "lens": pulls pixels from inside the pill toward its rim, so the backdrop looks bent where
 * the glass is thick, and splits the RGB channels slightly (chromatic dispersion) like iOS 26.
 *
 * Coordinates arrive in layer space, which is the bar size plus [pad] on every side.
 */
private const val LIQUID_GLASS_AGSL = """
uniform shader content;
uniform float2 size;
uniform float2 pad;
uniform float cornerRadius;
uniform float refraction;

half4 main(float2 coord) {
    float2 p = coord - pad - size * 0.5;
    float2 q = abs(p) - (size * 0.5 - float2(cornerRadius));

    // Signed distance to the rounded rect (negative inside) and outward direction.
    float d = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - cornerRadius;
    float2 dir;
    if (q.x > 0.0 || q.y > 0.0) {
        dir = normalize(sign(p) * max(q, 0.0));
    } else if (q.x > q.y) {
        dir = float2(sign(p.x), 0.0);
    } else {
        dir = float2(0.0, sign(p.y));
    }

    // 1 right at the rim, 0 once we are `rim` px inside.
    float rim = max(cornerRadius * 0.9, 1.0);
    float edge = clamp(1.0 + d / rim, 0.0, 1.0);
    float bend = edge * edge * refraction;

    float2 base = coord - dir * bend;
    float2 split = dir * bend * 0.12;
    half r = content.eval(base - split).r;
    half2 ga = content.eval(base).ga;
    half b = content.eval(base + split).b;
    return half4(r, ga.x, b, ga.y);
}
"""

/** Isolated so API 33-only classes are never touched on older devices. */
object LiquidGlassShader {

    @RequiresApi(33)
    fun create(): Any = RuntimeShader(LIQUID_GLASS_AGSL)

    @RequiresApi(33)
    fun effect(
        shader: Any,
        blurPx: Float,
        width: Float,
        height: Float,
        pad: Float,
        cornerRadiusPx: Float,
        refractionPx: Float,
    ): androidx.compose.ui.graphics.RenderEffect {
        val s = shader as RuntimeShader
        s.setFloatUniform("size", width, height)
        s.setFloatUniform("pad", pad, pad)
        s.setFloatUniform("cornerRadius", cornerRadiusPx)
        s.setFloatUniform("refraction", refractionPx)
        val refract = RenderEffect.createRuntimeShaderEffect(s, "content")
        val blur = RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP)
        // Chain applies the inner effect first: blur the backdrop, then bend it.
        return RenderEffect.createChainEffect(refract, blur).asComposeRenderEffect()
    }
}
