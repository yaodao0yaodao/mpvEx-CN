# HDR 输出使用说明

mpvEx 的 HDR 输出功能移植自 [mpvRx](https://github.com/Riteshp2001/mpvRx)，其中三种模式使用 [hdr-toys](https://github.com/natural-harmonia-gropius/hdr-toys) 的 GLSL 着色器处理色调和色域。hdr-toys 主要用于 HDR 到 SDR 的色彩转换，并以 MIT License 发布。

> HDR 输出仍是实验性功能。显示效果取决于视频、屏幕能力、系统色彩管理和 GPU 驱动；选用不适合片源的模式可能造成颜色或亮度异常。

## 如何开启

1. 进入“设置 → 解码器”。
2. 打开“HDR 输出（实验性）”。
3. 选择 HDR 模式。
4. 退出当前播放并重新打开视频，使渲染后端和色彩管线完整生效。

开启 HDR 后，播放器会自动优先使用 `gpu-next`。设备支持 Vulkan 1.3 时会选择 Vulkan，否则使用 OpenGL；不必再手动组合渲染后端。

## 四种模式怎么选

| 模式 | 适合情况 | 处理方式 |
|---|---|---|
| BT.2100 PQ | HDR10 视频 | 使用 PQ 传递函数、Astra 色调映射和 Bottosson 色域映射 |
| BT.2100 HLG | HLG HDR 视频 | 使用 HLG 传递函数以及 Astra、Bottosson 着色器链 |
| BT.2020 | BT.2020 广色域内容，或不确定时先尝试 | 使用 BT.1886 与 Bottosson 进行较轻量的色域转换 |
| 线性 HDR | HDR 屏幕和支持 Vulkan 1.3 的设备 | 使用 `gpu-next` 原生线性光 HDR，不加载 hdr-toys 着色器 |

不知道选什么时，可以先选 **BT.2020**。HDR10 片源优先试 **BT.2100 PQ**，HLG 片源选择 **BT.2100 HLG**。

## 将 SDR 增强为 HDR

这个开关只在线性 HDR 模式下使用。它通过逆色调映射扩大 SDR 视频的亮度范围，并不会把普通 SDR 片源变成真正包含 HDR 信息的片源。

如果高光过亮、颜色不自然或暗部细节减少，请关闭此开关。

## 常见问题

- **颜色发灰或过饱和：** 先确认模式与片源匹配；不确定时关闭 HDR，对比 mpv 的自动处理结果。
- **画面过暗或过亮：** 改用 BT.2020，或关闭 SDR→HDR 增强。
- **掉帧、发热或耗电明显增加：** 关闭 Anime4K/ArtCNN 等其他着色器，避免多个重型效果叠加。
- **线性 HDR 不可选：** 设备需要 Android 13、Vulkan 1.3 和 OpenGL ES 3.1；不满足条件时仍可使用另外三种 hdr-toys 模式。

GPU 去色带是独立功能，可在播放器的视频滤镜面板中选择 CPU 或 GPU，并调整迭代、阈值、范围和颗粒参数。
