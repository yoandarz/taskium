package com.taskium.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AlarmActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        if (android.os.Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true); }
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(48,64,48,48); box.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=new TextView(this); title.setText("⏰ Taskium"); title.setTextSize(28); box.addView(title);
        TextView text=new TextView(this); text.setText(getIntent().getStringExtra("body")); text.setTextSize(18); text.setPadding(0,32,0,40); box.addView(text);
        Button close=new Button(this); close.setText("Cerrar"); close.setOnClickListener(v->finish()); box.addView(close);
        setContentView(box);
    }
}
