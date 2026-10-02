import 'package:flutter/material.dart';
import 'package:flutter/physics.dart';
import 'package:flutter/services.dart';

import 'glass_surface.dart';

@immutable
class TabSpec {
  const TabSpec(this.label, this.icon, [IconData? activeIcon]) : activeIcon = activeIcon ?? icon;

  final String label;

  /// Outline glyph for resting tabs.
  final IconData icon;

  /// Filled glyph for the tab under the lens, like iOS tab bars.
  final IconData activeIcon;
}

const _selectedColor = Color(0xFF4C8DFF);

/// A faint dark halo so white and blue glyphs stay readable over bright backdrops.
const _legibility = [Shadow(color: Color(0x59000000), blurRadius: 4)];

/// Glass of the "lens" that lifts out of the bar while it is pressed.
// No blur on purpose: the lens must keep the icons under it sharp, it only bends them.
const _lensStyle = GlassStyle(
  blur: 0,
  tint: Color(0x1FFFFFFF),
  saturation: 1.4,
  refraction: 16,
);

/// Follows the finger with a bit of lag, so the lens feels like it has weight.
final _followSpring = SpringDescription.withDampingRatio(mass: 1, stiffness: 600, ratio: 0.85);

/// Bouncy settle into a tab after release or a plain tap.
final _settleSpring = SpringDescription.withDampingRatio(mass: 1, stiffness: 350, ratio: 0.7);

final _pressSpring = SpringDescription.withDampingRatio(mass: 1, stiffness: 400, ratio: 0.6);

/// The iOS 26 tab bar: a glass pill with the tabs.
///
/// - Idle: a flat capsule sits behind the selected tab.
/// - Press (anywhere in the bar): the capsule becomes a glass lens that scales up and refracts the
///   bar's own icons as well as the page behind.
/// - Drag: the lens trails the finger; the tab under it is hovered live and a haptic tick plays
///   each time it changes.
/// - Release: the lens springs into the nearest tab and that tab is selected.
/// - A plain tap is the same gesture without movement.
class LiquidTabBar extends StatefulWidget {
  const LiquidTabBar({
    super.key,
    required this.tabs,
    required this.selected,
    required this.onSelected,
    required this.style,
    this.height = 64,
  });

  final List<TabSpec> tabs;
  final int selected;
  final ValueChanged<int> onSelected;
  final GlassStyle style;
  final double height;

  @override
  State<LiquidTabBar> createState() => _LiquidTabBarState();
}

class _LiquidTabBarState extends State<LiquidTabBar> with TickerProviderStateMixin {
  static const _padding = 6.0;

  /// Indicator position in tab units (0 = first tab, may overshoot while springing).
  late final AnimationController _pos =
      AnimationController.unbounded(vsync: this, value: widget.selected.toDouble());

  /// 0 = idle capsule, 1 = fully lifted lens (overshoots on purpose).
  late final AnimationController _press = AnimationController.unbounded(vsync: this, value: 0);

  bool _pressed = false;
  int _hovered = 0;
  double _lastX = 0;

  int get _count => widget.tabs.length;

  @override
  void initState() {
    super.initState();
    _hovered = widget.selected;
    _pos.addListener(_onPositionTick);
  }

  @override
  void didUpdateWidget(LiquidTabBar old) {
    super.didUpdateWidget(old);
    // Selection changed from outside (not by our own gesture): slide to it.
    if (old.selected != widget.selected && !_pressed) {
      _springPos(widget.selected.toDouble(), _settleSpring);
    }
  }

  @override
  void dispose() {
    _pos.dispose();
    _press.dispose();
    super.dispose();
  }

  void _onPositionTick() {
    final hovered = _pos.value.round().clamp(0, _count - 1);
    if (hovered != _hovered) {
      _hovered = hovered;
      if (_pressed) HapticFeedback.selectionClick();
    }
  }

  void _springPos(double to, SpringDescription spring) {
    _pos.animateWith(SpringSimulation(spring, _pos.value, to, _pos.velocity));
  }

  void _springPress(double to) {
    _press.animateWith(SpringSimulation(_pressSpring, _press.value, to, _press.velocity));
  }

  double _targetFor(double x, double itemWidth) =>
      (x / itemWidth - 0.5).clamp(0.0, _count - 1.0);

  void _down(PointerDownEvent e, double itemWidth) {
    _pressed = true;
    _lastX = e.localPosition.dx;
    _springPress(1);
    _springPos(_targetFor(_lastX, itemWidth), _followSpring);
  }

