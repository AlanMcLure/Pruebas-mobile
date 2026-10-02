import 'dart:ui' as ui;

import 'package:flutter/material.dart';

import 'glass_surface.dart';
import 'liquid_tab_bar.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await GlassShaders.init();
  runApp(const GlassNavApp());
}

class GlassNavApp extends StatelessWidget {
  const GlassNavApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Glass Nav (Flutter)',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(brightness: Brightness.dark, useMaterial3: true),
      home: const GlassNavDemo(),
    );
  }
}

enum Variant {
  /// No blur at all: what you get if you just "fake" glass with alpha.
  translucent('1 Translúcido', GlassStyle(blur: 0, tint: Color(0x9914171C))),

  /// Real backdrop blur + saturation boost.
  blur('2 Blur real', GlassStyle(blur: 14, tint: Color(0x590E1116), saturation: 1.7)),

  /// Blur + refraction and dispersion at the rim (needs Impeller).
  liquid(
    '3 Liquid',
    GlassStyle(blur: 3, tint: Color(0x380E1116), saturation: 1.5, refraction: 22),
  );

  const Variant(this.label, this.style);

  final String label;
  final GlassStyle style;
}

const _tabs = [
  TabSpec('Dashboard', Icons.home_rounded),
  TabSpec('Diary', Icons.article_outlined),
  TabSpec('Library', Icons.menu_book_rounded),
  TabSpec('Settings', Icons.settings_outlined),
];

class GlassNavDemo extends StatefulWidget {
  const GlassNavDemo({super.key});

  @override
  State<GlassNavDemo> createState() => _GlassNavDemoState();
}

class _GlassNavDemoState extends State<GlassNavDemo> {
  Variant _variant = Variant.liquid;
  int _tab = 0;

  @override
  Widget build(BuildContext context) {
    final padding = MediaQuery.paddingOf(context);

    return Scaffold(
      body: Stack(
        children: [
          // The backdrop: loud scrolling content so blur, saturation and refraction are easy to see.
          const Positioned.fill(child: DemoContent()),

          Positioned(
            top: padding.top + 8,
            left: 16,
            right: 16,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Wrap(
                  spacing: 8,
                  children: [
                    for (final v in Variant.values)
                      _Chip(
                        label: v.label,
                        active: v == _variant,
                        onTap: () => setState(() => _variant = v),
                      ),
                  ],
                ),
                const SizedBox(height: 6),
                _Chip(
                  label: 'Impeller: ${ui.ImageFilter.isShaderFilterSupported ? 'sí' : 'no'} · '
                      'shader: ${GlassShaders.program != null ? 'sí' : 'no'} · '
                      'refracción: ${GlassShaders.refractionAvailable ? 'sí' : 'no'}',
                  active: false,
                  small: true,
                ),
              ],
            ),
          ),

          Positioned(
            left: 24,
            right: 24,
            bottom: padding.bottom + 12,
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                Expanded(
                  child: LiquidTabBar(
                    tabs: _tabs,
                    selected: _tab,
                    onSelected: (i) => setState(() => _tab = i),
                    style: _variant.style,
                  ),
                ),
                const SizedBox(width: 12),
                _ActionButton(style: _variant.style),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ActionButton extends StatelessWidget {
  const _ActionButton({required this.style});

  final GlassStyle style;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: 'Añadir',
      excludeSemantics: true,
      child: GestureDetector(
        onTap: () {},
        child: SizedBox.square(
          dimension: 64,
          child: GlassSurface(
            style: style,
            radius: 32,
            child: const Center(child: Icon(Icons.add, color: Colors.white, size: 30)),
          ),
        ),
      ),
    );
  }
}

class _Chip extends StatelessWidget {
  const _Chip({required this.label, required this.active, this.onTap, this.small = false});

  final String label;
  final bool active;
  final VoidCallback? onTap;
  final bool small;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: EdgeInsets.symmetric(horizontal: small ? 8 : 12, vertical: small ? 4 : 8),
        decoration: BoxDecoration(
          color: active ? Colors.white : Colors.black.withValues(alpha: 0.55),
          borderRadius: BorderRadius.circular(small ? 8 : 50),
        ),
        child: Text(
          label,
          style: TextStyle(color: active ? Colors.black : Colors.white, fontSize: small ? 12 : 13),
        ),
      ),
    );
  }
}

class DemoContent extends StatelessWidget {
  const DemoContent({super.key});

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [Color(0xFF0B1026), Color(0xFF1B0B2E)],
        ),
      ),
      child: ListView.separated(
        padding: const EdgeInsets.fromLTRB(16, 150, 16, 160),
        itemCount: 40,
        separatorBuilder: (_, _) => const SizedBox(height: 16),
        itemBuilder: (_, i) => _DemoCard(index: i),
      ),
    );
  }
}

class _DemoCard extends StatelessWidget {
  const _DemoCard({required this.index});

  final int index;

  @override
  Widget build(BuildContext context) {
    final hue = (index * 37.0) % 360;
    final a = HSVColor.fromAHSV(1, hue, 0.85, 1).toColor();
    final b = HSVColor.fromAHSV(1, (hue + 60) % 360, 0.9, 0.8).toColor();
    return ClipRRect(
      borderRadius: BorderRadius.circular(24),
      child: Container(
        height: 150,
        decoration: BoxDecoration(gradient: LinearGradient(colors: [a, b])),
        child: CustomPaint(
          painter: const _StripesPainter(),
          child: Align(
            alignment: Alignment.bottomLeft,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Text(
                'Tarjeta ${index + 1}',
                style: const TextStyle(color: Colors.white, fontSize: 28, fontWeight: FontWeight.bold),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _StripesPainter extends CustomPainter {
  const _StripesPainter();

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = Colors.white.withValues(alpha: 0.35)
      ..strokeWidth = 4;
    for (var x = -size.height; x < size.width; x += 28) {
      canvas.drawLine(Offset(x, size.height), Offset(x + size.height, 0), paint);
    }
  }

  @override
  bool shouldRepaint(_StripesPainter oldDelegate) => false;
}
