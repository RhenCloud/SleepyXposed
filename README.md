# SleepyXposed

一个 Xposed 模块，用于监控前台应用程序切换并自动报告到 Sleepy 服务器。

An Xposed module that monitors foreground app switches and automatically reports to Sleepy server.

## 功能特性 / Features

- 🔍 实时监控前台应用切换 / Real-time monitoring of foreground app switches
- 🌐 自动上报到 Sleepy 服务器 / Automatic reporting to Sleepy server
- 🔋 电池状态跟踪 / Battery status tracking (percentage + charging state)
- ⚙️ 可配置的服务器设置 / Configurable server settings
- 📱 获取当前前台应用名称 / Get current foreground app name
- 📝 内置日志系统 / Built-in logging system
- 🎯 延迟上报机制（防抖动）/ Delayed reporting (debouncing)

## 关于 Sleepy / About Sleepy

Sleepy 是一个用于显示个人在线状态和正在使用软件的项目。本模块作为 Android 客户端，可以将设备状态实时同步到 Sleepy 服务器。

Sleepy is a project for displaying personal online status and currently used applications. This module serves as an Android client to sync device status to Sleepy server in real-time.

- 演示站点 / Demo Site: [sleepy.wyf9.top](https://sleepy.wyf9.top)
- 项目地址 / Project: [sleepy-project/sleepy](https://github.com/sleepy-project/sleepy)
- API 文档 / API Docs: [api.md](https://github.com/sleepy-project/sleepy/blob/main/doc/api.md)

## 工作原理 / How It Works

该模块通过 hook Android 系统的 ActivityRecord.completeResumeLocked 方法来检测前台应用的切换。当新的应用进入前台时，模块会：

This module works by hooking into Android system's ActivityRecord.completeResumeLocked method to detect foreground app switches. When a new app comes to the foreground, the module will:

1. 检测包名变化 / Detect package name changes
2. 获取应用显示名称 / Get app display name
3. 读取电池状态（电量和充电状态）/ Read battery status (level and charging state)
4. 发送到 Sleepy 服务器 / Send to Sleepy server via API
5. 记录操作日志 / Log the operation

## 系统要求 / Requirements

- Android 7.0 (API 24) 或更高版本 / Android 7.0 (API 24) or higher
- Xposed Framework 或 LSPosed 已安装 / Xposed Framework or LSPosed installed
- Root 权限 / Root access
- 网络连接 / Internet connection
- Sleepy 服务器（自建或使用公开服务）/ Sleepy server (self-hosted or public service)

## 安装 / Installation

### 方式 1：下载预编译的 APK / Method 1: Download Pre-built APK

从 [GitHub Actions](https://github.com/RhenCloud/SleepyXposed/actions) 工作流中下载最新构建的 APK 文件。每次推送到主分支时，都会自动编译生成 APK。

Download the latest built APK from the [GitHub Actions](https://github.com/RhenCloud/SleepyXposed/actions) workflow. APK files are automatically compiled on every push to the main branch.

### 方式 2：手动构建 / Method 2: Build Manually

1. 安装 Xposed Framework 或 LSPosed
   Install Xposed Framework or LSPosed

2. 构建并安装本模块：
   Build and install this module:
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### 安装和配置步骤 / Installation and Configuration Steps

1. 安装 APK 并打开应用
   Install the APK and open the app

2. 配置 Sleepy 服务器信息：
   Configure Sleepy server settings:
   - **服务器地址 / Server URL**: `https://your-sleepy-server.com/api/device/set`
   - **服务器密钥 / Server Secret**: 你的 Sleepy 服务器密钥 / Your Sleepy server secret
   - **设备 ID / Device ID**: 唯一设备标识符（如 `phone-1`）/ Unique device identifier (e.g., `phone-1`)
   - **显示名称 / Display Name**: 在 Sleepy 页面上显示的名称 / Name shown on Sleepy page
   - **启用上报 / Enable Reporting**: 开启后才会上报数据 / Enable to start reporting

3. 点击"保存配置"按钮
   Click "Save Configuration" button

4. 在 Xposed/LSPosed 管理器中启用模块
   Enable the module in Xposed/LSPosed manager

5. 勾选 "系统框架 (android)" 作为作用域
   Check "System Framework (android)" as the scope

6. 重启设备
   Reboot your device

7. 检查 Sleepy 服务器页面，应该能看到你的设备状态
   Check your Sleepy server page, you should see your device status

## 使用方法 / Usage

### 查看日志 / View Logs

安装并启用模块后，它会自动工作。您可以通过以下方式查看日志：

After installation and activation, the module works automatically. You can view logs using:

```bash
adb logcat | grep "SleepyXposed"
```

### 状态格式 / Status Format

上报到 Sleepy 服务器的状态格式：

Status format reported to Sleepy server:

```
AppName[Battery%]Icon
```

示例 / Examples:
- `Chrome[85%]⚡️` - Chrome 浏览器，85% 电量，正在充电
- `微信[42%]🔋` - WeChat, 42% battery, not charging

### 配置示例 / Configuration Example

```
Server URL: https://sleepy.example.com/api/device/set
Server Secret: your-secret-key-here
Device ID: my-phone
Display Name: 我的手机 / My Phone
Enable Reporting: ✓ (checked)
```

## API 集成 / API Integration

本模块实现了 Sleepy API 的 `/api/device/set` 端点：

This module implements Sleepy API's `/api/device/set` endpoint:

**请求格式 / Request Format:**
```json
{
  "secret": "your-secret",
  "id": "device-id",
  "show_name": "Display Name",
  "using": true,
  "status": "AppName[80%]⚡️"
}
```

**响应 / Response:**
```json
{
  "success": true
}
```

更多 API 详情请参考：[Sleepy API 文档](https://github.com/sleepy-project/sleepy/blob/main/doc/api.md)

For more API details, see: [Sleepy API Documentation](https://github.com/sleepy-project/sleepy/blob/main/doc/api.md)

## 高级功能 / Advanced Features

### 获取当前前台应用 / Get Current Foreground App

模块提供了公共方法来获取当前前台应用的信息（可用于其他 Xposed 模块）：

The module provides public methods to get information about the current foreground app (usable by other Xposed modules):

```kotlin
// 获取当前前台应用的包名 / Get current foreground package name
val packageName = ForegroundAppMonitor.getCurrentForegroundPackage()

// 获取当前前台应用的 Activity 名称 / Get current foreground activity name
val activityName = ForegroundAppMonitor.getCurrentForegroundActivity()

// 获取当前前台应用的完整组件名 / Get current foreground component name
val componentName = ForegroundAppMonitor.getCurrentForegroundComponentName()

// 获取应用显示名称（需要 Context）/ Get app display name (requires Context)
val displayName = ForegroundAppMonitor.getAppDisplayName(context, packageName ?: "")
```

### 自定义扩展 / Custom Extensions

如需添加自定义逻辑，可以修改 `ForegroundAppMonitor.kt` 中的 `executeCustomOperations` 方法：

To add custom logic, modify the `executeCustomOperations` method in `ForegroundAppMonitor.kt`:

```kotlin
private fun executeCustomOperations(packageName: String, activityName: String?) {
    // 在这里添加您的自定义逻辑
    // Add your custom logic here
    
    when (packageName) {
        "com.example.app" -> {
            // 特定应用的操作
            // Operations for specific app
        }
    }
}
```

### 配置说明 / Configuration Details

配置文件存储在系统的 SharedPreferences 中：
Configuration is stored in system SharedPreferences:

- **文件名 / File**: `sleepy_config`
- **存储位置 / Location**: `/data/data/android/shared_prefs/sleepy_config.xml`
- **权限 / Permission**: 只有系统进程可以访问 / Only accessible by system process

配置项 / Configuration Keys:
- `server_url`: Sleepy 服务器 API 地址
- `secret`: 服务器认证密钥
- `id`: 设备唯一标识符
- `show_name`: 设备显示名称
- `enabled`: 是否启用上报（true/false）

## 技术细节 / Technical Details

### Hook 点 / Hook Point

模块 hook 了 `com.android.server.wm.ActivityRecord.completeResumeLocked` 方法，这个方法在 Activity 完成恢复到前台时被调用。

The module hooks `com.android.server.wm.ActivityRecord.completeResumeLocked`, which is called when an Activity completes resuming to the foreground.

### 包名跟踪 / Package Tracking

模块维护一个 `lastForegroundPackage` 变量来跟踪上一个前台应用，只有当包名发生变化时才触发操作，避免重复上报。

The module maintains a `lastForegroundPackage` variable to track the previous foreground app, only triggering operations when the package name changes to avoid duplicate reports.

### 延迟上报 / Delayed Reporting

使用 1 秒延迟机制（防抖动），避免快速切换应用时产生大量请求。

Uses a 1-second delay mechanism (debouncing) to avoid excessive requests when quickly switching apps.

### 电池信息 / Battery Information

从系统 BatteryManager 读取：
Read from system BatteryManager:
- 电量百分比 / Battery percentage
- 充电状态 / Charging status
- 格式化为状态文本的一部分 / Formatted as part of status text

### 网络请求 / Network Requests

使用 OkHttp 客户端：
Uses OkHttp client:
- 连接超时：10 秒 / Connect timeout: 10s
- 读取超时：10 秒 / Read timeout: 10s
- 失败自动重试 / Retry on connection failure
- 异步回调处理 / Asynchronous callback handling

## 代码结构 / Code Structure

```
app/src/main/java/com/rhencloud/sleepyxposed/
├── ForegroundAppMonitor.kt      # 主要 Hook 逻辑和 Sleepy 集成 / Main hook logic and Sleepy integration
├── SleepyApiClient.kt            # Sleepy API 客户端 / Sleepy API client
├── LogRepository.kt              # 日志系统 / Logging system
└── MainActivity.kt               # 配置界面 / Configuration UI

app/src/main/res/
├── layout/
│   └── activity_main.xml         # 配置界面布局 / Configuration UI layout
└── values/
    └── strings.xml               # 字符串资源 / String resources
```

## 日志示例 / Log Example

```
SleepyXposed: Successfully hooked into system
SleepyXposed: Config loaded: My Phone
SleepyXposed: App switched to: com.android.launcher3/.Launcher
SleepyXposed: Sent: Launcher[85%]⚡️
SleepyXposed: App switched to: com.android.chrome/.MainActivity
SleepyXposed: Browser opened
SleepyXposed: Sent: Chrome[82%]⚡️
```

## 开发 / Development

### 构建项目 / Build Project

```bash
./gradlew build
```

### 生成 APK / Generate APK

```bash
./gradlew assembleDebug
```

## 注意事项 / Notes

- 本模块仅在系统服务进程 (android) 中激活 / This module only activates in the system server process (android)
- 确保在 Xposed/LSPosed 中勾选了正确的作用域 / Ensure correct scope is selected in Xposed/LSPosed
- 需要配置有效的 Sleepy 服务器才能上报 / Requires valid Sleepy server configuration to report
- 使用 1 秒延迟避免频繁切换产生过多请求 / Uses 1-second delay to avoid excessive requests from frequent switching
- 配置修改后需要重启设备才能生效 / Device reboot required after configuration changes

## 故障排除 / Troubleshooting

### 模块不工作 / Module Not Working

1. 确认 Xposed/LSPosed 已正确安装 / Confirm Xposed/LSPosed is correctly installed
2. 确认模块已在管理器中启用 / Confirm module is enabled in manager
3. 确认已勾选 "系统框架" 作用域 / Confirm "System Framework" scope is checked
4. 打开应用检查配置是否完整 / Open app and check configuration is complete
5. 确保 "启用上报" 开关已打开 / Ensure "Enable Reporting" switch is on
6. 重启设备 / Reboot device
7. 检查 logcat 日志 / Check logcat logs

### 无法连接到服务器 / Cannot Connect to Server

1. 检查服务器 URL 是否正确 / Check server URL is correct
2. 确保设备有网络连接 / Ensure device has internet connection
3. 验证服务器密钥是否正确 / Verify server secret is correct
4. 检查服务器是否在线 / Check if server is online
5. 查看 logcat 中的错误信息 / Check error messages in logcat

### 查看日志 / View Logs

```bash
# 实时查看日志 / View logs in real-time
adb logcat | grep "SleepyXposed"

# 保存日志到文件 / Save logs to file
adb logcat | grep "SleepyXposed" > sleepy_xposed.log

# 查看最近的日志 / View recent logs
adb logcat -d | grep "SleepyXposed"
```

## 相关项目 / Related Projects

- [Sleepy](https://github.com/sleepy-project/sleepy) - Sleepy 服务器项目 / Sleepy server project
- [Sleepy-Android](https://github.com/sleepy-project/Sleepy-Android) - 基于无障碍服务的 Android 客户端 / Accessibility service-based Android client

## 许可证 / License

MIT License - 详见 LICENSE 文件 / See LICENSE file for details

## 贡献 / Contributing

欢迎提交 Issue 和 Pull Request！

Issues and Pull Requests are welcome!

## 作者 / Author

Rhen Cloud

## 免责声明 / Disclaimer

本项目仅供学习和研究使用。使用本模块需要 Root 权限，可能会影响系统稳定性。请谨慎使用，作者不对使用本模块造成的任何问题负责。

This project is for educational and research purposes only. Using this module requires root access and may affect system stability. Use with caution, the author is not responsible for any issues caused by using this module.
