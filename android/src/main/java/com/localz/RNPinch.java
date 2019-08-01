package com.localz;

import android.os.AsyncTask;
import android.util.Log;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.UnexpectedNativeTypeException;
import com.facebook.react.bridge.WritableMap;

import com.localz.pinch.models.HttpRequest;
import com.localz.pinch.models.HttpResponse;
import com.localz.pinch.utils.HttpUtil;
import com.localz.pinch.utils.JsonUtil;

import org.json.JSONException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class RNPinch extends ReactContextBaseJavaModule {

    private final String TAG = "RNPinch";

    private static final String OPT_METHOD_KEY = "method";
    private static final String OPT_HEADER_KEY = "headers";
    private static final String OPT_BODY_KEY = "body";
    private static final String OPT_SSL_PINNING_KEY = "sslPinning";
    private static final String OPT_TIMEOUT_KEY = "timeoutInterval";
    private static final String OPT_MUTUAL_AUTH_KEY = "mutualAuth";

    private HttpUtil httpUtil;
    private String packageName = null;
    private String displayName = null;
    private String version = null;
    private String versionCode = null;

    public RNPinch(ReactApplicationContext reactContext) {
        super(reactContext);
        httpUtil = new HttpUtil();
        try {
            PackageManager pManager = reactContext.getPackageManager();
            packageName = reactContext.getPackageName();
            PackageInfo pInfo = pManager.getPackageInfo(packageName, 0);
            ApplicationInfo aInfo = pManager.getApplicationInfo(packageName, 0);
            displayName = pManager.getApplicationLabel(aInfo).toString();
            version = pInfo.versionName;
            versionCode = String.valueOf(pInfo.versionCode);
        } catch (NameNotFoundException nnfe) {
            Log.d(TAG,"RNAppInfo: package name not found");
        }
    }

    @Override
    public String getName() {
        return "RNPinch";
    }

    @ReactMethod
    public void fetch(String endpoint, ReadableMap opts, Promise promise) {
        new FetchTask(opts, promise).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, endpoint);
    }

    private class FetchTask extends AsyncTask<String, Promise, WritableMap> {
        private ReadableMap opts;
        private Promise promise;

        public FetchTask(ReadableMap opts, Promise promise) {
            this.opts = opts;
            this.promise = promise;
        }

        @Override
        protected WritableMap doInBackground(String... endpoint) {

            try {
                WritableMap response = Arguments.createMap();
                HttpRequest request = new HttpRequest(endpoint[0]);

                if (opts.hasKey(OPT_BODY_KEY)) {
                    request.body = opts.getString(OPT_BODY_KEY);
                }
                if (opts.hasKey(OPT_METHOD_KEY)) {
                    request.method = opts.getString(OPT_METHOD_KEY);
                }
                if (opts.hasKey(OPT_HEADER_KEY)) {
                    request.headers = JsonUtil.convertReadableMapToJson(opts.getMap(OPT_HEADER_KEY));
                }
                if (opts.hasKey(OPT_SSL_PINNING_KEY)) {
                    ReadableMap sslPinning = opts.getMap(OPT_SSL_PINNING_KEY);
                    if (sslPinning.hasKey("cert")) {
                        String cert = sslPinning.getString("cert");
                        request.sslPinningCerts = new String[]{cert};
                    } else if (sslPinning.hasKey("certs")) {
                        ReadableArray certsStrings = sslPinning.getArray("certs");
                        String[] certs = new String[certsStrings.size()];
                        for (int i = 0; i < certsStrings.size(); i++) {
                            certs[i] = certsStrings.getString(i);
                        }
                        request.sslPinningCerts = certs;
                    }
                }
                if (opts.hasKey(OPT_TIMEOUT_KEY)) {
                    request.timeout = opts.getInt(OPT_TIMEOUT_KEY);
                }
                if (opts.hasKey(OPT_MUTUAL_AUTH_KEY)) {
                    ReadableMap mutualAuth = opts.getMap(OPT_MUTUAL_AUTH_KEY);
                    if (mutualAuth.hasKey("cert")) {
                        request.mutualAuthCert = mutualAuth.getString("cert");
                    }
                    if (mutualAuth.hasKey("password")) {
                        request.mutualAuthPassword = mutualAuth.getString("password");
                    }
                }

                HttpResponse httpResponse = httpUtil.sendHttpRequest(request);
                Log.e("LOG ERROR HTTP RESPONSE",httpResponse.bodyString);
                response.putInt("status", httpResponse.statusCode);
                response.putString("statusText", httpResponse.statusText);
                response.putString("bodyString", httpResponse.bodyString);
                response.putMap("headers", httpResponse.headers);

                return response;
            } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | KeyManagementException e) {
                WritableMap error = Arguments.createMap();
                error.putString("errorMessage", e.toString());
                error.putString("errorCode", "1401");
                Log.w("RNPinch", e);
                return error;
            } catch (SocketTimeoutException e) {
                WritableMap error = Arguments.createMap();
                error.putString("errorMessage", e.toString());
                error.putString("errorCode", "1408");

                Log.w("RNPinch", e);
                return error;
            } catch (JSONException | IOException | UnexpectedNativeTypeException | NoSuchAlgorithmException e) {
                Log.e("ERROR JSON EXCEPTION",e.toString());
                WritableMap error = Arguments.createMap();
                error.putString("errorMessage", e.toString());
                error.putString("errorCode", "1000");
                Log.w("RNPinch", e);
                return error;
            }

        }

        @Override
        protected void onPostExecute(WritableMap response) {

            if (response.hasKey("errorMessage")) {
                promise.resolve(response);
                // disabled this because cause exception
                //promise.reject(response.getString("errorCode"),response.getString("errorMessage"));
            } else {
                Log.e("ON POST RESPONSE HTTPS", response.toString());
                promise.resolve(response);
            }
        }
    }
}
