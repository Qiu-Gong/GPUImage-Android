package com.alien.gif;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;

import com.getkeepsafe.relinker.ReLinker;

public class GifDecode {

    private static final String TAG = "GifInterface";
    private static final String LIB_NAME = "gif-lib";
    private static final int ERROR_VALUE = -1;

    private static volatile boolean hasLoaded = false;
    private long instanceId = ERROR_VALUE;

    private static void loadLibrary(Context context) {
        if (!hasLoaded) {
            synchronized (GifDecode.class) {
                try {
                    if (!hasLoaded) {
                        if (Build.VERSION.SDK_INT >= 24) {
                            ReLinker.loadLibrary(context, LIB_NAME);
                        } else {
                            System.loadLibrary(LIB_NAME);
                        }
                        hasLoaded = true;
                    }
                } catch (Throwable ignore) {
                    if (ignore.getMessage() != null) {
                        Log.e(TAG, ignore.getMessage());
                    }
                }
            }
        }
    }

    public GifDecode(Context context) {
        loadLibrary(context);
    }

    public void loadGif(String path) {
        instanceId = onNativeLoadGif(path);
        if (instanceId == ERROR_VALUE) {
            Log.e(TAG, "loadGif error");
        }
    }

    public void release() {
        long result = onNativeRelease(instanceId);
        if (result != ERROR_VALUE) {
            Log.e(TAG, "release error");
        }
        instanceId = result;
    }

    public int getWidth() {
        if (instanceId == ERROR_VALUE) {
            Log.e(TAG, "getWidth error");
            return ERROR_VALUE;
        }

        return onNativeWidth(instanceId);
    }

    public int getHeight() {
        if (instanceId == ERROR_VALUE) {
            Log.e(TAG, "getHeight error");
            return ERROR_VALUE;
        }

        return onNativeHeight(instanceId);
    }

    public int updateFrame(Bitmap bitmap) {
        if (instanceId == ERROR_VALUE) {
            Log.e(TAG, "updateFrame error");
            return ERROR_VALUE;
        }

        return onNativeUpdateFrame(instanceId, bitmap);
    }

    private static native long onNativeLoadGif(String path);

    private static native long onNativeRelease(long instanceId);

    private static native int onNativeWidth(long instanceId);

    private static native int onNativeHeight(long instanceId);

    private static native int onNativeUpdateFrame(long instanceId, Bitmap bitmap);
}
