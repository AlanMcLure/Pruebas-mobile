# Pruebas mobile: barra de pestañas "glass" de iOS 26 en Android

Cuatro pruebas independientes para saber si se puede replicar la barra de pestañas de cristal de iPhone (iOS 26 "Liquid Glass") fuera de iOS, y con qué stack sale mejor. No es un producto: es un banco de pruebas. Este archivo es la guía para cualquier agente o persona que trabaje aquí. Resumen para humanos y estado de cada prueba: `README.md`.

## Qué se replica

Una píldora de cristal con 4 pestañas (Dashboard, Diary, Library, Settings) más un botón circular `+` aparte, ambos de cristal. Comportamiento:

- **Reposo:** cápsula plana tras la pestaña seleccionada.
- **Presionar** en cualquier punto de la barra: la cápsula se levanta como una lente de cristal más grande.
- **Arrastrar:** la lente sigue al dedo con un resorte; la pestaña bajo ella se resalta en azul en vivo y vibra con cada cambio.
- **Soltar:** la lente rebota hasta la pestaña más cercana y esa se selecciona. Un toque simple es el mismo gesto sin arrastre.

Tres variantes en cada app, con un selector arriba: **1 Translúcido** (solo alfa), **2 Blur real** (blur del fondo), **3 Lente/Liquid** (blur ligero + refracción donde el stack lo permite).

## Estructura

Cada carpeta es un proyecto independiente que se abre por separado. No hay build en la raíz.

```
01-kotlin-compose/     Kotlin + Compose, barra hecha a mano
02-kotlin-native-bar/  Kotlin + Compose, NavigationBar estándar de Material 3 con el mismo cristal
03-flutter/            Flutter (Android + iPhone)
04-react-native/       React Native con Expo (SDK 57)
capturas/              Capturas de la versión WEB de 03 y 04 (no de un móvil)
```

Las dos de Kotlin tienen distinto `applicationId` (`com.alanmclure.glassnav` y `.nativebar`) para instalarse juntas.

## Qué se puede y qué no (leer antes de afirmar nada)

La regla principal de este repo: **no digas que algo funciona si no lo has visto funcionar**. Cada README tiene una sección "Estado" que separa lo verificado de lo no verificado; mantenla honesta cuando cambies algo.

| Prueba | Compilada/ejecutada aquí | Cómo |
|---|---|---|
| 01 y 02 Kotlin | **No.** | El Android SDK no se puede descargar en el entorno de CI (`dl.google.com` devuelve 403, y Google Maven redirige ahí). Todo lo de Kotlin se escribió contra la documentación y está **sin compilar ni probar**. |
| 03 Flutter | Análisis, tests y web. | `flutter analyze`, `flutter test` y una compilación web probada con Chromium. El shader GLSL compila con `impellerc` (`flutter build bundle`). |
| 04 React Native | Tipos y web. | `npx tsc --noEmit` y `npx expo export --platform web`, probado con Chromium. |

Lo que web **no** puede verificar: la refracción de Flutter (necesita Impeller, que web no tiene), el blur nativo de Android e iOS, la vibración, el rendimiento y la sensación real del gesto. Las capturas de `capturas/` son web y los fondos no son idénticos entre stacks; no las presentes como capturas de un móvil.

Si el entorno bloquea un host (`403`/`EGRESS_BLOCKED`), no lo rodees: dilo y trabaja con lo local (ver trampas).

## Comandos

```
# 01 y 02 (Kotlin): abrir en Android Studio. Desde terminal, sin verificar en CI:
./gradlew assembleDebug

# 03 Flutter
cd 03-flutter
flutter pub get
flutter analyze
flutter test
flutter run                        # móvil Android conectado, o iPhone con Xcode (solo Mac)

# 04 React Native (Expo)
cd 04-react-native
npm install
npx tsc --noEmit
npx expo export --platform web     # comprobar que compila
npx expo start                     # Expo Go o web
npx expo run:android               # development build; expo-blur y Reanimated llevan código nativo
```

Verificar visualmente en web (lo que se hizo para las capturas):

- **Flutter:** `web/` no está en el repo. Copia `lib/`, `shaders/` y `pubspec.yaml` a una carpeta temporal, ejecuta `flutter create --platforms=web .` y `flutter build web --release --no-web-resources-cdn`.
- **React Native:** `npx expo export --platform web` y sirve `dist/` con `python3 -m http.server`.
- Captura con Playwright (Chromium está en `/opt/pw-browsers/chromium`): viewport 390×800, `deviceScaleFactor: 2`, baja el scroll con `mouse.wheel`, presiona la primera pestaña con `mouse.down()`, arrastra con `mouse.move()` y suelta con `mouse.up()`.
- No dejes `dist/`, `build/` ni `node_modules/` en el commit.

## Mantener las cuatro en sincronía

Cuando cambies el aspecto o el gesto, cámbialo en **las cuatro** (o di cuál queda atrás y por qué). Valores actuales:

