package cordova.plugin.screenshot;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.FileObserver;
import android.provider.MediaStore;
import android.util.Log;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ScreenshotDetector extends CordovaPlugin {
    private FileObserver fileObserver;
    private CallbackContext callbackContext;
    private boolean isWaitingForPermission = false;
    public static final String READ_EXTERNAL_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE;
    public static final String READ_MEDIA_IMAGES = Manifest.permission.READ_MEDIA_IMAGES;
    public static final int SEARCH_REQ_CODE = 0;
    private static final String PERMISSION_DENIED_ERROR = "Permission denied by user.";
    public static String TAG = "SSSSSSSSSSS";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "execute() called with: action = [" + action + "], args = [" + args + "], callbackContext = [" + callbackContext + "]");
        if (action.equals("startWatch")) {
            this.callbackContext = callbackContext;
            startWatch();
            return true;
        } else if (action.equals("stopWatch")) {
            stopWatch();
            return true;
        }
        return false;
    }

    private void startWatch() {
        Log.d(TAG, "startWatch() called ");
        // 先停止已有监听
        stopWatch();
        
        if (cordova.hasPermission(READ_EXTERNAL_STORAGE)) {
            Log.d(TAG, "startWatch() Has Store permission .........");
            startWatchingFile();
        } else {
            // 设置标志位表示正在等待权限
            isWaitingForPermission = true;
            // 动态申请
            getReadPermission(SEARCH_REQ_CODE);
        }
    }

    private void startWatchingFile() {
        List<String> paths = getValidScreenshotPaths();
        Log.d(TAG, "startWatchingFile() called with: path = [" + paths + "]");
        // 创建组合观察者
        fileObserver = new CompositeFileObserver(paths) {
            protected void onScreenshotDetected(String filePath) {
                PluginResult result = new PluginResult(PluginResult.Status.OK, filePath);
                result.setKeepCallback(true);
                callbackContext.sendPluginResult(result);
            }
        };
        fileObserver.startWatching();
    }

    protected void getReadPermission(int requestCode) {
        Log.d(TAG, "getReadPermission() called with: requestCode = [" + requestCode + "]");
        cordova.requestPermission(this, requestCode, READ_EXTERNAL_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        Log.d(TAG, "onRequestPermissionsResult() called with: requestCode = [" + requestCode + "], permissions = [" + Arrays.toString(permissions) + "], grantResults = [" + Arrays.toString(grantResults) + "]");
        
        // 重置等待权限标志
        isWaitingForPermission = false;
        
        if (grantResults.length == 0) {
            Log.e(TAG, "onRequestPermissionResult: 权限请求被取消");
            this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Permission request was cancelled"));
            return;
        }

        boolean granted = true;
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                granted = false;
                break;
            }
        }

        if (!granted) {
            Log.e(TAG, "onRequestPermissionResult: 用户拒绝了权限 .........");
            this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, PERMISSION_DENIED_ERROR));
            return;
        }

        if (requestCode == SEARCH_REQ_CODE) {
            Log.d(TAG, "onRequestPermissionResult: 权限已授予，开始监听");
            // 确保在主线程执行
            cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startWatchingFile();
                }
            });
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        Log.d(TAG, "onResume() called with: multitasking = [" + multitasking + "]");
        
        // 检查是否在等待权限并且现在有了权限
        if (isWaitingForPermission && cordova.hasPermission(READ_EXTERNAL_STORAGE)) {
            Log.d(TAG, "onResume: 从权限请求返回，现在有权限了");
            isWaitingForPermission = false;
            startWatchingFile();
        }
    }

    private void stopWatch() {
        Log.d(TAG, "stopWatch() called .........");
        if (fileObserver != null) {
            fileObserver.stopWatching();
            fileObserver = null;
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() called .........");
        stopWatch();
    }

    private static final List<String> SCREENSHOT_PATHS = Arrays.asList(
            Environment.DIRECTORY_PICTURES + "/Screenshots",
            "DCIM/Screenshots",
            "Pictures/Screenshots",
            "MIUI/ScreenShot",
            "Huawei/Screenshot",
            "Vivo/Screenshot",
            "ColorOS/Screenshot"
    );

    private List<String> getValidScreenshotPaths() {
        List<String> validPaths = new ArrayList<>();
        File externalDir = Environment.getExternalStorageDirectory();

        for (String relativePath : SCREENSHOT_PATHS) {
            File path = new File(externalDir, relativePath);
            if (path.exists() && path.isDirectory()) {
                validPaths.add(path.getAbsolutePath());
                Log.d(TAG, "Found screenshot path: " + path.getAbsolutePath());
            }
        }

        File publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (publicDir.exists()) {
            validPaths.add(publicDir.getAbsolutePath());
        }

        return validPaths;
    }

    public List<String> findScreenshotFileSavePath(Context context) {
        Log.d(TAG, "findScreenshotUris() called with: context = [" + context + "]");
        List<String> paths = new ArrayList<>();
        List<Uri> result = new ArrayList<>();
        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.RELATIVE_PATH
        };
        String selection = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ? OR " +
                MediaStore.Images.Media.RELATIVE_PATH + " LIKE ? OR " +
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " = ?";
        String[] selectionArgs = {
                "%Screenshots%",
                "%DCIM/Screenshots%",
                "ScreenCapture"
        };
        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                MediaStore.Images.Media.DATE_TAKEN + " DESC"
        )) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                    result.add(uri);
                }
            }
            for (Uri uri : result) {
                paths.add(uri.getPath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return paths;
    }

    private abstract static class CompositeFileObserver extends FileObserver {
        private List<FileObserver> observers = new ArrayList<>();

        public CompositeFileObserver(List<String> paths) {
            super(paths.get(0));
            for (String path : paths) {
                observers.add(new SinglePathObserver(path));
            }
        }

        @Override
        public void startWatching() {
            for (FileObserver observer : observers) {
                observer.startWatching();
            }
        }

        @Override
        public void stopWatching() {
            for (FileObserver observer : observers) {
                observer.stopWatching();
            }
        }

        @Override
        public void onEvent(int event, String path) {
            // 由子观察者处理
        }

        protected abstract void onScreenshotDetected(String filePath);

        private class SinglePathObserver extends FileObserver {
            private final String basePath;

            public SinglePathObserver(String path) {
                super(path, FileObserver.CLOSE_WRITE);
                this.basePath = path;
            }

            @Override
            public void onEvent(int event, String file) {
                if (file != null && isScreenshotFile(file)) {
                    String fullPath = basePath + File.separator + file;
                    onScreenshotDetected(fullPath);
                }
            }

            private boolean isScreenshotFile(String filename) {
                String lowerName = filename.toLowerCase(Locale.US);
                return lowerName.toLowerCase().contains("screenshot") ||
                        lowerName.contains("截屏") ||
                        lowerName.startsWith("scr_") ||
                        lowerName.endsWith(".png") ||
                        lowerName.endsWith(".jpg");
            }
        }
    }
}