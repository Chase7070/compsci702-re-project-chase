# Attack 自测思路 · Reverse-Engineering Self-Test Roadmap

> **日期 / Date：** 2026-05-19  
> **目标 APK / Target APK：** `/Users/chase/Documents/Part/not_found/APK_release/attack/app-release_L.apk`（signed release）  
> **课程语境 / Course context：** COMPSCI 702 · Reverse Engineering · Attack / Fortify  
> **配套文档 / Related docs：** [fortify-test-plan.md](fortify-test-plan.md) · [attack-evidence-template.md](attack-evidence-template.md)

---

## 1. 本次自测要回答什么 · What You Are Proving

你不是在「学会装 Ghidra」，而是在用**攻击者视角（attacker perspective）**估算：**别人破解你这个 signed release APK 有多难**。

需要回答的四个核心问题：

| # | 问题（中文） | Question (EN) |
|---|--------------|----------------|
| Q1 | APK 里有没有 **native `.so`**？ | Does the APK ship **native libraries**? |
| Q2 | 没有 `.so` 时，**Ghidra** 还优先吗？ | Without `.so`, is **Ghidra** still the first tool? |
| Q3 | 应先 **jadx / apktool / MITM / Frida** 中的哪一个？ | Which comes first: **jadx**, **apktool**, **MITM**, or **Frida**? |
| Q4 | 课程 Lab 流程如何落到**真实 APK**？ | How do course labs map to **this APK**? |

**一句话结论 / One-liner：**  
本 release APK **无 `lib/*.so`**，属于典型 **Java/Kotlin DEX** 应用 → **优先 jadx + apktool**，再用 **mitmproxy** 验 **SSL pinning（证书固定）**，静态不够时用 **Frida** 做 **runtime hook（运行时挂钩）**；**Ghidra 暂非重点**。

---

## 2. APK 结构检查结果 · APK Structure Check

已对 `app-release_L.apk` 做 `unzip -l` 检查：

| 组件 / Component | 结果 / Result | 含义 / Implication |
|----------------|---------------|---------------------|
| `classes.dex` | ✅ 有 | 主逻辑在 **DEX（Dalvik bytecode）**，适合 **jadx** 反编译 |
| `AndroidManifest.xml` | ✅ 有（打包形式） | 用 **apktool** 解包后可读权限、组件、网络安全配置 |
| `resources.arsc` | ✅ 有 | 资源与字符串表；配合 apktool 看 `res/` |
| `lib/**/*.so` | ❌ **无** | **无 JNI / native 层**；Ghidra 分析 native 的优先级低 |

因此：逆向主战场在 **smali/DEX + 网络 + 运行时**，不在 **ARM native**。

### 2.1 复现命令 · Commands to Reproduce

```bash
APK="/Users/chase/Documents/Part/not_found/APK_release/attack/app-release_L.apk"

unzip -l "$APK" | grep -E 'classes\.dex|lib/.*\.so|AndroidManifest|resources\.arsc'
unzip -l "$APK" | grep 'lib/.*\.so' || echo "No native .so found"
```

### 2.2 Ghidra 安装检查（课程外，可选）· Ghidra Install Check

课程 Lab 主要教 **workspace / import / analyze**，完整安装多在 Canvas announcement。本地是否已装 Ghidra：

```bash
which ghidra
brew list --cask | grep -i ghidra
ls /Applications | grep -i ghidra
find ~ -name "ghidraRun" 2>/dev/null
```

**GUI 启动 / GUI launch：** `open -a Ghidra` 或运行解压目录下的 `./ghidraRun`。

> **何时再用 Ghidra / When to revisit Ghidra：** 若日后 APK 出现 `lib/arm64-v8a/*.so`、JNI、`System.loadLibrary`，或你要分析 **native 加密 / anti-debug**，再把 Ghidra 提到前列。

---

## 3. 工具优先级 · Tool Priority (This APK)

| 优先级 | 工具 | 主要用途（EN） | 主要用途（中文） |
|--------|------|----------------|------------------|
| **1** | **jadx** | Decompile `classes.dex` → Java/Kotlin-like source | 反编译看业务与 Fortify 逻辑 |
| **2** | **apktool** | Decode manifest, `res/`, **smali** | 解包看权限、网络安全配置、改包重签测试 |
| **3** | **mitmproxy** / Charles | Observe HTTPS, test **certificate pinning** | 验证 release 能否被普通用户 CA 抓包 |
| **4** | **Frida** | **Dynamic analysis**, hook OkHttp / decrypt | 静态看不清时的运行时取证 |
| **5** | **Ghidra** | Native / JNI / `.so` | **当前 APK 不适用优先** |
| 辅助 | `strings` | Quick string sweep on APK | 快速扫明文字符串 |

与课程资料对应关系：

