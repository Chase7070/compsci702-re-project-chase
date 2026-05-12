# Fortify · StringFog 现状总结 · Chase7070 · 2026-05-12

**文件：** `2026-05-12_Chase7070_stringfog-status.md`  
**主题：** 工程内 StringFog 接入现状、R8/ProGuard 与 Gson 配套规则、Retrofit endpoint 迁出注解后的边界说明，以及 Release 静态验证建议（基于事实勘误修订稿）。

---

## 一、针对 StringFog 已经做了哪些

### 1. 已接入 StringFog 插件和运行时库

工程已经完成：

- Gradle StringFog 插件接入  
- StringFog xor runtime 依赖接入  
- `StringFogExtension` 配置  
- R8 / ProGuard keep 规则补充  

作用是：

- 对应用自有包中可被 StringFog 处理的普通字符串字面量进行编译期处理  
- 运行时通过 StringFog 相关逻辑恢复字符串语义  
- 降低 JADX 直接全文搜索明文字面量定位业务代码的成功率  

需要注意的是，当前 StringFog 配置里有：

`debug = false`

因此不能简单写成「Debug 与 Release 都一定经过字符串 fog」。更准确的说法是：

- Release 上 R8 + StringFog 的字面量处理效果是主要验证对象；  
- Debug 是否加密、加密到什么程度，需要以实际 dex / JADX 检查结果为准；  
- 不宜默认 Debug 与 Release 的字符串处理效果完全相同。

---

### 2. fogPackages 已覆盖自有包

当前配置覆盖的是：

`com.example.reverseengineeringbuildphase`

这表示 StringFog 目标主要是你们自己的应用代码包。

它的意义是：

- 优先处理自有业务代码中的字符串  
- 不主动 fog 第三方库  
- 降低对 Retrofit / OkHttp / Gson / AndroidX 等依赖库造成兼容问题的风险  

这个选择是合理的。对课程项目来说，重点不是把所有依赖库也搞复杂，而是提高攻击者通过 JADX 搜你们业务字符串、接口路径、提示文案来快速定位关键逻辑的成本。

---

### 3. 已补充 R8 / ProGuard 兼容规则

工程中已经保留或新增了多类 keep 规则，包括：

- StringFog 生成类 / StringFog runtime 相关 keep  
- Retrofit 注解与接口相关 keep  
- Gson 注解相关 keep  
- Gson TypeToken 相关 keep  
- `data.model` 包下 model 类 keep  

其中：

```proguard
-keep class com.example.reverseengineeringbuildphase.data.model.** { *; }
```

以及 Gson `TypeToken` 相关 keep，是为了避免 Release 下 R8 过度混淆或裁剪后，Gson 反序列化出现类型丢失问题，例如反序列化成 `LinkedTreeMap` 后再发生 `ClassCastException`。

这部分和 StringFog 本身不是同一个功能，但它们共同服务于一个目标：**Release 开启 R8 / StringFog 后，仍保证 Retrofit / Gson / model 反序列化流程稳定。**

更细的背景见同目录：`release-gson-r8-auth-classcast.md`。

---

### 4. 已明确 StringFog 的边界

当前工程中的 StringFog 主要用于：

- 降低静态字符串搜索命中率  
- 提高 JADX 阅读和定位关键代码的成本  
- 减少部分业务字符串以明文常量形式直接暴露  

但它不等于真正意义上的 token 安全。

需要保留这个边界说明：

- StringFog 不防运行时抓包  
- 不防 Frida hook  
- 不防内存中读取运行时明文  
- 不替代服务端鉴权  
- 不保证 API key / token 在客户端绝对安全  

更准确地说，它是课程 Fortify 语境下的静态分析加固措施，而不是完整的密钥保护方案。

---

## 二、这次 Retrofit endpoint 相关改动做了哪些

### 5. 已将 Retrofit endpoint 从注解路径迁出

原来的 Retrofit 接口形式可能是：

```kotlin
@POST("auth")
@POST("generate_image")
```

这类字符串属于 Retrofit 注解参数，通常不适合依赖 StringFog 处理。在 JADX 中，它们可能作为注解元数据直接可见，攻击者可以通过搜索 `auth`、`generate_image` 快速定位 `ApiService` 和相关调用链。

现在已经改成：

```kotlin
@POST
suspend fun xxx(@Url path: String, ...)
```

路径由 `EndpointProvider` 在运行时提供，例如：

```kotlin
"a" + "u" + "t" + "h"
"generate" + "_" + "image"
```

这一步的意义是：

