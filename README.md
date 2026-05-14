# COMPSCI 702 · Reverse Engineering Project (Android)  
# COMPSCI 702 · 逆向工程课程项目（Android）

**EN:** University of Auckland **COMPSCI 702** course project: an Android client that talks **directly** to the course server. Main flow: **prompt → `/auth` → `/generate_image` → image preview → save to gallery**.

**中文：** 奥克兰大学 **COMPSCI 702** 课程 Android 客户端，**仅直连**课程服务器。主流程：**输入 prompt → `/auth` → `/generate_image` → 图片预览 → 保存到相册**。

---

## Requirements / 环境要求

**EN:** JDK 17 · Android Studio (stable recommended) · Android SDK (API 34 recommended) · standard AVD or USB-debugged device.

**中文：** JDK 17 · Android Studio（建议稳定版）· Android SDK（建议 API 34）· 标准模拟器或已开启 USB 调试的真机。

---

## Quick start / 快速开始

**EN**

1. Open the **repository root** in Android Studio (folder with `settings.gradle.kts`).  
2. Create local secrets (never commit):

   ```bash
   cp secrets.example.properties secrets.properties
   ```

   Set at least: `API_BASE_URL` (HTTPS base URL; trailing slash per backend). You may also set local `AUTH_HEADER_VALUE` for bootstrap compatibility: debug writes it into `src/debug/assets`, and locally built release can also use it if `/session` is unavailable. Release builds additionally require `CERT_PINS`.  
3. Run the **app** configuration (default **debug**).

**中文**

1. 在 Android Studio 中打开**仓库根目录**（含 `settings.gradle.kts`）。  
2. 本地创建密钥文件（**不要**提交到 Git）：

   ```bash
   cp secrets.example.properties secrets.properties
   ```

   至少填写：`API_BASE_URL`（课程服务器 HTTPS 基址，末尾斜杠按后端要求）。如需本地 bootstrap 兼容，可额外填写 `AUTH_HEADER_VALUE`；debug 会写入 `src/debug/assets`，本地构建的 release 在 `/session` 未就绪时也可使用该值。构建 release 时还需要配置 `CERT_PINS`。  
3. 运行 **app**（默认 **debug**）。

**More / 详细步骤与排错：** [docs/build/install-and-run.md](docs/build/install-and-run.md)

---

## Build / 构建

| Variant / 变体 | Command / 命令 | Notes / 说明 |
|----------------|------------------|----------------|
| Debug | `./gradlew :app:assembleDebug` | **EN:** Dev & emulator; APK in `app/build/outputs/apk/debug/`. **中文：** 日常开发与模拟器演示。 |
| Release | `./gradlew :app:assembleRelease` | **EN:** R8 + shrink resources; APK in `app/build/outputs/apk/release/`. May be **unsigned**—add signing in Android Studio if needed. **中文：** 已启用混淆与资源收缩；默认可能为**未签名** APK，需在 Android Studio 配置签名后再装真机。 |

---

## Documentation / 文档索引

| | |
|--|--|
| **EN** | [Install and run](docs/build/install-and-run.md) · [Repository structure](docs/build/repository-structure.md) · [Fortify log](docs/fortify/2026-04-21.md) · [Dev log · 2026-05-09](docs/fortify/2026-05-09.md) · [Dev log · 2026-05-11 · StringFog](docs/fortify/2026-05-11_Chase7070_fog_v1.1.md) · [Dev log · 2026-05-12 · fog v1.2](docs/fortify/2026-05-12_Chase7070_fog_v1.2.md) · [StringFog status · 2026-05-12](docs/fortify/2026-05-12_Chase7070_stringfog-status.md) · [Session · pinning · Keystore · 2026-05-13](docs/fortify/2026-05-13_session-pinning-keystore.md) · [Fortify test plan](docs/attack/fortify-test-plan.md) · [Attack evidence template](docs/attack/attack-evidence-template.md) · [Build and security declaration](docs/submission/build-and-security-declaration.md) · [Final submission checklist](docs/submission/final-submission-checklist.md) |
| **中文** | [安装与运行](docs/build/install-and-run.md) · [仓库结构](docs/build/repository-structure.md) · [Fortify 记录](docs/fortify/2026-04-21.md) · [开发日志 · 2026-05-09](docs/fortify/2026-05-09.md) · [开发日志 · 2026-05-11 · StringFog](docs/fortify/2026-05-11_Chase7070_fog_v1.1.md) · [开发日志 · 2026-05-12 · fog v1.2](docs/fortify/2026-05-12_Chase7070_fog_v1.2.md) · [StringFog 现状总结 · 2026-05-12](docs/fortify/2026-05-12_Chase7070_stringfog-status.md) · [会话 · 证书固定 · Keystore · 2026-05-13](docs/fortify/2026-05-13_session-pinning-keystore.md) · [Fortify 测试计划](docs/attack/fortify-test-plan.md) · [攻击证据模板](docs/attack/attack-evidence-template.md) · [构建与安全声明](docs/submission/build-and-security-declaration.md) · [最终提交检查清单](docs/submission/final-submission-checklist.md) |

