import { BlurView } from 'expo-blur';
import { LinearGradient } from 'expo-linear-gradient';
import type { ReactNode, RefObject } from 'react';
import { StyleSheet, View, type StyleProp, type ViewStyle } from 'react-native';

/**
 * [blur] is expo-blur's intensity (1-100, 0 = no blur, just the tint).
 * [tint] is painted over the blurred backdrop.
 */
export type GlassStyle = {
  blur: number;
  tint: string;
};

type Props = {
  style: GlassStyle;
  radius: number;
  /** Android only: the BlurTargetView that holds the content to blur. Ignored elsewhere. */
  blurTarget?: RefObject<View | null>;
  containerStyle?: StyleProp<ViewStyle>;
  children?: ReactNode;
};

/**
 * A glass surface: native blur of whatever is behind it, a tint and a specular rim.
 *
 * Unlike Flutter/Compose, React Native has no built-in backdrop blur. expo-blur wraps the native
 * one (UIVisualEffectView on iOS, Dimezis BlurView on Android, CSS backdrop-filter on web). On
 * Android the content to blur must be wrapped in a <BlurTargetView> and passed via [blurTarget];
 * the surface must be a SIBLING of that target, not inside it.
 */
export function GlassSurface({ style, radius, blurTarget, containerStyle, children }: Props) {
  return (
    <View style={[{ borderRadius: radius, overflow: 'hidden' }, containerStyle]}>
      {style.blur > 0 && (
        <BlurView
          blurTarget={blurTarget}
          blurMethod="dimezisBlurViewSdk31Plus"
          tint="systemUltraThinMaterialDark"
          intensity={style.blur}
          style={StyleSheet.absoluteFill}
        />
      )}
      {/* Soft highlight that fades from the top, like light on a glass edge. */}
      <LinearGradient
        pointerEvents="none"
        colors={['rgba(255,255,255,0.16)', 'rgba(255,255,255,0)']}
        style={StyleSheet.absoluteFill}
      />
      <View
        pointerEvents="none"
        style={[
          StyleSheet.absoluteFill,
          {
            borderRadius: radius,
            backgroundColor: style.tint,
            borderWidth: 1,
            borderColor: 'rgba(255,255,255,0.16)',
            // Specular rim: brighter on top, like light catching the edge of glass.
            borderTopColor: 'rgba(255,255,255,0.6)',
          },
        ]}
      />
      {children}
    </View>
  );
}
