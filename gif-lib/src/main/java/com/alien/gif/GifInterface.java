package com.alien.gif;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;

import com.getkeepsafe.relinker.ReLinker;

public class GifInterface {

    private static final String TAG = "GifInterface";
    private static final String LIB_NAME = "gif-lib";

    private static volatile boolean hasLoaded = false;
    private long instanceId = -1;

    private static void loadLibrary(Context context) {
        if (!hasLoaded) {
            synchronized (GifInterface.class) {
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

    public GifInterface(Context context) {
        loadLibrary(context);
    }

    public void loadGif(String path) {
        instanceId = onNativeLoadGif(path);
        if (instanceId == -1) {
            Log.d(TAG, "loadGif error");
        }
    }

    public int getWidth() {
        return onNativeWidth(instanceId);
    }

    public int getHeight() {
        return onNativeHeight(instanceId);
    }

    private static native long onNativeLoadGif(String path);

    private static native int onNativeWidth(long instanceId);

    private static native int onNativeHeight(long instanceId);

    private static native int onNativeUpdateFrame(long instanceId, Bitmap bitmap);
}
