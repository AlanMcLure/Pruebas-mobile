# 02 · Kotlin con la barra estándar de Material 3

Misma app y mismo efecto de cristal que [`01-kotlin-compose`](../01-kotlin-compose) (ver su README para cómo funciona el blur y la refracción). La diferencia: aquí las pestañas son el componente estándar `NavigationBar` / `NavigationBarItem` de Material 3, con el contenedor transparente y el cristal dibujado detrás.

Qué comparar con la 01:

- Qué hereda gratis del componente estándar: indicador de selección, ripple, accesibilidad, tamaño.
- Qué no encaja: `NavigationBar` está pensada como barra de ancho completo y de 80 dp de alto, no como píldora flotante; la hemos recortado con el cristal, pero su layout interno sigue siendo el de Material.
- `NavigationBar` **no** tiene "presionar y arrastrar para cambiar de pestaña". Se lo añadimos con la misma capa de gestos de la 01 (`LiquidTabs.kt`) por encima: su indicador sustituye al de Material y los items estándar solo siguen a la pestaña "hovered". Esto es lo que más interesa comparar: cuánto del componente estándar se mantiene al forzarle el comportamiento de iOS.
- Los items de Material van con 8 dp de separación, así que el indicador puede desalinearse unos pocos dp respecto al centro de cada pestaña.

Mismas limitaciones y el mismo estado: **sin compilar ni probar**. `applicationId` distinto (`com.alanmclure.glassnav.nativebar`) para instalarla junto a la 01.
