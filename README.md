# Taskium 2.0.1

PWA instalable para Windows y móvil, construida a partir de Taskium de escritorio y del patrón técnico probado en GymLedger 2.0.8.

## Funciones
- Tareas puntuales: permanecen hasta marcarlas como hechas.
- Tareas recurrentes acumulables: cada N días nace una unidad de deuda desde una fecha de origen fija.
- Pagar deuda no mueve el calendario de recurrencia.
- Prioridad manual mediante subir/bajar.
- Hasta tres alarmas por día de la semana y posposición de 1 a 60 minutos.
- IndexedDB local y funcionamiento offline.
- Sincronización opcional con Supabase mediante cuenta de usuario.
- Web Push para alertas aunque la PWA esté cerrada, una vez concedido permiso y sincronizados los ajustes.
- Tema claro/oscuro.
- Importación de JSON del Taskium de escritorio.

## Prueba local
Debe servirse por HTTP/HTTPS (no abrir index.html con file://). Por ejemplo:

    python -m http.server 8000

Luego abrir http://localhost:8000/taskium2/ si la carpeta está bajo el directorio servido, o servir directamente esta carpeta.

## Publicación
El contenido de esta carpeta puede publicarse directamente en GitHub Pages. Incluye `.nojekyll`, `manifest.webmanifest` y `service-worker.js`.

## Supabase
Esta compilación apunta al proyecto ya configurado `GigPlan` y usa exclusivamente recursos `taskium_*`. La clave incluida es publicable, apta para cliente web; no contiene `service_role`.

## Alertas
Para recibir alertas con la app cerrada:
1. Iniciar sesión en Taskium.
2. Pulsar `Activar notificaciones` y conceder permiso.
3. Configurar al menos una alarma y sincronizar.
4. Debe existir al menos una tarea puntual o deuda recurrente pendiente cuando llegue la hora.

En iPhone/iPad, las notificaciones web requieren instalar la PWA en la pantalla de inicio y una versión compatible de iOS/iPadOS.