- **Pestaña seleccionada/hovered:** azul `#4C8DFF`; las demás, blanco.
- **Cápsula de reposo:** blanco al 20 %. **Lente:** crece 10 % en X y 18 % en Y; la pestaña bajo ella, un 8 %.
- **Tintes por variante** (sobre `#14171C` / `#0E1116`): Translúcido ~50 %, Blur ~20 %, Lente ~16 %. **Saturación** ~1,3. **Blur** en la variante 2: Compose 10 dp, Flutter sigma 8, React Native intensidad 40. En la variante 3 es menor: Compose 3 dp, Flutter sigma 2, React Native intensidad 30.
- **Brillo:** degradado blanco 16 % → transparente desde arriba, más un borde de 1 px más claro arriba que abajo.
- **Resortes** (masa 1): seguir al dedo, rigidez 600 y amortiguación 0,85; asentarse, 350 y 0,7; presionar, 400 y 0,6.
- **Iconos:** un conjunto coherente por stack, con contorno en reposo y relleno en la pestaña activa (como iOS): Flutter `CupertinoIcons`, React Native Ionicons, Kotlin Material Outlined/Filled.
- **La lente va por encima de los iconos** en Flutter y React Native (los refracta). En Kotlin hoy solo refracta el fondo; es una diferencia conocida, no un bug.
- **Accesibilidad:** cada pestaña mantiene semántica de pestaña con acción de clic, porque la capa de gestos se queda con el toque.

## Qué da cada stack

| | Blur del fondo | Refracción | Gesto |
|---|---|---|---|
| Kotlin/Compose | Hecho a mano: grabar el contenido en un `GraphicsLayer` y dibujar la parte de debajo con `RenderEffect` (API 31+). | Shader AGSL (API 33+). | `LiquidTabs.kt`, independiente de cómo se dibujen las pestañas. |
| Flutter | `BackdropFilter`, un widget. | `ImageFilter.shader` con GLSL, **solo con Impeller**; si no, cae a lente sin refracción. | `LiquidTabBar` con `Listener` y `SpringSimulation`. |
| React Native | `expo-blur` (nativo en iOS/Android, CSS en web). | **No es posible** sin pasar toda la pantalla a Skia o escribir un módulo nativo. | Reanimated + Gesture Handler. |

## Trampas conocidas

- **Compose:** el blur necesita que la superficie se dibuje *después* del contenido (hermano posterior en un `Box`). La barra no se invalida al hacer scroll; que el blur se actualice con scroll es la duda principal sin comprobar. En 02, `NavigationBar` no tiene "presionar y arrastrar": la capa de gestos va encima y sustituye su indicador.
- **Flutter:** no envolver una superficie de cristal en `Opacity` (crea una capa aislada y el fondo queda vacío). `ImageFilter.shader` exige que el primer uniform sea un `vec2` con el tamaño de la textura y que haya un `sampler2D`; los tamaños van en píxeles físicos (multiplica por `devicePixelRatio`). El filtro compara el shader por identidad, así que si cambian los uniforms hay que crear otro shader. En GLES el eje Y se invierte (el shader lo compensa, sin comprobar). Una etiqueta que ocupa dos líneas desborda la pestaña: etiquetas en una línea.
- **React Native:** en Android, el contenido a desenfocar va dentro de un `<BlurTargetView>` y cada `<BlurView>` recibe su `ref` en `blurTarget`; el cristal es *hermano* del target, no hijo. Sin eso cae a translúcido. En web, `tint="dark"` pinta un gris casi opaco; por eso se usa `systemUltraThinMaterialDark`. `expo-blur` y Reanimated tienen código nativo: Expo Go no basta para ver el blur en Android.
- **React Native, Expo:** `docs.expo.dev` y los servicios de `expo install` están bloqueados en CI. Las versiones compatibles salen de `node_modules/expo/bundledNativeModules.json` y la API de cada paquete se lee de sus tipos en `node_modules/<paquete>/build` o `src`. Con red, ejecuta `npx expo install --check` y `npx expo-doctor`. No crees `ios/` ni `android/` a mano (Continuous Native Generation). `04-react-native/AGENTS.md` es el de la plantilla de Expo; léelo también al trabajar ahí.
- **Contraste:** el azul seleccionado se lee mal sobre fondos verdes/cian brillantes (el cristal de iOS se adapta al contenido; el nuestro no). Es una limitación conocida de la demo, no del gesto.
- **Shell:** no uses `pkill -f "<patrón>"` si el patrón aparece en tu propio comando: mata tu shell. Guarda el PID al arrancar el servidor y usa `kill <pid>`.
- **Capturas:** mira siempre la captura ampliada antes de juzgar el blur; a tamaño pequeño los errores no se ven.

## Reglas de trabajo

- Responde en español. El código, los nombres de archivo y los mensajes de commit van en inglés; los comentarios del código, en inglés.
- Los commits terminan con la línea de atribución que indique el entorno.
- No crees una pull request salvo que te lo pidan.
- No inventes datos ni resultados: si algo no se ha podido ejecutar, dilo (en el README y al usuario).
- Cada prueba es independiente: no compartas código entre carpetas; la duplicación entre 01 y 02 es deliberada.
- Antes de cambiar una API de Expo, Flutter o Compose, comprueba su firma real (tipos instalados, fuentes del SDK) en vez de fiarte de la memoria.

## Pendiente

Ejecutar 01 y 02 en un Android real (nunca se han compilado); ver la refracción de 03 en un móvil con Impeller; ver el blur nativo de 04 en Android e iOS; medir rendimiento; ajustar el contraste del azul seleccionado o cambiar el fondo de la demo por uno oscuro como la captura de referencia de iPhone; decidir cuál de los cuatro stacks seguir.
