package com.example.screenshotlistener;

import android.os.FileObserver;
import org.apache.cordova.*;
import org.json.JSONArray;
import java.io.File;

public class ScreenshotListener extends CordovaPlugin {
    private FileObserver observer;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        if ("start".equals(action)) {
            startWatching(callbackContext);

            return true;
        }

        return false;
    }

    private void startWatching(CallbackContext callbackContext) {
        String path = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/Screenshots";

        observer = new FileObserver(path, FileObserver.CREATE) {
            @Override
            public void onEvent(int event, String file) {
                if (file != null) {
                    webView.getEngine().evaluateJavascript("cordova.fireDocumentEvent('screenshotTaken');", null);
                }
            }
        };
        observer.startWatching();
        callbackContext.success("Watching started");
    }
}
