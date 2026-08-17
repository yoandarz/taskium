package com.taskium.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class AlarmScheduler {
    private AlarmScheduler() {}

    public static final class Result {
        public int enabled = 0;
        public int scheduled = 0;
        public long nextWhen = 0L;
        public boolean exactAllowed = false;
        public String error = "";

        public String summary() {
            String next = nextWhen > 0
                ? new SimpleDateFormat("EEE HH:mm", Locale.getDefault()).format(new Date(nextWhen))
                : "ninguna";
            String base = scheduled + " programada" + (scheduled == 1 ? "" : "s") + " · próxima " + next;
            if (!error.isEmpty()) base += " · error " + error;
            return base;
        }
    }

    public static Result scheduleAll(Context context, String json) {
        Result result = new Result();
        try {
            JSONObject state = new JSONObject(json);
            JSONObject settings = state.getJSONObject("settings");
            JSONArray days = settings.getJSONArray("alarmsByDay");
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            result.exactAllowed = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();

            for (int di = 0; di < 7; di++) {
                JSONArray slots = days.getJSONObject(di).getJSONArray("slots");
                for (int si = 0; si < 3; si++) {
                    int requestCode = 2100 + di * 10 + si;
                    PendingIntent pi = pending(context, requestCode, false);
                    am.cancel(pi);

                    JSONObject slot = slots.getJSONObject(si);
                    if (!slot.optBoolean("enabled", false)) continue;
                    result.enabled++;

                    long when = next(di, slot.optInt("hour", 9), slot.optInt("minute", 0));
                    try {
                        if (result.exactAllowed) {
                            PendingIntent show = showApp(context, requestCode);
                            am.setAlarmClock(new AlarmManager.AlarmClockInfo(when, show), pi);
                        } else {
                            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
                        }
                        result.scheduled++;
                        if (result.nextWhen == 0L || when < result.nextWhen) result.nextWhen = when;
                    } catch (Exception e) {
                        if (result.error.isEmpty()) result.error = e.getClass().getSimpleName() + ": " + safeMessage(e);
                    }
                }
            }
        } catch (Exception e) {
            result.error = e.getClass().getSimpleName() + ": " + safeMessage(e);
        }
        saveResult(context, result);
        return result;
    }

    public static boolean scheduleDiagnostic(Context context, long delayMs) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            boolean exactAllowed = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
            if (!exactAllowed) return false;
            long when = System.currentTimeMillis() + Math.max(15_000L, delayMs);
            PendingIntent pi = pending(context, 2999, true);
            am.cancel(pi);
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            context.getSharedPreferences("taskium", Context.MODE_PRIVATE).edit()
                .putLong("diagnostic_when", when)
                .apply();
            return true;
        } catch (Exception e) {
            context.getSharedPreferences("taskium", Context.MODE_PRIVATE).edit()
                .putString("diagnostic_error", e.getClass().getSimpleName() + ": " + safeMessage(e))
                .apply();
            return false;
        }
    }

    private static PendingIntent pending(Context c, int requestCode, boolean diagnostic) {
        Intent i = new Intent(c, AlarmReceiver.class)
            .setAction(diagnostic ? "TASKIUM_DIAGNOSTIC" : "TASKIUM_ALARM_" + requestCode)
            .putExtra("diagnostic", diagnostic);
        return PendingIntent.getBroadcast(c, requestCode, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent showApp(Context c, int requestCode) {
        Intent i = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, 9000 + requestCode, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static long next(int mondayIndex, int hour, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        int target = ((mondayIndex + 1) % 7) + 1; // Sun=1, Mon=2 ...
        int add = (target - now.get(Calendar.DAY_OF_WEEK) + 7) % 7;
        c.add(Calendar.DAY_OF_YEAR, add);
        if (c.getTimeInMillis() <= now.getTimeInMillis()) c.add(Calendar.DAY_OF_YEAR, 7);
        return c.getTimeInMillis();
    }

    private static void saveResult(Context c, Result r) {
        c.getSharedPreferences("taskium", Context.MODE_PRIVATE).edit()
            .putInt("diag_enabled", r.enabled)
            .putInt("diag_scheduled", r.scheduled)
            .putLong("diag_next_when", r.nextWhen)
            .putBoolean("diag_exact", r.exactAllowed)
            .putString("diag_error", r.error)
            .putLong("diag_last_schedule", System.currentTimeMillis())
            .apply();
    }

    private static String safeMessage(Exception e) {
        String m = e.getMessage();
        return m == null ? "sin detalle" : m.replace('\n', ' ');
    }
}
