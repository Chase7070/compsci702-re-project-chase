# Attack 之后的 Fortify 实施记录 · Post-Attack Fortify Summary

> **日期 / Date：** 2026-05-20  
> **作者 / Author：** Chase  
> **依据文档 / Based on：**  
> - [2026-05-19_attack-self-test-roadmap.md](../attack/2026-05-19_attack-self-test-roadmap.md)  
> - [2026-05-19-attack-results-and-fortify-suggestions.md](../attack/2026-05-19-attack-results-and-fortify-suggestions.md)  
> **课程语境 / Course context：** COMPSCI 702 · Reverse Engineering · Fortify（加固）回应 Attack（攻击）自测结论

---

## 1. 背景与目标 · Background

### 1.1 攻击自测结论（摘要）

依据 **2026-05-19** 对 signed **release APK** 的静态分析（**JADX** + **apktool**）：

| 发现 | 含义 |
|------|------|
| APK 为 **DEX-only**，无 `lib/*.so` | **Ghidra** 非当前优先工具 |
| **StringFog（XOR）** 可被按 APK 内逻辑完整 **recovery（还原）** | 不宜再作为 **bootstrap_token** 核心保护 |
| release 中无 `auth_bootstrap.txt`、无 `BuildConfig` 长期 token 明文 | 既有构建隔离部分有效 |
| **Certificate Pinning**、**EncryptedSharedPreferences**、**IntegrityChecker** 已存在 | 网络与运行时层需继续强化 |

攻击报告建议方向：**R8/ProGuard** +（可选）**dProtect** + **自定义加密（AES）** + **签名/环境检测** + **Pinning**；**StringFog** 仅保留给普通 UI 字符串。

### 1.2 本次 Fortify 目标

在**不违反作业约束**（无商业混淆器、app 直连课程 server、无 Play Integrity 等额外联网）前提下，针对攻击面中最致命的 **bootstrap 凭证静态还原** 做加固，并修复/加强 **签名校验（signature verification）** 链路的可运维性。

---

## 2. 已完成的 Fortify 工作 · Implemented

### 2.1 Bootstrap / API 凭证保护（核心）

| 措施 | 说明 | 主要文件 |
|------|------|----------|
| **不再用 StringFog 保护 bootstrap** | **API key / bootstrap** 不再以 `StringFog.decrypt(byte[], byte[])` 形式出现在 DEX；**StringFog** 仍用于 `com.example.reverseengineeringbuildphase` 包内普通 UI 字符串 | `app/build.gradle.kts` |
| **`RuntimeCredentialProvider`（运行时凭证提供）** | release 在运行时从编译期生成的 **AES-GCM** 密文片段解密 bootstrap；**debug** 仍走 `auth_bootstrap.txt` + `LOCAL_BOOTSTRAP_AUTH_VALUE` | `security/credential/RuntimeCredentialProvider.kt` |
| **编译期分片加密 `generateCredentialFragments`** | 从本地 `secrets.properties` 读取 `AUTH_HEADER_VALUE`，用 **AES-GCM** 加密后拆为 `MaterialSegmentAlpha`…`Delta` 四个类，输出到 `app/build/generated/credential-src/`（不提交仓库） | `gradle/credential-fragments.gradle.kts` |
| **密钥派生 `CredentialKeyDeriver`** | **AES** 密钥由 `packageName` + **签名证书 SHA-256** + `versionCode` + 分散 **salt** 经 **SHA-256** 派生；**重签名（re-sign）** 后密钥变化，解密失败 | `CredentialKeyDeriver.kt`、`CredentialSaltMaterial.kt` |
| **release 移除 `BuildConfig.LOCAL_BOOTSTRAP_AUTH_VALUE` 明文** | release 的 `LOCAL_BOOTSTRAP_AUTH_VALUE` 为空；避免与旧方案一样把 token 打进 **BuildConfig** | `app/build.gradle.kts` |

