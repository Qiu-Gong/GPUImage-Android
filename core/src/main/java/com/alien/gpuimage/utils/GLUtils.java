package com.alien.gpuimage.utils;

import android.graphics.Bitmap;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glTexParameterf;

/**
 * OpenGL 工具类。
 */
@Keep
public class GLUtils {

    public static final FloatBuffer RECTANGLE_VERTEX_BUFFER = createFloatBuffer(new float[]{-1, -1, 1, -1, -1, 1, 1, 1});
    public static final FloatBuffer RECTANGLE_2D_TEXTURE_BUFFER = createFloatBuffer(new float[]{0, 0, 1, 0, 0, 1, 1, 1});
    public static final FloatBuffer RECTANGLE_2D_TEXTURE_MIRROR_Y_BUFFER = createFloatBuffer(new float[]{1, 0, 0, 0, 1, 1, 0, 1});
    public static final FloatBuffer RECTANGLE_2D_TEXTURE_MIRROR_X_BUFFER = createFloatBuffer(new float[]{0, 1, 1, 1, 0, 0, 1, 0});
    public static final FloatBuffer RECTANGLE_2D_TEXTURE_MIRROR_X_Y_BUFFER = createFloatBuffer(new float[]{1, 1, 0, 1, 1, 0, 0, 0});

    public static final FloatBuffer FRONT_RECTANGLE_2D_TEXTURE_MIRROR_Y_BUFFER_0 = createFloatBuffer(new float[]{0, 1, 1, 1, 0, 0, 1, 0});
    public static final FloatBuffer FRONT_RECTANGLE_2D_TEXTURE_MIRROR_Y_BUFFER_90 = createFloatBuffer(new float[]{1, 1, 1, 0, 0, 1, 0, 0});
    public static final FloatBuffer FRONT_RECTANGLE_2D_TEXTURE_MIRROR_Y_BUFFER_180 = createFloatBuffer(new float[]{1, 0, 0, 0, 1, 1, 0, 1});
    public static final FloatBuffer FRONT_RECTANGLE_2D_TEXTURE_MIRROR_Y_BUFFER_270 = createFloatBuffer(new float[]{0, 0, 0, 1, 1, 0, 1, 1});
    public static final FloatBuffer[] FRONT_RECTANGLE_2D_TEXTURE_MIRROR_Y_BUFFER = new FloatBuffer[]{
            FRONT_RECTANGLE_2D_TEXTURE_MIRROR_Y_BUFFER_0, FRONT_RECTANGLE_2D_TEXTURE_MIRROR_Y_BUFFER_90,
            FRONT_RECTANGLE_2D_TEXTURE_MIRROR_Y_BUFFER_180, FRONT_RECTANGLE_2D_TEXTURE_MIRROR_Y_BUFFER_270};
    public static final FloatBuffer REAR_RECTANGLE_2D_TEXTURE_MIRROR_Y_BUFFER_0 = createFloatBuffer(new float[]{0, 1, 0, 0, 1, 1, 1, 0});
    public static final FloatBuffer REAR_RECTANGLE_2D_TEXTURE_MIRROR_Y_BUFFER_90 = createFloatBuffer(new float[]{0, 0, 1, 0, 0, 1, 1, 1});
    public static final FloatBuffer REAR_RECTANGLE_2D_TEXTURE_MIRROR_Y_BUFFER_180 = createFloatBuffer(new float[]{1, 0, 1, 1, 0, 0, 0, 1});
    public static final FloatBuffer REAR_RECTANGLE_2D_TEXTURE_MIRROR_Y_BUFFER_270 = createFloatBuffer(new float[]{1, 1, 0, 1, 1, 0, 0, 0});
    public static final FloatBuffer[] REAR_RECTANGLE_2D_TEXTURE_MIRROR_Y_BUFFER = new FloatBuffer[]{
            REAR_RECTANGLE_2D_TEXTURE_MIRROR_Y_BUFFER_0, REAR_RECTANGLE_2D_TEXTURE_MIRROR_Y_BUFFER_90,
            REAR_RECTANGLE_2D_TEXTURE_MIRROR_Y_BUFFER_180, REAR_RECTANGLE_2D_TEXTURE_MIRROR_Y_BUFFER_270};


