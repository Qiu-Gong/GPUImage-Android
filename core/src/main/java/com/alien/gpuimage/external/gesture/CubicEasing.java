package com.alien.gpuimage.external.gesture;

public final class CubicEasing {

    public static float easeOut(float time, float start, float end, long duration) {
        return end * ((time = time / duration - 1.0f) * time * time + 1.0f) + start;
    }

    public static float easeIn(float time, float start, float end, long duration) {
        return end * (time /= duration) * time * time + start;
    }

    public static float easeInOut(float time, float start, float end, long duration) {
        return (time /= duration / 2.0f) < 1.0f ? end / 2.0f * time * time * time + start : end / 2.0f * ((time -= 2.0f) * time * time + 2.0f) + start;
    }
}
