# Android 轻量级 Kiosk Launcher App 设计文档

## 1. 项目定位

本项目不使用 Device Owner，也不使用 Lock Task Mode。

核心思路是：

```text
默认 Launcher
+ 应用白名单主页
+ 管理员密码
+ 设备管理器权限
+ 辅助服务
+ 悬浮窗遮挡/拦截快捷入口
```

目标不是做企业级强管控，而是做一个足够实用的轻量 Kiosk Launcher，防止普通用户通过桌面、快捷菜单、通知栏、设置入口退出受控环境。

---

## 2. 整体架构

```text
KioskActivity
 ├── SetupWizardFragment
 ├── HomeFragment
 ├── AdminLoginFragment
 ├── AdminPanelFragment
 ├── AppWhitelistFragment
 ├── PermissionGuideFragment
 └── KioskPolicyFragment

KioskAccessibilityService
KioskOverlayService
KioskDeviceAdminReceiver
```

---

## 3. 核心能力

## 3.1 默认 Launcher

App 声明为 HOME Launcher：

```xml
<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.HOME" />
    <category android:name="android.intent.category.DEFAULT" />
</intent-filter>
```

用户进入桌面时，会回到本应用主页。

---

## 3.2 白名单主页

主页只显示管理员允许的 App。

功能：

```text
1. 扫描已安装应用
2. 管理员选择白名单
3. 白名单 App 显示在 HomeFragment
4. 点击后启动目标应用
5. 返回桌面时回到 Kiosk Launcher
```

---

## 3.3 管理员权限

使用 Android Device Admin 权限，主要用于增加卸载门槛。

```xml
<receiver
    android:name=".receiver.KioskDeviceAdminReceiver"
    android:permission="android.permission.BIND_DEVICE_ADMIN"
    android:exported="true">
    <meta-data
        android:name="android.app.device_admin"
        android:resource="@xml/device_admin" />
    <intent-filter>
        <action android:name="android.app.action.DEVICE_ADMIN_ENABLED" />
    </intent-filter>
</receiver>
```

用途：

```text
1. 防止普通用户直接卸载 App
2. 作为管理员模式的基础权限
3. 在设置向导中引导用户启用
```

注意：

Device Admin 不是 Device Owner，不能彻底控制系统，但可以提升普通用户关闭/卸载 App 的难度。

---

## 3.4 辅助服务

辅助服务用于监听用户是否进入系统设置、通知栏、最近任务、多任务界面等风险页面。

职责：

```text
1. 监听窗口变化
2. 识别系统设置页面
3. 识别通知栏/快捷设置面板
4. 识别最近任务页面
5. 必要时执行返回/Home
6. 通知悬浮窗服务显示遮挡层
```

示例策略：

```kotlin
override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    val pkg = event?.packageName?.toString() ?: return

    if (pkg == "com.android.settings") {
        performGlobalAction(GLOBAL_ACTION_BACK)
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    if (isSystemUiPackage(pkg)) {
        overlayController.showBlockOverlay()
    }
}
```

常见系统包：

```text
com.android.systemui
com.android.settings
com.google.android.permissioncontroller
com.android.permissioncontroller
```

---

## 3.5 悬浮窗拦截层

使用 `SYSTEM_ALERT_WINDOW` 权限实现悬浮窗遮挡。

用途：

```text
1. 遮挡通知栏快捷设置区域
2. 遮挡设置按钮
3. 遮挡最近任务中的敏感入口
4. 弹出“当前模式不允许此操作”
```

悬浮窗建议做成顶部拦截层：

```text
位置：屏幕顶部
高度：状态栏高度 + 快捷设置区域预估高度
类型：TYPE_APPLICATION_OVERLAY
触摸：可拦截
显示条件：检测到风险页面时显示
```

不要长期全屏遮挡，避免影响正常白名单 App 使用。

---

## 4. 首次配置向导

首次进入流程：

```text
1. 设置管理员密码
2. 选择白名单应用
3. 引导设置为默认 Launcher
4. 引导启用 Device Admin
5. 引导开启辅助服务
6. 引导开启悬浮窗权限
7. 完成配置，进入主页
```

---

## 5. 管理员后台

管理员入口建议隐藏：

```text
连续点击 Logo 5 次
或长按空白区域 3 秒
或输入固定手势
```