    public static final float[] IDENTITY_MAT_2X2 = new float[]{1, 0, 0, 1};
    public static final float[] IDENTITY_MAT_UPSIDE_DOWN_2X2 = new float[]{1, 0, 0, 1};
    public static final float[] Y_MIRROR_R0_MAT_2X2 = new float[]{1, 0, 0, -1};
    public static final float[] Y_MIRROR_R90_MAT_2X2 = new float[]{0, -1, -1, 0};
    public static final float[] Y_MIRROR_R180_MAT_2X2 = new float[]{-1, 0, 0, 1};
    public static final float[] Y_MIRROR_R270_MAT_2X2 = new float[]{0, 1, 1, 0};

    public static final float[] Y_MIRROR_R0_MAT_2X2_F = new float[]{-1, 0, 0, -1};
    public static final float[] Y_MIRROR_R90_MAT_2X2_F = new float[]{0, -1, 1, 0};
    public static final float[] Y_MIRROR_R180_MAT_2X2_F = new float[]{1, 0, 0, 1};
    public static final float[] Y_MIRROR_R270_MAT_2X2_F = new float[]{0, 1, -1, 0};

    // Rear vertex matrix list
    public static final float[][] Y_MIRROR_ROTATE_MAT_2X2 = new float[][]{Y_MIRROR_R0_MAT_2X2, Y_MIRROR_R90_MAT_2X2, Y_MIRROR_R180_MAT_2X2, Y_MIRROR_R270_MAT_2X2};
    // Front vertex matrix list
    public static final float[][] Y_MIRROR_ROTATE_MAT_2X2_F = new float[][]{Y_MIRROR_R0_MAT_2X2_F, Y_MIRROR_R90_MAT_2X2_F, Y_MIRROR_R180_MAT_2X2_F, Y_MIRROR_R270_MAT_2X2_F};

    public static final float[] FILTER_R0_MAT_2X2 = new float[]{1, 0, 0, -1};
    public static final float[] FILTER_R90_MAT_2X2 = new float[]{0, 1, 1, 0};
    public static final float[] FILTER_R180_MAT_2X2 = new float[]{-1, 0, 0, 1};
    public static final float[] FILTER_R270_MAT_2X2 = new float[]{0, -1, -1, 0};
    public static final float[][] FILTER_ROTATE_MAT_2X2 = new float[][]{FILTER_R0_MAT_2X2, FILTER_R90_MAT_2X2, FILTER_R180_MAT_2X2, FILTER_R270_MAT_2X2};

    public static final float[] IDENTITY_MAT_4X4 = new float[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    public static final float[] TEX_MAT_R0_4X4 = new float[]{1, 0, 0, 0, 0, -1, 0, 0, 0, 0, 1, 0, 0, 1, 0, 1};
    // Rear default
    public static final float[] TEX_MAT_R90_4X4 = new float[]{0, -1, 0, 0, -1, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1};
    public static final float[] TEX_MAT_R180_4X4 = new float[]{-1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1};
    // Front default
    public static final float[] TEX_MAT_R270_4X4 = new float[]{0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};

    // Rear texture matrix list
    public static final float[][] TEX_MAT_ROTATE_4X4 = new float[][]{TEX_MAT_R90_4X4, TEX_MAT_R90_4X4, TEX_MAT_R90_4X4, TEX_MAT_R90_4X4};
    // Front texture matrix list
    public static final float[][] TEX_MAT_ROTATE_4X4_F = new float[][]{TEX_MAT_R270_4X4, TEX_MAT_R90_4X4, TEX_MAT_R270_4X4, TEX_MAT_R90_4X4};


    public static File saveDir;


    /**
     * 根据指定的着色器脚本创建着色器程序。
     *
     * @param vertexSource   顶点着色器脚本
     * @param fragmentSource 片段着色器脚本
     * @return 着色器程序
     * @throws RuntimeException 当着色器程序创建失败的时候抛出异常
     */
    public static int createProgram(@NonNull String vertexSource, @NonNull String fragmentSource) {
        int program = 0;
        int vertexShader = 0;
        int fragmentShader = 0;
        try {
            vertexShader = createShader(GLES20.GL_VERTEX_SHADER, vertexSource);
            fragmentShader = createShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
            program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            GLES20.glLinkProgram(program);
            int[] result = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, result, 0);
            if (result[0] == GLES20.GL_FALSE) {
                String error = GLES20.glGetProgramInfoLog(program);
                GLES20.glDeleteProgram(program);
                throw new RuntimeException("Failed to create shader program: " + error);
            }
        } finally {
            GLES20.glDeleteShader(vertexShader);
            GLES20.glDeleteShader(fragmentShader);
        }
        return program;
    }

