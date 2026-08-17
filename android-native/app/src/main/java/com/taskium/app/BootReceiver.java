package com.taskium.app;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if (Build.VERSION.SDK_INT >= 31 && AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(i.getAction())) {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            if (!am.canScheduleExactAlarms()) return;
        }
        String json = c.getSharedPreferences("taskium", Context.MODE_PRIVATE).getString("state", "");
        if (!json.isEmpty()) AlarmScheduler.scheduleAll(c, json);
    }
}
