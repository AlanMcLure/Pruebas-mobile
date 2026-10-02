# 04 · React Native (Expo)

La misma barra de cristal de iOS 26 que en `01`, `02` y `03`, con React Native (Expo SDK 57). Es la que más se queda corta de las cuatro, y aquí está por qué.

## Qué hay

| Archivo | Qué es |
|---|---|
| `src/GlassSurface.tsx` | Superficie de cristal con `expo-blur` (blur nativo del fondo) + tinte + borde con brillo. |
| `src/LiquidTabBar.tsx` | La píldora con el gesto de iOS, hecho con Reanimated y Gesture Handler: presionar, arrastrar con resorte, soltar y encajar, vibración al cambiar de pestaña. |
| `src/DemoContent.tsx` | Contenido de colores con scroll para ver el blur. |
| `App.tsx` | Demo: selector de variantes, barra y botón `+`. |

Variantes: 1 Translúcido, 2 Blur real, 3 Lente (blur + lente que se levanta al presionar, **sin refracción**).

## Qué da React Native y qué no

- **Blur de fondo:** sí, pero no viene con React Native. `expo-blur` envuelve el nativo: `UIVisualEffectView` en iOS, [Dimezis BlurView](https://github.com/Dimezis/BlurView) en Android y `backdrop-filter` de CSS en web.
- **Android pide una estructura concreta:** el contenido a desenfocar debe ir dentro de un `<BlurTargetView>` y cada `<BlurView>` recibe su `ref` en `blurTarget`; el cristal tiene que ser *hermano* del target, no hijo. Sin eso cae a un simple fondo translúcido (y avisa por consola). El modo `dimezisBlurViewSdk31Plus` solo desenfoca de verdad desde Android 12.
- **Refracción: no.** React Native no puede aplicar un shader al fondo de la pantalla. Las vías serían (a) pasar toda la pantalla a [`@shopify/react-native-skia`](https://shopify.github.io/react-native-skia/) y usar un shader allí, o (b) escribir un módulo nativo (AGSL en Android, Metal en iOS). Ninguna es pequeña, así que la variante 3 solo hace crecer y aclarar la lente.
- **Gesto:** sin problema. Es el mismo que en Flutter y Compose.

## Interacción

- **Reposo:** cápsula plana tras la pestaña seleccionada.
- **Presionar** en cualquier punto de la barra: la cápsula se levanta como una lente más grande y más clara.
- **Arrastrar:** la lente sigue al dedo con un resorte; la pestaña bajo ella se resalta en azul en vivo y vibra con cada cambio.
- **Soltar:** rebota hasta la pestaña más cercana y la selecciona. Tocar sin mover es el mismo gesto.

## Cómo ejecutarlo

```
cd 04-react-native
npm install
npx expo start          # abre en el móvil con Expo Go o en web
```

**Importante:** `expo-blur` y Reanimated incluyen código nativo. Para ver el blur real en Android necesitas una *development build* (`npx expo run:android`, o EAS), no basta con Expo Go. En web (`npx expo start --web`) funciona directamente.

## Estado: qué está verificado y qué no

Verificado en un entorno sin móvil:

- `tsc --noEmit`: sin errores.
- `expo export --platform web`: compila.
- Probado en Chromium (web): el blur, el cambio entre variantes, la lente al presionar, el arrastre entre pestañas y la selección al soltar funcionan. Las capturas están en [`../capturas`](../capturas).

**No verificado** (necesita móvil):

- **El blur nativo en Android**, que es lo más delicado: depende de la estructura `BlurTargetView`/`blurTarget`, y el blur de Dimezis tiene sus propias limitaciones de rendimiento. No lo he visto funcionar.
- El blur nativo en iOS.
- La vibración (en web no hay) y la sensación del gesto: los valores del resorte están elegidos a ojo.
- Rendimiento.

Nota: las versiones de los paquetes de Expo se tomaron de `node_modules/expo/bundledNativeModules.json` porque `expo install` y la documentación web de Expo estaban bloqueadas en el entorno donde se escribió. Conviene ejecutar `npx expo install --check` y `npx expo-doctor` con red.
