package io.github.sumitsingh34.bitewise;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

/**
 * BiteWise is a web app (index.html at the top of the repository). This activity shows it in a
 * WebView, serving the copy bundled in assets/web/ from a private https address so it works
 * offline and stores its data (localStorage) inside the app.
 */
public class MainActivity extends Activity {
    // Android's reserved address for app-bundled files; nothing is fetched from the internet for it.
    private static final String HOST = "appassets.androidplatform.net";
    private static final String START_URL = "https://" + HOST + "/index.html";

    private static final int REQ_CAMERA = 1;
    private static final int REQ_SAVE = 2;
    private static final int REQ_OPEN = 3;

    private WebView web;
    private PermissionRequest pendingCamera;
    private ValueCallback<Uri[]> pendingFiles;
    private String pendingSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Strips the size of the status bar and navigation bar, coloured to match the app.
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(R.color.paper));
        View top = new View(this);
        web = new WebView(this);
        web.setBackgroundColor(getColor(R.color.paper));
        View bottom = new View(this);
        bottom.setBackgroundColor(getColor(R.color.bar));
        root.addView(top, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0));
        root.addView(web, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(bottom, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0));
        setContentView(root);
        setUpSystemBars(root, top, bottom);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setMediaPlaybackRequiresUserGesture(false);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                return HOST.equals(url.getHost()) ? bundledFile(url.getPath()) : null;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (HOST.equals(request.getUrl().getHost())) return false;
                // Any outside link opens in the phone's browser, not inside the app.
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, request.getUrl()));
                } catch (ActivityNotFoundException ignored) {
                }
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                // Only the barcode scanner asks, and only for the camera.
                boolean camera = Arrays.asList(request.getResources()).contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE);
                if (!camera || !HOST.equals(request.getOrigin().getHost())) {
                    request.deny();
                } else if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                } else {
                    pendingCamera = request;
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
                }
            }

            @Override
            public void onPermissionRequestCanceled(PermissionRequest request) {
                if (request == pendingCamera) pendingCamera = null;
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                // Import backup
                if (pendingFiles != null) pendingFiles.onReceiveValue(null);
                pendingFiles = callback;
                Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("*/*");
                try {
                    startActivityForResult(pick, REQ_OPEN);
                    return true;
                } catch (ActivityNotFoundException e) {
                    pendingFiles = null;
                    return false;
                }
            }
        });

        web.addJavascriptInterface(new Bridge(), "BiteWiseAndroid");

        if (savedInstanceState == null || web.restoreState(savedInstanceState) == null) {
            web.loadUrl(START_URL);
        }
    }

    /** Called from index.html as window.BiteWiseAndroid. */
    private class Bridge {
        /** Export backup: asks where to save, then writes the file. */
        @JavascriptInterface
        public void saveFile(String name, String text) {
            runOnUiThread(() -> {
                if (web.getUrl() == null || !HOST.equals(Uri.parse(web.getUrl()).getHost())) return;
                pendingSave = text;
                Intent save = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("application/json")
                        .putExtra(Intent.EXTRA_TITLE, name);
                try {
                    startActivityForResult(save, REQ_SAVE);
                } catch (ActivityNotFoundException e) {
                    pendingSave = null;
                    toast("Couldn't open a place to save the file");
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uri = resultCode == RESULT_OK && data != null ? data.getData() : null;
        if (requestCode == REQ_OPEN && pendingFiles != null) {
            pendingFiles.onReceiveValue(uri == null ? null : new Uri[]{uri});
            pendingFiles = null;
        } else if (requestCode == REQ_SAVE && pendingSave != null) {
            String text = pendingSave;
            pendingSave = null;
            if (uri == null) return;
            try (OutputStream out = getContentResolver().openOutputStream(uri, "w")) {
                if (out == null) throw new IOException("no output stream");
                out.write(text.getBytes(StandardCharsets.UTF_8));
                toast("Backup saved");
            } catch (IOException | SecurityException e) {
                toast("Couldn't save the backup");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (requestCode != REQ_CAMERA || pendingCamera == null) return;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            pendingCamera.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
        } else {
            pendingCamera.deny();
        }
        pendingCamera = null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        // Let the app close a sheet or step back first; leave only when there's nothing to close.
        web.evaluateJavascript("typeof androidBack === 'function' && androidBack()", handled -> {
            if (!"true".equals(handled)) super.onBackPressed();
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    protected void onDestroy() {
        web.destroy();
        super.onDestroy();
    }

    private void toast(String message) {
        web.evaluateJavascript("typeof toast === 'function' && toast(" + JSONObject.quote(message) + ")", null);
    }

    private WebResourceResponse bundledFile(String path) {
        if (path == null || path.equals("/")) path = "/index.html";
        if (!path.contains("..")) {
            try {
                InputStream in = getAssets().open("web" + path);
                String type = mimeType(path);
                return new WebResourceResponse(type, type.startsWith("text/") || type.endsWith("json") ? "utf-8" : null, in);
            } catch (IOException ignored) {
            }
        }
        return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found", Collections.emptyMap(), null);
    }

    private static String mimeType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".js")) return "text/javascript";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".woff2")) return "font/woff2";
        return "application/octet-stream";
    }

    private void setUpSystemBars(View root, View top, View bottom) {
        Window w = getWindow();
        boolean dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Draw behind the system bars and fill their space with the strips above and below the page.
            w.setDecorFitsSystemWindows(false);
            w.setStatusBarColor(Color.TRANSPARENT);
            w.setNavigationBarColor(Color.TRANSPARENT);
            w.setNavigationBarContrastEnforced(false);
            int light = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            WindowInsetsController c = w.getInsetsController();
            if (c != null) c.setSystemBarsAppearance(dark ? 0 : light, light);
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                Insets ime = insets.getInsets(WindowInsets.Type.ime());
                v.setPadding(bars.left, 0, bars.right, 0);
                top.getLayoutParams().height = bars.top;
                bottom.getLayoutParams().height = Math.max(bars.bottom, ime.bottom);
                top.requestLayout();
                bottom.requestLayout();
                return WindowInsets.CONSUMED;
            });
        } else {
            w.setStatusBarColor(getColor(R.color.paper));
            int flags = dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                w.setNavigationBarColor(getColor(R.color.bar));
                if (!dark) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            } else {
                w.setNavigationBarColor(Color.BLACK);
            }
            w.getDecorView().setSystemUiVisibility(flags);
        }
    }
}