### 2.2 R8 / ProGuard（保留并配合）

| 措施 | 说明 |
|------|------|
| **Release minify + shrink** | `minifyEnabled true`、`shrinkResources true` 保持开启 |
| **ProGuard 规则** | 保留 Gson/Retrofit/StringFog 必要 **keep**；为 `RuntimeCredentialProvider`、`CredentialCipher` 等保留入口；收紧部分 StringFog 全包 keep |
| **与 Gson release 兼容** | 沿用 `data.model.**` 等 keep，避免 `/auth` **ClassCastException**（见 [release-gson-r8-auth-classcast.md](release-gson-r8-auth-classcast.md)） |

### 2.3 签名校验 / 完整性（加强）

| 措施 | 说明 |
|------|------|
| **`RELEASE_SIGNING_SHA256`** | 本地 `secrets.properties` 配置 **release 签名证书 SHA-256**（小写、无冒号），写入 `BuildConfig.EXPECTED_SIGNING_CERT_SHA256` |
| **Release 严格校验** | **Debug** 跳过；**release** 若 expected 为空或 **actual ≠ expected** 则 `signingCertMatchesExpected` 为 false |
| **证书指纹算法** | 对 `Signature.toByteArray()` 解析为 **X509Certificate**，再对 **`cert.encoded`** 做 **SHA-256**（与 **keytool / apksigner** 一致） |
| **证书来源优先级** | API 28+：优先 **`apkContentsSigners[0]`**（与 **apksigner --print-certs** 对齐），fallback `signingCertificateHistory.last` |
| **UI + 网络双层拦截** | `MainViewModel` 与 `SensitiveRequestGuardInterceptor` 均依赖同一校验 |

### 2.4 泄露面收敛（token 不进入日志）

| 措施 | 说明 |
|------|------|
| **`toString()` 脱敏** | `SessionCreateRequest`、`SessionRefreshRequest`、`SessionResponse`、`AuthResponse`、`GenerateImageRequest` 覆盖 **toString**，不输出 **bootstrap_token / access_token / refresh_token** |
| **Release 无 HttpLoggingInterceptor** | 仅 **debug** 注册 **BASIC** 级别日志 |

### 2.5 既有能力保留（攻击路线图中已列）

以下在 attack 自测路线中已标注为「逆向时会遇到」，本次**未拆除**，仅与 bootstrap 方案衔接：

- **Certificate Pinning（证书固定）** — `CERT_PINS` / `PinningConfigurator` / OkHttp **CertificatePinner**
- **Host allowlist** — `HostAllowlistInterceptor`、`AllowedHosts`
- **EncryptedSharedPreferences + Keystore** — `TokenManager`
- **IntegrityChecker** — debuggable、debugger、root、Frida/Xposed、安装来源等
- **Network Security Config** — 禁止 **cleartext traffic**
- **Retrofit `@Url` + `EndpointProvider`** — 降低注解内 path 明文暴露（StringFog 可处理分片）

### 2.6 诊断支持（便于 release 联调）

| 措施 | 说明 |
|------|------|
| **`REBP-Signing` 日志** | **release** 签名校验失败时 `Log.w`，仅打印 expected/actual **长度与前 8 位**、**source**、**error**；不打印完整 hash 或任何 token |

过滤命令：

```bash
adb logcat -s REBP-Signing
```

---

## 3. 刻意未做 / 延后的项 · Deferred

