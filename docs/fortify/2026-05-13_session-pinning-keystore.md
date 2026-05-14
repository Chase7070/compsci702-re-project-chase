# Fortify 开发日志 · 2026-05-13

**主题：** 会话与凭证迁移（去 BuildConfig 长期 token）、OkHttp 证书固定、401 刷新骨架、EncryptedSharedPreferences、轻量完整性策略与实现记录。

**范围：** `app/build.gradle.kts`、`NetworkModule`、`AuthorizationInterceptor` 数据源、`SessionRepository`、`TokenManager`（`androidx.security:security-crypto`）、`TokenRefreshAuthenticator`、`PinningConfigurator`、`IntegrityChecker`、`MainViewModel` / `MainActivity`、`ReverseEngineeringApp`、`NetworkErrorMapper`、`secrets.example.properties`、`.gitignore`。

---

## 1. 已实现能力摘要

### 1.1 成员 A（网络链路）

| 项 | 说明 |
|----|------|
| 移除 `BuildConfig.AUTHORIZATION_TOKEN` | 不再通过 BuildConfig 注入长期 Authorization 明文常量；dex 中不再出现该字段名与值。 |
| 凭证来源 | `preBuild` 任务 `syncAuthBootstrap` 仅在 **debug** 变体写入 `src/debug/assets/auth_bootstrap.txt`（已 `.gitignore`）；release 仍不打包该 debug bootstrap asset，但本地构建的 release 现可直接读取 `AUTH_HEADER_VALUE` 作为兼容兜底。启动后统一落入 **EncryptedSharedPreferences**（AES-GCM + Keystore，经 `MasterKeys`）。 |
| 拦截器 | `AuthorizationInterceptor` 从 `TokenManager.getAuthorizationHeaderValue()` 读取（与既有「原始 token、无 Bearer 前缀」约定一致）。 |
| `POST /session` / `POST /session/refresh` | Retrofit 已声明；启动时优先尝试 `createSession`；若服务端尚未完全切换 session 契约、返回 4xx/5xx 或网络失败，则回退到本地 bootstrap（debug asset 或 `LOCAL_BOOTSTRAP_AUTH_VALUE`）以保持兼容。 |
| 401 与单次重放 | `TokenRefreshAuthenticator`：仅当请求已带 `Authorization` 且存在 **refresh_token** 时尝试刷新并重放一次（`X-Auth-Retry` 防循环）；无 refresh 时直接返回 401 给上层，**不清空**仅静态 access 的场景。刷新失败则清空会话并 `AppSessionEvents` 通知 UI。 |
| 证书固定 | `CERT_PINS`（逗号分隔）经 `BuildConfig.CERTIFICATE_PINS` 注入：裸 `sha256/...` 作用于 **BASE_URL host**，`host=sha256/...` 可作用于额外 allowlist host。`CertificatePinner` 现要求 **release 为每个 allowed host 都配置 pin**；未覆盖时将在构建/运行时拒绝继续。 |
| Pin 失败文案 | `NetworkErrorMapper` 对 `SSLPeerUnverifiedException` 给出用户可读说明（不暴露内部 pin 细节）。 |

### 1.2 成员 C（平台与本地，基础版）

| 项 | 说明 |
|----|------|
| Keystore 链路 | `TokenManager` 使用 `EncryptedSharedPreferences` + `MasterKeys.getOrCreate(AES256_GCM_SPEC)`（`security-crypto:1.0.0`）。 |
| 完整性 / 风险 | `IntegrityChecker`：`release` 下检测 **FLAG_DEBUGGABLE**、**调试器连接**、**root/test-keys**、**Frida/Xposed/LSPosed/Substrate 痕迹**、非常见安装来源（Android 11+）；可选 `RELEASE_SIGNING_SHA256` 与证书比对。除 `MainViewModel` 外，网络层 `SensitiveRequestGuardInterceptor` 也会在高风险或签名校验失败时阻止敏感请求。 |

