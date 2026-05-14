# Install and Run Guide / 安装与运行指南

This guide explains how to clone, configure, build, and run this Android project locally.  
本指南说明如何在本地克隆、配置、构建并运行此 Android 项目。

## 1) Repository / 仓库地址

- GitHub (SSH): `git@github.com:Chase7070/compsci702-re-project-chase.git`

## 2) Prerequisites / 环境要求

- JDK 17
- Android Studio (latest stable recommended)
- Android SDK installed (API 34 recommended)
- AVD emulator or USB-debug-enabled physical device

- JDK 17
- Android Studio（建议稳定版）
- 已安装 Android SDK（建议 API 34）
- Android 模拟器或开启 USB 调试的真机

## 3) Clone Project / 克隆项目

```bash
git clone git@github.com:Chase7070/compsci702-re-project-chase.git
cd compsci702-re-project-chase
```

Open Android Studio, then choose the project root directory (contains `settings.gradle.kts`).  
在 Android Studio 中打开工程根目录（包含 `settings.gradle.kts`）。

## 4) Local Secrets Setup / 本地密钥配置

This project reads local auth config from `secrets.properties` at repository root.  
项目会从仓库根目录的 `secrets.properties` 读取本地认证配置。

Required keys:

- `API_BASE_URL`
- `AUTH_HEADER_VALUE` (optional local bootstrap value; debug copies it into `app/src/debug/assets/auth_bootstrap.txt`, and locally built release can also use it if `/session` is unavailable)

Optional keys (see `secrets.example.properties`):

- `CERT_PINS` — OkHttp certificate pins; required for release builds. `sha256/...` applies to `API_BASE_URL`, while `host=sha256/...` can pin extra allowlisted hosts
- `RELEASE_SIGNING_SHA256` — expected release signing cert fingerprint (hex)

You can copy from template:

```bash
cp secrets.example.properties secrets.properties
```

Then edit `secrets.properties` locally with your own values.  
然后在本地修改 `secrets.properties`，填入你自己的配置值。

> Do not commit real secrets. Keep `secrets.properties` local-only.  
> 不要提交真实密钥，`secrets.properties` 仅保留在本地。

## 5) Build and Run / 构建与运行

### Android Studio

1. Wait for Gradle Sync to finish.
2. Select a target device (AVD or physical device).
3. Run the `app` configuration.

1. 等待 Gradle 同步完成。
2. 选择运行设备（模拟器或真机）。
3. 运行 `app` 配置。

### Command Line

Build debug APK:

```bash
./gradlew :app:assembleDebug
```

APK output:

- `app/build/outputs/apk/debug/app-debug.apk`

Install to connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 6) Common Issues / 常见问题

- **Gradle sync fails**: check internet, proxy, and Android SDK setup.  
  **Gradle 同步失败**：检查网络、代理和 Android SDK 配置。

- **Network request errors**: verify `API_BASE_URL`; if the app depends on local bootstrap compatibility, also verify `AUTH_HEADER_VALUE`. Release additionally requires valid `CERT_PINS`.  
  **网络请求失败**：检查本地 `API_BASE_URL`；若当前依赖本地 bootstrap 兼容路径，也要检查 `AUTH_HEADER_VALUE`。构建 release 还需要有效的 `CERT_PINS`。

- **Release gets 401 on `/auth`**: if the backend has not fully switched to `/session`, confirm local `AUTH_HEADER_VALUE` is set so release can seed `LOCAL_BOOTSTRAP_AUTH_VALUE` as a compatibility fallback.  
  **release 在 `/auth` 返回 401**：如果后端尚未完全切换到 `/session`，请确认本地已填写 `AUTH_HEADER_VALUE`，以便 release 通过 `LOCAL_BOOTSTRAP_AUTH_VALUE` 做兼容回退。

- **429 Too Many Requests**: backend rate limit reached; wait and retry later.  
  **429 请求过多**：触发后端限流，稍后再试。

## 7) Security Reminder / 安全提醒

- Use test/local keys for development only.
- Final submission must use the correct real key locally.
- Never share keys in commits, PR comments, docs, screenshots, or chat.

- 开发阶段仅使用测试/本地密钥。
- 最终提交前需在本地替换为正确的正式 key。
- 不要在提交、PR 评论、文档、截图或聊天中泄露密钥。
