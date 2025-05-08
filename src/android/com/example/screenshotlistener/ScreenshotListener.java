// ScreenshotListener.java
package com.example.screenshotlistener;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebViewImpl;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;

public class ScreenshotListener extends CordovaPlugin {
    private ScreenshotContentObserver observer;
    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if ("start".equals(action)) {
            this.callbackContext = callbackContext;
            startObserver();
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            return true;
        }
        return false;
    }

    private void startObserver() {
        Handler handler = new Handler(Looper.getMainLooper());
        CordovaWebViewImpl webViewImpl = (CordovaWebViewImpl) this.webView;
        observer = new ScreenshotContentObserver(handler, cordova.getActivity(), webViewImpl);
        cordova.getActivity().getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
        );
    }

    @Override
    public void onDestroy() {
        if (observer != null) {
            cordova.getActivity().getContentResolver().unregisterContentObserver(observer);
        }
    }

    class ScreenshotContentObserver extends ContentObserver {
        private Context context;
        private CordovaWebViewImpl webView;

        public ScreenshotContentObserver(Handler handler, Context context, CordovaWebViewImpl webView) {
            super(handler);
            this.context = context;
            this.webView = webView;
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);

            String[] projection;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                projection = new String[]{
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED
                };
            } else {
                projection = new String[]{
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DATE_ADDED
                };
            }

            Cursor cursor = context.getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    MediaStore.Images.Media.DATE_ADDED + " DESC LIMIT 1"
            );

            if (cursor != null && cursor.moveToFirst()) {
                String nameOrPath;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    int nameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
                    nameOrPath = cursor.getString(nameColumn);
                } else {
                    int dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                    nameOrPath = cursor.getString(dataColumn);
                }
                cursor.close();

                if (nameOrPath != null && nameOrPath.toLowerCase().contains("screenshot")) {
                    Log.d("ScreenshotObserver", "Detected screenshot: " + nameOrPath);

                    // Send JS event
                    webView.getEngine().evaluateJavascript("cordova.fireDocumentEvent('screenshotTaken');", null);

                    // Send plugin callback result
                    if (callbackContext != null) {
                        PluginResult result = new PluginResult(PluginResult.Status.OK, "screenshot_detected");
                        result.setKeepCallback(true);
                        callbackContext.sendPluginResult(result);
                    }
                }
            }
        }
    }
}