### 1.3 成员 B / D（本迭代边界）

- **未实现** 服务端 `timestamp/nonce`、Redis 去重、限流与统一 `traceId` 错误体（需独立后端仓库与契约）。
- **文档：** 本文件作为本次客户端改动的 Fortify 记录；联调矩阵（MITM、重放、过期）仍建议由测试负责人在 `docs/fortify` 后续补「验证结果」小节。

---

## 2. 配置说明（`secrets.properties` / `secrets.example.properties`）

| 键 | 作用 |
|----|------|
| `API_BASE_URL` | 同前，`BuildConfig.BASE_URL`。 |
| `AUTH_HEADER_VALUE` | 本地 bootstrap 值；debug 写入 `src/debug/assets/auth_bootstrap.txt`（勿提交仓库），首次启动导入加密存储。本地构建的 release 在 `/session` 不可用时也可使用该值兜底。 |
| `CERT_PINS` | release 必填。示例：`sha256/Y+qVcAbTbJUkv0N0yR2D7+qaY+yBS8BGRAG0U5ukZec=,sha256/kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4=`（对应当时 `ai.elliottwen.info` 链上 leaf + 中间证书 SPKI；**轮换后需更新**）。若存在额外 allowlist host，可写成 `cdn.example.com=sha256/...`。debug 留空可关闭 pinning，便于本地代理调试。 |
| `RELEASE_SIGNING_SHA256` | 可选，release 签名证书 SHA-256 **十六进制小写无冒号**；空则跳过签名校验。 |

---

## 3. 构建与验证建议

1. `./gradlew :app:assembleDebug` / `assembleRelease`  
2. **JADX：** 确认无 `AUTHORIZATION_TOKEN` 常量；release 包中不应再包含 `auth_bootstrap.txt`。debug 包若包含该 asset，属本地联调用途。  
3. **Pinning：** 安装用户 CA 后，在配置 `CERT_PINS` 的 release 上访问应失败，且 UI 出现证书类中文提示；未配置 pins 的 release 应在构建或启动时失败。  
4. **刷新：** 需后端实现 `session` / `session/refresh` 并返回 `refresh_token` 后，401 自动刷新路径才会生效。

---

## 4. 主要改动文件列表

- `app/build.gradle.kts` — 移除 `AUTHORIZATION_TOKEN`；增加 `CERTIFICATE_PINS`、`EXPECTED_SIGNING_CERT_SHA256`；`syncAuthBootstrap`（仅 debug 生成 asset）；release pin 校验；`security-crypto` 依赖。  
- `app/src/main/java/.../data/security/TokenManager.kt` — 新建。  
- `app/src/main/java/.../data/repository/SessionRepository.kt` — 新建。  
- `app/src/main/java/.../data/session/AppSessionEvents.kt` — 新建。  
- `app/src/main/java/.../data/network/NetworkModule.kt`、`PinningConfigurator.kt`、`SensitiveRequestGuardInterceptor.kt`、`TokenRefreshAuthenticator.kt`、`EndpointProvider.kt`、`ApiService.kt`  
- `app/src/main/java/.../data/model/SessionDtos.kt`  
- `app/src/main/java/.../security/IntegrityChecker.kt`  
- `ReverseEngineeringApp.kt`、`MainViewModel.kt`、`MainActivity.kt`、`NetworkErrorMapper.kt`  
- `app/src/main/res/values/strings.xml`  
- `.gitignore`、`secrets.example.properties`  

---

## 5. 风险与后续

- **Bootstrap 仍存在于 debug：** `auth_bootstrap.txt` 已限制为本地 debug 联调折中；若后端 `/session` 完整可用，后续仍建议彻底删除该 fallback。  
- **Pin 轮换：** `CERT_PINS` 需运维与客户端同步更新，建议保留双 pin。  
- **Installer 启发式** 可能误判侧载渠道 → 仅 `Medium` 提示，不阻断。