管理员后台功能：

```text
1. 修改白名单
2. 修改管理员密码
3. 开启/关闭辅助服务检测
4. 开启/关闭悬浮窗拦截
5. 打开系统设置
6. 临时退出 Kiosk 模式
7. 关闭设备管理器权限
```

---

## 6. 页面结构

```text
HomeFragment
- 显示白名单 App
- 隐藏管理员入口
- 禁用返回键

SetupWizardFragment
- 首次配置流程

AdminLoginFragment
- 管理员密码验证

AdminPanelFragment
- 管理配置入口

AppWhitelistFragment
- 扫描和选择应用

PermissionGuideFragment
- 引导开启 Launcher / Device Admin / 辅助服务 / 悬浮窗权限
```

---

## 7. 数据设计

### KioskSettings

```kotlin
data class KioskSettings(
    val setupCompleted: Boolean,
    val adminPasswordHash: String,
    val adminPasswordSalt: String,
    val kioskEnabled: Boolean,
    val accessibilityEnabled: Boolean,
    val overlayEnabled: Boolean,
    val deviceAdminEnabled: Boolean
)
```

### WhitelistApp

```kotlin
data class WhitelistApp(
    val packageName: String,
    val appName: String,
    val enabled: Boolean,
    val sortOrder: Int
)
```

---

## 8. 服务设计

## 8.1 KioskAccessibilityService

职责：

```text
1. 监听窗口变化
2. 判断当前前台包名
3. 发现黑名单页面后返回 Home
4. 通知 OverlayService 显示遮挡层
```

黑名单包名建议可配置：

```text
com.android.settings
com.android.systemui
com.google.android.permissioncontroller
com.android.permissioncontroller
```

---

## 8.2 KioskOverlayService

职责：

```text
1. 管理悬浮窗显示
2. 显示顶部遮挡层
3. 显示全屏警告层
4. 自动隐藏遮挡层
```

悬浮窗类型：

```kotlin
WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
```

---

## 8.3 KioskDeviceAdminReceiver

职责：

```text
1. 接收设备管理器启用状态
2. 提高卸载门槛
3. 管理员后台中允许主动关闭
```

---

## 9. 推荐包结构

```text
com.example.kiosk
 ├── ui
 │   ├── KioskActivity.kt
 │   ├── home
 │   ├── setup
 │   ├── admin
 │   └── whitelist
 ├── service
 │   ├── KioskAccessibilityService.kt
 │   └── KioskOverlayService.kt
 ├── receiver
 │   └── KioskDeviceAdminReceiver.kt
 ├── policy
 │   ├── KioskPolicyManager.kt
 │   ├── AccessibilityMonitor.kt
 │   └── OverlayController.kt
 ├── data
 │   ├── datastore
 │   ├── db
 │   └── repository
 └── util
```

---

## 10. MVP 开发范围

第一版只做：

```text
1. 单 Activity + Fragment
2. 设置为默认 Launcher
3. 设置管理员密码
4. 扫描已安装应用
5. 配置应用白名单
6. 白名单主页
7. 管理员隐藏入口
8. Device Admin 权限引导
9. 辅助服务引导
10. 悬浮窗权限引导
11. 检测设置页并自动返回
12. 检测 SystemUI 并显示遮挡层
```

不做：

```text
1. Device Owner
2. Lock Task Mode
3. Android Enterprise
4. 远程策略
5. Root 权限
6. ROM 级定制
```

---

## 11. 风险点

```text
1. 用户仍可能通过系统设置关闭辅助服务
2. 不同 ROM 的 SystemUI 包名和窗口结构不同
3. 悬浮窗权限可能被系统限制
4. Android 高版本对后台服务限制更严格
5. Device Admin 只能增加卸载门槛，不能彻底防卸载
6. 辅助服务和悬浮窗组合不是强安全边界
```

---

## 12. 结论

本项目采用轻量级 Kiosk 方案：

```text
默认 Launcher 负责入口控制
白名单主页负责应用限制
Device Admin 负责提高卸载门槛
辅助服务负责检测越界行为
悬浮窗负责遮挡快捷菜单和设置入口
```

该方案适合自有设备、低强度管控、门店平板、展示设备、儿童模式、简单业务终端等场景。
