package com.taskium.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String TASKIUM_URL = "https://yoandarz.github.io/taskium/";
    private static final String TASKIUM_HOST = "yoandarz.github.io";
    private static final int REQ_NOTIFICATIONS = 210;
    private static final String PREF_PERMISSION_FLOW = "permission_flow";
    private static final int STATUS_COLOR = Color.rgb(95, 114, 100);
    private static final int NAV_COLOR = Color.rgb(247, 245, 239);

    private WebView webView;
    private FrameLayout root;
    private View statusBarScrim;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(NAV_COLOR);

        root = new FrameLayout(this);
        root.setBackgroundColor(NAV_COLOR);

        webView = new WebView(this);
        FrameLayout.LayoutParams webParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        root.addView(webView, webParams);

        statusBarScrim = new View(this);
        statusBarScrim.setBackgroundColor(STATUS_COLOR);
        FrameLayout.LayoutParams scrimParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            Gravity.TOP
        );
        root.addView(statusBarScrim, scrimParams);

        setContentView(root);
        configureSystemBars();
        configureWebView();
        webView.loadUrl(TASKIUM_URL);
    }

    private void configureSystemBars() {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), root);
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(true);

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );

            FrameLayout.LayoutParams webLp = (FrameLayout.LayoutParams) webView.getLayoutParams();
            webLp.topMargin = bars.top;
            webLp.bottomMargin = bars.bottom;
            webView.setLayoutParams(webLp);

            FrameLayout.LayoutParams scrimLp = (FrameLayout.LayoutParams) statusBarScrim.getLayoutParams();
            scrimLp.height = bars.top;
            statusBarScrim.setLayoutParams(scrimLp);

            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        if (Build.VERSION.SDK_INT >= 26) s.setSafeBrowsingEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if ("https".equalsIgnoreCase(u.getScheme()) && TASKIUM_HOST.equalsIgnoreCase(u.getHost())) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, u));
                return true;
            }

            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri u = Uri.parse(url);
                if ("https".equalsIgnoreCase(u.getScheme()) && TASKIUM_HOST.equalsIgnoreCase(u.getHost())) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, u));
                return true;
            }
        });
        webView.addJavascriptInterface(new NativeBridge(this), "TaskiumNativeAndroid");
    }

    @Override protected void onResume() {
        super.onResume();
        SharedPreferences p = getSharedPreferences("taskium", Context.MODE_PRIVATE);
        String json = p.getString("state", "");
        if (!json.isEmpty()) AlarmScheduler.scheduleAll(this, json);

        if (p.getBoolean(PREF_PERMISSION_FLOW, false) && permissionsReady()) {
            p.edit().putBoolean(PREF_PERMISSION_FLOW, false).apply();
            Toast.makeText(this, "Alarmas locales activadas en Android.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIFICATIONS) {
            if (notificationsAllowed()) {
                requestExactPermissionOrFinish();
            } else {
                getSharedPreferences("taskium", Context.MODE_PRIVATE).edit()
                    .putBoolean(PREF_PERMISSION_FLOW, false)
                    .apply();
                Toast.makeText(this, "Taskium necesita permiso de notificaciones para mostrar las alarmas.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    private boolean notificationsAllowed() {
        return Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean exactAllowed() {
        try {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            return Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean permissionsReady() {
        return notificationsAllowed() && exactAllowed();
    }

    private void requestExactPermissionOrFinish() {
        if (Build.VERSION.SDK_INT >= 31 && !exactAllowed()) {
            Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } else {
            getSharedPreferences("taskium", Context.MODE_PRIVATE).edit()
                .putBoolean(PREF_PERMISSION_FLOW, false)
                .apply();
            Toast.makeText(this, "Alarmas locales activadas en Android.", Toast.LENGTH_SHORT).show();
        }
    }

    private void startPermissionFlow() {
        getSharedPreferences("taskium", Context.MODE_PRIVATE).edit()
            .putBoolean(PREF_PERMISSION_FLOW, true)
            .apply();
        if (Build.VERSION.SDK_INT >= 33 && !notificationsAllowed()) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
            return;
        }
        requestExactPermissionOrFinish();
    }

    private String diagnosticsJson() {
        try {
            SharedPreferences p = getSharedPreferences("taskium", Context.MODE_PRIVATE);
            boolean fullScreen = true;
            if (Build.VERSION.SDK_INT >= 34) {
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                fullScreen = nm.canUseFullScreenIntent();
            }
            long next = p.getLong("diag_next_when", 0L);
            String nextText = next > 0 ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(next)) : "";
            String err = p.getString("diag_error", "").replace("\\", "\\\\").replace("\"", "\\\"");
            boolean stateLoaded = !p.getString("state", "").isEmpty();
            return "{"
                + "\"version\":\"2.1.3\","
                + "\"exact\":" + exactAllowed() + ","
                + "\"notifications\":" + notificationsAllowed() + ","
                + "\"fullScreen\":" + fullScreen + ","
                + "\"stateLoaded\":" + stateLoaded + ","
                + "\"enabled\":" + p.getInt("diag_enabled", 0) + ","
                + "\"scheduled\":" + p.getInt("diag_scheduled", 0) + ","
                + "\"next\":\"" + nextText + "\","
                + "\"error\":\"" + err + "\""
                + "}";
        } catch (Exception e) {
            return "{\"version\":\"2.1.3\",\"error\":\"diagnostics unavailable\"}";
        }
    }

    public static class NativeBridge {
        private final MainActivity activity;
        NativeBridge(MainActivity activity) { this.activity = activity; }

        @JavascriptInterface public String syncState(String json) {
            try {
                activity.getSharedPreferences("taskium", Context.MODE_PRIVATE).edit()
                    .putString("state", json)
                    .putLong("state_received_at", System.currentTimeMillis())
                    .apply();
                AlarmScheduler.Result r = AlarmScheduler.scheduleAll(activity, json);
                return r.error.isEmpty() ? "ok" : "error:" + r.error;
            } catch (Exception e) {
                return "error:" + e.getClass().getSimpleName();
            }
        }

        @JavascriptInterface public String alarmStatus() {
            return activity.diagnosticsJson();
        }

        @JavascriptInterface public void requestAlarmPermissions() {
            activity.runOnUiThread(activity::startPermissionFlow);
        }

        @JavascriptInterface public String testAlarm60s() {
            boolean ok = AlarmScheduler.scheduleDiagnostic(activity, 60_000L);
            return ok ? "ok" : "error";
        }
    }
}
