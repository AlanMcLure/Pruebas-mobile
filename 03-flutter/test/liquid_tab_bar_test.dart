import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:glass_nav/glass_surface.dart';
import 'package:glass_nav/liquid_tab_bar.dart';

const _tabs = [
  TabSpec('Dashboard', Icons.home_rounded),
  TabSpec('Diary', Icons.article_outlined),
  TabSpec('Library', Icons.menu_book_rounded),
  TabSpec('Settings', Icons.settings_outlined),
];

const _style = GlassStyle(blur: 4, tint: Color(0x330E1116), saturation: 1.5);

Future<List<int>> _pump(WidgetTester tester) async {
  final selections = <int>[];
  var selected = 0;
  await tester.pumpWidget(
    MaterialApp(
      home: Scaffold(
        body: Center(
          child: SizedBox(
            width: 400,
            child: StatefulBuilder(
              builder: (context, setState) => LiquidTabBar(
                tabs: _tabs,
                selected: selected,
                style: _style,
                onSelected: (i) => setState(() {
                  selected = i;
                  selections.add(i);
                }),
              ),
            ),
          ),
        ),
      ),
    ));
  return selections;
}

void main() {
  testWidgets('tapping a tab selects it', (tester) async {
    final selections = await _pump(tester);

    await tester.tap(find.text('Library'));
    await tester.pumpAndSettle();

    expect(selections, [2]);
  });

  testWidgets('press, drag across tabs and release selects the tab under the finger',
      (tester) async {
    final selections = await _pump(tester);
    final first = tester.getCenter(find.text('Dashboard'));
    final last = tester.getCenter(find.text('Settings'));

    final gesture = await tester.startGesture(first);
    await tester.pump(const Duration(milliseconds: 100));
    // Nothing is committed while the finger is down.
    expect(selections, isEmpty);

    await gesture.moveTo(last, timeStamp: const Duration(milliseconds: 200));
    await tester.pump(const Duration(milliseconds: 300));
    expect(selections, isEmpty);

    await gesture.up();
    await tester.pumpAndSettle();

    expect(selections, [3]);
  });

  testWidgets('releasing on the already selected tab changes nothing', (tester) async {
    final selections = await _pump(tester);

    final gesture = await tester.startGesture(tester.getCenter(find.text('Dashboard')));
    await tester.pump(const Duration(milliseconds: 100));
    await gesture.up();
    await tester.pumpAndSettle();

    expect(selections, isEmpty);
  });

  testWidgets('tabs expose button semantics with their label', (tester) async {
    final handle = tester.ensureSemantics();
    await _pump(tester);

    expect(
      tester.getSemantics(find.bySemanticsLabel('Diary')),
      matchesSemantics(
        label: 'Diary',
        isButton: true,
        hasTapAction: true,
        hasSelectedState: true,
        isSelected: false,
      ),
    );
    handle.dispose();
  });
}
