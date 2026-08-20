package com.homecare.asiyah;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

public class MainActivity extends AppCompatActivity {
    private static final String APP_URL = "https://script.google.com/macros/s/AKfycbxlucVeCKeSpWBkc0y2CK8SO-z-b3kYI2bgEjo8dg8aAoLQJn5FwIqvIWnvG6fh-Dl-rQ/exec";
    private WebView webView;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String pendingCallback = "onNativeBarcodeResult";

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUserAgentString(s.getUserAgentString() + " HomeCareAsiyahNative/1.0");

        webView.addJavascriptInterface(new AndroidBridge(), "Android");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if (u != null && ("http".equals(u.getScheme()) || "https".equals(u.getScheme()))) return false;
                return true;
            }
            @Override public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }
        });
        webView.loadUrl(APP_URL);
    }

    private void startScanner(String callback) {
        pendingCallback = (callback == null || callback.trim().isEmpty()) ? "onNativeBarcodeResult" : callback.trim();
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .enableAutoZoom()
                .build();
        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(this, options);
        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String raw = barcode.getRawValue();
                    sendBarcodeToWeb(raw == null ? "" : raw);
                })
                .addOnCanceledListener(() -> sendBarcodeToWeb(""))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Scanner gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    sendBarcodeToWeb("");
                });
    }

    private void sendBarcodeToWeb(String code) {
        final String safeCode = org.json.JSONObject.quote(code == null ? "" : code);
        final String safeCallback = pendingCallback.replaceAll("[^A-Za-z0-9_$]", "");
        String js = "javascript:(function(){try{var c=" + safeCode + ";var f=window[" + org.json.JSONObject.quote(safeCallback) + "];if(typeof f==='function'){f(c);}else if(typeof window.onNativeBarcodeResult==='function'){window.onNativeBarcodeResult(c);}}catch(e){console.error(e);}})();";
        mainHandler.post(() -> webView.evaluateJavascript(js, null));
    }

    public class AndroidBridge {
        @JavascriptInterface public String scanBarcode(String callback) {
            mainHandler.post(() -> startScanner(callback));
            return "";
        }
        @JavascriptInterface public String getAppVersion() { return "1.0"; }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
}
