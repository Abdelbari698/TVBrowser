package com.monlauncher.tvbrowser;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.storage.StorageManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;

public class MainActivity extends Activity {

    WebView webView;
    ProgressBar progressBar;

    static final String LAUNCHER_FILE = "launcher.html";

    // BroadcastReceiver interne — détecte USB branché pendant que l'app tourne
    BroadcastReceiver usbReceiver;

    static final String[] USB_DIRS = {
        "/storage/usb/", "/storage/usb0/", "/storage/usb1/", "/storage/usb2/",
        "/storage/usbdisk/", "/storage/usbdisk0/", "/storage/usbdisk1/",
        "/storage/usb-storage/", "/storage/USB/", "/storage/USB0/", "/storage/USB1/",
        "/mnt/usb/", "/mnt/usb0/", "/mnt/usb1/", "/mnt/usb2/",
        "/mnt/usbdisk/", "/mnt/usbdisk0/", "/mnt/usbdisk1/",
        "/mnt/usb_storage/", "/mnt/usbstorage/",
        "/mnt/USB/", "/mnt/USB0/", "/mnt/USB1/",
        "/mnt/external_sd/", "/mnt/extsd/", "/mnt/sdcard1/", "/mnt/sdcard2/",
        "/mnt/ext_card/", "/mnt/external/", "/mnt/media_rw/",
        "/mnt/media_rw/udisk0/", "/mnt/media_rw/udisk1/",
        "/mnt/media_rw/usb0/", "/mnt/media_rw/usb1/", "/mnt/media_rw/usbdisk/",
        "/storage/external_storage/usb0/", "/storage/external_storage/usb1/",
        "/storage/external_storage/sdcard1/",
        "/storage/udisk0/", "/storage/udisk1/",
        "/mnt/udisk/", "/mnt/udisk0/", "/mnt/udisk1/",
        "/mnt/sdcard/udisk/", "/mnt/sdcard/usb_storage/",
        "/storage/sdcard1/", "/storage/sdcard2/",
        "/storage/emulated/0/", "/sdcard/", "/sdcard0/", "/sdcard1/",
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Plein écran TV
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        setContentView(R.layout.activity_main);
        progressBar = findViewById(R.id.progressBar);
        webView     = findViewById(R.id.webview);

        configureWebView();

        // Si lancé depuis USB branché, attendre 2s que le système monte la clé
        boolean fromUsb = getIntent().getBooleanExtra("from_usb", false);
        if (fromUsb) {
            Toast.makeText(this, "Clé USB détectée, chargement...", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(new Runnable() {
                @Override public void run() { loadBestUrl(); }
            }, 2000);
        } else {
            loadBestUrl();
        }

        // Écoute USB branché en temps réel pendant que l'app tourne
        registerUsbReceiver();
    }

    void registerUsbReceiver() {
        usbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Attendre 2s que Android monte la clé
                new Handler().postDelayed(new Runnable() {
                    @Override public void run() {
                        Toast.makeText(MainActivity.this, "Clé USB détectée !", Toast.LENGTH_SHORT).show();
                        loadBestUrl();
                    }
                }, 2000);
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        filter.addDataScheme("file");
        registerReceiver(usbReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (usbReceiver != null) {
            try { unregisterReceiver(usbReceiver); } catch (Exception ignored) {}
        }
    }

    void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        s.setBuiltInZoomControls(false);
        s.setSupportZoom(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                String url = req.getUrl().toString();
                if (url.startsWith("intent://") || url.startsWith("intent:#")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "App non installee", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
                    catch (Exception e) { view.loadUrl(url); }
                    return true;
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                progressBar.setVisibility(progress < 100 ? View.VISIBLE : View.GONE);
                progressBar.setProgress(progress);
            }
        });
    }

    void loadBestUrl() {
        String found = findLauncherFile();
        if (found != null) {
            webView.loadUrl("file://" + found);
            Toast.makeText(this, "Launcher lance !", Toast.LENGTH_SHORT).show();
        } else {
            webView.loadUrl("file:///android_asset/index.html");
        }
    }

    String findLauncherFile() {
        // Méthode 1 : liste statique
        for (String dir : USB_DIRS) {
            File f = new File(dir + LAUNCHER_FILE);
            if (f.exists() && f.canRead()) return f.getAbsolutePath();
        }
        // Méthode 2 : /proc/mounts
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/mounts"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    String mount = parts[1];
                    if (mount.contains("usb") || mount.contains("udisk")
                            || mount.contains("sdcard") || mount.contains("external")
                            || mount.contains("media_rw") || mount.contains("storage")) {
                        File f = new File(mount + "/" + LAUNCHER_FILE);
                        if (f.exists() && f.canRead()) { br.close(); return f.getAbsolutePath(); }
                    }
                }
            }
            br.close();
        } catch (Exception ignored) {}
        // Méthode 3 : scan dynamique
        for (String root : new String[]{ "/storage", "/mnt" }) {
            File rootDir = new File(root);
            if (!rootDir.exists()) continue;
            File[] children = rootDir.listFiles();
            if (children == null) continue;
            for (File child : children) {
                if (!child.isDirectory()) continue;
                File f = new File(child, LAUNCHER_FILE);
                if (f.exists() && f.canRead()) return f.getAbsolutePath();
                File[] sub = child.listFiles();
                if (sub == null) continue;
                for (File s : sub) {
                    if (!s.isDirectory()) continue;
                    File f2 = new File(s, LAUNCHER_FILE);
                    if (f2.exists() && f2.canRead()) return f2.getAbsolutePath();
                }
            }
        }
        // Méthode 4 : StorageManager
        try {
            StorageManager sm = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
            Method getVols = sm.getClass().getMethod("getVolumeList");
            Object[] vols = (Object[]) getVols.invoke(sm);
            if (vols != null) {
                for (Object vol : vols) {
                    Method getPath = vol.getClass().getMethod("getPath");
                    String path = (String) getPath.invoke(vol);
                    if (path != null) {
                        File f = new File(path + "/" + LAUNCHER_FILE);
                        if (f.exists() && f.canRead()) return f.getAbsolutePath();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack(); return true;
        }
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            loadBestUrl(); return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
