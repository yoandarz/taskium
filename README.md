# Taskium 2.1.0

PWA offline-first con Supabase para sincronización de datos y **alarmas locales nativas** en los dispositivos instalados.

## Arquitectura

- IndexedDB mantiene la copia local y permite trabajar sin conexión.
- Supabase sincroniza tareas, ajustes y horarios entre dispositivos.
- Windows usa `TaskiumAlarmBridge.exe` + Programador de tareas de Windows.
- Android usa la app nativa de Taskium + `AlarmManager`.
- La hora de una alarma ya no depende de que un cron de Supabase coincida exactamente con ese minuto.

## Publicación web

Sube el contenido de la carpeta `taskium2` a GitHub Pages como en las versiones anteriores.

La página web pura sigue sirviendo para tareas y sincronización, pero para alarmas locales fiables con la aplicación cerrada instala la pieza nativa correspondiente al dispositivo.

## Windows

Ejecuta una vez `TaskiumAlarmBridge.exe`. Se copia a `%LOCALAPPDATA%\Taskium`, se inicia automáticamente al entrar en Windows y escucha solo en `127.0.0.1:51337`.

Taskium le envía una copia de las tareas y horarios cuando cambian o se sincronizan. El Bridge registra los horarios en el Programador de tareas de Windows y, cuando toca, calcula la deuda localmente y abre una ventana de Taskium.

## Android

La app Android carga la web publicada de Taskium y expone un puente nativo. Al cambiar o sincronizar horarios, Android registra las alarmas en `AlarmManager`. El receptor calcula la deuda localmente cuando dispara la alarma.

## Nota

Cada dispositivo usa la última copia que haya sincronizado. Supabase sigue siendo la fuente común de datos, pero **no participa en el disparo de una alarma ya registrada localmente**.
