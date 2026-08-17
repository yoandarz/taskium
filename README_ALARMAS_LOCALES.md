# Taskium 2.1.0 — alarmas locales

Taskium separa ahora dos responsabilidades:

- Supabase sincroniza tareas, ajustes y horarios entre dispositivos.
- El dispositivo instalado programa y dispara sus propias alarmas localmente.

## Windows
Instala `TaskiumAlarmBridge.exe` una vez. Taskium web/PWA le envía una copia local de tareas y alarmas por `localhost`. El Bridge registra los horarios en el Programador de tareas de Windows y muestra una ventana de Taskium cuando corresponde. No necesita Internet para disparar una alarma ya registrada.

## Android
La aplicación nativa de Taskium carga la web publicada y expone un puente JavaScript. Al sincronizar o cambiar una alarma, Android registra alarmas exactas con AlarmManager y muestra una notificación local. No usa el cron de Supabase para sonar.

## Web pura
La página web puede seguir usándose para tareas y sincronización, pero una pestaña/PWA web por sí sola no garantiza alarmas locales exactas con la aplicación cerrada. Para alarmas fiables usa el Bridge de Windows o la app Android.