- **静态反编译** → jadx（对应 Lab 里「看 Java 逻辑」）
- **解包与 smali** → apktool（对应「改资源 / Manifest / 重打包」）
- **中间人攻击 MITM** → mitmproxy（对应「抓 `/auth`、`/generate_image`」）
- **动态插桩** → Frida（对应「运行时 URL / header / 解密结果」）
- **Ghidra** → 课程里强调 native；**本 APK 无 native，故后置**

---

## 4. 推荐分析路线（五步）· Recommended Five-Step Workflow

### Step 1 · jadx — 静态反编译 / Static Decompilation

**目的：** 建立攻击面地图（attack surface map）。

**重点搜索 / Search targets：**

| 关键词 | 说明 |
|--------|------|
| `API_BASE_URL` / `BuildConfig` | 服务端地址与构建注入字段 |
| `Authorization` / `TokenManager` | 凭证如何进入请求头 |
| `Retrofit` / `OkHttp` / `@POST` / `@Url` | 接口与路径（部分 path 可能运行时拼接） |
| `CertificatePinner` / `PinningConfigurator` | **证书固定** 实现位置 |
| `IntegrityChecker` / `SensitiveRequestGuardInterceptor` | **完整性校验**、高风险阻断 |
| `StringFog` / `StringFogImpl` | **字符串混淆** 解密调用点 |
| `EncryptedSharedPreferences` / `MasterKeys` | 本地加密存储 |
| `RELEASE_SIGNING_SHA256` | 可选 **签名校验** |

**通过/加固预期（对照 fortify-test-plan S1–S3）：**

- 不应再出现 `BuildConfig.AUTHORIZATION_TOKEN` 与真实长期 token 明文
- release 中不应有 `auth_bootstrap.txt`（debug 才有）
- 敏感字符串不应「一搜即得」；StringFog 可提高 **jadx 静态搜索成本**，但不等于 secret-safe

**留档：** jadx 搜索截图 → 见 [attack-evidence-template.md](attack-evidence-template.md) 命名如 `S1_jadx_search_auth_token.png`

---

### Step 2 · apktool — 解包 / Decode & Smali

**目的：** 看配置层与是否便于 **重打包（repackage）+ 重签名（re-sign）**。

```bash
apktool d app-release_L.apk -o decoded-release
```

**重点路径 / Paths to inspect：**

- `AndroidManifest.xml` → `android:debuggable`、`allowBackup`、`usesCleartextTraffic`
- `res/xml/network_security_config.xml`（若存在）→ 是否信任用户 CA
- `smali/` → 改 smali 后重签，测 **runtime signature check** 能否发现篡改
- `assets/` → 确认 **无** `auth_bootstrap.txt`（release）

**Fortify 相关测试编号：** S2、以及「重打包测试」见下文 Step 5。

---

### Step 3 · MITM — mitmproxy / 抓包

**目的：** 验证 **SSL/TLS pinning** 是否挡住「普通攻击者」：用户 CA + 代理，**无 root、无 Frida**。

**步骤概要：**

1. 设备安装 **用户 CA（user-installed CA）**
2. 启动 `mitmproxy`，配置 Wi‑Fi 代理
3. 安装 **release APK**（非 debug）
4. 走完整流程：`/auth`（或 session）→ `/generate_image`

**预期（N2）：** release 应 **失败** — 证书校验错误、无业务明文、客户端有用户可读错误（不泄露 pin 细节）。

**对照：** debug 可关 pin / 用 bootstrap 联调（N1）— 不要把 debug 结果当成 release 防护证明。

**可选进阶（剩余风险，非必做）：** `JustTrustMe`、专业 pin-bypass — 用于说明「无法抵御高级攻击」，见 [build-and-security-declaration.md](../submission/build-and-security-declaration.md)。

---

### Step 4 · Frida — 动态分析 / Runtime Hook

**何时上 Frida：** jadx 里字符串被 **StringFog** 加密、URL 运行时拼接、或要看 **内存中** 的 Authorization / refresh 结果。

**可 hook 方向（示例思路，非完整脚本）：**

- OkHttp `Interceptor` / `RealCall` → 最终 URL、headers
- `StringFog` 解密相关类 → 运行时明文
- `IntegrityChecker` 返回值 → 是否判定 **root / Frida / Xposed** 痕迹
- `TokenManager.getAuthorizationHeaderValue()` → 会话凭证（证明 **R2 剩余风险**）

**预期（R2）：** release 在检测到 Hook 痕迹时，**SensitiveRequestGuard** 应阻断敏感请求 — 记录是否成功阻断与你的绕过难度。

---

### Step 5 · 重打包测试 · Repackage & Re-sign Test

**目的：** 验证 **完整性 / 签名校验（integrity / signature verification）** 是否让「改包再装」变难。

