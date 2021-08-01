### 项目背景

`GPUImage `框架是开源 IOS 类库，基于 `OpenGL` 进行图像和视频处理。而 Android 项目开发过程中，苦于没有一套这样完善的框架，做出的技术方案时常跟 IOS 无法对齐，导致方案不统一的问题。

github 上有一个 [android-gpuimage](https://github.com/cats-oss/android-gpuimage)，但在项目应用过程中无法添加自定义滤镜和串联渲染链路。做为demo显示滤镜效果还是可以使用，但是在实际项目开发中即使做了大量修改，仍然无法满足项目需求。这个工程没有很好的吸取 IOS 那套多输入，多输出的核心精髓，只是引用了一些酷炫滤镜。



### GPUImage-Android

`GPUImage-Android`  https://github.com/Qiu-Gong/GPUImage-Android 吸取了 `IOS` 多输入多输出核心精髓，剔除了大量冗余接口，增加 Android 常用输入输出方式，重新设计了内存自动回收机制。我们举几种场景例子。

##### 例子1

一张图片，分别显示4种滤镜效果。我们需要创建 `input` 如 `Picture` 输入，多个 `Filter` 效果，还有需要显示的 `output` 如 `ImageView，SurfaceView，TextureView`，当然这三个不是系统控件，而是继承了 output 功能。

![例子1](https://raw.githubusercontent.com/Qiu-Gong/GPUImage-Android/main/md/WX20210715-070049.png)

以下伪代码：

```kotlin
val picture = Picture(bitmap, true)
val filter1 = Filter1()
val filter2 = Filter2()
val filter22 = Filter22()
val filter3 = Filter3()
val filter33 = Filter33()
val filter4 = Filter4()
// 第一路
picture?.addTarget(filter1)
filter1?.addTarget(imageview)
// 第二路
picture?.addTarget(filter2)
filter2?.addTarget(filter22)
filter22?.addTarget(surfaceview)
// 第三路
picture?.addTarget(filter3)
filter3?.addTarget(filter33)
filter33?.addTarget(textureview)
// 第四路
filter3?.addTarget(filter4)
filter4?.addTarget(imageview2)
// 执行
picture?.processPicture()
```

从上面例子中，可以看出非常灵活组合滤镜和输出。一个输入可以多输出，输出还可以继续再多个输出，形成了多链路渲染。

##### 例子2

一个视频，加入滤镜和水印，一边播放，一边保存。我们需要创建 `input` 如 `ExoplayerPipeline` 这个已集成 `exoplayer` 播放器当输入功能。创建 `Filter1` 和 `WaterMarkFilter` 滤镜。`output` 显示的 TextureView，以及mp4格式编码的 VideoEncoder。

![视频](https://raw.githubusercontent.com/Qiu-Gong/GPUImage-Android/main/md/WX20210715-070928.png)

以下伪代码：

```kotlin
val exoPipeline = ExoplayerPipeline()
val filter1 = Filter1()
val watermark = WaterMarkFilter()
val encoder = VideoEncoder()
val simpleExoPlayer = SimpleExoPlayer()

// 配置链路
exoPipeline?.addTarget(filter1)
filter1?.addTarget(watermark)
watermark?.addTarget(textureview)
watermark?.addTarget(encoder)
// 设置 exoplayer 的 surface 为 exoPipeline 的 surface
simpleExoPlayer.setVideoSurface(exoPipeline!!.getSurface())

// exo 配置开启
.......
```

上面例子，当视频播放完成后，那么编码也就同步完成了。目前只支持视频保存，音频还未加入。

##### 例子3

两张图片进行融合，一张原图，一张只带 `a通道` 的mask图片 (a通道为透明通道)。

![mask](https://raw.githubusercontent.com/Qiu-Gong/GPUImage-Android/main/md/WX20210715-073225.png)

以下伪代码：

```kotlin
val picture = Picture()
val mask = Picture()
val maskBlendFilter = MaskBlendFilter()

picture.addTarget(maskBlendFilter)
mask.addTarget(maskBlendFilter)
maskBlendFilter.addTarget(textureView)

picture.processPicture()
mask.processPicture()
```

前面讲的都是多输出的方式，例子3主要是讲解多输入的方式同样也是可行的。目前只支持的2通道输入，后面会随需求陆续添加更多通道。



### 以下为总设计图

![设计总图](https://raw.githubusercontent.com/Qiu-Gong/GPUImage-Android/main/md/WX20210714-072349.png)

该项目主要有 7 大块组成，**多输入**，**多输出**，**EGL环境**，**GLProgram GL程序管理**，**GLContext 核心类也是主类**，**Filter 所有滤镜父类**，**FrameBufferCache 纹理缓存管理类** 。下面对各个模块简单描述。

1. 输入有 `Picture` bitmap 转纹理做为输入。 `TextureInput` 传入纹理直接输入。`VideoDecoder` 负责把视频解码后，每一帧转为纹理做为输入。 `ExoplayerPipeline`  做为 exoplayer surface 桥接类，输出每一帧将转为纹理做为输入。
2. 输出有 `BitmapView` 将纹理转 bitmap 做为输出。 `ImageView` 将纹理转 bitmap 直接显示到屏幕。  `SurfaceView`  `TextureView` 这两个surface显示类，你将 `SurfaceView` 做为显示窗口，也可以将  `TextureView` 做为显示窗口，这两个的差异性可以查看下这里 [SurfaceView及TextureView区别](https://blog.csdn.net/while0/article/details/81481771)，正常情况下我们首选 SurfaceView，在有些情况不能满足时再选择 TextureView。  `VideoEncoder` 视频编码类，把纹理编码为 mp4 视频格式进行存储，你可以把图片上效果后转为视频保存起来，也可以把解码后的视频再合成起来。
3. EGL环境。要使用GPU是需要创建环境的，用EGL创建起环境来。这样就可以使用 OpenGL 来操作 GPU 了。 `WindowSurface` 是创建显示窗口，如 SurfaceView, TextureView, ImageReaderPipeline, ImageReaderPipeline 就需要创建这个。 `OffscreenSurface` 是离屏渲染，EGL环境起来后要立马创建 OffscreenSurface，因为后面的所有渲染操作，都离不开它。
4. GLProgram OpenGL 程序管理。要让GPU工作，是需要载入程序的。通常有2种一种是片段着色器，另外一种是顶点做色器。GLProgram 负责编译载入这些，并生成对应的 program id。
5. GLContext 做为唯一外部可以访问的核心类。负责接口转发，线程管理，环境创建与释放，program缓存管理，。
6. Filter所有滤镜父类，继承 **input **和 **output** 接口。所有的滤镜，都是继承这个进行修改。
7. FrameBufferCache 纹理缓存管理。**FrameBuffer** 用来生产纹理并绑定到 **FBO**。**FrameBufferCache** 使用纹理复用机制，当需要 FrameBuffer 时从 FrameBufferCache 取出，使用完回收到 FrameBufferCache 中。使用复用机制可以大大减少纹理的创建，减少内存使用。



### 目录结构