    @IntDef({GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_CUBE_MAP, GLES11Ext.GL_TEXTURE_EXTERNAL_OES})
    private @interface Target {
    }

    @IntDef({GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_MIRRORED_REPEAT, GLES20.GL_REPEAT})
    private @interface WrapMode {
    }

    @IntDef({
            GLES20.GL_NEAREST,
            GLES20.GL_LINEAR,
            GLES20.GL_NEAREST_MIPMAP_NEAREST,
            GLES20.GL_NEAREST_MIPMAP_LINEAR,
            GLES20.GL_LINEAR_MIPMAP_LINEAR,
            GLES20.GL_LINEAR_MIPMAP_NEAREST
    })
    private @interface FilterMode {
    }

    /**
     * 根据指定的参数创建纹理。
     *
     * @param textures   要创建的纹理集合，可以一次性创建多张纹理
     * @param target     纹理类型
     * @param wrapMode   纹理环绕方式
     * @param filterMode 纹理过滤方式
     */
    public static void createTextures(@NonNull int[] textures, @Target int target, @WrapMode int wrapMode, @FilterMode int filterMode) {
        GLES20.glGenTextures(textures.length, textures, 0);
        for (int texture : textures) {
            GLES20.glBindTexture(target, texture);
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, wrapMode);
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, wrapMode);
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, filterMode);
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, filterMode);
        }
    }

    /**
     * 创建 2D 纹理，纹理类型为 {@link GLES20#GL_TEXTURE_2D}，纹理环绕方式为 {@link GLES20#GL_CLAMP_TO_EDGE}，
     * 纹理过滤方式为 {@link GLES20#GL_LINEAR}。
     *
     * @param textures 要创建的纹理集合，可以一次性创建多张纹理
     */
    public static void createTextures2D(@NonNull int[] textures) {
        createTextures(textures, GLES20.GL_TEXTURE_2D, GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_LINEAR);
    }

    /**
     * 创建指定宽高的 2D 纹理，纹理类型为 {@link GLES20#GL_TEXTURE_2D}，纹理环绕方式为 {@link GLES20#GL_CLAMP_TO_EDGE}，
     * 纹理过滤方式为 {@link GLES20#GL_LINEAR}。
     *
     * @param textures 要创建的纹理集合，可以一次性创建多张纹理
     * @param width    宽度
     * @param height   高度
     */
    public static void createTextures2D(@NonNull int[] textures, @IntRange(from = 0) int width, @IntRange(from = 0) int height) {
        createTextures2D(textures);
        for (int texture : textures) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        }
    }

    /**
     * 创建 OES 纹理。
     *
     * @param textures 要创建的纹理集合，可以一次性创建多张纹理
     */
    public static void createExternalOESTextures(@NonNull int[] textures) {
        createTextures(textures, GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_LINEAR);
    }

    @IntDef({GLES20.GL_VERTEX_SHADER, GLES20.GL_FRAGMENT_SHADER})
    private @interface ShaderType {
    }

    /**
     * 根据指定的着色器类型和脚本生成着色器。
     *
     * @param shaderType 着色器类型
     * @param source     着色器脚本
     * @return 着色器
     * @throws RuntimeException 当着色器创建失败的时候抛出异常
     * @see GLES20#GL_VERTEX_SHADER
     * @see GLES20#GL_FRAGMENT_SHADER
     */
    public static int createShader(@ShaderType int shaderType, @NonNull String source) {
        int shader = GLES20.glCreateShader(shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] result = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, result, 0);
        if (result[0] == GLES20.GL_FALSE) {
            String error = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("Failed to create shader: " + error);
        }
        return shader;
    }

    /**
     * 创建 FBO 对象。
     *
     * @param fbo      FBO 对象集合
     * @param textures 纹理集合
     * @param width    宽度
     * @param height   高度
     */
    public static void createFrameBuffers(int[] fbo, int[] textures, int width, int height) {
        createTextures2D(textures);
        GLES20.glGenFramebuffers(fbo.length, fbo, 0);
        for (int i = 0; i < fbo.length; ++i) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[i]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textures[i], 0);
            int fboStatus = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
            int error = GLES20.glGetError();
            if (fboStatus != GLES20.GL_FRAMEBUFFER_COMPLETE && error != GLES20.GL_NO_ERROR) {
                throw new RuntimeException("Failed to create frame buffers: status = " + fboStatus + "; error = " + error);
            }
        }
    }


    public static void BindFrameBuffers(int[] fbo, int texture, int width, int height) {
        GLES20.glGenFramebuffers(1, fbo, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texture, 0);
        int fboStatus = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        int error = GLES20.glGetError();
        if (fboStatus != GLES20.GL_FRAMEBUFFER_COMPLETE && error != GLES20.GL_NO_ERROR) {
            throw new RuntimeException("Failed to create frame buffers: status = " + fboStatus + "; error = " + error);
        }
    }

    /**
     * float[] 转 FloatBuffer
     *
     * @param buffer buffer数组
     * @return FloatBuffer
     */
    public static FloatBuffer createFloatBuffer(float[] buffer) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(buffer.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        floatBuffer.put(buffer);
        floatBuffer.position(0);
        return floatBuffer;
    }

    /**
     * 创建 FloatBuffer
     *
     * @return FloatBuffer
     */
    public static FloatBuffer createFloatBuffer(int size) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(size * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        floatBuffer.position(0);
        return floatBuffer;
    }

    /**
     * 保存纹理内容，格式为jpg字节数组 （测试工具方法）
     *
     * @param ids    纹理id，传0表示读取当前显存
     * @param width  width
     * @param height height
     */
    public static byte[] readTextureToJPG(int ids, int width, int height) {
        ByteBuffer rgbaBuf = ByteBuffer.allocateDirect(width * height * 4);
        rgbaBuf.order(ByteOrder.nativeOrder());
        if (ids == 0) {
            // 读取显存数据
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, rgbaBuf);
        } else {
            // 读取纹理数据
            readTexture(ids, rgbaBuf, width, height);
        }

        ByteArrayOutputStream outputSteam = new ByteArrayOutputStream();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        rgbaBuf.rewind();
        rgbaBuf.position(0);
        bitmap.copyPixelsFromBuffer(rgbaBuf);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputSteam);
        return outputSteam.toByteArray();
    }

    /**
     * 保存纹理内容，格式为Bitmap （测试工具方法）
     *
     * @param ids    纹理id，传0表示读取当前显存
     * @param width  width
     * @param height height
     */
    public static void saveTextureToSdcard(int ids, int width, int height, String fileName) {
        ByteBuffer rgbaBuf = ByteBuffer.allocateDirect(width * height * 4);
        rgbaBuf.order(ByteOrder.nativeOrder());
        if (ids == 0) {
//             读取显存数据
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, rgbaBuf);
        } else {
            // 读取纹理数据
            readTexture(ids, rgbaBuf, width, height);
        }

