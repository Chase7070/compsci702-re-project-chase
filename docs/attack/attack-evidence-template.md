# Fortify 攻击验证证据模板

> 用途：配合 `docs/attack/fortify-test-plan.md` 收集截图、命令输出、日志和结论。  
> 适用场景：课程验收、组内复核、最终提交材料整理。  
> 建议：每个测试项单独建一个小节，并统一命名截图与附件，便于助教复现与阅读。

---

## 1. 记录基本信息

### 1.1 测试人

- 姓名：
- 日期：
- 分支 / Commit：
- 构建变体：`debug` / `release`
- 设备型号：
- Android 版本：

### 1.2 构建信息

- APK 文件名：
- APK SHA-256：
- 是否签名：
- `API_BASE_URL` 是否为课程正式地址：
- `CERT_PINS` 是否已配置：
- `RELEASE_SIGNING_SHA256` 是否已配置：

---

## 2. 证据目录建议

建议统一放到本地未提交目录，例如：

```text
evidence/
  static/
  mitm/
  runtime/
  release/
```

建议命名格式：

```text
S1_jadx_search_auth_token.png
S2_apktool_assets_listing.png
N2_mitmproxy_release_fail.png
R2_hook_risk_block.png
```

---

## 3. 单项证据模板

以下模板可重复复制，用于每个测试项。

---

### [测试编号] 标题

**编号：** 例如 `S1` / `N2` / `R2`  
**类别：** 静态分析 / MITM / 运行时风险 / 业务链路  
**目标：** 简述测试目的  
**预期：** 简述预期结果

#### 3.1 测试步骤

1.  
2.  
3.  

#### 3.2 使用工具

- 工具：
- 版本：
- 命令 / 操作：

```bash
# 在此粘贴命令
```

#### 3.3 原始证据

**截图：**

- ``
- ``

**命令输出：**

```text
# 在此粘贴关键输出
```

**日志 / 错误信息：**

```text
# 在此粘贴关键日志
```

#### 3.4 结果判定

- 是否通过：`通过 / 不通过 / 部分通过`
- 判定理由：
- 是否符合测试计划预期：

#### 3.5 风险说明

- 若失败，失败原因是什么：
- 是否影响课程验收：
- 是否需要后续修复：

#### 3.6 备注

-  

---

## 4. 静态分析证据模板

### 4.1 `S1` 检查 release 中是否仍有长期 token 常量

**建议留档：**

- `jadx` 搜索 `AUTHORIZATION_TOKEN`
- 搜索已知 token 片段
- `strings` 搜索结果

**示例填写：**

```text
搜索关键字：AUTHORIZATION_TOKEN
结果：0 命中

搜索关键字：<真实 token 前 8 位>
结果：0 命中
```

---

### 4.2 `S2` 检查 release 中是否仍包含 bootstrap asset

**建议留档：**

- `apktool d` 后的 `assets/` 目录截图
- `rg "auth_bootstrap.txt"` 结果

**示例填写：**

```bash
apktool d app-release.apk -o decoded-release
rg "auth_bootstrap.txt" "decoded-release"
```

```text
结果：未命中
```

---

## 5. MITM / Pinning 证据模板

### 5.1 `N2` release + 用户 CA + `mitmproxy`

**建议留档：**

- 设备已安装用户 CA 的截图
- `mitmproxy` 未成功解密敏感请求的截图
- 客户端证书失败提示截图

**判定建议：**

- 若客户端报证书校验失败、代理侧无可读请求体，可判定通过

---

### 5.2 `N3` release 未配置 `CERT_PINS`

**建议留档：**

- release 构建失败截图
- 终端错误输出

**示例输出：**

```text
Release builds require CERT_PINS in secrets.properties.
```

---

## 6. 运行时风险证据模板

### 6.1 `R1` root / test-keys / 异常安装来源

**建议留档：**

- 风险提示截图
- 敏感请求被阻止的界面截图

---

### 6.2 `R2` Hook / Frida / Xposed 痕迹

**建议留档：**

- 风险触发界面
- 日志中敏感请求被拦截的记录

**说明：**

- 不要求证明“完全防住 Hook”
- 只需证明当前实现可以识别高风险环境并阻止敏感请求

---

## 7. 业务链路证据模板

### 7.1 `A1` 正常流程

**建议留档：**

- 输入 prompt 截图
- 成功生成图片截图
- 保存图片成功提示截图

---

### 7.2 `A2` token 过期后自动刷新

**建议留档：**

- 401 后刷新成功的日志
- 最终请求成功的界面截图

**备注：**

- 仅在后端 `/session` / `/session/refresh` 已可用时执行

---

### 7.3 `A3` refresh 失败

**建议留档：**

- refresh 失败后的提示
- 会话清空后重新登录/重新获取凭证的界面

---

## 8. 汇总结论模板

完成若干测试后，可附上如下结论段：

```text
本次验证结果表明：当前 Fortify 强化已有效降低静态分析直接提取长期凭证、普通用户 CA 型 MITM 抓包以及高风险运行环境下继续发起敏感请求的成功率。与此同时，系统仍存在对高级 Hook/Frida/Xposed 与专业 pin-bypass 技术无法完全抵御的剩余风险，该部分已在提交材料中如实声明。
```

---

## 9. 使用建议

- 截图前先做脱敏
- 不要在文档中贴真实 token 全值
- 命令输出尽量保留关键行即可
- 每项测试至少保留 1 张截图或 1 份命令输出
- 对失败项也保留证据，方便解释 residual risk
