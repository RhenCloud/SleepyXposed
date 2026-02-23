# SleepyXposed - Sleepy 客户端的 Xposed 模块实现

<!-- [[简体中文]](README.md) | [[English]](README_en.md) -->

[![GitHub License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![GitHub Release](https://img.shields.io/badge/release-v1.0-blue)](https://github.com/RhenCloud/SleepyXposed/releases)

一个 Xposed 模块，用于实时监控 Android 设备的前台应用切换，并自动同步到 Sleepy 服务器，用于个人在线状态的展示。

**重要：本项目部分代码使用AI技术生成，请您谨慎使用。**

## ✨ 核心功能

- 🔍 **实时应用监控** - 无缝监控前台应用程序切换
- 🌐 **自动上报** - 将应用状态自动报告到 Sleepy 服务器
- 🔋 **电池追踪** - 实时获取设备电量百分比和充电状态
- ⚙️ **灵活配置** - 用户友好的设置界面，支持自定义服务器
- 📱 **应用信息** - 获取并显示当前前台应用的详细信息
- 📝 **日志系统** - 内置多级别日志，便于调试和问题排查

## TODO

- [ ] 优化 UI 交互
- [ ] 优化日志系统
- [ ] 增加锁屏检测

## 📋 系统要求

| 要求               | 版本/说明                  |
| ------------------ | -------------------------- |
| **Android**        | 7.0 (API 24) 或更高        |
| **Xposed/LSPosed** | 已安装并激活               |
| **权限**           | Root 访问权限              |
| **网络**           | 互联网连接（用于上报数据） |

## 🚀 快速开始

### 1. 安装模块

#### 方式 A：使用预编译 APK（推荐）

从 [GitHub Actions](https://github.com/RhenCloud/SleepyXposed/actions) 下载最新的预编译 APK 文件：

1. 进入 Actions 页面
2. 选择最新的成功运行
3. 下载 `app-release.apk` 文件
4. 在设备上安装 APK

#### 方式 B：从源代码构建

```bash
# 克隆仓库
git clone https://github.com/RhenCloud/SleepyXposed.git
cd SleepyXposed

# 构建 APK
./gradlew assembleRelease

# 使用 adb 安装（可选）
adb install app/build/outputs/apk/release/app-release.apk
```

### 2. 配置 Sleepy 服务器

启动应用后，进行以下配置：

#### 必填项

| 字段           | 说明                   | 示例                                     |
| -------------- | ---------------------- | ---------------------------------------- |
| **服务器地址** | Sleepy 服务器地址      | `https://your-sleepy.com`                |
| **服务器密钥** | Sleepy 认证密钥        | `your-secret-key-here`                   |
| **设备 ID**    | 唯一标识此设备         | `android-phone-1`                        |

#### 可选项

| 字段         | 说明                     | 默认值   |
| ------------ | ------------------------ | -------- |
| **显示名称** | 在 Sleepy 页面显示的名称 | 设备型号 |
| **启用上报** | 是否启用数据上报         | 禁用     |

点击 **"保存配置"** 按钮保存设置。

### 3. 启用 Xposed 模块

1. 打开 Xposed/LSPosed 管理器
2. 找到 **"SleepyXposed"** 模块
3. **启用** 该模块
4. 在作用域中勾选 **"系统框架 (android)"**
5. 重启设备

### 4. 验证工作状态

- 打开 SleepyXposed 应用
- 检查日志栏，查看是否有活动日志
- 访问 Sleepy 服务器页面，验证设备是否在线并显示当前应用

## 📖 使用指南

### 查看实时日志

```bash
adb logcat | grep "SleepyXposed"
```

### 常见问题排查

| 问题           | 解决方案                                                                                    |
| -------------- | ------------------------------------------------------------------------------------------- |
| 应用未上报数据 | 1. 检查 "启用上报" 是否打开<br>2. 验证网络连接<br>3. 检查日志是否有错误信息                 |
| 连接超时       | 1. 验证服务器地址是否正确<br>2. 检查网络连接<br>3. 确认防火墙设置                           |
| 模块不工作     | 1. 检查 Xposed/LSPosed 是否启用<br>2. 验证设备是否已重启<br>3. 查看 Xposed 日志获取错误信息 |
| 电量信息不显示 | 确认设备未禁用电池状态权限                                                                  |

### API 集成示例

如果你正在运行自己的 Sleepy 服务器，可以按照以下格式集成数据：

```bash
curl -X POST https://your-server.com/api/device/set \
  -H "Content-Type: application/json" \
  -d '{
    "secret": "your-secret-key",
    "device_id": "android-phone-1",
    "current_app": "com.example.app",
    "app_name": "Example App",
    "battery": 85,
    "charging": true
  }'
```

## 🔧 技术栈

- **语言**: Kotlin, Java
- **框架**: Android Framework, Xposed Framework
- **网络**: OkHttp
- **异步**: Kotlin Coroutines
- **UI**: Android Material Design

## 🔐 安全性说明

- 密钥安全：服务器密钥存储在本地 SharedPreferences 中，建议使用 Android Keystore 进一步加密
- 网络安全：建议使用 HTTPS 与服务器通信
- 权限最小化：模块仅请求必要的权限
- 隐私保护：应用信息仅用于本地显示和服务器上报，不做其他用途

## 📝 关于 Sleepy

Sleepy 是一个个人在线状态和应用展示项目：

- **官方地址**: [sleepy-project/sleepy](https://github.com/sleepy-project/sleepy)
- **官方演示站点**: [sleepy.wyf9.top](https://sleepy.wyf9.top)
- **RhenCloud的个人站点**: [sleepy.rhen.cloud](https://sleepy.rhen.cloud)

## 📄 许可证

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📞 联系方式

- GitHub Issues: [RhenCloud/SleepyXposed/issues](https://github.com/RhenCloud/SleepyXposed/issues)
- Email: <i@rhen.cloud>

## 🙏 致谢

感谢以下社区及项目的支持：

- [LSPosed](https://github.com/LSPosed/LSPosed)
- [Sleepy](https://github.com/sleepy-project/sleepy)