  void _move(PointerMoveEvent e, double itemWidth) {
    if (!_pressed) return;
    _lastX = e.localPosition.dx;
    _springPos(_targetFor(_lastX, itemWidth), _followSpring);
  }

  void _up(double itemWidth) {
    if (!_pressed) return;
    _pressed = false;
    _springPress(0);
    final index = (_lastX / itemWidth).floor().clamp(0, _count - 1);
    if (index != widget.selected) widget.onSelected(index);
    _springPos(index.toDouble(), _settleSpring);
  }

  @override
  Widget build(BuildContext context) {
    final innerHeight = widget.height - 2 * _padding;

    return SizedBox(
      height: widget.height,
      child: GlassSurface(
        style: widget.style,
        radius: widget.height / 2,
        child: Padding(
          padding: const EdgeInsets.all(_padding),
          child: LayoutBuilder(
            builder: (context, constraints) {
              final itemWidth = constraints.maxWidth / _count;
              return Listener(
                behavior: HitTestBehavior.opaque,
                onPointerDown: (e) => _down(e, itemWidth),
                onPointerMove: (e) => _move(e, itemWidth),
                onPointerUp: (_) => _up(itemWidth),
                onPointerCancel: (_) => _up(itemWidth),
                child: AnimatedBuilder(
                  animation: Listenable.merge([_pos, _press]),
                  builder: (context, _) {
                    final press = _press.value;
                    final left = _pos.value * itemWidth;
                    return Stack(
                      clipBehavior: Clip.none,
                      children: [
                        // Resting state: the flat capsule from the reference screenshot.
                        Positioned(
                          left: left,
                          width: itemWidth,
                          top: 0,
                          bottom: 0,
                          child: Opacity(
                            opacity: (1 - press).clamp(0.0, 1.0),
                            child: DecoratedBox(
                              decoration: BoxDecoration(
                                color: Colors.white.withValues(alpha: 0.20),
                                borderRadius: BorderRadius.circular(innerHeight / 2),
                              ),
                            ),
                          ),
                        ),
                        Row(
                          children: [
                            for (var i = 0; i < _count; i++)
                              Expanded(
                                child: _TabItem(
                                  spec: widget.tabs[i],
                                  selected: i == widget.selected,
                                  hovered: i == _hovered,
                                  press: press,
                                  onTap: () => widget.onSelected(i),
                                ),
                              ),
                          ],
                        ),
                        // Pressed state: glass lens ABOVE the icons so it refracts them too.
                        // No Opacity here (see GlassSurface); it simply appears with the press.
                        if (press > 0.02)
                          Positioned(
                            left: left,
                            width: itemWidth,
                            top: 0,
                            bottom: 0,
                            child: IgnorePointer(
                              child: Transform.scale(
                                scaleX: 1 + 0.10 * press,
                                scaleY: 1 + 0.18 * press,
                                child: GlassSurface(style: _lensStyle, radius: innerHeight / 2),
                              ),
                            ),
                          ),
                      ],
                    );
                  },
                ),
              );
            },
          ),
        ),
      ),
    );
  }
}

/// Pure visuals: touches are handled by [LiquidTabBar]. [hovered] is the tab currently under the
/// lens (changes live while dragging); it is magnified a little while the bar is pressed.
class _TabItem extends StatelessWidget {
  const _TabItem({
    required this.spec,
    required this.selected,
    required this.hovered,
    required this.press,
    required this.onTap,
  });

  final TabSpec spec;
  final bool selected;
  final bool hovered;
  final double press;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final color = hovered ? _selectedColor : Colors.white;
    final scale = hovered ? 1 + 0.08 * press : 1.0;
    // For screen readers: the gesture layer owns touch, this keeps the tab actionable.
    return Semantics(
      button: true,
      selected: selected,
      label: spec.label,
      onTap: onTap,
      excludeSemantics: true,
      child: Transform.scale(
        scale: scale,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(hovered ? spec.activeIcon : spec.icon, color: color, size: 26, shadows: _legibility),
            // Labels never wrap or follow huge accessibility text sizes: the bar has a fixed height.
            MediaQuery.withClampedTextScaling(
              maxScaleFactor: 1.15,
              child: Text(
                spec.label,
                maxLines: 1,
                softWrap: false,
                overflow: TextOverflow.fade,
                style: TextStyle(
                  color: color,
                  shadows: _legibility,
                  fontSize: 11,
                  fontWeight: hovered ? FontWeight.w600 : FontWeight.w500,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
