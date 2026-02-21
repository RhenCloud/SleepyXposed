# SleepyXposed

一个 Xposed 模块，用于监控前台应用程序切换并执行自定义操作。

An Xposed module that monitors foreground app switches and executes custom operations.

## 功能特性 / Features

- 🔍 实时监控前台应用切换 / Real-time monitoring of foreground app switches
- 📝 记录应用切换日志 / Log app switch events
- ⚙️ 可自定义操作 / Customizable operations
- 🎯 针对特定应用执行操作 / Execute operations for specific apps
- 📱 获取当前前台应用名称 / Get current foreground app name

## 工作原理 / How It Works

该模块通过 hook Android 系统的 ActivityRecord.completeResumeLocked 方法来检测前台应用的切换。当新的应用进入前台时，模块会：

This module works by hooking into Android system's ActivityRecord.completeResumeLocked method to detect foreground app switches. When a new app comes to the foreground, the module will:

1. 检测包名变化 / Detect package name changes
2. 记录切换事件 / Log the switch event
3. 执行自定义操作 / Execute custom operations

## 系统要求 / Requirements

- Android 7.0 (API 24) 或更高版本 / Android 7.0 (API 24) or higher
- Xposed Framework 或 LSPosed 已安装 / Xposed Framework or LSPosed installed
- Root 权限 / Root access

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

### 安装步骤 / Installation Steps

1. 在 Xposed/LSPosed 管理器中启用模块
   Enable the module in Xposed/LSPosed manager

2. 勾选 "系统框架 (android)" 作为作用域
   Check "System Framework (android)" as the scope

3. 重启设备
   Reboot your device

## 使用方法 / Usage

安装并启用模块后，它会自动工作。您可以通过以下方式查看日志：

After installation and activation, the module works automatically. You can view logs using:

```bash
adb logcat | grep "SleepyXposed"
```

## 自定义操作 / Customization

要添加自定义操作，请编辑 `ForegroundAppMonitor.kt` 中的 `executeCustomOperations` 方法：

To add custom operations, edit the `executeCustomOperations` method in `ForegroundAppMonitor.kt`:

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

### 获取当前前台应用 / Get Current Foreground App

模块现在提供了公共方法来获取当前前台应用的信息：

The module now provides public methods to get information about the current foreground app:

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

这些方法可以在您的自定义操作中或其他 Xposed 模块中调用。

These methods can be called from your custom operations or other Xposed modules.

**使用示例 / Usage Example:**

```kotlin
private fun executeCustomOperations(packageName: String, activityName: String?) {
    // 获取并记录当前前台应用信息
    // Get and log current foreground app info
    val currentPkg = ForegroundAppMonitor.getCurrentForegroundPackage()
    val currentComponent = ForegroundAppMonitor.getCurrentForegroundComponentName()
    
    XposedBridge.log("SleepyXposed: Current foreground app: $currentComponent")
    
    // 您可以在这里使用这些信息做更多操作
    // You can do more with this information here
}
```

## 日志示例 / Log Example

```
SleepyXposed: Foreground app switched to: com.android.launcher3/.Launcher
SleepyXposed: [OPERATION] New foreground app: com.android.launcher3
SleepyXposed: Foreground app switched to: com.android.chrome/.Main
SleepyXposed: [OPERATION] Browser opened
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

### 代码结构 / Code Structure

```
app/
├── src/main/
│   ├── assets/
│   │   └── xposed_init           # Xposed 入口配置 / Xposed entry configuration
│   ├── java/com/rhencloud/sleepyxposed/
│   │   └── ForegroundAppMonitor.kt  # 主要 Hook 逻辑 / Main hook logic
│   ├── res/
│   │   └── values/
│   │       └── strings.xml       # 字符串资源 / String resources
│   └── AndroidManifest.xml       # 应用清单 / App manifest
└── build.gradle                  # 应用级构建配置 / App-level build config
```

## 技术细节 / Technical Details

### Hook 点 / Hook Point

模块 hook 了 `com.android.server.wm.ActivityRecord.completeResumeLocked` 方法，这个方法在 Activity 完成恢复到前台时被调用。

The module hooks `com.android.server.wm.ActivityRecord.completeResumeLocked`, which is called when an Activity completes resuming to the foreground.

### 包名跟踪 / Package Tracking

模块维护一个 `lastForegroundPackage` 变量来跟踪上一个前台应用，只有当包名发生变化时才触发操作。

The module maintains a `lastForegroundPackage` variable to track the previous foreground app, only triggering operations when the package name changes.

## 注意事项 / Notes

- 本模块仅在系统服务进程 (android) 中激活 / This module only activates in the system server process (android)
- 确保在 Xposed/LSPosed 中勾选了正确的作用域 / Ensure correct scope is selected in Xposed/LSPosed
- 日志可以通过 logcat 查看 / Logs can be viewed via logcat
- 频繁的应用切换可能产生大量日志 / Frequent app switches may generate many logs

## 故障排除 / Troubleshooting

### 模块不工作 / Module Not Working

1. 确认 Xposed/LSPosed 已正确安装 / Confirm Xposed/LSPosed is correctly installed
2. 确认模块已在管理器中启用 / Confirm module is enabled in manager
3. 确认已勾选 "系统框架" 作用域 / Confirm "System Framework" scope is checked
4. 重启设备 / Reboot device
5. 检查 logcat 日志 / Check logcat logs

### 查看日志 / View Logs

```bash
# 实时查看日志 / View logs in real-time
adb logcat | grep "SleepyXposed"

# 保存日志到文件 / Save logs to file
adb logcat | grep "SleepyXposed" > sleepy_xposed.log
```

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
