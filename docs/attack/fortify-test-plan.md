# Fortify 安全测试计划

> 适用范围：COMPSCI 702 Reverse Engineering Project Android 客户端  
> 目的：验证当前 Fortify 强化是否提高了静态分析、普通 MITM 抓包、运行时 Hook 与凭证复用的难度。  
> 注意：本计划用于课程验收与证据留档，不等同于生产级渗透测试。

---

## 1. 测试目标

本计划聚焦以下四类风险：

1. **静态提取风险**  
   使用 `jadx`、`apktool`、`strings` 等工具检查是否还能直接找到长期凭证、关键字符串、认证链路入口。

2. **抓包与 MITM 风险**  
   使用 `mitmproxy`、Charles、用户 CA、`JustTrustMe` 等方式验证 release 是否会阻断常规代理抓包。

3. **运行时提取风险**  
   使用 Hook/Frida 视角评估当前实现是否仍容易从运行时直接拿到 Authorization 或刷新链路凭证。

4. **残余风险与可解释性**  
   明确哪些能力已经增强，哪些能力仍属于剩余风险，避免在提交文档中夸大防护效果。

---

## 2. 测试环境

### 2.1 构建环境

- JDK 17
- Android Studio 稳定版
- Android SDK（建议 API 34）
- Gradle Wrapper（仓库自带）

### 2.2 设备环境

- 标准 Android 模拟器（用于 debug 回归）
- 真机或可安装 release APK 的测试设备

### 2.3 工具环境

- `jadx`
- `apktool`
- `strings`
- `mitmproxy` 或 Charles
- 用户安装 CA 证书
- 可选：`JustTrustMe`
- 可选：Frida / Xposed / LSPosed（仅用于证明剩余风险，不作为课程必做项）

---

## 3. 测试前准备

1. 本地创建 `secrets.properties`
2. 配置 `API_BASE_URL`
3. debug 联调如需 fallback，可配置 `AUTH_HEADER_VALUE`
4. release 构建必须配置 `CERT_PINS`
5. 如启用签名校验，配置 `RELEASE_SIGNING_SHA256`

---

## 4. 测试矩阵

| 编号 | 类别 | 目标 | 预期结果 |
|------|------|------|----------|
| S1 | 静态分析 | release APK 中不应存在 `BuildConfig.AUTHORIZATION_TOKEN` | 搜索不到该字段与真实值 |
| S2 | 静态分析 | release APK 中不应打包 `auth_bootstrap.txt` | 解包后不存在该文件 |
| S3 | 静态分析 | 关键 host、路径、认证逻辑不应过于直白 | 允许仍看到部分必要字符串，但长期凭证不应直接可得 |
| N1 | 抓包 | debug 构建可用于本地联调 | debug 可按需要关闭 pinning 或使用 debug fallback |
| N2 | 抓包 | release + 用户 CA + `mitmproxy` | 应失败 |
| N3 | 抓包 | release 未配置 `CERT_PINS` | 应在构建或运行前失败 |
| N4 | 抓包 | 额外 allowlist host 未配置 pin | release 应拒绝继续 |
| R1 | 运行时风险 | root / test-keys / 异常安装来源 | 触发中风险提示或限制 |
| R2 | 运行时风险 | Hook / Frida / Xposed 痕迹 | 触发高风险阻断敏感请求 |
| A1 | 业务链路 | access token 正常使用 | 正常请求成功 |
| A2 | 业务链路 | access token 过期 + refresh 成功 | 自动刷新并单次重放 |
| A3 | 业务链路 | refresh 失败 | 清空会话并提示重新登录 |

---

## 5. 详细测试步骤

### 5.1 静态分析测试

#### S1. 检查 release 中是否仍有长期 token 常量

**步骤：**

1. 构建 release APK
2. 用 `jadx` 打开 APK
3. 搜索：
   - `AUTHORIZATION_TOKEN`
   - 已知真实 token 片段
   - `Authorization`
4. 用 `strings` 对 APK 进行字符串检索

**通过条件：**

- 不出现 `BuildConfig.AUTHORIZATION_TOKEN`
- 不出现真实长期凭证明文

**留档建议：**

- `jadx` 搜索截图
- `strings | rg` 命令输出截图

---

#### S2. 检查 release 中是否仍包含 bootstrap asset

**步骤：**

1. 对 release APK 使用 `apktool d`
2. 查看解包目录中的 `assets/`
3. 搜索 `auth_bootstrap.txt`

**通过条件：**

- release 解包结果中不存在 `auth_bootstrap.txt`

