![banner](fastlane/metadata/android/en-US/images/featureGraphic.png)

# mpvEx-CN（mpvExtended 简体中文维护版）
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/yaodao0yaodao/mpvEx-CN.svg?logo=github&label=GitHub&cacheSeconds=3600)](https://github.com/yaodao0yaodao/mpvEx-CN/releases/latest)
[![GitHub all releases](https://img.shields.io/github/downloads/yaodao0yaodao/mpvEx-CN/total?logo=github&cacheSeconds=3600)](https://github.com/yaodao0yaodao/mpvEx-CN/releases/latest)

> [!IMPORTANT]
> **新版开发预告**
>
> 项目正在进行大规模重构，预计于 **2026 年 9 月中旬**更新新版。新版计划引入 Android 原生 [ExoPlayer](https://exoplayer.dev/) 播放内核，解决 MPV 内核只能通过 Linear HDR 实现屏幕真实 HDR 输出链路的问题。
>
> - **MPV 内核：** 专门用于 SDR→HDR 增强、AI 超分辨率和特效字幕等高级播放功能。
> - **ExoPlayer 内核：** 负责 Android 原生 HDR 输出链路，重点提供更高性能和更低功耗；缺点是无法使用特效字幕。

这是面向中文用户独立维护的 Android mpv/libmpv 开源视频播放器，源自 [marlboro-advance/mpvEx](https://github.com/marlboro-advance/mpvEx)，播放器内的软件名称仍为 **mpvEx**。项目提供完整简体中文界面、ARM64 APK、Anime4K/ArtCNN 放大、SDR→HDR 增强、智能播放保护、字幕轨标题智能匹配等实用改进。

ArtCNN、SDR→HDR 增强、智能渲染后端及部分播放性能保护的设计与实现参考并适配自 [Riteshp2001/mpvRx](https://github.com/Riteshp2001/mpvRx)。由于两个项目的代码结构不同，本项目采用了独立适配；温控判断按 Android 官方 API 的数值含义重新实现，并非直接复制。

- **汉化日期：** 2026-08-19
- **汉化时版本：** v1.2.9
- **当前中文版版本：** v1.3.2-test.2（测试版）
- **独立包名：** `io.github.yaodao0yaodao.mpvex`，可与上游版本同时安装
- **构建架构：** 仅提供 `arm64-v8a`
- **附加改动：** 解码器优先级与临时切换；硬件解码增强兼容模式；画面比例永久保存；字幕轨标题智能选择；音量调节优化；可靠缩略图；Anime4K/ArtCNN；SDR→HDR 增强；智能渲染与播放保护；无首帧和声音泄漏的历史进度续播；应用内更新指向本项目 Releases
- **维护与构建：** 本项目已停止无验证的上游自动合并；每次推送均由 GitHub Actions 构建 ARM64 APK，上游改动会在人工检查和适配后按需引入

## 界面预览

<div class="image-row" align="center">
  <img src="docs/images/player-landscape.jpg" alt="mpvEx 横屏播放界面与 SDR→HDR 增强控件" width="98%" />
</div>

<div class="image-row" align="center" justify-content="space-between">
  <img src="docs/images/library.jpg" alt="mpvEx 中文媒体库" width="23.5%" />
  <img src="docs/images/decoder-settings.jpg" alt="mpvEx 中文解码器设置" width="23.5%" />
  <img src="docs/images/mpv-profiles.jpg" alt="mpvEx MPV 配置档" width="23.5%" />
  <img src="docs/images/about-update.jpg" alt="mpvEx 关于与更新界面" width="23.5%" />
</div>

## 项目简介

mpvExtended 是基于 libmpv 的 Android 视频播放器，源自 [mpv-android](https://github.com/mpv-android/mpv-android)。它把 mpv 强大的格式兼容性、渲染与脚本能力，整合到适合触屏操作的 Material 3 界面中，支持硬件/软件解码、字幕与外部音轨、画中画、后台播放、网络串流、SMB/FTP/WebDAV、播放列表、逐帧导航和画面缩放等功能。

本仓库重点维护完整的简体中文界面。若发现漏译、术语错误或上游同步造成的界面回退，请在本项目的 [Issues](https://github.com/yaodao0yaodao/mpvEx-CN/issues) 中反馈，并附上界面路径和截图。

### 维护定位

本项目希望保持播放器简单、稳定，不以增加大量界面自定义选项为目标。维护重点是让软件在中文环境下更易理解、更顺手，并选择性加入确实有助于播放体验、兼容性、性能或功耗的实用功能。

这也是本项目与 [Riteshp2001/mpvRx](https://github.com/Riteshp2001/mpvRx) 的定位差异：我们会参考并适配其中合适的播放功能，但不会继续移植主题、布局等 UI 自定义体系。对界面外观和布局自定义有较高要求的用户，建议直接使用功能更丰富的 [mpvRx](https://github.com/Riteshp2001/mpvRx)。

## 中文版增强

相较于上游 [marlboro-advance/mpvEx](https://github.com/marlboro-advance/mpvEx)，本项目主要增加或调整了：

- 完整简体中文界面，以及更适合中文片源的字幕轨智能匹配和在线字幕默认语言。
- 独立包名、ARM64 APK、项目内更新检查，以及每次推送自动构建。
- 可排序的“解码器优先级”；播放界面可临时切换三种解码模式，退出播放后恢复设置顺序。
- 硬件解码增强兼容模式可保留 `gpu-next`，并在播放期间暂停 Vulkan、线性 HDR 和 Anime4K；原生 HDR 仍按片源类型自动输出。
- 画面比例永久保存、音量调节优化、缩略图可靠性改进，以及不会泄漏文件开头画面或声音的历史进度续播。
- Anime4K、ArtCNN，以及面向高分辨率、温度和播放压力的自动保护。
- 播放界面 SDR→HDR 增强、智能渲染后端和播放性能优化。
- [Anime4K/ArtCNN 中文说明](docs/Anime4K.zh-CN.md)与[解码器和省电配置说明](docs/Decoder-and-Battery.zh-CN.md)。

## 上游介绍

**mpvExtended is a fork of [mpv-android](https://github.com/mpv-android/mpv-android), built on the libmpv library. It aims
to combine the powerful features of mpv with an easy to use interface and additional
features.**

- Simpler and Easier to Use UI
- Material3 Expressive Design
- Advanced Configuration and Scripting
- Enhanced Playback Features
- Picture-in-Picture (PiP)
- Background Playback
- High-Quality Rendering
- Network Streaming
- File Management
- Completely free and open source and without any ads or excessive permissions
- Media picker with tree and folder view modes
- External Subtitle support
- Zoom gesture
- External Audio support
- Search Functionality
- SMB/FTP/WebDAV support
- Custom Playlist management support

**This project is still in development and is expected to have bugs. Please report any bugs you find in
the [upstream Issues](https://github.com/marlboro-advance/mpvEx/issues) section.**

---

## Installation

### Stable Release
从本项目的 [GitHub Releases](https://github.com/yaodao0yaodao/mpvEx-CN/releases) 下载最新的已签名 ARM64 APK。

[![Download Release](https://img.shields.io/badge/Download-ARM64%20APK-blue?style=for-the-badge)](https://github.com/yaodao0yaodao/mpvEx-CN/releases/latest)

> 本项目使用独立包名和签名，不能覆盖安装上游版本；两者可以共存。

> `v1.3.2-test.2` 是解码器切换重构测试版，修复了切换解码器时过早销毁 GPU/硬件合成资源导致的原生层闪退。请在 [Releases](https://github.com/yaodao0yaodao/mpvEx-CN/releases) 页面手动选择；GitHub 的“最新稳定版”仍指向 v1.3.1。

---

## Building

### Prerequisites

- JDK 17
- Android SDK with build tools 34.0.0+
- Git (for version information in builds)

### APK Variant

This fork builds only **arm64-v8a**, for modern 64-bit ARM Android devices.

---

## Releases

### Setting Up Release Signing

To enable automatic signing for release builds in GitHub Actions, you need to configure the
following secrets in your GitHub repository:

1. Navigate to your repository on GitHub
2. Go to **Settings** → **Secrets and variables** → **Actions**
3. Add the following repository secrets:

| Secret Name              | Description                                          |
|--------------------------|------------------------------------------------------|
| `SIGNING_KEYSTORE`       | Base64-encoded keystore file (`.jks` or `.keystore`) |
| `SIGNING_KEY_ALIAS`      | The alias name used when creating the keystore       |
| `SIGNING_STORE_PASSWORD` | Password for the keystore file                       |
| `KEY_PASSWORD`           | Password for the key (can be same as store password) |

#### Encoding Your Keystore

To encode your keystore file to base64:

**Linux/macOS:**

```bash
base64 -i your-keystore.jks | tr -d '\n' > keystore.txt
```

**Windows (PowerShell):**

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("your-keystore.jks")) | Out-File -FilePath keystore.txt -NoNewline
```

Copy the contents of `keystore.txt` and paste it as the value for the `SIGNING_KEYSTORE` secret.

### Creating a Release

1. 发布标签必须与应用的 `versionName` 完全一致；修复版本递增补丁号（例如 `1.3.0` → `1.3.1`）
2. Commit the changes
3. Create and push a tag:
   ```bash
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin v1.0.0
   ```
4. GitHub Actions will automatically build and sign the ARM64 APK, generate its SHA-256 checksum, and publish the stable release

### Creating a Preview Release

1. Create and push a preview tag:
   ```bash
   git tag -a v1.0.0-preview.1 -m "Preview release"
   git push origin v1.0.0-preview.1
   ```
2. GitHub Actions will create a pre-release automatically

---

## Acknowledgments

- [mpv-android](https://github.com/mpv-android)
- [mpvKt](https://github.com/abdallahmehiz/mpvKt)
- [Next player](https://github.com/anilbeesetti/nextplayer)
- [Gramophone](https://github.com/FoedusProgramme/Gramophone)
- [mpvRx](https://github.com/Riteshp2001/mpvRx)（ArtCNN、SDR→HDR 增强及部分性能功能参考实现）

---

## Support the Project <img src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Smilies/Heart%20with%20Ribbon.png" alt="Heart with Ribbon" width="25" height="25" />

If you find mpvExtended useful, consider supporting the development:

[![UPI](https://img.shields.io/badge/UPI-aadiinarvekar@upi-blue?style=for-the-badge&logo=google-pay&logoColor=white)](upi://pay?pa=aadiinarvekar@upi)

---
## Star History <img src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Travel%20and%20places/Star.png" alt="Star" width="25" height="25" />

<a href="https://www.star-history.com/#marlboro-advance/mpvEx&type=date&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=marlboro-advance/mpvEx&type=date&theme=dark&legend=top-left" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=marlboro-advance/mpvEx&type=date&legend=top-left" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=marlboro-advance/mpvEx&type=date&legend=top-left" />
 </picture>
</a>
