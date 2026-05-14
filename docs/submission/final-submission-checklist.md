# Final Submission Checklist

> 用途：最终提交前逐项确认源码、APK、文档、安全声明与证据是否齐全。  
> 建议：封板前由成员 D 主持逐项勾选，其他成员负责各自部分确认。

---

## 1. 仓库与分支状态

- [ ] 当前提交基于预定提交分支（如 `develop` 或封板分支）
- [ ] `main` / `develop` 不包含真实 `secrets.properties`
- [ ] 未提交真实 token、Authorization header、签名文件
- [ ] 变更已 Review 或至少由组内他人复核
- [ ] 当前提交点可稳定构建

---

## 2. 构建前检查

- [ ] 本地 `secrets.properties` 已正确配置
- [ ] `API_BASE_URL` 指向课程服务器
- [ ] release 构建已配置 `CERT_PINS`
- [ ] 如启用签名校验，`RELEASE_SIGNING_SHA256` 已正确填写
- [ ] Android SDK / JDK / Gradle 环境正常

---

## 3. APK 构建结果

### Debug

- [ ] `./gradlew :app:assembleDebug` 成功
- [ ] debug APK 可正常安装
- [ ] debug 可用于基本联调

### Release

- [ ] `./gradlew :app:assembleRelease` 成功
- [ ] release APK 已签名或已按提交要求完成签名
- [ ] release APK 可正常安装
- [ ] release 主流程可运行

---

## 4. Fortify 功能检查

- [ ] Release 启用 R8 / ProGuard
- [ ] Release 不输出敏感网络日志
- [ ] `usesCleartextTraffic=false`
- [ ] `Network Security Config` 生效
- [ ] Host allowlist 生效
- [ ] 图片 URL 仅允许 HTTPS + allowlist host
- [ ] Release 已配置证书 pin
- [ ] 高风险环境下敏感请求会被阻断或限制
- [ ] 本地 token 不以普通明文 SharedPreferences 保存

---

## 5. 静态分析检查

- [ ] `jadx` 中未直接出现 `BuildConfig.AUTHORIZATION_TOKEN`
- [ ] release APK 中未打包 `auth_bootstrap.txt`
- [ ] `strings` 未直接暴露真实长期凭证
- [ ] 关键字符串暴露已降至可接受水平

---

## 6. 抓包与证书检查

- [ ] debug 构建在本地联调策略下可使用代理（如需要）
- [ ] release + 用户 CA 无法被普通 MITM 成功抓取
- [ ] release 未配置 `CERT_PINS` 时会失败
- [ ] 额外 allowlist host 已全部配置 pin
- [ ] pin 失败时客户端提示不泄露内部实现细节

---

## 7. 运行时风险检查

- [ ] root / test-keys / 异常安装来源可触发风险提示
- [ ] Hook / Frida / Xposed 痕迹可触发高风险阻断
- [ ] 签名不一致时敏感请求被限制
- [ ] 风险分级行为与文档描述一致

---

## 8. 认证与业务链路检查

- [ ] 正常 `/auth` → `/generate_image` 主流程成功
- [ ] release 认证链路可正常获取 Authorization
- [ ] 若后端支持 `/session` / `/session/refresh`，自动刷新路径验证通过
- [ ] refresh 失败不会无限重试
- [ ] 会话失效时会触发清理与重新登录/重新获取凭证流程

---

## 9. 文档检查

- [ ] `docs/build/install-and-run.md` 已与当前实现一致
- [ ] `docs/fortify/2026-05-13_session-pinning-keystore.md` 已反映当前状态
- [ ] `docs/attack/fortify-test-plan.md` 已完成或部分完成填写
- [ ] `docs/attack/attack-evidence-template.md` 已用于留档
- [ ] `docs/submission/build-and-security-declaration.md` 已检查内容准确
- [ ] `README.md` 中的文档索引可用

---

## 10. 证据材料检查

- [ ] 静态分析截图已保存
- [ ] MITM / pinning 测试截图已保存
- [ ] 运行时风险测试截图或日志已保存
- [ ] 业务主流程成功截图已保存
- [ ] 所有截图已脱敏
- [ ] 所有日志已去除真实密钥

---

## 11. 合规声明检查

- [ ] 未使用商业混淆器
- [ ] 已声明使用 Android R8 / ProGuard
- [ ] 已声明使用 StringFog（如适用）
- [ ] 已说明当前措施是“提高门槛”而非“绝对不可逆向”
- [ ] Residual risks 已如实说明

---

## 12. 最终提交包检查

- [ ] 源码已整理
- [ ] APK 已准备
- [ ] 构建说明文档已准备
- [ ] 安全声明文档已准备
- [ ] 测试与证据材料已准备
- [ ] 提交材料文件名清晰
- [ ] 最终压缩包或提交目录结构已核对

---

## 13. 封板确认

- [ ] 成员 A 已确认客户端网络与 token 链路
- [ ] 成员 B 已确认后端契约 / 风控说明
- [ ] 成员 C 已确认平台安全与风险检测说明
- [ ] 成员 D 已确认测试、证据与提交文档
- [ ] 组内已完成最终交叉检查

---

## 14. 最终一句话确认

在提交前，请确认下面这句话成立：

> 当前提交版本可稳定构建，release APK 可运行，核心 Fortify 强化点已有实现与文档支撑，测试证据已留档，且未提交任何真实密钥。
