package com.taskium.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.os.Build;
import android.provider.Settings;
import androidx.core.app.NotificationCompat;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL = "taskium_alarm";

    @Override public void onReceive(Context context, Intent intent) {
        boolean diagnostic = intent != null && intent.getBooleanExtra("diagnostic", false);
        if (diagnostic) {
            context.getSharedPreferences("taskium", Context.MODE_PRIVATE).edit()
                .putLong("diagnostic_fired_at", System.currentTimeMillis())
                .apply();
            show(context, "Prueba nativa correcta. Android ha disparado la alarma local de Taskium.");
            return;
        }

        String json = context.getSharedPreferences("taskium", Context.MODE_PRIVATE).getString("state", "");
        if (json.isEmpty()) return;
        String body = pendingText(json);
        if (body == null) {
            AlarmScheduler.scheduleAll(context, json);
            return;
        }
        show(context, body);
        AlarmScheduler.scheduleAll(context, json);
    }

    private void show(Context context, String body) {
        createChannel(context);
        Intent full = new Intent(context, AlarmActivity.class)
            .putExtra("body", body)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent fullPi = PendingIntent.getActivity(context, 777, full, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Taskium")
            .setContentText(body.split("\\n")[0])
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(fullPi)
            .setFullScreenIntent(fullPi, true);
        ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE))
            .notify((int)(System.currentTimeMillis()%100000), b.build());
    }

    private void createChannel(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Alarmas de Taskium", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Alarmas locales de tareas pendientes");
            ch.enableVibration(true);
            ch.setSound(Settings.System.DEFAULT_ALARM_ALERT_URI, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
            ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }

    static String pendingText(String json) {
        try {
            JSONObject state = new JSONObject(json);
            JSONArray tasks = state.getJSONArray("tasks");
            StringBuilder lines = new StringBuilder();
            int total = 0;
            LocalDate today = LocalDate.now();
            for (int i = 0; i < tasks.length(); i++) {
                JSONObject t = tasks.getJSONObject(i);
                String kind = t.optString("taskKind", "one_time");
                String text = t.optString("text", "Tarea");
                if ("one_time".equals(kind)) {
                    total++;
                    lines.append("• ").append(text).append("\n");
                    continue;
                }
                LocalDate anchor;
                try { anchor = LocalDate.parse(t.optString("anchorDate", today.toString())); }
                catch (Exception e) { anchor = today; }
                int interval = Math.max(1, t.optInt("intervalDays", 1));
                long days = Math.max(0, ChronoUnit.DAYS.between(anchor, today));
                int generated = 1 + (int)(days / interval);
                int debt = Math.max(0, generated - Math.max(0, t.optInt("completedCount", 0)));
                if (debt > 0) {
                    total += debt;
                    lines.append("• ").append(text).append(" — ").append(debt)
                        .append(debt == 1 ? " pendiente" : " pendientes").append("\n");
                }
            }
            if (total == 0) return null;
            return "Tienes " + total + (total == 1 ? " pendiente." : " pendientes.") + "\n\n" + lines;
        } catch (Exception e) {
            return "Hay tareas pendientes en Taskium.";
        }
    }
}
