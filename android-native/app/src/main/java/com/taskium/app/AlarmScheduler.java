package com.taskium.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;

public final class AlarmScheduler {
    private AlarmScheduler() {}

    public static void scheduleAll(Context context, String json) {
        try {
            JSONObject state = new JSONObject(json);
            JSONObject settings = state.getJSONObject("settings");
            JSONArray days = settings.getJSONArray("alarmsByDay");
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            for (int di = 0; di < 7; di++) {
                JSONArray slots = days.getJSONObject(di).getJSONArray("slots");
                for (int si = 0; si < 3; si++) {
                    int requestCode = 2100 + di * 10 + si;
                    PendingIntent pi = pending(context, requestCode);
                    am.cancel(pi);

                    JSONObject slot = slots.getJSONObject(si);
                    if (!slot.optBoolean("enabled", false)) continue;

                    long when = next(di, slot.optInt("hour", 9), slot.optInt("minute", 0));
                    boolean exactAllowed = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
                    if (exactAllowed) {
                        PendingIntent show = showApp(context, requestCode);
                        am.setAlarmClock(new AlarmManager.AlarmClockInfo(when, show), pi);
                    } else {
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static PendingIntent pending(Context c, int requestCode) {
        Intent i = new Intent(c, AlarmReceiver.class).setAction("TASKIUM_ALARM_" + requestCode);
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
}
