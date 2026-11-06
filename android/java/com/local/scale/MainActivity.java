package com.local.scale;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private WebView web;
    private ValueCallback<Uri[]> fileCb;
    private static final int REQ_FILE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        web = new WebView(this);
        web.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        web.setBackgroundColor(Color.parseColor("#F7F6F3"));

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);            // localStorage —— 数据就存在这里
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);              // API 30+ 默认关闭，必须显式打开
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);

        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> cb, FileChooserParams params) {
                fileCb = cb;
                try {
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");            // 各家文件管理器给 .json 报的 MIME 不一致，别过滤太死
                    startActivityForResult(i, REQ_FILE);
                } catch (Exception e) {
                    fileCb = null;
                    return false;
                }
                return true;
            }
        });
        web.addJavascriptInterface(new Bridge(), "AndroidApp");

        setContentView(web);

        if (Build.VERSION.SDK_INT >= 23) {
            getWindow().setStatusBarColor(Color.parseColor("#F7F6F3"));
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_FILE) {
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                result = new Uri[]{ data.getData() };
            }
            if (fileCb != null) {
                fileCb.onReceiveValue(result);
                fileCb = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /** 给网页用的导出接口：把 JSON 写到系统“下载”目录 */
    public class Bridge {
        @JavascriptInterface
        public void saveState(String json) {
            try {
                File f = new File(getFilesDir(), "state.json");
                FileOutputStream fo = new FileOutputStream(f);
                fo.write(json.getBytes("UTF-8"));
                fo.flush();
                fo.close();
            } catch (Exception e) { /* 忽略，网页里还有 localStorage */ }
        }

        @JavascriptInterface
        public String loadState() {
            try {
                File f = new File(getFilesDir(), "state.json");
                if (!f.exists()) return "";
                byte[] b = new byte[(int) f.length()];
                java.io.FileInputStream fi = new java.io.FileInputStream(f);
                int off = 0, n;
                while (off < b.length && (n = fi.read(b, off, b.length - off)) > 0) off += n;
                fi.close();
                return new String(b, 0, off, "UTF-8");
            } catch (Exception e) { return ""; }
        }

        @JavascriptInterface
        public String saveText(String name, String content) {
            try {
                if (Build.VERSION.SDK_INT >= 29) {
                    return Q.save(MainActivity.this, name, content);
                }
                File dir = getExternalFilesDir(null);
                File f = new File(dir, name);
                FileOutputStream fo = new FileOutputStream(f);
                fo.write(content.getBytes("UTF-8"));
                fo.flush();
                fo.close();
                return f.getAbsolutePath();
            } catch (Exception e) {
                return "ERR:" + e.getMessage();
            }
        }

        @JavascriptInterface
        public void openUrl(final String url) {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            } catch (Exception e) { /* 没有浏览器就算了 */ }
        }

        @JavascriptInterface
        public void toast(final String msg) {
            runOnUiThread(new Runnable() {
                public void run() { Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show(); }
            });
        }
    }

    /** 只有 Android 10+ 才会加载这个类，避免旧系统校验时找不到 MediaStore.Downloads */
    static class Q {
        static String save(Activity a, String name, String content) throws Exception {
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            v.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = a.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (uri == null) return "ERR:无法创建文件";
            OutputStream os = a.getContentResolver().openOutputStream(uri);
            if (os == null) return "ERR:无法写入";
            os.write(content.getBytes("UTF-8"));
            os.flush();
            os.close();
            return "下载/" + name;
        }
    }
}