- endpoint 不再直接写在 Retrofit 注解参数中  
- 路径从 Retrofit 注解元数据迁移到自有代码方法体  
- 运行时 path 拼接逻辑位于自有包中  
- 让 StringFog / R8 有机会处理这些方法体中的字符串片段  
- 降低通过 JADX 直接搜索 Retrofit 注解路径定位 API 方法的成功率  

---

### 6. 但完整 endpoint 不一定从 APK 中完全消失

需要特别说明：这次改动解决的是 **Retrofit 注解元数据中的 endpoint 明文路径**，不是保证整个 APK 中完全没有 endpoint 相关字符串。

根据当前工程事实，`MainViewModel` 和 `ImageRepository` 中仍然存在包含路径片段的 UI / 错误文案字面量，例如 `/auth`、`/generate_image`。这些可能出现在：状态提示、错误信息、prefix 文案、异常信息、debug / UI 展示文本。

所以总结中不能写「完整 endpoint 已经一定从 APK 中消失」。

更准确的说法应该是：

- endpoint 已经从 Retrofit 注解路径中迁出；  
- 但由于业务层 UI / 错误文案中仍包含 `/auth`、`/generate_image` 等字符串，JADX / strings 仍可能通过这些业务字符串命中 endpoint 名称。  

后续验证时不能只搜 `generate_image`，还应该同时搜 `/generate_image`、`/auth`、`auth`，并结合命中位置判断是：Retrofit 注解暴露、`EndpointProvider` 拼接片段、业务 UI / 错误文案、第三方库字符串、Gson 协议字段。

---

## 三、目前没有做的内容

以下判断仍然成立：本次 Retrofit + StringFog 相关迭代没有做这些改动。

- 没有迁出 BuildConfig  
- 没有改变 `API_BASE_URL` 注入方式  
- 没有改变 `AUTHORIZATION_TOKEN` 注入方式  
- 没有改变 `secrets.properties` 机制  
- 没有改变 `buildConfigField` 注入逻辑  
- 没有修改 Gson 协议字段  
- 没有修改请求体 model  
- 没有修改 response model  
- 没有改动服务器 API 协议  
- 没有引入 native/JNI 保存敏感逻辑  
- 没有引入自建 proxy server  
- 没有通过服务端中转隐藏课程 server  

这些「不做」是合理的：课程要求 App 直接访问课程 server；当前重点是降低静态分析成本；不应为了隐藏字符串破坏 API 协议或扩大实现风险。

---

## 四、建议 JADX / strings 验证后再决定要不要改的内容

下面这些内容不建议现在盲目继续改，应在 Release APK 经过 JADX / strings 验证后，再根据实际命中情况决定。

### 1. BuildConfig 中 URL / token 是否需要迁出

当前工程没有修改 BuildConfig 和 secrets 注入方式；这一步建议先保持现状。

需要通过 release APK 检查：真实 API token、base URL、server domain 是否仍以明文出现；BuildConfig 字段是否被 R8 内联到调用处；StringFog 是否实际处理了相关字符串。

若验证显示真实 token / URL 没有在自有代码中明文出现，则不建议迁出 BuildConfig。若仍明显可见，再考虑后续单独迭代（SecretsProvider、分片拼接等）。这属于「密钥注入方式调整」，不应和本次 Retrofit endpoint 调整混在一起。

### 2. Authorization / Bearer 是否还需要拆分

后续应验证：`Authorization`、`Bearer`、真实 header value、真实 token。若命中在自有代码且构成有意义的业务暴露，可考虑后续拆分或放入 provider。若命中来自 `okhttp3.*`、`retrofit2.*` 等第三方依赖，则不需要处理。

### 3. Base URL / server domain 是否还需要拆分

谨慎区分「业务代码中不必要的明文暴露」与 `networkSecurityConfig`、host allowlist、Manifest 等必须可读的域名配置。

### 4. `/auth`、`/generate_image` 业务文案是否要收敛

若验证后仍介意静态命中，可考虑将用户可见或日志中的文案改为更泛化表述；这属于 UI / 错误文案收敛，与 Retrofit 接口改造分开记录。

### 5. Gson `@SerializedName` 不建议为 StringFog 强行修改

协议字段应保留；报告中可写：JSON field names are protocol-level strings and are intentionally preserved for API compatibility.

### 6. 第三方库中的通用字符串不用处理

报告中可写：Remaining generic HTTP strings from third-party libraries are expected and outside the scope of StringFog.

---

## 五、建议现在的验证清单

改完后建议验证 **Release**，而不是只看 Debug。

### 1. 构建 release APK

```bash
./gradlew clean
./gradlew :app:assembleRelease
```