| 项 | 攻击/建议文档中的位置 | 未做原因 |
|----|------------------------|----------|
| **dProtect** | 加强建议表 | 需 **GitHub Packages** 凭证；与 **R8/Gson/Retrofit** 组合风险高；已用 **R8 + 自定义 AES 凭证层** 替代 |
| **商业混淆器** | 明确禁止 | 作业约束 |
| **Play Integrity / SafetyNet** | 不建议列表 | 可能产生课程 server **以外** 通信 |
| **第三方中转 server** | 不建议 | 必须直连 `https://ai.elliottwen.info` |
| **服务端轮换 bootstrap / 吊销** | 风险说明 | 团队无法控制课程 **server** |
| **Gradle `signingConfigs` 写入仓库** | 运维便利性 | keystore 密码不宜入库；仍用 **Generate Signed APK** + 本地 `secrets.properties` |
| **移除 `REBP-Signing` 诊断日志** | — | 留待 release 稳定后提交前可删或改为仅 debug（当前便于排查） |
| **Ghidra 分析** | 自测路线图 | 当前 APK **无 native `.so`**，与 fortify 实现无直接依赖 |
| **Frida / MITM 完整复测填表** | 自测路线图 Step 3–4 | 属 **attack 证据** 流程，非本次代码改动范围；建议在 rebp **Signed APK** 上按 [fortify-test-plan.md](../attack/fortify-test-plan.md) 补证据 |

---

## 4. 调试过程记录 · Debug Timeline

本节记录 **release 点击 Generate 报「应用签名与预期不一致」** 的排查过程，便于报告与答辩引用。

### 4.1 现象

- **UI：**「生成已阻止。」「应用签名与预期不一致，已禁止发起生成请求。」
- **触发：** `MainViewModel` → `IntegrityChecker.signingCertMatchesExpected()` 返回 false。

### 4.2 已排除的假设

| 假设 | 验证方式 | 结果 |
|------|----------|------|
| rebp **Signed APK** 本身签错 | `apksigner verify --print-certs` on `app-release.apk` | SHA-256 = `5470eb27...`（与 `RELEASE_SIGNING_SHA256` 一致） |
| `EXPECTED` 未注入 | JADX：R8 内联后仍为 `5470eb27...` | 正常 |
| 仅 **hex 格式化** 问题 | 改为 `b.toInt() and 0xff` | 仍失败 |
| 仅 **history[0] vs apksigner** | 改为优先 `apkContentsSigners` | 仍失败 |

### 4.3 根因（最终确认）

通过 **`adb pull`** 模拟器内 **`base.apk`** 并用 **apksigner** 验签：

| 对象 | 签名 DN | SHA-256 前缀 |
|------|---------|----------------|
| 模拟器内已安装包 | **CN=Android Debug** | **`1e08a903...`** |
| `secrets.properties` / **expected** | rebp（Guang Liu） | **`5470eb27...`** |
| logcat **`REBP-Signing`** | — | `expected.pref=5470eb27` **`actual.pref=1e08a903`** `source=apkContentsSigners[0]` |

**结论：** 签名校验逻辑正常；设备上运行的是 **Android Studio Run / Assemble 安装的 debug 签名包**，而 **BuildConfig** 与凭证加密按 **rebp release 证书** 构建，故 **match=false** 为预期行为。

### 4.4 正确运行方式（方案 B）

1. **不要**用 Run ▶ 安装用于验证 **release 签名校验** 的构建。  
2. **Build → Clean Project → Generate Signed App Bundle or APK**（`rebp_release_key.jks`，alias `rebp`）。  
3. `adb uninstall com.example.reverseengineeringbuildphase`  
4. `adb install -r <rebp-signed-app-release.apk>`  
5. 可选：`adb logcat -s REBP-Signing`，Generate 时不应再出现 signing **mismatch**（或 `actual.pref=5470eb27`）。

### 4.5 中间代码修复（与根因并行，仍保留）

| 修复 | 目的 |
|------|------|
| Gradle **`java.security` → import `KeyStore`** | 修复 `build.gradle.kts` 脚本编译错误 |
| **`credential-fragments` 自配置** | 避免跨脚本 `configureCredentialFragmentGeneration` 不可见 |
| **`(0xNN).toByte()` 生成分片** | 修复 Kotlin **byteArrayOf** 大于 0x7f 字面量编译错误 |
| **X509 `cert.encoded` 哈希** | 与 **apksigner** 指纹算法一致 |
| **`REBP-Signing` 诊断日志** | 快速区分 expected/actual/source |

