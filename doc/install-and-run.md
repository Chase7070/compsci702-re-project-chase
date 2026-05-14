# 安装与运行说明（现状与 GitHub 克隆后）

本文说明 **ReverseEngineeringBuildPhase** Android 工程在本地与 **从 GitHub 克隆之后** 如何配置、构建与运行。

## 项目现状（摘要）

| 项目 | 说明 |
|------|------|
| 类型 | Android 原生应用（Kotlin），包名 `com.example.reverseengineeringbuildphase` |
| 最低 SDK | 24；目标 / 编译 SDK | 34 |
| 构建 | Gradle（Kotlin DSL），根目录 `settings.gradle.kts`、`build.gradle.kts`，模块 `app/` |
| 网络 | 应用通过 Retrofit/OkHttp 调用后端；需 **INTERNET** 权限（已在 Manifest 中声明） |
| 测试 | `app/src` 当前仅有 `main` 源码集；`src/test`、`src/androidTest` 无测试类时，单元测试 / 仪器化测试任务可能为空或需自行补充 |

## 环境要求

- **JDK 17**（与 `app/build.gradle.kts` 中 `JavaVersion.VERSION_17` / `jvmTarget = "17"` 一致）。
- **Android Studio**（推荐最新稳定版）或已配置 **Android SDK** 与命令行工具的本地环境。
- 运行调试：Android **模拟器（AVD）** 或 **USB 调试已开启的真机**。

## 从 GitHub 克隆之后

```bash
git clone <你的仓库 HTTPS 或 SSH URL>
cd ReverseEngineeringBuildPhase
```

使用 Android Studio：**File → Open**，选择克隆下来的**工程根目录**（包含 `settings.gradle.kts` 的目录），等待 Gradle Sync 完成。

命令行仅构建（需已安装 JDK 且 `ANDROID_HOME` 等已配置）：

```bash
./gradlew :app:assembleDebug
```

生成的调试 APK 位于：

`app/build/outputs/apk/debug/app-debug.apk`

安装到已连接设备：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 必改配置（后端地址与令牌）

后端基址与鉴权令牌写在 **`app/build.gradle.kts`** 的 `defaultConfig` 里，通过 **BuildConfig** 注入（字段名 `BASE_URL`、`AUTHORIZATION_TOKEN`）：

- 将 `buildConfigField("String", "BASE_URL", ...)` 改为你的 API 根 URL（需以 `/` 结尾或与客户端拼接逻辑一致）。
- 将 `buildConfigField("String", "AUTHORIZATION_TOKEN", ...)` 改为你的令牌。

**安全建议（上传 GitHub 后务必执行）**

- 不要将真实令牌长期提交到公开仓库。常见做法：在 `local.properties` 或环境变量中注入占位符，仅在本地覆盖；或使用 CI 密钥管理。克隆公开仓库后，请自行填入私有配置并避免再次提交密钥。

代码注释中说明：Authorization 头应为 **原始 token**，不要加 `Bearer ` 前缀，否则 `/auth` 可能返回 401。

修改 `build.gradle.kts` 后需 **Sync Project**，再重新运行或执行 `assembleDebug`。

## 在 Android Studio 中选择运行设备

若存在多台模拟器或真机：

1. 点击运行按钮左侧的 **设备下拉框**。
2. 选择目标 **AVD** 或 **物理设备**；未启动的 AVD 可在选中后由 IDE 自动启动。
3. 也可通过 **View → Tool Windows → Device Manager** 管理并先启动某一模拟器。

固定默认设备：**Run → Edit Configurations…** → 选择 `app` → 在部署目标选项中指定设备（具体名称因 Android Studio 版本略有不同）。

## 模拟器显示异常（黑屏、EGL 警告）

若在模拟器上出现黑屏或 Logcat 中出现 `EGL_emulation`、`HWUI` 相关警告，可尝试：

- 在 AVD 设置中将 **Graphics** 改为 **Software**。
- **Cold Boot** 或 **Wipe Data** 后重试；必要时更换 System Image（API 级别或是否带 Google Play）。

## 故障排查速查

| 现象 | 建议 |
|------|------|
| `Unresolved reference: viewModels` | 确认 `app/build.gradle.kts` 已包含 `androidx.activity:activity-ktx` 依赖，并已 Sync。 |
| 网络请求失败 | 检查 `BASE_URL`、令牌、设备网络；查看 Logcat 与 `NetworkErrorMapper` 映射后的文案。 |
| Gradle 无法下载依赖 | 检查网络与仓库配置；企业网络可能需要代理。 |

## 版本与文档

- 依赖版本以 `app/build.gradle.kts` 为准（例如 `activity-ktx`、`lifecycle`、`Retrofit`、`Coil` 等）。
- 按日开发记录见 `doc/` 下日期命名的 md 文件（如 `2026.4.15.md`）。
