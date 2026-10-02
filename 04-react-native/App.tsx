import { Ionicons } from '@expo/vector-icons';
import { BlurTargetView } from 'expo-blur';
import { StatusBar } from 'expo-status-bar';
import { useRef, useState } from 'react';
import { Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider, useSafeAreaInsets } from 'react-native-safe-area-context';

import { DemoContent } from './src/DemoContent';
import { GlassSurface, type GlassStyle } from './src/GlassSurface';
import { LiquidTabBar, type TabSpec } from './src/LiquidTabBar';

type Variant = { label: string; style: GlassStyle };

const VARIANTS: Variant[] = [
  // No blur at all: what you get if you just "fake" glass with alpha.
  { label: '1 Translúcido', style: { blur: 0, tint: 'rgba(20,23,28,0.5)' } },
  // Real backdrop blur (native on iOS/Android, CSS backdrop-filter on web).
  { label: '2 Blur real', style: { blur: 40, tint: 'rgba(14,17,22,0.2)' } },
  // Blur + the lifted lens on press. Still no refraction: see LiquidTabBar.
  { label: '3 Lente', style: { blur: 30, tint: 'rgba(14,17,22,0.16)' } },
];

const TABS: TabSpec[] = [
  { label: 'Dashboard', icon: 'home-outline', activeIcon: 'home' },
  { label: 'Diary', icon: 'document-text-outline', activeIcon: 'document-text' },
  { label: 'Library', icon: 'book-outline', activeIcon: 'book' },
  { label: 'Settings', icon: 'settings-outline', activeIcon: 'settings' },
];

export default function App() {
  return (
    <GestureHandlerRootView style={styles.root}>
      <SafeAreaProvider>
        <Demo />
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}

function Demo() {
  const insets = useSafeAreaInsets();
  const blurTarget = useRef<View>(null);
  const [variant, setVariant] = useState(2);
  const [tab, setTab] = useState(0);
  const glass = VARIANTS[variant].style;

  return (
    <View style={styles.root}>
      <StatusBar style="light" />

      {/* Android needs the content to blur wrapped in a BlurTargetView, with the glass as its sibling. */}
      <BlurTargetView ref={blurTarget} style={StyleSheet.absoluteFill}>
        <DemoContent />
      </BlurTargetView>

      <View style={[styles.top, { top: insets.top + 8 }]}>
        <View style={styles.chips}>
          {VARIANTS.map((v, i) => (
            <Chip key={v.label} label={v.label} active={i === variant} onPress={() => setVariant(i)} />
          ))}
        </View>
        <Chip label={`${Platform.OS} · refracción: no`} small />
      </View>

      <View style={[styles.bottom, { bottom: insets.bottom + 12 }]}>
        <View style={styles.bar}>
          <LiquidTabBar
            tabs={TABS}
            selected={tab}
            onSelected={setTab}
            style={glass}
            blurTarget={blurTarget}
          />
        </View>
        <Pressable accessibilityRole="button" accessibilityLabel="Añadir">
          <GlassSurface style={glass} radius={32} blurTarget={blurTarget} containerStyle={styles.action}>
            <View style={styles.actionIcon}>
              <Ionicons name="add" size={30} color="#fff" />
            </View>
          </GlassSurface>
        </Pressable>
      </View>
    </View>
  );
}

function Chip({
  label,
  active = false,
  small = false,
  onPress,
}: {
  label: string;
  active?: boolean;
  small?: boolean;
  onPress?: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      style={[
        styles.chip,
        small && styles.chipSmall,
        { backgroundColor: active ? '#fff' : 'rgba(0,0,0,0.55)' },
      ]}
    >
      <Text style={{ color: active ? '#000' : '#fff', fontSize: small ? 12 : 13 }}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#12102a' },
  top: { position: 'absolute', left: 16, right: 16, gap: 6, alignItems: 'flex-start' },
  chips: { flexDirection: 'row', gap: 8, flexWrap: 'wrap' },
  chip: { paddingHorizontal: 12, paddingVertical: 8, borderRadius: 50 },
  chipSmall: { paddingHorizontal: 8, paddingVertical: 4, borderRadius: 8 },
  bottom: {
    position: 'absolute',
    left: 24,
    right: 24,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  bar: { flex: 1 },
  action: { width: 64, height: 64 },
  actionIcon: { flex: 1, alignItems: 'center', justifyContent: 'center' },
});
