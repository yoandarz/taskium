# Taskium Android 2.1.3

Versión nativa final del motor local de alarmas de Taskium.

Cambios principales:
- conserva AlarmManager y las alarmas locales ya validadas en Android;
- usa el icono real de Taskium, tomado de los recursos de la PWA;
- respeta la barra de estado y el recorte de pantalla en Android 15 / SDK 35;
- mantiene la vista directa de tareas pendientes al tocar una alarma;
- elimina la prueba automática de diagnóstico de 60 segundos al activar alarmas.
