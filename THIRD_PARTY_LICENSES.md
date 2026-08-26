# 第三方组件与许可证声明

项目自身源代码继续使用根目录 [LICENSE](LICENSE) 中的 Apache License 2.0。引入第三方组件不会替换项目主许可证；各组件仍受其自身许可证约束。

| 组件 | 来源 | 许可证与说明 |
|---|---|---|
| mpvEx / mpvKt 代码基础 | [marlboro-advance/mpvEx](https://github.com/marlboro-advance/mpvEx) | Apache-2.0；保留上游版权与提交历史 |
| 部分播放器功能设计与适配参考 | [Riteshp2001/mpvRx](https://github.com/Riteshp2001/mpvRx) | mpvRx 当前仓库许可证适用于直接移植的代码；本项目在相应文档中注明来源 |
| Anime4K GLSL | [bloc97/Anime4K](https://github.com/bloc97/Anime4K) | MIT；每个着色器保留完整版权头 |
| ArtCNN C4F32 / DN / DS GLSL | [Artoriuz/ArtCNN](https://github.com/Artoriuz/ArtCNN) | MIT；Copyright © Joao Chrisostomo、Kacper Michajłow，文件内保留完整许可证 |
| AniSD ArtCNN 与 Ani4Kv2 ArtCNN 模型 | [Sirosky/Upscale-Hub](https://github.com/Sirosky/Upscale-Hub) | 模型按 [CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/) 分发；ArtCNN 架构为 MIT；文件内保留作者与许可证声明 |
| hdr-toys GLSL | [natural-harmonia-gropius/hdr-toys](https://github.com/natural-harmonia-gropius/hdr-toys) | MIT；完整文本位于 `app/src/main/assets/shaders/hdr-toys/LICENSE` |

APK 内分发的着色器是对应官方 Release 的原始文件，未删除或替换其许可证头。若上游模型许可证限制与项目主许可证不同，使用和再分发时应同时遵守更具体的第三方许可证。
