package com.stepan.tracker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Context;
import android.content.Intent;
import android.app.DownloadManager;
import android.net.Uri;
import android.os.Environment;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private WebView webView;
    private static final String REMOTE_URL = "https://raw.githubusercontent.com/Efimo/tracker/main/docs/index.html";
    private static final String CACHE_FILE = "tracker_cache.html";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = getWindow();
        window.setStatusBarColor(Color.parseColor("#f5f5fa"));
        window.setNavigationBarColor(Color.parseColor("#ffffff"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            );
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.getAttributes().layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        webView = new WebView(this);
        webView.setFitsSystemWindows(true);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setTextZoom(100);

        String ua = settings.getUserAgentString();
        settings.setUserAgentString(ua + " StepanTrackerApp/1.0");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.contains("jsonbin.io") || url.contains("openai.com") || url.contains("api.github.com")) {
                    return false;
                }
                if (url.contains("github.com/") || url.contains("githubusercontent.com") || url.endsWith(".apk")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } catch (Exception e) {
                        view.loadUrl(url);
                    }
                    return true;
                }
                view.loadUrl(url);
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient());

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
                    request.setTitle("Обновление трекера");
                    request.setDescription("Скачивание " + filename);
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    dm.enqueue(request);
                    Toast.makeText(MainActivity.this, "Скачивание...", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                }
            }
        });

        webView.setBackgroundColor(Color.parseColor("#f5f5fa"));

        // Setup notifications
        NotificationHelper.createChannel(this);
        NotificationHelper.scheduleAll(this);
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }

        // Load: try remote first, then cache, then assets
        loadTracker();
    }

    private void loadTracker() {
        // First show cached/assets immediately for fast start
        String cached = readCache();
        if (cached != null) {
            webView.loadDataWithBaseURL("file:///android_asset/", cached, "text/html", "UTF-8", null);
        } else {
            webView.loadUrl("file:///android_asset/tracker.html");
        }

        // Then fetch fresh version in background
        if (isOnline()) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executor.execute(() -> {
                String html = fetchRemote();
                if (html != null && html.length() > 1000) {
                    saveCache(html);
                    handler.post(() -> {
                        // Check if content changed
                        String oldCached = cached;
                        if (oldCached == null || !oldCached.equals(html)) {
                            webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
                        }
                    });
                }
            });
        }
    }

    private boolean isOnline() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    private String fetchRemote() {
        try {
            URL url = new URL(REMOTE_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");

            int code = conn.getResponseCode();
            if (code != 200) return null;

            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            conn.disconnect();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void saveCache(String html) {
        try {
            FileOutputStream fos = openFileOutput(CACHE_FILE, Context.MODE_PRIVATE);
            fos.write(html.getBytes("UTF-8"));
            fos.close();
        } catch (Exception e) {
            // ignore
        }
    }

    private String readCache() {
        try {
            File f = new File(getFilesDir(), CACHE_FILE);
            if (!f.exists()) return null;
            FileInputStream fis = new FileInputStream(f);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
