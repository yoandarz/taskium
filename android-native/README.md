# Taskium Android 2.1.1

Aplicación Android nativa ligera de Taskium. La interfaz se carga exclusivamente desde https://yoandarz.github.io/taskium/ y el puente nativo solo queda disponible en esa WebView.

Las alarmas se guardan localmente con AlarmManager. En Android 12+ se solicita acceso especial a Alarmas y recordatorios; en Android 13+ también se solicita permiso de notificaciones. Al conceder el acceso exacto, las alarmas se reprograman automáticamente.

Si no hay tareas pendientes en el momento de la alarma, Taskium no muestra aviso.
