#version 460 core
precision highp float;

#include <flutter/runtime_effect.glsl>

// Backdrop "lens" for ImageFilter.shader (Impeller only).
// Uniform order matters: the engine fills uSize (the first vec2) with the size of the backdrop
// texture and binds the filter input to the first sampler. All sizes are in physical pixels.
uniform vec2 uSize;
uniform float uCornerRadius;
uniform float uRefraction;
uniform float uSaturation;
uniform sampler2D uTexture;

out vec4 fragColor;

void main() {
  vec2 coord = FlutterFragCoord().xy;
  vec2 p = coord - uSize * 0.5;
  vec2 q = abs(p) - (uSize * 0.5 - vec2(uCornerRadius));

  // Signed distance to the rounded rect (negative inside) and outward direction.
  float d = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - uCornerRadius;
  vec2 dir;
  if (q.x > 0.0 || q.y > 0.0) {
    dir = normalize(sign(p) * max(q, 0.0));
  } else if (q.x > q.y) {
    dir = vec2(sign(p.x), 0.0);
  } else {
    dir = vec2(0.0, sign(p.y));
  }

  // 1 right at the rim, 0 once we are `rim` px inside. Pull pixels from the inside toward the
  // rim (a lens) and split the colour channels slightly (chromatic dispersion).
  float rim = max(uCornerRadius * 0.9, 1.0);
  float edge = clamp(1.0 + d / rim, 0.0, 1.0);
  float bend = edge * edge * uRefraction;
  vec2 base = coord - dir * bend;
  vec2 split = dir * bend * 0.12;

  vec2 uvR = (base - split) / uSize;
  vec2 uvG = base / uSize;
  vec2 uvB = (base + split) / uSize;
#ifdef IMPELLER_TARGET_OPENGLES
  uvR.y = 1.0 - uvR.y;
  uvG.y = 1.0 - uvG.y;
  uvB.y = 1.0 - uvB.y;
#endif

  vec4 g = texture(uTexture, uvG);
  vec3 col = vec3(texture(uTexture, uvR).r, g.g, texture(uTexture, uvB).b);
  float luma = dot(col, vec3(0.299, 0.587, 0.114));
  col = mix(vec3(luma), col, uSaturation);
  fragColor = vec4(col, g.a);
}
