# Pruebas mobile: barra de navegación "glass" en Android

Pregunta: ¿se puede replicar en Android la barra de navegación de cristal de iPhone (iOS 15+ "material" translúcido, iOS 26 "Liquid Glass")?

**Respuesta corta: sí, con límites.** Hay tres niveles, y esta app los pone uno junto a otro para compararlos.

## Qué hay en la app

Una barra flotante tipo píldora sobre contenido muy colorido que se desplaza. Arriba, un selector cambia entre:

| Variante | Qué hace | Mínimo |
|---|---|---|
| 1 Translúcido | Relleno con alfa + borde. No desenfoca nada. Es lo que se ve en Android < 12. | API 26 |
| 2 Blur real | Desenfoca de verdad lo que hay *detrás* de la barra y sube la saturación, como iOS. | API 31 (Android 12) |
| 3 Liquid | Blur ligero + refracción en los bordes y dispersión cromática (shader AGSL). Se parece al Liquid Glass de iOS 26. | API 33 (Android 13) |

En pantalla aparece el nivel de API del dispositivo y qué efectos soporta. En dispositivos más antiguos las variantes 2 y 3 caen a la 1.

## Cómo funciona (y por qué no es trivial)

Android **no tiene** un "desenfoca lo que hay detrás de mí" para vistas dentro de una ventana:

- `Modifier.blur` / `View.setRenderEffect` desenfocan el *propio contenido* del nodo, no su fondo.
- `Window.setBackgroundBlurRadius` / `blurBehind` solo funcionan para ventanas enteras (diálogos, ventanas flotantes), no para una barra dentro de tu pantalla.

La solución (`GlassBackdrop.kt`):

1. El contenido de la pantalla se graba en un `GraphicsLayer` (`backdropSource`).
2. La barra, que se dibuja después, graba en su propio `GraphicsLayer` la porción de ese contenido que tiene debajo, le aplica un `RenderEffect` (blur, y en la variante 3 un shader AGSL encadenado) y lo recorta con su forma.
3. Encima pinta tinte y un borde con brillo especular (más claro arriba).

`LiquidGlassShader.kt` es el shader: calcula la distancia al borde de la píldora y desplaza la muestra hacia el interior cuanto más cerca del borde, separando ligeramente R/G/B.

## Estado: SIN COMPILAR NI PROBAR

Este código se escribió en un entorno sin Android SDK (la descarga está bloqueada por la red) y **no se ha compilado ni ejecutado**. Las APIs (`GraphicsLayer.record`, `renderEffect`, `RuntimeShader`) se han escrito según la documentación y se contrastó el patrón `record`/`drawLayer` con el código fuente de Haze. Puede haber errores de compilación o visuales que haya que ajustar. Cosas concretas a comprobar en un dispositivo:

- Que el blur se actualice mientras haces scroll bajo la barra (la barra no se invalida con el scroll; depende de que el `RenderNode` grabado se refresque). Si va a saltos, la salida es añadir un contador de versión como hace Haze.
- Que el shader AGSL compile (si falla, lanza excepción en la variante 3).
- Rendimiento: la variante 3 aplica blur + shader en cada fotograma.

## Cómo ejecutarlo

Abrir la carpeta en Android Studio (AGP 8.10.1, Kotlin 2.0.0, igual que `glyph-studio`) y ejecutar en un móvil o emulador. A diferencia de las apps de Glyph, **no necesita un Nothing Phone**: funciona en cualquier Android 12+ (la refracción, en 13+). Para probar el fallback, usa un emulador API 26-30.

## Si el resultado gusta: alternativas ya hechas

- [Haze](https://github.com/chrisbanes/haze): blur de fondo para Compose, muy mantenido. Resuelve el refresco, rendimiento y fallback en API < 31 (scrim translúcido). Es la opción más segura para producción si basta con blur.
- [Backdrop (Kyant0)](https://github.com/Kyant0/AndroidLiquidGlass): Liquid Glass para Compose con refracción AGSL. Más cercano a iOS 26, API 33+.

## Límites frente a iOS

- Menos de API 31: no hay blur real posible con APIs públicas; solo translucidez.
- El shader de refracción necesita API 33; en el resto no existe.
- iOS lo hace con el compositor del sistema (barato); aquí cada superficie de cristal es una pasada extra de GPU. Una barra es asumible; muchas superficies de cristal a la vez, no tanto.
- La barra de gestos/navegación del sistema no se puede hacer de cristal: se puede dejar transparente (edge-to-edge) y poner debajo nuestra barra.
