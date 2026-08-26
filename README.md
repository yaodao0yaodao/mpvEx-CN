![banner](fastlane/metadata/android/en-US/images/featureGraphic.png)

# mpvEx-CN（mpvExtended 简体中文维护版）
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/yaodao0yaodao/mpvEx-CN.svg?logo=github&label=GitHub&cacheSeconds=3600)](https://github.com/yaodao0yaodao/mpvEx-CN/releases/latest)
[![GitHub all releases](https://img.shields.io/github/downloads/yaodao0yaodao/mpvEx-CN/total?logo=github&cacheSeconds=3600)](https://github.com/yaodao0yaodao/mpvEx-CN/releases/latest)

这是面向中文用户独立维护的 Android mpv/libmpv 开源视频播放器，源自 [marlboro-advance/mpvEx](https://github.com/marlboro-advance/mpvEx)，播放器内的软件名称仍为 **mpvEx**。项目提供完整简体中文界面、AI 超分辨率、SDR→HDR 增强、自动播放保护、字幕轨标题智能匹配等实用改进。

ArtCNN、HDR、ThumbFast 定位预览、字幕双指缩放、统计/控制台及部分播放逻辑参考并适配自 [Riteshp2001/mpvRx](https://github.com/Riteshp2001/mpvRx)。两个项目结构不同，本项目按自身定位重新组合，并未移植 mpvRx 的 UI 自定义体系。

- **汉化日期：** 2026-08-19
- **汉化时版本：** v1.2.9
- **当前测试版本：** v1.4.0-test.1；包含播放器控制、解码回退、AI 超分辨率与运行时自动控制的大版本重构
- **独立包名：** `io.github.yaodao0yaodao.mpvex`，可与上游版本同时安装
- **附加改动：** 解码器优先级与临时切换、AI 超分辨率、SDR→HDR 增强、自动裁黑边、ThumbFast 定位预览、自动播放保护，以及更适合中文片源的字幕选择
- **维护与构建：** 已停止无验证的上游自动合并；每次提交由 GitHub Actions 自动构建，上游改动只在人工检查和适配后引入

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
- 独立包名、项目内更新检查，以及每次提交自动构建。
- 可排序的解码器优先级；硬件解码增强由用户主动启用，播放界面切换只在本次播放器内生效。
- 硬件解码增强保留 `gpu-next`，播放期间暂停 Vulkan、线性 HDR 与 AI 超分辨率，并明确提示其字幕兼容 HDR 实际为 HDR→SDR。
- 播放速度单击在 1×/2×/3× 循环；进度条一次提交定位并强制使用 ThumbFast 预览；字幕支持双指缩放。
- 保守、稳定且为下方硬字幕预留空间的自动剪切黑边，画面比例与 AI 放大倍率按剪切后的有效画面计算。
- Anime4K A/B/C/C+ 与多种 ArtCNN/AniSD 模型；仅在显示区域确实需要约 1.2× 放大时临时加载。
- 原生 HDR 自动进入线性 HDR；SDR→HDR 增强由播放控件永久开关；硬件解码增强下显示片源 HDR 类型但不伪装成 HDR 输出。
- 全新的低帧率自动控制，不监控温度，只在连续 10 秒实际渲染不足时逐级临时减负，切换文件后恢复。
- 音量调节优化、可靠缩略图、无首帧或声音泄漏的历史进度续播、低电量解码建议、中文统计页与控制台。
- [Anime4K/ArtCNN 中文说明](docs/Anime4K.zh-CN.md)与[解码器和省电配置说明](docs/Decoder-and-Battery.zh-CN.md)。

## 上游介绍

mpvExtended 是基于 libmpv 的 [mpv-android](https://github.com/mpv-android/mpv-android) 分支，目标是把 mpv 的强大能力放进易用的移动界面，并补充实用播放功能。上游提供 Material 3 Expressive 界面、高级配置与脚本、画中画、后台播放、高质量渲染、网络串流、文件管理、文件夹媒体选择、外部字幕与音轨、手势缩放、搜索、SMB/FTP/WebDAV 以及自定义播放列表；软件完全开源、无广告，也不索取与功能无关的权限。

上游仍在开发，可能存在问题。仅在上游原版也能复现的问题请反馈到 [上游 Issues](https://github.com/marlboro-advance/mpvEx/issues)；本项目新增功能或中文界面问题请反馈到 [mpvEx-CN Issues](https://github.com/yaodao0yaodao/mpvEx-CN/issues)。

---

## Installation

### Stable Release
从本项目的 [GitHub Releases](https://github.com/yaodao0yaodao/mpvEx-CN/releases) 下载最新的已签名 APK。

[![Download Release](https://img.shields.io/badge/Download-APK-blue?style=for-the-badge)](https://github.com/yaodao0yaodao/mpvEx-CN/releases/latest)

> 本项目使用独立包名和签名，不能覆盖安装上游版本；两者可以共存。

> `v1.4.0-test.1` 是大版本重构测试版，请在 [Releases](https://github.com/yaodao0yaodao/mpvEx-CN/releases) 页面手动选择；GitHub 的“最新稳定版”仍指向 v1.3.1。

---

## Building

### Prerequisites

- JDK 17
- Android SDK with build tools 34.0.0+
- Git (for version information in builds)

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
4. GitHub Actions will automatically build and sign the APK, generate its SHA-256 checksum, and publish the stable release

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
- [ArtCNN](https://github.com/Artoriuz/ArtCNN)、[Upscale-Hub](https://github.com/Sirosky/Upscale-Hub)、[Anime4K](https://github.com/bloc97/Anime4K)

完整来源与许可证见 [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md)。项目主许可证仍为 Apache-2.0。

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
