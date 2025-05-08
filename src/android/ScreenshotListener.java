package com.example.screenshotlistener;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebViewImpl;

import org.json.JSONArray;
import org.json.JSONException;

public class ScreenshotListener extends CordovaPlugin {
    private ScreenshotContentObserver observer;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if ("start".equals(action)) {
            startObserver();
            callbackContext.success("Screenshot observer started.");

            return true;
        }

        return false;
    }

    private void startObserver() {
        Handler handler = new Handler(Looper.getMainLooper());
        observer = new ScreenshotContentObserver(handler, cordova.getActivity());
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

    static class ScreenshotContentObserver extends ContentObserver {
        private Context context;

        public ScreenshotContentObserver(Handler handler, Context context) {
            super(handler);
            this.context = context;
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);

            Cursor cursor = context.getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_ADDED},
                    null,
                    null,
                    MediaStore.Images.Media.DATE_ADDED + " DESC LIMIT 1"
            );

            if (cursor != null && cursor.moveToFirst()) {
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                cursor.close();

                if (path != null && path.toLowerCase().contains("screenshot")) {
                    Log.d("ScreenshotObserver", "Detected screenshot: " + path);
                    ((CordovaWebViewImpl)((cordova.getActivity()).appView))
                            .getEngine().evaluateJavascript("cordova.fireDocumentEvent('screenshotTaken');", null);
                }
            }
        }
    }
}