---

## Fortify status / 加固状态

**Done / 已完成**

- **EN:** Release **R8 / ProGuard** + resource shrinking; minimal **keep** rules for Retrofit / Gson. Release builds omit OkHttp `HttpLoggingInterceptor`; debug keeps `Level.BASIC`.  
- **中文：** Release 启用 **R8/ProGuard** 与资源收缩；为 Retrofit/Gson 保留最小 **keep** 规则。Release 不注册 OkHttp 网络日志拦截器；Debug 仍使用 `BASIC` 便于调试。
- **EN:** **Network Security Config** (no cleartext; release trusts **system CAs only**; debuggable builds may trust **user** CAs). **`allowBackup=false`**, **`usesCleartextTraffic=false`**. Outbound **host allowlist** (OkHttp) + **HTTPS + host** checks on image URLs; successful **GET** responses must declare an **image** `Content-Type`. Coil + gallery save share the same OkHttp stack.  
- **中文：** **网络安全配置**（禁止明文；**release** 仅系统 CA；**debuggable** 构建可信任 **user** CA 便于调试）。**`allowBackup=false`**、**`usesCleartextTraffic=false`**。出站 **Host 白名单**（OkHttp）与图片 URL **HTTPS + 域名** 校验；成功 **GET** 的 **Content-Type** 须为图片类型。Coil 预览与相册保存共用同一 OkHttp 客户端。
- **EN:** **StringFog** (MegatronKing, open source): `gradle-plugin:5.2.0` + `xor:5.0.0`; encrypts string literals under `com.example.reverseengineeringbuildphase` only. **Retrofit `@POST` paths** and **`@SerializedName` JSON keys** may still appear in JADX; this layer targets static string search cost, not secret safety.  
- **中文：** **StringFog**（开源）：`gradle-plugin:5.2.0` + `xor:5.0.0`；仅对 `com.example.reverseengineeringbuildphase` 包内字面量做处理。**Retrofit 注解路径**与 **`@SerializedName` 字段名**仍可能被 JADX 直接看到；本层用于降低静态搜索命中率，**不**等同于密钥安全。
- **EN:** Debug-only bootstrap auth value is written to `src/debug/assets/auth_bootstrap.txt`; release builds no longer package that asset. Release also requires certificate pins and blocks sensitive requests in high-risk environments.  
- **中文：** 调试用 bootstrap 鉴权值仅写入 `src/debug/assets/auth_bootstrap.txt`；release 不再打包该 asset。release 还要求配置证书 pin，并会在高风险环境下阻止敏感请求。

**Planned / 计划中**（渐进、符合课程要求）

- **EN:** Mild signing / integrity checks (no hard anti-emulator).  
- **中文：** 温和签名/完整性检测（不做强硬反模拟器）。

**Details / 详情：** [docs/fortify/2026-04-21.md](docs/fortify/2026-04-21.md)

---

## Compliance & secrets / 合规与密钥

**EN:** App talks **only** to the course server—no third-party proxies or unrelated outbound traffic. Do not commit `secrets.properties`, real tokens, or final keys. No commercial obfuscators; use Android **R8**.

**中文：** App **仅**与课程服务器通信，禁止第三方中转或无关外联。勿将 `secrets.properties`、真实 token、最终提交用密钥提交到仓库。不使用商业混淆器，使用 Android 自带 **R8**。

---

*Course assignment repository—not a production product.*  
*课程作业仓库，非生产级产品。*