//         保存ByteBuffer
        saveRgbToSdcard(rgbaBuf, fileName, width, height);
    }

    /**
     * 保存纹理内容，格式为Bitmap （测试工具方法）
     *
     * @param ids    纹理id，传0表示读取当前显存
     * @param width  width
     * @param height height
     */
    public static void saveTextureToSdcard(int ids, int width, int height, int number) {
        ByteBuffer rgbaBuf = ByteBuffer.allocateDirect(width * height * 4);
        rgbaBuf.order(ByteOrder.nativeOrder());
        if (ids == 0) {
            // 读取显存数据
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, rgbaBuf);
        } else {
            // 读取纹理数据
            readTexture(ids, rgbaBuf, width, height);
        }

        // 保存ByteBuffer
        saveRgbToSdcard(rgbaBuf, saveDir.getAbsolutePath() + "/gl_dump_Texture" + width + "_" + height + "number=" + number + ".jpg", width, height);
    }

    /**
     * 将纹理fbo保存为jpeg图片
     *
     * @param fbo    纹理的fbo
     * @param width
     * @param height
     */
    public static void saveFboToSdcard(int fbo, int width, int height) {
        ByteBuffer rgbaBuf = ByteBuffer.allocateDirect(width * height * 4);
        rgbaBuf.order(ByteOrder.nativeOrder());
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, rgbaBuf);
        // 保存ByteBuffer
        saveRgbToSdcard(rgbaBuf, Environment.getExternalStorageDirectory().getAbsolutePath() + "/gl_dump_Fbo" + width + "_" + height + ".jpg", width, height);
    }

    /**
     * 将纹理数据读取到一个ByteBuffer中
     *
     * @param fbo
     * @param width
     * @param height
     * @param buffer
     */
    public static void readPixelsToBuffer(int fbo, int width, int height, ByteBuffer buffer) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
    }

    /**
     * 从纹理中读取数据保存到ByteBuffer
     *
     * @param ids    纹理id
     * @param data   ByteBuffer
     * @param width  ByteBuffer width
     * @param height ByteBuffer height
     */
    private static void readTexture(int ids, ByteBuffer data, int width, int height) {
        int[] offscreenFBO = new int[1];
        GLES20.glGenFramebuffers(1, offscreenFBO, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, offscreenFBO[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, ids, 0);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, data);
        GLES20.glDeleteFramebuffers(1, offscreenFBO, 0);
    }

    /**
     * 保存纹理数据到sdcard
     *
     * @param buf      Buffer
     * @param filename 文件路径
     * @param width    bitmap width
     * @param height   bitmap height
     */
    public static void saveRgbToSdcard(Buffer buf, String filename, int width, int height) {
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(filename));
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            buf.rewind();
            buf.position(0);
            bmp.copyPixelsFromBuffer(buf);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bmp.recycle();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static Bitmap createBitmapFromTexture(int texture, int width, int height) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(width * height * 4);
        readTexture(texture, byteBuffer, width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(byteBuffer);
        return bitmap;
    }

    /**
     * @param fbo    读取fbo后转Bitmap
     * @param width  fbo width
     * @param height fbo height
     * @return 返回Bitmap数据
     */
    public static Bitmap createBitmapFromFbo(int fbo, int width, int height) {
        ByteBuffer byteBuffer;
        byteBuffer = ByteBuffer.allocateDirect(width * height * 4);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.rewind();
        byteBuffer.position(0);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, byteBuffer);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(byteBuffer);
        return bitmap;
    }

    public static ByteBuffer createBufferFromFbo(int fbo, int width, int height) {
        ByteBuffer byteBuffer;
        byteBuffer = ByteBuffer.allocateDirect(width * height * 4);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.rewind();
        byteBuffer.position(0);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, byteBuffer);
        return byteBuffer;
    }

    public static void createBufferFromFbo(ByteBuffer byteBuffer, int fbo, int width, int height) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, byteBuffer);
    }


    /**
     * 根据指定的 [ByteBuffer] 和尺寸创建 [Bitmap]
     *
     * @param buffer 数据源
     * @param width  宽度
     * @param height 高度
     * @return 创建好的 [Bitmap]
     */
    public static Bitmap createBitmapFromBuffer(ByteBuffer buffer, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        return bitmap;
    }

    /**
     * 根据指定的 [ByteBuffer] 和尺寸创建 [Bitmap]
     *
     * @param data   数据源
     * @param width  宽度
     * @param height 高度
     * @return 创建好的 [Bitmap]
     */
    public static Bitmap createBitmapFromByteArray(byte[] data, int width, int height) {
        Bitmap bitmap = null;
        try {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            ByteBuffer byteBuffer = ByteBuffer.wrap(data);
            bitmap.copyPixelsFromBuffer(byteBuffer);
        } catch (OutOfMemoryError error) {
            error.printStackTrace();
        }

        return bitmap;
    }

    /**
     * 保存bitmap到本地
     *
     * @param bitmap   需要保存的bitmap
     * @param savePath 保存路径
     * @return
     */
    public static String saveBitmap(Bitmap bitmap, String savePath, int number) {
        File filePic;
        if (savePath == null || savePath.isEmpty()) {
            savePath = "/sdcard/MeituCamera/dump_pic/";
        }
        try {
            SimpleDateFormat sTimeFormat = new SimpleDateFormat("yyyy-MM-dd_hh:mm:ss.SSS");
            if (number > 0) {
                filePic = new File(savePath + sTimeFormat.format(new Date()) + "_" + number + ".jpg");
            } else {
                filePic = new File(savePath + sTimeFormat.format(new Date()) + ".jpg");
            }
            if (!filePic.exists()) {
                filePic.getParentFile().mkdirs();
                filePic.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(filePic);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return filePic.getAbsolutePath();
    }

    public static void deleteTextures(int[] textures) {
        GLES20.glDeleteTextures(0, textures, 0);
    }

    ////////// 新增工具方法 ///////////

    /**
     * bitmap加载纹理
     *
     * @param img     bitmap图片
     * @param recycle 是否回收
     */
    public static int loadTexture(Bitmap img, boolean recycle, int format) {
        if (img == null || img.isRecycled()) {
            return 0;
        }

        int[] textures = new int[1];
        glGenTextures(1, textures, 0);
        glBindTexture(GL_TEXTURE_2D, textures[0]);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        try {
            if (format == GLES20.GL_LUMINANCE) {
                GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
                android.opengl.GLUtils.texImage2D(GL_TEXTURE_2D, 0, format, img, GL_UNSIGNED_BYTE, 0);
                GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 4);
            } else {
                android.opengl.GLUtils.texImage2D(GL_TEXTURE_2D, 0, format, img, GL_UNSIGNED_BYTE, 0);

            }
        } catch (Exception e) {
            return 0;
        }
        if (recycle) img.recycle();
        return textures[0];
    }

    /**
     * buffer加载纹理
     */
    public static int loadTexture(ByteBuffer buffer, int width, int height, int format) {
        if (width <= 0 || height <= 0) {
            return 0;
        }

        int[] textures = new int[1];
        glGenTextures(1, textures, 0);
        glBindTexture(GL_TEXTURE_2D, textures[0]);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        if (buffer == null) {
            GLES20.glTexImage2D(GL_TEXTURE_2D, 0, format, width, height, 0, format, GL_UNSIGNED_BYTE, buffer);
        } else {
            try {
                if (format == GLES20.GL_LUMINANCE) {
                    GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
                    GLES20.glTexImage2D(GL_TEXTURE_2D, 0, format, width, height, 0, format, GL_UNSIGNED_BYTE, buffer);
                    GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 4);
                } else {
                    GLES20.glTexImage2D(GL_TEXTURE_2D, 0, format, width, height, 0, format, GL_UNSIGNED_BYTE, buffer);

                }
            } catch (Exception e) {
                return 0;
            }
        }

        return textures[0];
    }

    public static Bitmap readTextureToBitmap(int texture, int width, int height) {
        int[] fbo = new int[1];
        GLES20.glGenFramebuffers(1, fbo, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[0]);

        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texture, 0);

        int fboStatus = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (fboStatus != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e("GLUtils", "initFBO failed, status: " + fboStatus);
        }
        Bitmap bmp = readFboToBitmap(fbo[0], width, height);
        GLES20.glDeleteFramebuffers(1, fbo, 0);
        return bmp;
    }

    /**
     * @param fbo    读取fbo后转Bitmap
     * @param width  fbo width
     * @param height fbo height
     * @return 返回Bitmap数据
     */
    public static Bitmap readFboToBitmap(int fbo, int width, int height) {
        ByteBuffer byteBuffer;
        byteBuffer = ByteBuffer.allocateDirect(width * height * 4);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.rewind();
        byteBuffer.position(0);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, byteBuffer);
        return rgbaBufferToBitmap(byteBuffer, width, height);
    }

    public static Bitmap rgbaBufferToBitmap(Buffer buffer, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        return bitmap;
    }

    /**
     * 是否支持浮点纹理，跑一次全局记录
     */
    public static boolean isSupportFloatTexture() {
        int[] nOffScreenFramebuffer = new int[1];
        GLES20.glGenFramebuffers(1, nOffScreenFramebuffer, 0);

        int[] nTexture = new int[1];
        GLES20.glGenTextures(1, nTexture, 0);
        GLES20.glBindTexture(GL_TEXTURE_2D, nTexture[0]);
        int nWidth = 32, nHeight = 32;
        GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GLES30.GL_RGBA32F, nWidth, nHeight, 0, GLES20.GL_RGBA, GLES20.GL_FLOAT, null);

        GLES20.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        GLES20.glViewport(0, 0, nWidth, nHeight);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, nOffScreenFramebuffer[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, nTexture[0], 0);

        int nStatus = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        GLES20.glBindTexture(GL_TEXTURE_2D, 0);
        if (nStatus != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            return false;
        }

        GLES20.glDeleteFramebuffers(1, nOffScreenFramebuffer, 0);
        GLES20.glDeleteTextures(1, nTexture, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        return true;
    }
}
