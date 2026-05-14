# Build And Security Declaration

> For COMPSCI 702 Reverse Engineering Project submission  
> 适用于 COMPSCI 702 Reverse Engineering Project 提交材料

---

## 1. Build Environment

### English

- JDK: 17
- Android Studio: stable version recommended
- Android SDK: API 34 recommended
- Build system: Gradle Wrapper from repository

### 中文

- JDK：17
- Android Studio：建议稳定版
- Android SDK：建议 API 34
- 构建系统：仓库自带 Gradle Wrapper

---

## 2. Build Steps

### English

1. Clone the repository.
2. Create local `secrets.properties` from `secrets.example.properties`.
3. Set `API_BASE_URL`.
4. For local debug fallback only, set `AUTH_HEADER_VALUE` if needed.
5. For release builds, set `CERT_PINS`.
6. Optionally set `RELEASE_SIGNING_SHA256` if release signing certificate verification is used.
7. Build:

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

### 中文

1. 克隆仓库。
2. 复制 `secrets.example.properties` 为本地 `secrets.properties`。
3. 配置 `API_BASE_URL`。
4. 如需本地 debug fallback，可额外配置 `AUTH_HEADER_VALUE`。
5. 构建 release 时必须配置 `CERT_PINS`。
6. 如启用 release 签名校验，可额外配置 `RELEASE_SIGNING_SHA256`。
7. 构建命令：

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

---

## 3. Security Hardening Summary

### English

This project currently applies the following hardening measures:

- Release uses Android R8 / ProGuard and resource shrinking
- Sensitive logging is reduced in release
- Cleartext traffic is disabled
- Network Security Config is enabled
- Host allowlist is enforced for outbound requests
- Image URLs must be HTTPS and allowlisted
- Certificate pinning is required in release
- Debug-only bootstrap auth asset is no longer packaged in release
- Local token storage uses `EncryptedSharedPreferences`
- Basic integrity/risk detection is enabled

### 中文

当前项目已实现以下安全强化：

- Release 启用 Android R8 / ProGuard 与资源收缩
- Release 收敛敏感日志输出
- 禁用明文流量
- 启用 Network Security Config
- 对外联 host 实施 allowlist
- 图片 URL 必须为 HTTPS 且在白名单中
- Release 要求配置证书 pin
- 调试用 bootstrap 认证 asset 不再打包进 release
- 本地 token 使用 `EncryptedSharedPreferences` 存储
- 已接入基础完整性 / 风险检测

---

## 4. Obfuscator Declaration

### English

This submission uses only open-source or officially supported Android hardening/obfuscation tools:

- Android R8 / ProGuard
- StringFog (open source)

No commercial obfuscator is used.

### 中文

本次提交仅使用开源或 Android 官方支持的强化 / 混淆工具：

- Android R8 / ProGuard
- StringFog（开源）

未使用任何商业混淆器。

---

## 5. Secrets Handling Declaration

### English

- Real secrets are not committed to the repository.
- `secrets.properties` is local-only and gitignored.
- `secrets.example.properties` contains placeholders only.
- Debug fallback values are for local development only and must not be treated as production-safe.

### 中文

- 真实密钥不会提交到仓库。
- `secrets.properties` 仅保留在本地并被 git 忽略。
- `secrets.example.properties` 仅包含占位值。
- debug fallback 仅用于本地联调，不能视为生产安全方案。

---

## 6. Residual Risk Declaration

### English

The current hardening increases reverse-engineering and interception difficulty, but does not claim complete resistance against advanced runtime hooking, Frida/Xposed instrumentation, or professional pin-bypass techniques.

### 中文

当前强化措施的目标是提高逆向分析与常规抓包难度，但并不宣称能够完全抵御高级运行时 Hook、Frida/Xposed 注入或专业 pin-bypass 技术。

---

## 7. Submission Reminder

### English

Before final submission:

- Verify release build succeeds
- Verify release pins are configured
- Verify release APK does not package debug bootstrap auth asset
- Verify no real key appears in source-controlled files

### 中文

最终提交前请确认：

- release 构建成功
- release 已配置证书 pin
- release APK 未打包 debug bootstrap 认证 asset
- 版本控制文件中不包含真实密钥
