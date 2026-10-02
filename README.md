# Pruebas mobile

Pruebas para replicar en Android la barra de pestañas de cristal de iPhone (iOS 26 "Liquid Glass"): una píldora translúcida con las pestañas, la seleccionada resaltada en azul, y un botón circular `+` separado.

| Carpeta | Stack | Estado |
|---|---|---|
| [`01-kotlin-compose`](01-kotlin-compose) | Kotlin + Compose, barra hecha a mano | Escrita, sin compilar |
| [`02-kotlin-native-bar`](02-kotlin-native-bar) | Kotlin + Compose, `NavigationBar` estándar de Material 3 con el mismo cristal detrás | Escrita, sin compilar |
| [`03-flutter`](03-flutter) | Flutter (Android + iPhone) | Analizada, con tests y probada en web; la refracción sin ver en móvil |
| [`04-react-native`](04-react-native) | React Native (Expo) | Compila y probada en web; blur nativo en móvil sin ver, sin refracción |

Cada carpeta es un proyecto independiente (se abre por separado en Android Studio). Las dos de Kotlin tienen distinto `applicationId`, así que se instalan a la vez en el mismo móvil para compararlas.

Las dos de Kotlin se escribieron en un entorno sin Android SDK y no se han compilado ni ejecutado. La de Flutter sí se ha analizado, probado con tests y ejecutado en web. Ver el README de cada carpeta para lo concreto que hay que comprobar.

## Capturas

En [`capturas/`](capturas) hay una comparativa de Flutter y React Native (reposo, presionada, arrastrando, soltada). Son capturas **de la versión web** de cada una, no de un móvil: en web no hay Impeller, así que la refracción de Flutter no aparece, y el scroll de fondo no es idéntico entre las dos, por lo que los colores bajo la barra difieren. Las dos de Kotlin no tienen capturas porque no se han podido compilar sin Android SDK.
