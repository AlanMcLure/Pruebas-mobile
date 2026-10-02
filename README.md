# Pruebas mobile

Pruebas para replicar en Android la barra de pestañas de cristal de iPhone (iOS 26 "Liquid Glass"): una píldora translúcida con las pestañas, la seleccionada resaltada en azul, y un botón circular `+` separado.

| Carpeta | Stack | Estado |
|---|---|---|
| [`01-kotlin-compose`](01-kotlin-compose) | Kotlin + Compose, barra hecha a mano | Escrita, sin compilar |
| [`02-kotlin-native-bar`](02-kotlin-native-bar) | Kotlin + Compose, `NavigationBar` estándar de Material 3 con el mismo cristal detrás | Escrita, sin compilar |
| `03-flutter` | Flutter (Android + iPhone) | Pendiente |
| `04-react-native` | React Native | Pendiente |

Cada carpeta es un proyecto independiente (se abre por separado en Android Studio). Las dos de Kotlin tienen distinto `applicationId`, así que se instalan a la vez en el mismo móvil para compararlas.

Ninguna se ha compilado ni ejecutado todavía: se escribieron en un entorno sin Android SDK. Ver el README de cada carpeta para lo concreto que hay que comprobar.
