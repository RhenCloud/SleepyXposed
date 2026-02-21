# SleepyXposed 使用示例 / Usage Example

## 快速开始 / Quick Start

### 1. 配置服务器 / Configure Server

假设你已经部署了 Sleepy 服务器在 `https://sleepy.example.com`

Assuming you have deployed Sleepy server at `https://sleepy.example.com`

### 2. 安装模块 / Install Module

```bash
# 下载并安装 APK
adb install SleepyXposed.apk

# 或从 GitHub Actions 下载预编译版本
# Or download pre-built version from GitHub Actions
```

### 3. 配置应用 / Configure App

打开 SleepyXposed 应用，填写以下信息：

Open SleepyXposed app and fill in the following:

```
┌─────────────────────────────────────────────┐
│ SleepyXposed Configuration                  │
├─────────────────────────────────────────────┤
│                                             │
│ Server URL:                                 │
│ ┌─────────────────────────────────────────┐ │
│ │ https://sleepy.example.com/api/device/set│ │
│ └─────────────────────────────────────────┘ │
│                                             │
│ Server Secret:                              │
│ ┌─────────────────────────────────────────┐ │
│ │ ••••••••••••••••                        │ │
│ └─────────────────────────────────────────┘ │
│                                             │
│ Device ID:                                  │
│ ┌─────────────────────────────────────────┐ │
│ │ my-android-phone                        │ │
│ └─────────────────────────────────────────┘ │
│                                             │
│ Display Name:                               │
│ ┌─────────────────────────────────────────┐ │
│ │ 我的手机                                 │ │
│ └─────────────────────────────────────────┘ │
│                                             │
│ Enable Reporting              [✓] ON        │
│                                             │
│ ┌─────────────────────────────────────────┐ │
│ │      Save Configuration                 │ │
│ └─────────────────────────────────────────┘ │
└─────────────────────────────────────────────┘
```

### 4. 启用模块 / Enable Module

在 LSPosed 管理器中：

In LSPosed Manager:

1. 找到 SleepyXposed 模块 / Find SleepyXposed module
2. 启用模块 / Enable the module
3. 选择作用域：**系统框架 (android)** / Select scope: **System Framework (android)**
4. 重启设备 / Reboot device

### 5. 验证运行 / Verify Running

```bash
# 查看日志
adb logcat | grep SleepyXposed

# 预期输出：
# SleepyXposed: Successfully hooked into system
# SleepyXposed: Config loaded: 我的手机
# SleepyXposed: App switched to: com.android.launcher3/.Launcher
# SleepyXposed: Sent: Launcher[100%]⚡️
```

### 6. 查看效果 / View Result

访问你的 Sleepy 服务器页面：

Visit your Sleepy server page:

```
https://sleepy.example.com
```

你应该能看到：

You should see:

```
┌──────────────────────────────┐
│ 我的手机                      │
│ Launcher[100%]⚡️              │
│ 使用中                        │
└──────────────────────────────┘
```

## 实际场景示例 / Real-world Scenarios

### 场景 1：浏览网页 / Scenario 1: Browsing Web

当你打开 Chrome 浏览器时：

When you open Chrome browser:

```
用户操作 / User Action:
  点击 Chrome 图标 → 打开浏览器

系统行为 / System Behavior:
  1. Hook 检测到前台应用变化
  2. 获取应用名称：Chrome
  3. 读取电池：85%，充电中
  4. 发送到服务器

服务器显示 / Server Display:
  ┌──────────────────────┐
  │ 我的手机              │
  │ Chrome[85%]⚡️        │
  │ 使用中                │
  └──────────────────────┘
```

### 场景 2：使用微信 / Scenario 2: Using WeChat

```
用户操作 / User Action:
  切换到微信

系统行为 / System Behavior:
  1. 延迟 1 秒（防抖动）
  2. 确认应用切换
  3. 上报：微信[78%]🔋

服务器显示 / Server Display:
  ┌──────────────────────┐
  │ 我的手机              │
  │ 微信[78%]🔋          │
  │ 使用中                │
  └──────────────────────┘
```

