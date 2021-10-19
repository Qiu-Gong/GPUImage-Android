package com.alien.gif;

import android.content.Context;
import android.os.Build;
import android.text.format.Formatter;
import android.util.Log;

import com.getkeepsafe.relinker.ReLinker;

import java.io.File;

/**
 * Compresses a GIF so it can be sent via MMS.
 * <p>
 * The entry point lives in its own class, we can defer loading the native GIF transcoding library
 * into memory until we actually need it.
 */
public class GifTranscoder {

    private static final String TAG = "GifTranscoder";

    private static final String LIB_NAME = "gif-lib";

    private static volatile boolean hasLoaded = false;

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

    public static boolean transcode(Context context, String filePath, String outFilePath) {
        loadLibrary(context);
        final long inputSize = new File(filePath).length();
        final boolean success = transcodeInternal(filePath, outFilePath);
        final long outputSize = new File(outFilePath).length();
        final float compression = (inputSize > 0) ? ((float) outputSize / inputSize) : 0;

        if (success) {
            Log.i(TAG, String.format("Resized GIF (%s) in => %s (%.0f%%)",
                    Formatter.formatShortFileSize(context, inputSize),
                    Formatter.formatShortFileSize(context, outputSize),
                    compression * 100.0f));
        }
        return success;
    }

    private static native boolean transcodeInternal(String filePath, String outFilePath);
}
