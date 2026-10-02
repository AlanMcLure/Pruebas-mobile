@AGENTS.md

## Notas para Claude Code
- La guía completa está en `AGENTS.md`; el estado de cada prueba, en el `README.md` de su carpeta. Léelos antes de cambiar nada.
- Cuando cambies el aspecto o el gesto, hazlo en las cuatro carpetas (ver "Mantener las cuatro en sincronía"). Si una se queda atrás, dilo.
- No presentes como verificado lo que solo está escrito: las dos de Kotlin no se han compilado nunca, y las capturas son de web, no de un móvil.
- Verifica los cambios visuales de Flutter y React Native en web con Chromium (Playwright) y mira la captura **ampliada** antes de dar nada por bueno.
- Si un host está bloqueado por la red del entorno, no lo rodees: usa lo local (tipos en `node_modules`, fuentes del SDK) y dilo.
- Los commits terminan con la línea de atribución que indique el entorno.
- No hagas `push` a otra rama que la indicada ni abras una PR sin que te lo pidan.
- Responde en español. El código, los nombres de archivo y los mensajes de commit van en inglés.
- No dejes `dist/`, `build/` ni `node_modules/` en los commits.