确认：Release 构建成功；R8 正常执行；StringFog transform 正常执行；APK 可安装（若使用未签名产物，需按课程环境选择签名或安装方式）。

### 2. 功能回归

完整走通：prompt 输入、`/auth` 请求、`/generate_image` 请求、图片预览、保存相册、错误处理不崩溃。因 Retrofit 已改为 `@POST` + `@Url`，重点确认：`baseUrl` 以 `/` 结尾；传入的 `path` 不以 `/` 开头；无双斜杠；路径与 host allowlist 一致。

### 3. strings 检查建议

不要只查一个词。建议至少查（多 dex 时需遍历 `classes*.dex`）：

```bash
unzip -p app/build/outputs/apk/release/<release-apk-name>.apk classes.dex | strings | grep -i "generate_image"
unzip -p app/build/outputs/apk/release/<release-apk-name>.apk classes.dex | strings | grep -i "/generate_image"
unzip -p app/build/outputs/apk/release/<release-apk-name>.apk classes.dex | strings | grep -i "/auth"
unzip -p app/build/outputs/apk/release/<release-apk-name>.apk classes.dex | strings | grep -i "authorization"
unzip -p app/build/outputs/apk/release/<release-apk-name>.apk classes.dex | strings | grep -i "bearer"
```

### 4. JADX 检查重点

重点看 `com.example.reverseengineeringbuildphase.*`，并对命中分类：注解、`EndpointProvider`、ViewModel/Repository 文案、Gson model、第三方库、资源等。

---

## 六、可以写进开发日志的总结

本次 StringFog 相关加固分为两层：

**第一层**为 StringFog 基础接入。工程通过 Gradle 插件、xor runtime、`fogPackages`、R8/ProGuard keep，使应用自有包中可处理的普通字符串字面量在编译期经过 StringFog 处理，运行时再恢复字符串语义。该措施主要用于降低 JADX 直接全文搜索字符串定位业务逻辑的成功率。由于当前 StringFog 配置包含 `debug = false`，后续静态效果验证应以 Release APK 为主要对象；Debug 包是否具有同等字符串处理效果需以实际 dex / JADX 检查为准。

**第二层**为 Retrofit endpoint 注解路径收敛。由于 `@POST("auth")` 与 `@POST("generate_image")` 属于 Retrofit 注解参数，通常不应假定其会被 StringFog 处理，因此本次已将 Retrofit 接口改为 `@POST` + `@Url`，并由 `EndpointProvider` 在运行时通过分片方式拼接 `auth` 与 `generate_image`。该改动将 endpoint path 从 Retrofit 注解元数据中迁出，降低攻击者通过 JADX 直接搜索注解路径定位 API 方法的效率。

需要注意的是，本次改动不保证完整 endpoint 字符串从 APK 中完全消失。当前 `MainViewModel` 与 `ImageRepository` 中仍可能存在包含 `/auth`、`/generate_image` 的 UI 状态文案、prefix 或异常信息。因此后续 JADX / strings 验证时应同时搜索 `generate_image`、`/generate_image`、`/auth` 等关键词，并结合命中位置判断是否需要进一步收敛业务文案。

本次未修改 BuildConfig、`secrets.properties`、`API_BASE_URL`、`AUTHORIZATION_TOKEN`、OkHttpClient、Retrofit 初始化、Gson 协议字段、请求/响应 model 或服务器协议。BuildConfig 中 URL/token 是否仍需迁移，将在后续 Release APK 的 JADX / strings 验证后再决定。若真实 token、base URL 或 Authorization header value 仍在自有代码中明文出现，再考虑迁移到 SecretsProvider / ConfigProvider 并采用分片拼接；若仅为第三方库、Gson 协议字段或必要配置文件命中，则不再处理。

---

## 七、一句话总结

目前已经完成的是：StringFog 基础接入；自有包 `fogPackages` 配置；R8 / ProGuard / Gson / TypeToken / model keep；Retrofit endpoint 从 `@POST("path")` 迁移到 `@POST` + `@Url`；`EndpointProvider` 运行时分片拼接 `auth` 和 `generate_image`。

后续建议通过 Release APK 的 JADX / strings 验证后再决定是否继续处理：BuildConfig 中 URL / token；Authorization / Bearer；base URL / server domain；`MainViewModel` / `ImageRepository` 中包含 `/auth`、`/generate_image` 的业务文案。

不建议为了 StringFog 盲目修改：Gson `@SerializedName`；请求/响应 model；第三方库字符串；课程 server API 协议；`secrets.properties` 机制。
