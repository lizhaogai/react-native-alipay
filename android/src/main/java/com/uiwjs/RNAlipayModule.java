package com.uiwjs.alipay;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.alipay.sdk.app.AuthTask;
import com.alipay.sdk.app.H5PayCallback;
import com.alipay.sdk.app.PayTask;
import com.alipay.sdk.app.EnvUtils;
import com.alipay.sdk.util.H5PayResultModel;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Promise;
// import com.facebook.react.bridge.Callback;

import java.net.URISyntaxException;
import java.util.Map;

public class RNAlipayModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;

    public RNAlipayModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNAlipay";
    }

    // @ReactMethod
    // public void sampleMethod(String stringArgument, int numberArgument, Callback callback) {
    //     // TODO: Implement some actually useful functionality
    //     callback.invoke("Received numberArgument: " + numberArgument + " stringArgument: " + stringArgument);
    // }

    @ReactMethod
    public void authInfo(final String infoStr, final Promise promise) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                AuthTask authTask = new AuthTask(getCurrentActivity());
                Map<String, String> map = authTask.authV2(infoStr, true);
                promise.resolve(getWritableMap(map));
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    @ReactMethod
    public void setAlipaySandbox(Boolean isSandbox) {
        if (isSandbox) {
            EnvUtils.setEnv(EnvUtils.EnvEnum.SANDBOX);
        } else {
            EnvUtils.setEnv(EnvUtils.EnvEnum.ONLINE);
        }
    }
    @ReactMethod
    public void alipay(final String orderInfo, final Promise promise) {
        Runnable payRunnable = new Runnable() {
            @Override
            public void run() {
                PayTask alipay = new PayTask(getCurrentActivity());
                Map<String, String> result = alipay.payV2(orderInfo, true);
                promise.resolve(getWritableMap(result));
            }
        };
        // 必须异步调用
        Thread payThread = new Thread(payRunnable);
        payThread.start();
    }

    @ReactMethod
    public void alipayWithH5(final String orderInfo, final Promise promise) {
        Log.e("TAG", "alipayWithH5() called with: orderInfo = [" + orderInfo + "], promise = [" + promise + "]");
        WebView webView = new WebView(getCurrentActivity());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(orderInfo);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        // 启用二方/三方 Cookie 存储和 DOM Storage
        // 注意：若要在实际 App 中使用，请先了解相关设置项细节。
        CookieManager.getInstance().setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }
        webView.getSettings().setDomStorageEnabled(true);

        webView.setVerticalScrollbarOverlay(true);
        webView.setWebViewClient(new WebViewClient(){


            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                final PayTask task = new PayTask(getCurrentActivity());
                task.payInterceptorWithUrl(url, true, new H5PayCallback() {
                    @Override
                    public void onPayResult(H5PayResultModel result) {
                        final String url = result.getReturnUrl();
//                        if (!TextUtils.isEmpty(url)) {
//                            getCurrentActivity().runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    Log.e("TAG", "zhifu");
//                                    view.loadUrl(url);
//                                }
//                            });
//                        }
                        String resultStatus = result.getResultCode();
                        Log.e("TAG", "支付结果++++++" + resultStatus);

                        WritableMap writableMap = Arguments.createMap();
                        writableMap.putString("resultStatus", resultStatus);
                        promise.resolve(writableMap);
                    }
                });
                return true;
            }
        });


    }
    public boolean parseScheme(String url) {
        if (url.contains("platformapi/startApp")){
            return true;
        } else if(url.contains("web-other")){
            return false;
        }else {
            return false;
        }
    }
    @ReactMethod
    public void getVersion(Promise promise) {
        PayTask payTask = new PayTask(getCurrentActivity());
        promise.resolve(payTask.getVersion());
    }

    private WritableMap getWritableMap(Map<String, String> map) {
        WritableMap writableMap = Arguments.createMap();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            writableMap.putString(entry.getKey(), entry.getValue());
        }
        return writableMap;
    }
}
