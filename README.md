<div align="center">
  <img src="docs/icon-yisou-0.3.6.png" alt="一搜图标：放大镜中的居中竖线" width="100">
  <h1>一搜 · NoFeed Android</h1>
  <p>一款开源的 Android 主动搜索工具 — 直达搜索，远离推荐信息流。</p>
  <p>
    <a href="https://github.com/oldjie/yisou/releases/latest">下载安装</a> ·
    <a href="#功能特性">功能特性</a> ·
    <a href="#界面预览">界面预览</a> ·
    <a href="#安装与使用">使用说明</a> ·
    <a href="PRIVACY.md">隐私政策</a>
  </p>
  <p>
    <a href="https://github.com/oldjie/yisou/releases/latest"><img alt="最新版本" src="https://img.shields.io/github/v/release/oldjie/yisou?label=Release&color=586e62"></a>
    <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-586e62">
    <a href="LICENSE"><img alt="AGPL-3.0-only" src="https://img.shields.io/badge/License-AGPL--3.0--only-586e62"></a>
  </p>
</div>

## 关于一搜

在统一的输入框中输入关键词，选择目标应用，即可发起对应应用内的搜索，减少打开首页后被推荐内容打断的机会。

无需账号，没有广告、订阅或统计 SDK；搜索历史保存在本机。

## 功能特性

- **主动搜索**：统一关键词输入，直达 11 个平台的搜索入口。
- **桌面小组件**：无需打开主界面即可唤起快捷搜索，支持图标横滑与翻页。
- **个性化设置**：支持默认搜索应用、应用手动排序、已安装应用优先，以及深色、浅色和跟随系统外观。
- **本地搜索历史**：可关闭保存、逐条删除或清空历史。
- **系统分享**：从其他应用分享文字到一搜，再选择目标平台搜索。
- **微信搜索辅助**：首次校准后通过可选的系统无障碍服务模拟点击与粘贴。

### 支持平台

豆瓣 · 淘宝 · 闲鱼 · 微信 · 哔哩哔哩 · 拼多多 · 小红书 · 美团 · 携程 · 高德地图 · 淘宝闪购

除微信外的十个平台通过应用搜索入口跳转；微信需要主动开启系统辅助服务。淘宝闪购使用独立客户端（包名 `me.ele`），与淘宝商品搜索分开。

## 界面预览

<div align="center">
  <img src="docs/home-yisou-0.6.0.png" alt="一搜首页" width="29%">
  <img src="docs/more-yisou-0.8.0.png" alt="全部十一项搜索平台" width="29%">
  <img src="docs/settings-dark-yisou-0.7.0.png" alt="深色设置界面" width="29%">
  <p><sub>首页 · 平台选择 · 深色外观（历史版本截图）</sub></p>
  <img src="docs/widget-middle-yisou-0.8.2.png" alt="0.8.2 桌面组件翻页" width="29%">
  <img src="docs/widget-input-yisou-0.6.0.png" alt="桌面快捷搜索窗口" width="29%">
  <p><sub>桌面组件 · 快捷搜索窗口</sub></p>
</div>

## 安装与使用

**系统要求：Android 8.0（API 26）及以上，通用架构。**

1. 前往 [Releases](https://github.com/oldjie/yisou/releases/latest) 下载最新公开预览版 APK。
2. 将 APK 复制到手机，通过系统文件管理器打开安装；系统可能要求允许该文件管理器安装应用。
3. 打开一搜，输入关键词并选择目标平台，发起搜索。

当前开发预览版：**0.8.2**。本地直接构建产物为 `dist/yisou-0.8.2.apk`。

桌面组件的更多使用说明见 [WIDGETS.md](docs/WIDGETS.md)。

## 版本更新

| 版本 | 更新内容 |
| --- | --- |
| **0.8.2** | 小组件按完整可见图标数量翻页，常见四个半图标宽度由 4 页减为 3 页，窄组件由 10 页减为 6 页；边缘裁切项目在下一页完整显示。 |
| 0.8.1 | 修复小组件箭头跳到末尾、漏掉中间应用的问题；支持逐页前进及返回，保持全部十一项可达。 |
| 0.8.0 | 新增美团、携程、高德地图、淘宝闪购；内置新图标，保留已有手动排序，新项追加在后；默认仍按已安装优先。 |

<details>
<summary>搜索入口兼容性说明</summary>

高德使用官方关键词搜索 URI，携程使用原版全局结果模块及其自身的无定位占位参数，不需要一搜获取位置。美团、携程、淘宝闪购未提供未经确认的网页搜索备用地址；未安装或无公开处理入口时显示提示。

新平台的链接及参数已做官方文档或原版代码核对，首次协议后的真实结果页还需手机复测，不能把启动成功视为搜索结果通过。

详见 [研究记录](docs/RESEARCH.md) 与 [验收记录](docs/TESTING.md)。

</details>

## 隐私与安全

应用无联网权限、广告或统计 SDK。默认在应用私有目录保存搜索历史，可关闭、逐条删除或清空。

微信搜索会覆盖剪贴板，并按保存位置模拟点击；不会读取旧剪贴板、界面控件树或截图。坐标可能因布局变化而误触，应重新校准。

详见 [隐私政策](PRIVACY.md) · [安全说明](SECURITY.md)。

## 开源与致谢

灵感来自 [NoFeed](https://apps.apple.com/us/app/nofeed/id6745481301)。本项目是独立编写的 Android 实现，与原作者及各目标平台无隶属关系。首页采用关键词输入框和应用图标，不复制原版素材或付费体系。未核实原版停服状态。

当前原创代码采用 [AGPL-3.0-only](LICENSE) 许可证。历史版本及第三方素材许可见 [LICENSING.md](LICENSING.md) 和 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

欢迎通过 [Issues](https://github.com/oldjie/yisou/issues) 提交反馈，贡献指南见 [CONTRIBUTING.md](CONTRIBUTING.md)。
