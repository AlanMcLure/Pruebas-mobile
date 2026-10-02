# 02 · Kotlin con la barra estándar de Material 3

Misma app y mismo efecto de cristal que [`01-kotlin-compose`](../01-kotlin-compose) (ver su README para cómo funciona el blur y la refracción). La diferencia: aquí las pestañas son el componente estándar `NavigationBar` / `NavigationBarItem` de Material 3, con el contenedor transparente y el cristal dibujado detrás.

Qué comparar con la 01:

- Qué hereda gratis del componente estándar: indicador de selección, ripple, accesibilidad, tamaño.
- Qué no encaja: `NavigationBar` está pensada como barra de ancho completo y de 80 dp de alto, no como píldora flotante; la hemos recortado con el cristal, pero su layout interno sigue siendo el de Material.
- El indicador de selección de Material es una píldora detrás del icono (no abarca el texto como en iOS).

Mismas limitaciones y el mismo estado: **sin compilar ni probar**. `applicationId` distinto (`com.alanmclure.glassnav.nativebar`) para instalarla junto a la 01.