---

## 5. 与 Attack 建议的对照 · Mapping

| 攻击报告建议 | 本项目状态 |
|--------------|------------|
| **R8 / ProGuard** | ✅ 已启用 |
| **dProtect** | ❌ 未接入（见 §3） |
| **弱化 StringFog 对 API key** | ✅ bootstrap 迁出 StringFog |
| **自定义 AES + 分片** | ✅ `RuntimeCredentialProvider` + Gradle 生成片段 |
| **签名校验 / 完整性** | ✅ 加强 + `RELEASE_SIGNING_SHA256` + 绑定解密 |
| **debug/root/Frida 检测** | ✅ 既有 `IntegrityChecker` |
| **Certificate Pinning** | ✅ 保留 |
| **StringFog 保留给 UI** | ✅ `fogPackages` 仍为应用包 |

---

## 6. 验证建议 · Verification

### 6.1 静态（对应 fortify-test-plan S1–S3）

```bash
# 源码与生成目录（排除 secrets.properties）；搜索已知被攻击还原过的 token 前缀或完整 hex
rg '<your-bootstrap-hex-pattern>' --glob '!secrets.properties'

# release APK 中不应有完整 bootstrap 明文（对 rebp Signed APK）
strings app-release.apk | rg '<same-pattern>'  # 应无匹配
```

**JADX：** 不应再通过单一 **StringFog.decrypt** 直接还原 bootstrap；应看到 **MaterialSegment*** + **AES/GCM** 相关类。

### 6.2 签名校验

```bash
apksigner verify --print-certs <rebp-signed.apk>
# 应与 RELEASE_SIGNING_SHA256 一致

adb logcat -s REBP-Signing
# Generate 后不应再出现 actual.pref=1e08a903（debug 证）
```

### 6.3 功能

- 输入 prompt → 请求 `https://ai.elliottwen.info` → 生成/保存图片。  
- **Debug Run** 仍可用于日常开发（跳过签名校验）。

---

## 附录：Release Signed APK 安装与 APKLab（临时约定）· APK Install Workflow

> **重要：** 本节为 **2026-05-20** 联调后的 **临时操作约定**，不改变上文既有技术结论；**未改写上文任何段落**。

### 当前推荐做法（请务必遵守）

在需要验证 **release 签名校验（signature verification）**、`RuntimeCredentialProvider`、**Generate** 等完整 release 行为时：

| 操作 | 是否推荐 |
|------|----------|
| **`adb uninstall` + `adb install -r` 安装 rebp Signed APK** | ✅ **推荐**（已验证可用） |
| **Android Studio Run ▶ 安装 release** | ❌ 不推荐（易变成 debug 签名或旧包） |
| **APKLab 安装 / 运行 APK 到模拟器** | ❌ **暂时不要用**（见下） |
| **APKLab 仅做静态分析**（Open APK、jadx、apktool，不装到模拟器） | ✅ 可以 |

**推荐命令（路径按本机 Signed APK 实际输出调整）：**

```bash
adb uninstall com.example.reverseengineeringbuildphase

adb install -r "/Users/chase/Documents/Part/not_found/APK_release/release/app-release.apk"
```

安装后可用 `apksigner verify --print-certs <同一路径 apk>` 确认 SHA-256 仍为 rebp 证书（前缀 `5470eb27`）；若需排查签名校验，见上文 §4 与 `adb logcat -s REBP-Signing`。

### 为什么暂时不要用 APKLab「运行 / 安装」release APK

实测现象：**同一份** 认为正确的 rebp Signed APK，用上述 **命令行 `adb install` 可正常运行**；通过 **APKLab** 装到模拟器后，仍可能出现 **「应用签名与预期不一致」** 等与 §4.3 相同类问题（例如 logcat 中 `actual.pref=1e08a903` 对应 **Android Debug** 证书）。

