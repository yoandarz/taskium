package com.taskium.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private static final String TASKIUM_URL = "https://yoandarz.github.io/taskium/";
    private static final String TASKIUM_HOST = "yoandarz.github.io";
    private WebView webView;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);
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
        webView.loadUrl(TASKIUM_URL);
    }

    @Override protected void onResume() {
        super.onResume();
        String json = getSharedPreferences("taskium", Context.MODE_PRIVATE).getString("state", "");
        if (!json.isEmpty()) AlarmScheduler.scheduleAll(this, json);
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    public static class NativeBridge {
        private final Activity activity;
        NativeBridge(Activity activity) { this.activity = activity; }

        @JavascriptInterface public String syncState(String json) {
            try {
                activity.getSharedPreferences("taskium", Context.MODE_PRIVATE).edit().putString("state", json).apply();
                AlarmScheduler.scheduleAll(activity, json);
                return "ok";
            } catch (Exception e) {
                return "error";
            }
        }

        @JavascriptInterface public String alarmStatus() {
            try {
                AlarmManager am = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
                boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
                boolean notifications = Build.VERSION.SDK_INT < 33 || activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
                return "{\"exact\":" + exact + ",\"notifications\":" + notifications + "}";
            } catch (Exception e) {
                return "{\"exact\":false,\"notifications\":false}";
            }
        }

        @JavascriptInterface public void requestAlarmPermissions() {
            activity.runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= 33 && activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    activity.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 210);
                }
                if (Build.VERSION.SDK_INT >= 31) {
                    AlarmManager am = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
                    if (!am.canScheduleExactAlarms()) {
                        Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + activity.getPackageName()));
                        activity.startActivity(i);
                    }
                }
            });
        }
    }
}
