# 2026-05-19 攻击结果和加强建议

> **分析对象：** 已签名的 Android release APK（`app-release_L.apk`）  
> **分析目的：** 评估攻击者能否从 APK 中获取访问课程 AI server 所需的凭证  
> **相关文档：** [攻击自测路线](2026-05-19_attack-self-test-roadmap.md) · [Fortify 测试计划](fortify-test-plan.md)

---

## 1. 攻击目标

本次分析对象为 **Android release APK**。攻击者假设目标是：在 APK 内找到可用于访问课程 AI server 的 **API key**、**Authorization token** 或 **secret token**，并据此构造合法 HTTP 请求。

具体而言，需要判断：

- 是否存在可直接复用的长期 **Authorization** 明文；
- 是否存在可换取会话的 **bootstrap_token** 或其它启动凭证；
- 静态分析能否还原 **Base URL**、请求路径与 header 注入逻辑。

---

## 2. 工具与分析路线

| 工具 | 用途 |
|------|------|
| **JADX** | 反编译 Java/Kotlin 逻辑；定位 **StringFog** 解密调用；搜索 token 与网络请求（Retrofit / OkHttp）相关代码 |
| **apktool** | 解包查看 **AndroidManifest.xml**、**resources**、**network_security_config**、**smali**；确认是否存在 **native `.so`** |

**结构结论：** 本次 release APK **不包含** `lib/*.so`，属于典型 **DEX-only** 应用。因此 **Ghidra**（面向 native / JNI）**不是优先工具**；分析重心在 jadx 与 apktool。

**推荐顺序：** JADX（静态逻辑）→ apktool（配置与 smali 交叉验证）→ 按需 MITM / Frida（本次报告以静态结果为主）。

---

## 3. 攻击结果

### 3.1 总体结论

- **未**在 APK 中直接找到运行时使用的 **Authorization access_token** 明文（该值应在会话建立后由服务端下发，并写入本地加密存储）。
- **已**通过 JADX 静态分析还原硬编码的 **bootstrap_token**。
- 该 **bootstrap_token** 作为参数传入 `SessionCreateRequest(bootstrap_token=...)`，用于向 server 换取 **access_token** / **refresh_token**。
- 若课程 server 仍接受该 bootstrap 凭证并完成 session 交换，则可认为：**本次针对 bootstrap 链路的静态破解成功**；攻击者无需先抓包即可获得换取会话的入口凭证。

### 3.2 提取到的 bootstrap_token

```
bootstrap_token: [在此填入实际 token，或引用组内/既有攻击报告中的取值]
```

> 说明：access_token / refresh_token 的运行时值位于 **EncryptedSharedPreferences** 等本地存储中，需动态分析或已成功换 session 后才能获得；静态阶段的重点是 bootstrap 入口。

---

## 4. 关键证据（JADX 路径）

以下路径为 JADX 反编译后的混淆类名；行号供复核时定位。

| 文件 | 行号 | 说明 |
|------|------|------|
| `jadx/sources/f0/a.java` | 42 | 解出 **Base URL**：`https://ai.elliottwen.info/` |
| `jadx/sources/f0/a.java` | 79 | 解出 **bootstrap_token** |
| `jadx/sources/g0/e.java` | 161 | 构造 **SessionCreateRequest** |
| `jadx/sources/f0/d.java` | 92 | 向请求注入 **Authorization** header |
| `jadx/sources/h0/b.java` | 28 | access token 本地存储 key：`authorization_header_value` |
| `jadx/sources/h0/b.java` | 29 | refresh token 本地存储 key：`refresh_token` |

**逻辑链（简述）：**

1. 静态还原 bootstrap →  
2. `SessionCreateRequest` 换 session →  
3. 服务端返回 access / refresh →  
4. 写入加密偏好存储 →  
5. 后续请求经拦截器注入 **Authorization** header。

---

## 5. StringFog 破解思路

### 5.1 问题本质

当前 **StringFog（XOR 模式）** 将字符串编译为：

- 密文 `byte[]`（cipher）
- 密钥 `byte[]`（key）
- 固定 **XOR 解密** 例程

三者均存在于 APK 内（DEX / smali），攻击者无需猜测算法，只需按 APK 内逻辑复现即可得到明文。因此 **不能** 将简单 XOR 视为对 API key 的有效保护。

