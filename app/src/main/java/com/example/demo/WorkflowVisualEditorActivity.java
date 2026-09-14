package com.example.demo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 工作流可视化编辑器 — 使用 WebView 承载 HTML/SVG 可视化编辑器
 * 移植自 st-chatu8 的 visualizeWorkflow()
 */
public class WorkflowVisualEditorActivity extends AppCompatActivity {

    public static final String EXTRA_WORKFLOW_JSON = "workflow_json";
    public static final String EXTRA_RESULT_JSON = "result_json";

    private WebView webView;
    private String workflowJson;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getColor(R.color.comfy_bg_dark));
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        workflowJson = getIntent().getStringExtra(EXTRA_WORKFLOW_JSON);
        if (workflowJson == null || workflowJson.isEmpty()) {
            finish();
            return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#1a1a2e"));

        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setSupportZoom(false);
        webView.getSettings().setUseWideViewPort(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new AndroidInterface(), "AndroidInterface");

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        webView.setLayoutParams(params);
        root.addView(webView);

        setContentView(root);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String escaped = workflowJson
                        .replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r");
                view.evaluateJavascript("loadWorkflow('" + escaped + "')", null);
            }
        });

        webView.loadUrl("file:///android_asset/workflow_visual_editor.html");
    }

    private class AndroidInterface {
        @JavascriptInterface
        public void onSave(String json) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_RESULT_JSON, json);
            setResult(RESULT_OK, resultIntent);
            finish();
        }

        @JavascriptInterface
        public void onClose() {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}
