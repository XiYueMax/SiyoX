<p align="center">
  <img src="app/src/main/res/drawable/logo.png" width="120" height="120" alt="SiyoX Logo" />
</p>

<p align="center">
  <strong>基于 Android 平台的《我的世界》客户端辅助与多网络验证注入框架</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-C++%20%7C%20Java-blue.svg" alt="Languages" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License" />
</p>

<p align="center">
  <a href="https://xiyuemax.github.io/SiyoX-Docs"><strong>点击访问 SiyoX 官方文档站</strong></a>
</p>

---

## 项目简介

**SiyoX** 是一个用于 Android 平台《我的世界》的客户端框架，主要实现了网络验证对接、资源包在线下载注入与自定义界面展示，核心网络加解密与通信由 Native C++ 处理。

> 📖 **官方在线文档**：[https://xiyuemax.github.io/SiyoX-Docs](https://xiyuemax.github.io/SiyoX-Docs)  
> 涵盖详尽的快速上手、三大网络验证配置详解、资源包注入流程与安全建议。

---

## 特性

- **多网络验证接入**：内置对接 EPIC（摇光云）、T3 验证、微验（WeiYan）三种主流网络验证，支持单码卡密登录、公告获取及更新检测。
- **Native C++ 底层**：网络请求、RC4 / RSA 加解密与数据签名均在 Native (JNI) 层执行，避免核心逻辑在 Java 层直接暴露。
- **资源在线下载与注入**：支持预置多资源包直链下载、MD5 完整性校验，并自动解压释放至游戏私有目录。
- **全配置集中化**：客户端名称、作者、默认公告、网络验证密钥以及资源包下载直链统一在 `SiyoX_Config.h` 中配置，无需改动 Java 业务代码。
- **轻量化工程**：剔除冗余依赖与构建缓存，源码结构清晰精简，默认支持一键无签名打包。

---

## 主配置文件说明

所有自定义参数与网络验证配置均在以下文件中修改：

📍 **配置文件路径**：[`app/src/main/cpp/SiyoX_Config.h`](app/src/main/cpp/SiyoX_Config.h)

### 支持配置项：
1. **验证模式选择**（`SIYOX_ACTIVE_VERIFY_TYPE`）：
   - `0`：关闭验证（免卡密直接进入）
   - `1`：EPIC（摇光云）
   - `2`：T3 验证
   - `3`：微验（WeiYan）
2. **客户端与版本信息**：
   - 客户端名称（`SIYOX_CLIENT_NAME`）、作者（`SIYOX_CLIENT_AUTHOR`）。
   - 内部版本号（`SIYOX_VERSION_CODE`，整数，网络验证比对使用，发布更新同步 +1）。
   - 外部显示版本号（直接在 AndroidManifest.xml / build.gradle.kts 中修改，客户端自动读取展示）。
3. **默认公告与更新弹窗**：离线或未拉取到公告/更新信息时显示的默认标题与内容。
4. **登录视频自定义配置**：支持配置是否替换启动背景视频（`SIYOX_ENABLE_LOGIN_VIDEO_REPLACE`）及 MP4 直链地址（`SIYOX_LOGIN_VIDEO_URL`）。
5. **右下角水印配置**：支持配置屏幕右下角水印文本（`SIYOX_WATERMARK_TEXT`）及是否允许在面板内开关。
6. **资源包配置**：支持配置多个直链下载地址、MD5 完整性校验开关及资源简介。
7. **验证平台参数**：对应验证平台的 AppKey、RC4 密钥、API 令牌及接口调用码。

---

## 网络验证后台配置说明

使用各验证平台时，需在对应管理后台完成以下设置以确保正常通信：

### 1. T3 验证后台配置
- **传输配置 ➔ 加密配置**：
  - 开启 **全局数据加解密**；
  - 加密算法选择 **`rc4`**；
  - 开启 **请求值加密** 与 **返回值加密**；
  - 请求值编码选择 **`HEX编码（16进制）`** 并保存。
- **效验配置**：
  - 开启 **时间戳校验** 与 **时间戳校验增强**。
- **返回值配置**：
  - 格式选择 **`JSON`**；
  - 开启 **JSON返回时间戳**；
  - `JSON_CODE` 类型选择 **`int`**。

---

### 2. 微验（WeiYan）后台配置
- 进入 **应用配置 ➔ 安全配置**；
- 数据加密类型选择：**`RC4加密-2 (hex)`**；
- 签名开关选择：**关闭**。

---

### 3. EPIC（摇光云）AppKey 获取方式
> ⚠️ **注意**：EPIC 手机软件端暂时无法直接获取 AppKey。
- 需使用浏览器访问网页端后台：👉 **[https://web.t60.top](https://web.t60.top)**
- 登录账号后，在后台**新建一个 APP**，即可获取对应的 `AppKey`

---

## 注意事项与安全提示

> [!CAUTION]
> ### 1. 资源包无加密说明
> **SiyoX 本身并未对资源包进行加密**。所有下载并注入的材质资源均会直接解压释放到《我的世界》私有目录中，用户可在私有目录中直接查看和提取。**不建议在公开或商业项目中使用该方式分发私密资源。**

> [!WARNING]
> ### 2. 公开发布前防护建议
> 如果你不在意上述特性并计划公开发布使用，在发布前**请务必务必务必**进行：
> 1. **源码混淆加密**
> 2. **Native 层代码保护**
> 3. **APK 软件加固**
> 
> 以防止软件被逆向破解或资源包下载直链被提取盗用。

---

## 编译环境与构建

### 环境要求
| 组件 | 推荐版本 |
|------|----------|
| **操作系统** | Windows 10 / 11 (64-bit) 或 Linux / macOS |
| **Java JDK** | JDK 21 (推荐 Android Studio 内置 JBR) |
| **Android SDK** | API 33 (Android 13) |
| **Gradle** | 9.6.1 |
| **Android Gradle Plugin** | 8.4.0 |
| **NDK** | r27b (27.1.12297006) |
| **CMake** | 3.28.0+ |

---

### 本地编译

在 PowerShell 中执行以下命令进行 Release 构建：

```powershell
# 1. 进入源码目录
cd xxx

# 2. 设置环境变量（根据本地实际路径调整）
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "C:\Users\Administrator\AppData\Local\Android\Sdk"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# 3. 执行编译（默认无签名打包）
.\gradlew.bat :app:assembleRelease
```

> 💡 编译产物位于 `app/build/outputs/apk/release/app-release-unsigned.apk`。

---

## 更新日志

### 🎨 v1.0.2 (2026-09-02)
- **新增灵动岛**：支持在游戏顶部实时显示客户端状态、动态时间、作者信息及下载进度条；支持在设置弹窗中调整缩放、圆角与屏幕双向偏移，滑动时弹窗半透明预览。
- **新增登录视频自定义替换**：支持游戏启动时自动下载并替换游戏内置登录背景视频（仅支持 MP4 直链）。
- **新增屏幕水印**：支持右下角展示自定义水印
- **开发者调试模式**：在关于页面连击 5 次版本号可开启开发者模式，支持灵动岛全动态模拟下载与弹窗调试。
- **安全与稳定性加固**：防御 Zip Slip 路径穿越、Native JNI 空指针免疫以及 I/O 句柄防泄露优化。

### 🚀 v1.0.1 (2026-09-01)
- **新增无验证模式**：支持关闭网络验证直接进入，面板授权状态与到期时间显示为「永久」。
- **新增独立版本更新弹窗**：支持在游戏闪屏页及各阶段稳定弹出，标题与内容支持在 `SiyoX_Config.h` 自定义；支持直链应用内下载与安装（带进度条），非直链跳转浏览器。
- **解耦内部版本号**：统一使用内部整数版本号（`SIYOX_VERSION_CODE`）进行网络验证与更新比对。
- **使用 R8 减小安装包体积**：开启 R8 编译优化，安装包体积由 5.78MB 减小至 1.96MB。
- **修复微验接口异常**：修复微验有时提示 `null` 的问题，优化网络请求解析。

### 🌟 v1.0.0 (2026-08-31)
- 初始版本发布，支持 EPIC、T3、微验三大网络验证平台。
- 核心网络通信与加解密下沉至 Native C++ 层。
- 支持预置资源包直链下载、MD5 校验与自动解压注入。
- 全局参数统一由 `SiyoX_Config.h` 集中管理。

---

## 开源协议

本项目基于 [Apache License 2.0](LICENSE) 协议开源。