### 5.2 解密公式

与 APK 内解密逻辑一致：

```text
plain[i] = cipher[i] ^ key[i % key.length]
```

其中 `i` 为字节下标，`key.length` 为密钥长度。

### 5.3 说明

此为 **按内置逻辑还原（recovery）**，**不是** 暴力破解（brute force）。在 jadx 中定位 StringFog 调用点后，对目标常量逐条套用上述公式即可得到 Base URL、bootstrap_token 等字符串。

apktool 解包后的 **smali** 中同样可见 cipher、key 与 XOR 循环，还原思路与 JADX 相同，但可读性更差。

---

## 6. apktool 结果

| 检查项 | 结果 |
|--------|------|
| 明文 API key / token | **未发现** 新的明文 key（与 JADX 结论一致：敏感串经 StringFog 处理） |
| `resources` / `assets` | **无** 明文 api-key；release 中 **无** `auth_bootstrap.txt` 类明文 asset |
| `AndroidManifest.xml` | `usesCleartextTraffic=false` |
| `network_security_config` | 禁止 **cleartext traffic**（禁止明文 HTTP） |
| `lib/*.so` | **不存在** native 库 |
| smali 中的 StringFog | 可按与 JADX 相同的 **XOR** 思路还原，难度高于 Java 反编译视图 |

---

## 7. 加强建议（合规、免费/开源或 Android 内置）

以下建议符合课程作业约束：不引入商业混淆器、不要求 app 经第三方 server 中转、不依赖无法控制的课程 server 改造。

| 措施 | 说明 |
|------|------|
| **R8 / ProGuard** | 混淆类名、方法名；压缩与删除无用代码；提高静态阅读成本 |
| **dProtect** | 免费开源；需在 **build document** 中声明；可做字符串加密、常量混淆、控制流混淆 |
| **替换或弱化 StringFog** | 避免用 **简单 XOR** 保护 API key / bootstrap；UI 普通字符串可继续混淆 |
| **自定义字符串加密** | 例如 **AES**、key 分段、运行时组合；避免所有解密逻辑集中在单一 `StringFog.decrypt` 入口 |
| **签名校验 / 完整性校验** | 检测 APK 是否被 **重打包（repackage）** 或 **重签名（re-sign）** |
| **debug / root / Frida / Xposed 检测** | 提高动态分析与环境篡改成本（与现有 `IntegrityChecker` 方向一致） |
| **Certificate Pinning（证书固定）** | 继续 **直连** 课程 server，不使用第三方代理 server；可降低常规 MITM 成功率 |

建议组合：**R8/ProGuard + dProtect + 自定义加密 + 签名/环境检测 + Pinning**，分层提高静态与动态攻击成本。

---

## 8. 不建议或不能使用

| 项 | 原因 |
|----|------|
| **商业混淆器** | 作业明确禁止 |
| **第三方中转 server** | 作业要求 app **必须直接** 与课程 server 通信 |
| **Play Integrity** 等 | 可能产生课程 server **以外** 的网络通信，不符合约束 |
| **服务端限流 / 吊销 / 监控** | 团队 **无法控制** 课程 server，不宜作为客户端 Fortify 的实现方案写入本报告 |

---

## 9. StringFog 是否保留

**结论：**

- **可以保留** StringFog 用于普通 **UI 字符串** 混淆，降低无关字符串的静态搜索命中率。
- **不应再** 将 StringFog（尤其 XOR 模式）作为 **API key / bootstrap_token** 的核心保护手段。

对 API key / bootstrap 类敏感数据，应采用：

**R8/ProGuard + dProtect + 自定义加密 + 签名/环境检测 + Certificate Pinning**

的组合方案；并从构建与密钥管理上避免将可换 session 的 bootstrap 以可静态完整还原的形式长期留在 DEX 中（若业务允许，应推动服务端轮换或缩短 bootstrap 生命周期——该点仅作风险说明，不依赖课程 server 改造实现）。

---

## 10. 文档信息

| 项目 | 内容 |
|------|------|
| 日期 | 2026-05-19 |
| 分析类型 | 静态分析（JADX + apktool） |
| Ghidra | 未作为优先工具（无 native `.so`） |
