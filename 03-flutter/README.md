# 03 · Flutter (Android + iPhone)

La misma barra de cristal de iOS 26 que en `01` y `02`, pero con Flutter: un solo código para Android e iPhone.

## Qué hay

| Archivo | Qué es |
|---|---|
| `lib/glass_surface.dart` | `GlassSurface`: blur del fondo + tinte + borde con brillo. En Flutter el blur de fondo es un widget (`BackdropFilter`), sin grabar capas a mano como en Compose. |
| `lib/liquid_tab_bar.dart` | `LiquidTabBar`: la píldora con el gesto de iOS (presionar, arrastrar con resorte, soltar y encajar, vibración al cambiar de pestaña). |
| `shaders/liquid_glass.frag` | Shader GLSL de la lente: refracción en el borde y dispersión cromática. |
| `lib/main.dart` | Demo: contenido de colores con scroll, selector de las 3 variantes y el botón `+`. |

Variantes (igual que en Kotlin): 1 Translúcido, 2 Blur real, 3 Liquid (refracción).

## Interacción

- **Reposo:** cápsula plana tras la pestaña seleccionada.
- **Presionar** en cualquier punto de la barra: la cápsula pasa a ser una lente de cristal que crece y refracta el fondo **y los iconos de la propia barra** (la lente se dibuja por encima de ellos, así que, a diferencia de la versión Kotlin, aquí sí los dobla como iOS).
- **Arrastrar:** la lente sigue al dedo con un resorte; la pestaña bajo ella se resalta en azul en vivo y vibra con cada cambio.
- **Soltar:** rebota hasta la pestaña más cercana y la selecciona. Tocar sin mover es el mismo gesto.

## Requisitos de plataforma

- **Blur y translucidez:** cualquier plataforma (también el motor Skia antiguo y web).
- **Refracción (variante 3):** necesita **Impeller** (`ImageFilter.shader`), que es el motor por defecto en iOS y en Android moderno. Si no está disponible, la variante 3 cae a una lente sin refracción (la pantalla muestra `Impeller / shader / refracción: sí|no`).
- Trampa conocida: no envolver una superficie de cristal en `Opacity`; crea una capa aislada y el fondo queda vacío. Por eso la lente aparece sin fundido.

## Cómo ejecutarlo

`android/` e `ios/` ya están creadas (`flutter create`). Con el SDK de Flutter instalado:

```
cd 03-flutter
flutter pub get
flutter run          # móvil Android conectado, o iPhone con Xcode (solo en Mac)
flutter test
```

## Estado: qué está verificado y qué no

Verificado en un entorno sin dispositivo (Flutter 3.47.6):

- `flutter analyze`: sin problemas.
- `flutter test`: 4 pruebas pasan (tocar una pestaña; presionar, arrastrar y soltar selecciona la de debajo del dedo y no confirma nada mientras el dedo está abajo; soltar en la pestaña ya seleccionada no cambia nada; semántica de botón/etiqueta para lectores de pantalla).
- El shader GLSL **compila** con `impellerc` (`flutter build bundle`).
- Compilado para web y probado en Chromium: la variante de blur, la lente al arrastrar y el cambio de pestaña se ven y funcionan. Eso detectó un defecto real (la lente desenfocaba los iconos), ya corregido.

**No verificado** (necesita un móvil con Impeller; web no lo tiene):

- La **refracción** y la dispersión cromática del shader: compila, pero no se ha visto renderizada. Es lo primero a mirar. Si se viera invertida en vertical, sería el eje Y de OpenGL ES (el shader lo compensa en esa ruta, pero no está comprobado).
- La sensación del gesto (valores del resorte, tamaño de la lente) y la vibración: elegidos a ojo, sin compararlos con una barra real de iPhone.
- Rendimiento en un móvil real.

## Alternativa ya hecha

[`liquid_glass_renderer`](https://pub.dev/packages/liquid_glass_renderer) (versión `0.2.0-dev` en el momento de escribir esto) implementa Liquid Glass para Flutter. No lo he probado; la pongo como punto de comparación si el shader propio se queda corto.