**目前没有单一、可复现的定论（root cause 未封闭）**，仅记录 **可能原因**，供日后继续研究：

| # | 可能原因 | 说明 |
|---|----------|------|
| 1 | **APKLab Rebuild 后安装的是 `dist/` 产物** | Open APK → Rebuild 会用 **uber-apk-signer** 重签；未在 VS Code 配置 rebp **keystore** 时，常为 **debug 签名**，与 `BuildConfig.EXPECTED_SIGNING_CERT_SHA256`（rebp）不一致。 |
| 2 | **Install 的目标并非 `APK_release/...` 原文件** | 菜单 **Install the APK** 可能指向解包目录下的 **重建 APK**，而非 Generate Signed APK 向导生成的那份。 |
| 3 | **误装 Gradle 输出路径** | `app/build/outputs/apk/release/app-release.apk` 在未配置 `signingConfigs.release` 时，本地常为 **debug.keystore 签名**，与 `APK_release/release/app-release.apk`（rebp）**同名不同签**。 |
| 4 | **与 Android Studio Run 混用** | 先 APKLab 再 Run，或反之，可能 **覆盖安装** 为 debug 签包；**Wipe data** 只清数据，**不能** 修正错误签名。 |
| 5 | **APKLab 安装链路是否二次签名未验证** | 文档支持配置 `apklab.keystorePath` 等，但本项目 **未** 做对照实验；不能排除 Install 前后签名被改写。 |
| 6 | **验证不足** | 若在 APKLab 安装后未执行 `adb pull` + `apksigner verify --print-certs`，易误以为「选的是同一个文件」而实际设备上已是 debug 签。 |

**日后若要研究：** 建议在 APKLab 每次 Install 后立即 `pull` 设备内 `base.apk` 与命令行安装对照 **DN / SHA-256**；并分别测试「仅 Install 原 apk」「Rebuild + Install」「配置 rebp keystore 后 Rebuild」三种路径。

### 与上文 §4.4 的关系

上文 **§4.4 正确运行方式** 仍有效；本附录 **额外强调**：在问题未查清前，**release 功能验证统一走命令行安装**，**不要用 APKLab 把包装进模拟器**，以免再次触发签名校验失败。

---

## 7. 残余风险声明 · Residual Risk

与 [build-and-security-declaration.md](../submission/build-and-security-declaration.md) 一致：

- 加固提高 **静态提取**、**常规 MITM**、一般 **Hook** 环境与 **重打包** 成本。  
- **不宣称**无法被专业 **pin-bypass**、长期 **Frida**、内存取证破解。  
- **运行时** 解密后 bootstrap 仍会出现在内存与 **HTTPS 请求体** 中。  
- 本地 **`secrets.properties`** 与构建机仍持有明文（仅不进入 Git）。

---

## 8. 相关文件索引 · File Index

| 类别 | 路径 |
|------|------|
| 凭证运行时 | `app/src/main/java/.../security/credential/` |
| 签名校验 | `app/src/main/java/.../security/IntegrityChecker.kt` |
| 网络拦截 | `.../data/network/SensitiveRequestGuardInterceptor.kt` |
| 构建 / 生成 | `app/build.gradle.kts`、`gradle/credential-fragments.gradle.kts` |
| ProGuard | `app/proguard-rules.pro` |
| 密钥模板 | `secrets.example.properties` |
| 攻击输入 | `docs/attack/2026-05-19-*.md` |
| 既有 Fortify | `docs/fortify/2026-05-13_session-pinning-keystore.md` 等 |

---

## 9. 变更记录 · Changelog

| 日期 | 说明 |
|------|------|
| 2026-05-20 | 初版：汇总 attack 后的 fortify 实施、未做项、签名校验调试与 rebp 安装说明 |
| 2026-05-20 | 附录：强调 release 暂勿用 APKLab 安装/运行，优先 adb；列 APKLab 可能原因（无定论） |