**说明：**

- debug 若存在该文件，属于本地联调 fallback，不计为 release 风险

---

### 5.2 抓包与 MITM 测试

#### N1. debug 联调测试

**步骤：**

1. 安装 debug APK
2. 配置本地调试代理
3. 发起正常 `/auth` → `/generate_image` 流程

**通过条件：**

- debug 在允许的调试配置下可以联调

---

#### N2. release + 用户 CA + `mitmproxy`

**步骤：**

1. 在设备上安装用户 CA
2. 启动 `mitmproxy`
3. 安装 release APK
4. 发起敏感请求

**通过条件：**

- 请求失败，或在客户端表现为证书校验/身份验证失败
- 不应被普通代理成功解密

**证据：**

- 代理端无可读业务明文
- 客户端错误提示截图

---

#### N3. release 未配置 pin

**步骤：**

1. 在本地移除 `CERT_PINS`
2. 执行 release 构建

**通过条件：**

- 构建失败，或运行初始化直接拒绝继续

---

#### N4. allowlist host 缺失 pin

**步骤：**

1. 设置 `ALLOWED_NETWORK_HOSTS`
2. 只为 `API_BASE_URL` 配 pin，不为额外 host 配 pin
3. 执行 release 构建/运行

**通过条件：**

- release 拒绝继续

---

### 5.3 运行时风险测试

#### R1. root / test-keys / 异常安装来源

**步骤：**

1. 在具备 root 或 test-keys 特征的环境中安装应用
2. 或通过侧载形成异常安装来源
3. 发起敏感请求

**通过条件：**

- 中风险环境出现提示
- 高风险环境阻断敏感请求

---

#### R2. Hook / Frida / Xposed 痕迹

**步骤：**

1. 在存在 Frida/Xposed/LSPosed 痕迹的环境中启动应用
2. 发起敏感请求

**通过条件：**

- 客户端或网络层阻断敏感请求

**说明：**

- 这项测试的目的不是证明“完全防住 Hook”
- 而是证明当前实现已具备基础风险识别与处置

---

### 5.4 认证与刷新链路测试

#### A1. 正常认证流程

**步骤：**

1. 启动应用
2. 发起正常生成流程

**通过条件：**

- 正常完成 `/auth` → `/generate_image`

---

#### A2. access token 过期后刷新成功

**前提：**

- 后端已支持 `/session` / `/session/refresh`
- 客户端已获取 refresh token

**通过条件：**

- 401 后自动刷新成功
- 原请求被单次重放
- 不出现无限循环

---

#### A3. refresh 失败

**通过条件：**

- 会话被清空
- 客户端提示重新登录/重新获取凭证
- 不会无限重试

---

## 6. 结果记录模板

### 6.1 记录表

| 编号 | 是否通过 | 证据位置 | 备注 |
|------|----------|----------|------|
| S1 |  |  |  |
| S2 |  |  |  |
| S3 |  |  |  |
| N1 |  |  |  |
| N2 |  |  |  |
| N3 |  |  |  |
| N4 |  |  |  |
| R1 |  |  |  |
| R2 |  |  |  |
| A1 |  |  |  |
| A2 |  |  |  |
| A3 |  |  |  |

---

## 7. 提交时建议写法

建议在最终报告中使用如下表述：

- 当前强化已明显提高静态字符串搜索、普通 MITM 抓包与本地明文存储提取的难度。
- release 不再打包 debug bootstrap 认证文件，并要求对所有 allowlist host 配置证书 pin。
- 敏感请求已增加运行环境风险判断，不再仅依赖 UI 层控制。
- 当前方案仍不能宣称“完全防止”高级运行时 Hook、Frida、Xposed 或专业 unpinning 绕过，相关部分应作为 residual risk 说明。

---

## 8. Residual Risks（需如实说明）

1. 当前 Hook / Frida / Xposed 检测属于基础实现，可被高级攻击者绕过。
2. 若后端 `/session` / `/session/refresh` 未完全落地，客户端仍可能保留 debug fallback 以便联调。
3. `StringFog` 与 R8 的目标是提高静态分析成本，不应表述为“密钥绝对安全”。
4. `CertificatePinner` 能显著增加常规代理抓包难度，但不能等同于完全防 MITM/Hook。

---

## 9. 一句话总结

本测试计划的目的不是证明“应用不可逆向”，而是证明：**当前 Fortify 强化已让攻击者更难从 APK 中直接拿到长期凭证、更难通过普通代理抓包获取敏感流量，并在高风险运行环境下更难继续发起敏感请求。**
