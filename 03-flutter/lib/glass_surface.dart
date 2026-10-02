import 'dart:ui' as ui;

import 'package:flutter/material.dart';

/// Visual parameters of the glass.
///
/// [blur] is the Gaussian sigma in logical pixels (0 = no blur; with no refraction either there is
/// no backdrop filter at all, just [tint]).
/// [saturation] 1 = unchanged; iOS boosts it (~1.5-1.8) so the blurred colours stay vivid.
/// [refraction] is how far (logical px) the rim bends the backdrop; 0 = no lens (needs Impeller).
@immutable
class GlassStyle {
  const GlassStyle({
    required this.blur,
    required this.tint,
    this.saturation = 1,
    this.refraction = 0,
  });

  final double blur;
  final Color tint;
  final double saturation;
  final double refraction;

  GlassStyle copyWith({double? blur, Color? tint, double? saturation, double? refraction}) =>
      GlassStyle(
        blur: blur ?? this.blur,
        tint: tint ?? this.tint,
        saturation: saturation ?? this.saturation,
        refraction: refraction ?? this.refraction,
      );
}

/// Loads the lens shader once. On failure (or unsupported platforms) [program] stays null and
/// every surface falls back to plain blur.
class GlassShaders {
  GlassShaders._();

  static ui.FragmentProgram? program;

  /// The refraction filter only exists with Impeller (default on Android and iOS).
  static bool get refractionAvailable => program != null && ui.ImageFilter.isShaderFilterSupported;

  static Future<void> init() async {
    try {
      program = await ui.FragmentProgram.fromAsset('shaders/liquid_glass.frag');
    } catch (e) {
      debugPrint('Lens shader not available, falling back to blur only: $e');
    }
  }
}

/// A glass surface: blurs whatever was painted *behind* it, tints it and draws a specular rim.
///
/// Unlike Android/Compose there is no layer to record: [BackdropFilter] already samples everything
/// painted earlier in the same stacking context, so the surface must sit above its backdrop (a
/// later sibling in a [Stack]). Do not wrap it in an [Opacity]: that creates an isolated layer and
/// the backdrop would be empty.
class GlassSurface extends StatefulWidget {
  const GlassSurface({
    super.key,
    required this.style,
    required this.radius,
    this.child,
  });

  final GlassStyle style;
  final double radius;
  final Widget? child;

  @override
  State<GlassSurface> createState() => _GlassSurfaceState();
}

class _GlassSurfaceState extends State<GlassSurface> {
  ui.FragmentShader? _shader;
  String? _shaderKey;

  /// The filter equality compares shader identity, so a new shader is created whenever the
  /// uniforms change; otherwise the engine would not repaint.
  ui.FragmentShader _shaderFor(Size size, double dpr) {
    final key = '${size.width}x${size.height}|$dpr|${widget.radius}|'
        '${widget.style.refraction}|${widget.style.saturation}';
    if (_shaderKey != key) {
      // The previous shader may still be referenced by the frame in flight, so it is not disposed
      // here (it only changes on resize/rotation); dispose() releases the latest one.
      _shader = GlassShaders.program!.fragmentShader()
        ..setFloat(0, size.width * dpr)
        ..setFloat(1, size.height * dpr)
        ..setFloat(2, widget.radius * dpr)
        ..setFloat(3, widget.style.refraction * dpr)
        ..setFloat(4, widget.style.saturation);
      _shaderKey = key;
    }
    return _shader!;
  }

  @override
  void dispose() {
    _shader?.dispose();
    super.dispose();
  }

  ui.ImageFilter? _filter(Size size, double dpr) {
    final style = widget.style;
    final lens = style.refraction > 0 && GlassShaders.refractionAvailable;
    if (style.blur <= 0 && !lens) return null;

    final blur = style.blur > 0
        ? ui.ImageFilter.blur(sigmaX: style.blur, sigmaY: style.blur, tileMode: TileMode.mirror)
        : null;

    if (lens) {
      // The shader also does the saturation boost. With no blur it only bends the backdrop,
      // which keeps whatever is behind it (e.g. the tab icons under the lens) sharp.
      final shader = ui.ImageFilter.shader(_shaderFor(size, dpr));
      return blur == null ? shader : ui.ImageFilter.compose(outer: shader, inner: blur);
    }
    return ui.ImageFilter.compose(outer: _saturation(style.saturation), inner: blur!);
  }

  static ColorFilter _saturation(double s) {
    const r = 0.299, g = 0.587, b = 0.114;
    return ColorFilter.matrix(<double>[
      (1 - s) * r + s, (1 - s) * g, (1 - s) * b, 0, 0, //
      (1 - s) * r, (1 - s) * g + s, (1 - s) * b, 0, 0,
      (1 - s) * r, (1 - s) * g, (1 - s) * b + s, 0, 0,
      0, 0, 0, 1, 0,
    ]);
  }

  @override
  Widget build(BuildContext context) {
    final radius = BorderRadius.circular(widget.radius);
    final dpr = MediaQuery.devicePixelRatioOf(context);

    return Stack(
      children: [
        Positioned.fill(
          child: LayoutBuilder(
            builder: (context, constraints) {
              final filter = _filter(constraints.biggest, dpr);
              final tint = ColoredBox(color: widget.style.tint);
              return ClipRRect(
                borderRadius: radius,
                child: filter == null
                    ? tint
                    : BackdropFilter(filter: filter, child: tint),
              );
            },
          ),
        ),
        Positioned.fill(
          child: IgnorePointer(child: CustomPaint(painter: _RimPainter(widget.radius))),
        ),
        if (widget.child != null) widget.child!,
      ],
    );
  }
}

/// Specular rim: bright on the top edge, fading toward the bottom, like light catching glass.
class _RimPainter extends CustomPainter {
  const _RimPainter(this.radius);

  final double radius;

  @override
  void paint(Canvas canvas, Size size) {
    final rect = (Offset.zero & size).deflate(0.5);
    final paint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1
      ..shader = ui.Gradient.linear(
        rect.topCenter,
        rect.bottomCenter,
        [Colors.white.withValues(alpha: 0.65), Colors.white.withValues(alpha: 0.06)],
      );
    canvas.drawRRect(RRect.fromRectAndRadius(rect, Radius.circular(radius)), paint);
  }

  @override
  bool shouldRepaint(_RimPainter oldDelegate) => oldDelegate.radius != radius;
}