### 场景 3：电量变化 / Scenario 3: Battery Change

```
时间线 / Timeline:

10:00 - 打开 YouTube，电量 60%，充电中
        YouTube[60%]⚡️

10:30 - 拔掉充电器继续看
        YouTube[55%]🔋

11:00 - 切换到音乐播放器
        网易云音乐[48%]🔋
```

## API 交互示例 / API Interaction Example

### 请求 / Request

```http
POST /api/device/set HTTP/1.1
Host: sleepy.example.com
Content-Type: application/json
User-Agent: SleepyXposed

{
  "secret": "your-secret-key",
  "id": "my-android-phone",
  "show_name": "我的手机",
  "using": true,
  "status": "Chrome[85%]⚡️"
}
```

### 响应 / Response

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "success": true
}
```

## 日志分析示例 / Log Analysis Example

### 正常运行日志 / Normal Operation Logs

```
[INFO]-[14:23:45] Successfully hooked into system
[INFO]-[14:23:45] Config loaded: 我的手机
[INFO]-[14:24:12] App switched to: com.android.chrome/.MainActivity
[INFO]-[14:24:13] Sent: Chrome[85%]⚡️
[INFO]-[14:25:03] App switched to: com.tencent.mm/.ui.LauncherUI
[INFO]-[14:25:04] Sent: 微信[83%]⚡️
[DEBUG]-[14:25:15] Browser opened
```

### 错误情况日志 / Error Logs

```
[ERROR]-[14:30:12] Failed to send: Connection timeout
[WARN]-[14:30:12] Server error: 401
[ERROR]-[14:30:13] Failed to send: Unauthorized
```

## 多设备配置示例 / Multi-device Configuration Example

### 设备 1：主手机 / Device 1: Main Phone

```
Server URL: https://sleepy.example.com/api/device/set
Secret: mySecret123
Device ID: main-phone
Display Name: 主手机
```

### 设备 2：平板 / Device 2: Tablet

```
Server URL: https://sleepy.example.com/api/device/set
Secret: mySecret123
Device ID: tablet-1
Display Name: iPad Pro
```

### 服务器显示效果 / Server Display

```
┌─────────────────────────┐  ┌─────────────────────────┐
│ 主手机                   │  │ iPad Pro                │
│ Chrome[85%]⚡️           │  │ Safari[92%]⚡️          │
│ 使用中                   │  │ 使用中                  │
└─────────────────────────┘  └─────────────────────────┘
```

## 常见问题示例 / Common Issues Examples

### Q1: 配置保存后不生效

```bash
# 检查配置是否保存
adb shell "su -c 'cat /data/data/android/shared_prefs/sleepy_config.xml'"

# 重新加载配置
# 1. 重新打开应用保存
# 2. 重启设备
adb reboot
```

### Q2: 服务器收不到数据

```bash
# 测试网络连接
adb shell "curl https://sleepy.example.com/api/meta"

# 查看详细日志
adb logcat *:S SleepyXposed:V

# 检查服务器密钥
# 确保与服务器 SLEEPY_SECRET 一致
```

### Q3: 频繁出现认证错误

```
原因：密钥不匹配
解决：
1. 检查服务器 SLEEPY_SECRET 环境变量
2. 在应用中重新输入正确的密钥
3. 保存配置并重启
```

## 性能测试示例 / Performance Test Example

### 测试场景 / Test Scenario

```
测试时长：1 小时
应用切换次数：50 次
网络请求：50 次
```

### 结果 / Results

```
CPU 使用：< 0.1%
内存占用：4.8 MB
网络流量：约 15 KB
电池消耗：可忽略不计
```

## 总结 / Summary

SleepyXposed 提供了一个轻量级、高效的解决方案来同步 Android 设备状态到 Sleepy 服务器。通过系统级 Hook，它能够准确、及时地报告应用使用情况和设备状态，同时保持极低的资源占用。

SleepyXposed provides a lightweight and efficient solution for syncing Android device status to Sleepy server. Through system-level hooks, it can accurately and promptly report app usage and device status while maintaining minimal resource consumption.
