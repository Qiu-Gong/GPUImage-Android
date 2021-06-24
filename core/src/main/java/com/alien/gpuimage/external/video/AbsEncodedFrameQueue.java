package com.alien.gpuimage.external.video;


import android.media.MediaCodec;
import android.os.Handler;

import com.alien.gpuimage.utils.Logger;

import java.nio.ByteBuffer;

public abstract class AbsEncodedFrameQueue<Output> {

    private static final String TAG = "EncodedFrameQueue";

    private ByteBuffer[] mBuffers;
    private MediaCodec.BufferInfo[] mBufferInfos;
    private int mReadIndex = 0;
    private int mWriteIndex = 0;
    private Object mLock = new Object();

    AbsEncodedFrameQueue(int maxFrameCount) {
        mBuffers = new ByteBuffer[maxFrameCount];
        mBufferInfos = new MediaCodec.BufferInfo[maxFrameCount];
    }

    public void cacheFrame(ByteBuffer frame, MediaCodec.BufferInfo info) {
        while (true) {
            synchronized (mLock) {
                if ((mWriteIndex + 1) % mBuffers.length == mReadIndex) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    break;
                }
            }
        }

        if (mBuffers[mWriteIndex] == null || mBuffers[mWriteIndex].capacity() < info.size) {
            mBuffers[mWriteIndex] = ByteBuffer.allocateDirect(info.size * 2);
        }
        mBuffers[mWriteIndex].rewind();
        mBuffers[mWriteIndex].put(frame);

        mBufferInfos[mWriteIndex] = info;
        mWriteIndex = (mWriteIndex + 1) % mBuffers.length;
    }


    /**
     * 帧信息写到输出流中
     *
     * @param output  输出类型
     * @param handler 在哪个线程执行输出数据写入
     */
    public void writeSampleData(final Output output, Handler handler) {
        handler.post(() -> {
            if (mWriteIndex == mReadIndex) {
                Logger.d(TAG, "no data write to output:");
                return;
            }
            long t = System.currentTimeMillis();
            try {
                doWriteSampleData(output, mBuffers[mReadIndex], mBufferInfos[mReadIndex]);

            } catch (IllegalStateException e) {
                Logger.e(TAG, "discard some encoded packet");
                e.printStackTrace();
            }

            t = System.currentTimeMillis() - t;
            if (t > 100) {
                Logger.d(TAG, "write sample data block for " + Long.toString(t) + " millisecond");
            }

            synchronized (mLock) {
                mReadIndex = (mReadIndex + 1) % mBuffers.length;
                mLock.notify();
            }
        });

    }

    /**
     * 帧信息写到输出流中
     *
     * @param outputStream 输出类型
     */
    protected abstract void doWriteSampleData(final Output outputStream, ByteBuffer byteBuffer, MediaCodec.BufferInfo byteInfo);
}
