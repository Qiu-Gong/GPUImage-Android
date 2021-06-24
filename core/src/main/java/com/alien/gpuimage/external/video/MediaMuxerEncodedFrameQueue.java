package com.alien.gpuimage.external.video;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;

import com.alien.gpuimage.utils.Logger;

import java.nio.ByteBuffer;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MediaMuxerEncodedFrameQueue extends AbsEncodedFrameQueue<MediaMuxer> {

    private static final String TAG = "MediaMuxerEncodedFrameQueue";
    private int mTrackIndex;

    public MediaMuxerEncodedFrameQueue(int maxFrameCount) {
        super(maxFrameCount);
    }

    public void setTrackIndex(int trackIndex) {
        mTrackIndex = trackIndex;
    }

    @Override
    protected void doWriteSampleData(MediaMuxer outputStream, ByteBuffer byteBuffer, MediaCodec.BufferInfo byteInfo) {

        try {
            Log.d("QIUGONG",
                    "onDataAvailable bufferInfo: flags=" + byteInfo.flags + " time=" + byteInfo.presentationTimeUs + " size=" + byteInfo.size + " offset="+byteInfo.offset
            );
            outputStream.writeSampleData(mTrackIndex, byteBuffer, byteInfo);

        } catch (IllegalStateException e) {
            Logger.e(TAG, "discard some encoded packet");
            e.printStackTrace();
        }
    }
}
