# SingleAppKiosk

[English](README.md)

SingleAppKiosk 是一个开源 Android Kiosk 桌面应用。它可以替代系统默认桌面，只展示管理员配置过的白名单应用，适合公开设备、门店设备、教学设备、展台演示设备和单用途 Android 终端。

项目保持轻量和独立，使用 Kotlin、Gradle、AndroidX 和原生 Android View 构建，不依赖后端服务，也不需要账号体系。

## 功能特性

- 支持作为 Android 默认桌面应用。
- 通过应用白名单控制可启动的应用。
- 进入设置页前需要管理员密码。
- 首次启动引导设置管理员密码。
- 支持设备管理器权限，提高被随意卸载的门槛。
- 支持无障碍服务，拦截部分系统 UI 操作区域。
- 内置英文和简体中文界面文案。
- 本地运行，无服务端依赖。

## 使用流程

1. 安装并打开 SingleAppKiosk。
2. 首次启动时设置管理员密码。
3. 使用管理员密码进入设置页。
4. 配置允许在 Kiosk 主页启动的应用白名单。
5. 在 Android 系统设置中将 SingleAppKiosk 设置为默认桌面。
6. 按需启用设备管理器和无障碍服务。
7. 返回 Kiosk 主页，仅启动白名单中的应用。

## 环境要求

- Android Studio 或命令行 Gradle 环境。
- JDK 21。
- 已安装 Android SDK API 37。
- Android 7.0 或更高版本设备，项目 `minSdk` 为 24。

## 构建

构建 Debug APK：

```bash
./gradlew assembleDebug
```

构建 Release APK：

```bash
./gradlew assembleRelease
```

APK 输出目录：

```text
app/build/outputs/apk/
```

## 安装

通过 ADB 安装 Debug 包：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

安装后进入 Android 系统设置，将 SingleAppKiosk 设置为默认桌面应用。

## 权限说明

SingleAppKiosk 会使用几个需要用户或管理员主动授权的 Android 能力：

- 默认桌面：按 Home 键时回到 SingleAppKiosk。
- 设备管理器：提高应用被随意卸载的门槛。
- 无障碍服务：辅助检测和拦截部分系统 UI 操作区域。

这些权限不会静默开启，必须由用户或设备管理员在系统设置中手动启用。

## 安全说明

SingleAppKiosk 是一个轻量 Kiosk 桌面，不是完整的企业级 MDM 方案。不同厂商、不同 Android 版本对默认桌面、系统 UI、无障碍覆盖层等能力的行为可能不同。

正式部署前，请在目标设备型号和目标 Android 版本上完整测试。对于批量托管设备，建议评估 Android Enterprise、Device Owner 模式或专业 MDM 方案。

## GitHub 自动发布 APK

仓库已包含 GitHub Actions workflow，可在发布 tag 时自动构建 APK 并上传到 GitHub Releases。

创建并推送 tag：

```bash
git tag v1.0.0
git push origin v1.0.0
```

推送后 GitHub Actions 会自动构建 APK，并将 APK 附加到对应 Release。

也可以在 GitHub Actions 页面手动运行该 workflow。

### 可选 Release 签名

如果没有配置签名 secrets，workflow 会上传 Debug APK 和未签名 Release APK。要发布已签名的 Release APK，请在 GitHub 仓库 Secrets 中添加：

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

将 keystore 文件转换成 `ANDROID_KEYSTORE_BASE64`：

```bash
base64 -w 0 release-keystore.jks
```

## 项目结构

```text
app/src/main/java/com/android/launcherkiosk/
  data/        本地设置和白名单存储
  policy/      设备策略和系统状态检测
  receiver/    设备管理器 Receiver
  service/     无障碍服务
  ui/          主页、首次配置、设置和白名单界面
```

## 开源协议

当前仓库尚未包含 LICENSE 文件。正式作为开源项目发布前，建议补充明确的开源协议。