1. apktool 解包 → 修改少量 smali（如日志、nop、替换字符串）
2. `apktool b` 重建 → `jarsigner` / `apksigner` **攻击者自签**
3. 安装并运行 → 观察是否闪退、提示风险、或网络层拒绝敏感请求

**关注类：** `IntegrityChecker` + `RELEASE_SIGNING_SHA256`（若在 `secrets.properties` 中配置）。

---

## 5. 与本项目 Fortify 的对照矩阵 · Mapping to Fortify Test Plan

自测时建议直接填 [fortify-test-plan.md](fortify-test-plan.md) 中的编号，证据写入 [attack-evidence-template.md](attack-evidence-template.md)。

| 编号 | 类别 | 自测动作（工具） | 预期 |
|------|------|------------------|------|
| **S1** | 静态 | jadx + `strings` 搜 token / `AUTHORIZATION_TOKEN` | 无长期凭证明文 |
| **S2** | 静态 | apktool 查 `assets/auth_bootstrap.txt` | release 不存在 |
| **S3** | 静态 | jadx 看 host、path、认证链 | 无「一搜即得」的完整攻击链 |
| **N1** | MITM | debug + 代理 | 允许联调 |
| **N2** | MITM | release + 用户 CA + mitmproxy | **应失败** |
| **N3** | 配置 | 去掉 `CERT_PINS` 构建 release | 构建或启动失败 |
| **N4** | 配置 | allowlist host 无 pin | release 拒绝 |
| **R1** | 运行时 | root / test-keys / 侧载 | 风险提示或限制 |
| **R2** | 运行时 | Frida / Xposed 痕迹 | 阻断敏感请求 |
| **A1–A3** | 业务 | 正常 token / refresh / 失败登出 | 功能仍可用 |

本项目已实现要点（逆向时你会遇到）— 详见 [2026-05-13_session-pinning-keystore.md](../fortify/2026-05-13_session-pinning-keystore.md)：

- **Certificate pinning（证书固定）** — `CERT_PINS` / `CertificatePinner`
- **EncryptedSharedPreferences + Keystore** — 本地 token 存储
- **StringFog** — 降低 jadx 静态字符串命中率（Retrofit path / `@SerializedName` 仍可能可见）
- **IntegrityChecker** — debuggable、debugger、root、Frida/Xposed、安装来源、可选签名 SHA-256
- **SensitiveRequestGuardInterceptor** — 高风险环境下阻止敏感请求

---

## 6. Ghidra：为什么当前不优先 · Why Ghidra Is Deprioritized

| Ghidra 擅长 | 本 APK 现状 |
|-------------|-------------|
| **Native C/C++**、JNI、`.so` 反汇编 | ❌ 无 `lib/*.so` |
| Native 层加密 / anti-debug | ❌ 逻辑在 Kotlin/Java |
| ARM 指令级分析 | ❌ 主战场是 **DEX** |

**结论：** 当前阶段把时间放在 **jadx → apktool → MITM → Frida**；Ghidra 留作「将来有 native 或课程专门 lab」再用。

---

## 7. 建议执行顺序与时间盒 · Suggested Order & Timeboxing

```text
Day 1  结构确认 + jadx 通读（2–3h）     → 输出攻击面笔记
Day 2  apktool + strings（1–2h）        → S1/S2 证据
Day 3  mitmproxy release 抓包（1–2h）   → N2 证据
Day 4  Frida 抽样 hook（可选，2h）      → R2 与剩余风险说明
Day 5  重打包 / 重签（可选，1–2h）      → 完整性结论
```

---

## 8. 风险声明（写进报告用）· Residual Risk Statement

与 [build-and-security-declaration.md](../submission/build-and-security-declaration.md) 一致：

- 当前 Fortify **提高**了静态提取、**常规 MITM**、一般 Hook 环境下的攻击成本。
- **不宣称**能完全抵御：专业 **pin-bypass**、长期 **Frida/Xposed** 驻留、物理取证、服务端未实现部分的攻击面。

自测的价值是：**用证据说明「难在哪里、哪里仍可能被突破」**，而不是证明「无法破解」。

---

## 9. 快速命令备忘 · Quick Command Cheat Sheet

```bash
# APK 路径
export APK="/Users/chase/Documents/Part/not_found/APK_release/attack/app-release_L.apk"

# 结构
unzip -l "$APK" | grep -E 'classes\.dex|lib/|AndroidManifest|resources\.arsc'

# jadx GUI（若已安装）
jadx-gui "$APK"

# apktool
apktool d "$APK" -o decoded-release

# strings 粗扫
strings "$APK" | grep -iE 'authorization|token|elliott|session|pin' | head -50

# mitmproxy（示例）
mitmproxy --listen-port 8080
```

---

## 10. 变更记录 · Changelog

| 日期 | 说明 |
|------|------|
| 2026-05-19 | 初版：基于 signed release APK 结构检查与课程/GPT 讨论整理；确立 jadx-first 自测路线 |
