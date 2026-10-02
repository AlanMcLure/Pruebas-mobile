import { Ionicons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';
import { useEffect, useState, type RefObject } from 'react';
import { StyleSheet, Text, View, type LayoutChangeEvent } from 'react-native';
import { Gesture, GestureDetector } from 'react-native-gesture-handler';
import Animated, {
  useAnimatedReaction,
  useAnimatedStyle,
  useSharedValue,
  withSpring,
} from 'react-native-reanimated';
import { scheduleOnRN } from 'react-native-worklets';

import { GlassSurface, type GlassStyle } from './GlassSurface';

type IconName = keyof typeof Ionicons.glyphMap;

export type TabSpec = {
  label: string;
  /** Outline glyph for resting tabs. */
  icon: IconName;
  /** Filled glyph for the tab under the lens, like iOS tab bars. */
  activeIcon: IconName;
};

const SELECTED = '#4C8DFF';
const PADDING = 6;

// Springs as plain Reanimated configs: damping = 2 * ratio * sqrt(stiffness * mass).
const spring = (stiffness: number, ratio: number) => ({
  mass: 1,
  stiffness,
  damping: 2 * ratio * Math.sqrt(stiffness),
});
/** Follows the finger with a bit of lag, so the lens feels like it has weight. */
const FOLLOW = spring(600, 0.85);
/** Bouncy settle into a tab after release or a plain tap. */
const SETTLE = spring(350, 0.7);
const PRESS = spring(400, 0.6);

type Props = {
  tabs: TabSpec[];
  selected: number;
  onSelected: (index: number) => void;
  style: GlassStyle;
  blurTarget?: RefObject<View | null>;
  height?: number;
};

/**
 * The iOS 26 tab bar gesture on a glass pill.
 *
 * - Idle: a flat capsule sits behind the selected tab.
 * - Press (anywhere in the bar): the capsule lifts into a brighter, larger glass lens.
 * - Drag: the lens trails the finger with a spring; the tab under it is hovered live and a haptic
 *   tick plays each time it changes.
 * - Release: the lens springs into the nearest tab and that tab is selected.
 * - A plain tap is the same gesture without movement.
 *
 * Not replicated: refraction. The lens only scales and brightens; it does not bend what is under
 * it (React Native has no backdrop shader without moving the whole screen into Skia).
 */
export function LiquidTabBar({ tabs, selected, onSelected, style, blurTarget, height = 64 }: Props) {
  const count = tabs.length;
  const innerHeight = height - 2 * PADDING;

  /** Indicator position in tab units (0 = first tab, may overshoot while springing). */
  const pos = useSharedValue(selected);
  /** 0 = idle capsule, 1 = fully lifted lens (overshoots on purpose). */
  const press = useSharedValue(0);
  const pressed = useSharedValue(false);
  const lastX = useSharedValue(0);
  const innerWidth = useSharedValue(0);
  const [hovered, setHovered] = useState(selected);

  // Selection changed from outside (not by our own gesture): slide to it.
  useEffect(() => {
    if (!pressed.value) pos.value = withSpring(selected, SETTLE);
  }, [selected, pos, pressed]);

  useAnimatedReaction(
    () => Math.min(Math.max(Math.round(pos.value), 0), count - 1),
    (current, previous) => {
      if (previous === null || current === previous) return;
      scheduleOnRN(setHovered, current);
      if (pressed.value) scheduleOnRN(Haptics.selectionAsync);
    },
  );

  const pan = Gesture.Pan()
    .minDistance(0)
    .onBegin((e) => {
      pressed.value = true;
      lastX.value = e.x;
      press.value = withSpring(1, PRESS);
      pos.value = withSpring(targetFor(e.x, innerWidth.value, count), FOLLOW);
    })
    .onUpdate((e) => {
      lastX.value = e.x;
      pos.value = withSpring(targetFor(e.x, innerWidth.value, count), FOLLOW);
    })
    .onFinalize(() => {
      if (!pressed.value) return;
      pressed.value = false;
      press.value = withSpring(0, PRESS);
      const itemWidth = innerWidth.value / count;
      const index = Math.min(Math.max(Math.floor(lastX.value / itemWidth), 0), count - 1);
      pos.value = withSpring(index, SETTLE);
      scheduleOnRN(onSelected, index);
    });

  const capsule = useAnimatedStyle(() => {
    const itemWidth = innerWidth.value / count;
    return {
      width: itemWidth,
      opacity: 1 - Math.min(Math.max(press.value, 0), 1),
      transform: [{ translateX: pos.value * itemWidth }],
    };
  });

  const lens = useAnimatedStyle(() => {
    const itemWidth = innerWidth.value / count;
    return {
      width: itemWidth,
      opacity: Math.min(Math.max(press.value, 0), 1),
      transform: [
        { translateX: pos.value * itemWidth },
        { scaleX: 1 + 0.1 * press.value },
        { scaleY: 1 + 0.18 * press.value },
      ],
    };
  });

  return (
    <GlassSurface
      style={style}
      radius={height / 2}
      blurTarget={blurTarget}
      containerStyle={{ height, padding: PADDING }}
    >
      <GestureDetector gesture={pan}>
        <View
          style={styles.inner}
          onLayout={(e: LayoutChangeEvent) => {
            innerWidth.value = e.nativeEvent.layout.width;
          }}
        >
          {/* Resting state: the flat capsule from the reference screenshot. */}
          <Animated.View
            pointerEvents="none"
            style={[styles.indicator, { borderRadius: innerHeight / 2 }, styles.capsule, capsule]}
          />
          <View style={styles.row}>
            {tabs.map((tab, i) => (
              <TabItem
                key={tab.label}
                spec={tab}
                selected={i === selected}
                hovered={i === hovered}
                index={i}
                pos={pos}
                press={press}
                onSelect={() => onSelected(i)}
              />
            ))}
          </View>
          {/* Pressed state: the lifted lens, above the icons. */}
          <Animated.View
            pointerEvents="none"
            style={[styles.indicator, { borderRadius: innerHeight / 2 }, styles.lens, lens]}
          />
        </View>
      </GestureDetector>
    </GlassSurface>
  );
}

function targetFor(x: number, width: number, count: number) {
  'worklet';
  const itemWidth = width / count;
  return Math.min(Math.max(x / itemWidth - 0.5, 0), count - 1);
}

type ItemProps = {
  spec: TabSpec;
  selected: boolean;
  hovered: boolean;
  index: number;
  pos: { value: number };
  press: { value: number };
  onSelect: () => void;
};

/** Pure visuals: touches are handled by the gesture. The tab under the lens grows while pressed. */
function TabItem({ spec, selected, hovered, index, pos, press, onSelect }: ItemProps) {
  const color = hovered ? SELECTED : '#FFFFFF';
  const scale = useAnimatedStyle(() => {
    const near = 1 - Math.min(Math.abs(pos.value - index), 1);
    return { transform: [{ scale: 1 + 0.08 * press.value * near }] };
  });

  return (
    <View
      style={styles.item}
      accessible
      accessibilityRole="tab"
      accessibilityLabel={spec.label}
      accessibilityState={{ selected }}
      // The gesture layer owns touch; this keeps the tab actionable for screen readers.
      onAccessibilityTap={onSelect}
    >
      <Animated.View style={[styles.itemContent, scale]}>
        <Ionicons
          name={hovered ? spec.activeIcon : spec.icon}
          size={26}
          color={color}
          style={styles.shadow}
        />
        <Text
          numberOfLines={1}
          maxFontSizeMultiplier={1.15}
          style={[styles.label, styles.shadow, { color, fontWeight: hovered ? '600' : '500' }]}
        >
          {spec.label}
        </Text>
      </Animated.View>
    </View>
  );
}

const styles = StyleSheet.create({
  inner: { flex: 1 },
  row: { flex: 1, flexDirection: 'row' },
  item: { flex: 1 },
  itemContent: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  label: { fontSize: 11 },
  // A faint dark halo so white and blue glyphs stay readable over bright backdrops.
  shadow: {
    textShadowColor: 'rgba(0,0,0,0.35)',
    textShadowOffset: { width: 0, height: 0 },
    textShadowRadius: 4,
  },
  indicator: { position: 'absolute', left: 0, top: 0, bottom: 0 },
  capsule: { backgroundColor: 'rgba(255,255,255,0.20)' },
  lens: {
    backgroundColor: 'rgba(255,255,255,0.14)',
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.35)',
    borderTopColor: 'rgba(255,255,255,0.7)',
  },
});
