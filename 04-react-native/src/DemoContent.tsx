import { LinearGradient } from 'expo-linear-gradient';
import { ScrollView, StyleSheet, Text, View } from 'react-native';

const CARDS = Array.from({ length: 40 }, (_, i) => i);

/** HSV (h in degrees, s/v in 0..1) to a hex colour. */
function hsv(h: number, s: number, v: number) {
  const f = (n: number) => {
    const k = (n + h / 60) % 6;
    return v - v * s * Math.max(0, Math.min(k, 4 - k, 1));
  };
  const hex = (x: number) => Math.round(x * 255).toString(16).padStart(2, '0');
  return `#${hex(f(5))}${hex(f(3))}${hex(f(1))}`;
}

/**
 * Deliberately loud scrolling content (saturated gradients, text, stripes) so blur and the
 * glass tint are easy to judge by eye while scrolling under the bar.
 */
export function DemoContent() {
  return (
    <View style={styles.root}>
      <ScrollView contentContainerStyle={styles.content}>
        {CARDS.map((i) => (
          <Card key={i} index={i} />
        ))}
      </ScrollView>
    </View>
  );
}

function Card({ index }: { index: number }) {
  const hue = (index * 37) % 360;
  return (
    <LinearGradient
      colors={[hsv(hue, 0.85, 1), hsv((hue + 60) % 360, 0.9, 0.8)]}
      start={{ x: 0, y: 0 }}
      end={{ x: 1, y: 1 }}
      style={styles.card}
    >
      {Array.from({ length: 16 }, (_, k) => (
        <View key={k} style={[styles.stripe, { left: k * 28 - 40 }]} />
      ))}
      <Text style={styles.cardText}>Tarjeta {index + 1}</Text>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#12102a' },
  content: { paddingTop: 150, paddingBottom: 160, paddingHorizontal: 16, gap: 16 },
  card: { height: 150, borderRadius: 24, overflow: 'hidden', justifyContent: 'flex-end', padding: 16 },
  stripe: {
    position: 'absolute',
    top: -60,
    width: 4,
    height: 300,
    backgroundColor: 'rgba(255,255,255,0.35)',
    transform: [{ rotate: '45deg' }],
  },
  cardText: { color: '#fff', fontSize: 28, fontWeight: 'bold' },
});
