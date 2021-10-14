#include <jni.h>
#include <string>
#include <android/bitmap.h>
#include "log.h"

//#define  argb(a, r, g, b) ( ((a) & 0xff) << 24 ) | ( ((b) & 0xff) << 16 ) | ( ((g) & 0xff) << 8 ) | ((r) & 0xff)

extern "C" {
#include "gif_lib.h"
}

struct GifBean {
    int current;
    int total;
    int time;
};

extern "C"
JNIEXPORT jlong JNICALL
Java_com_alien_gif_GifInterface_onNativeLoadGif(JNIEnv *env, jclass clazz, jstring path) {
    const char *path_c = env->GetStringUTFChars(path, 0);

    // gif 操作
    int gif_error;
    GifFileType *type = DGifOpenFileName(path_c, &gif_error);
    if (gif_error != D_GIF_SUCCEEDED) {
        LOGE("loadGif error:%d", gif_error);
        return -1;
    }

    DGifSlurp(type);

    auto *gifBean = static_cast<GifBean *>(malloc(sizeof(GifBean)));
    memset(gifBean, 0, sizeof(GifBean));
    gifBean->current = 0;
    gifBean->total = type->ImageCount;

    env->ReleaseStringUTFChars(path, path_c);
    return (jlong) (type);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_alien_gif_GifInterface_onNativeWidth(JNIEnv *env, jclass clazz, jlong instance_id) {
    auto *type = reinterpret_cast<GifFileType *>(instance_id);
    return type->SWidth;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_alien_gif_GifInterface_onNativeHeight(JNIEnv *env, jclass clazz, jlong instance_id) {
    auto *type = reinterpret_cast<GifFileType *>(instance_id);
    return type->SHeight;
}

inline int argb(unsigned char a, unsigned char r, unsigned char g, unsigned char b) {
    return (((a) & 0xff) << 24) | (((b) & 0xff) << 16) | (((g) & 0xff) << 8) | ((r) & 0xff);
}

int drawFrame(GifFileType *gifFileType, AndroidBitmapInfo info, int *pixels) {
    auto *bean = static_cast<GifBean *>(gifFileType->UserData);

    auto frame = gifFileType->SavedImages[bean->current];
    auto desc = frame.ImageDesc;
    auto *colorMap = desc.ColorMap;

    int *px = (int *) pixels;
    int *line;
    int pointPixel;
    for (int y = desc.Top; y < desc.Top + desc.Height; y++) {
        line = px;
        for (int x = desc.Left; x < desc.Left + desc.Width; x++) {
            pointPixel = (y - desc.Top) * desc.Width + (x - desc.Left);
            auto bits = frame.RasterBits[pointPixel];
            auto color = colorMap->Colors[bits];
            line[x] = argb(255, color.Red, color.Green, color.Blue);
        }
        px = (int *) ((char *) px + info.stride);
    }

    GraphicsControlBlock gcb;//获取控制信息
    DGifSavedExtensionToGCB(gifFileType, bean->current, &gcb);
    return gcb.DelayTime * 10;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_alien_gif_GifInterface_onNativeUpdateFrame(JNIEnv *env, jclass clazz, jlong instance_id,
                                                    jobject bitmap) {
    auto *type = reinterpret_cast<GifFileType *>(instance_id);
    int *pixels = nullptr;
    int delay = 0;

    // Bitmap 加锁
    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, bitmap, &info);
    AndroidBitmap_lockPixels(env, bitmap, reinterpret_cast<void **>(&pixels));

    // 绘制
    delay = drawFrame(type, info, pixels);

    // Bitmap 解锁
    AndroidBitmap_unlockPixels(env, bitmap);
    auto *gifBean = static_cast<GifBean *>(type->UserData);
    gifBean->current++;

    // 重置数据
    if (gifBean->current > gifBean->total - 1) {
        gifBean->current = 0;
        return -1;
    }

    return delay;
}
