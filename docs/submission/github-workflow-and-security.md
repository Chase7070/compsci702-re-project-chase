# GitHub Workflow and Security / GitHub 工作流与安全规范

This document is for team collaboration in this course project.  
本文件用于本课程项目的团队协作规范。

## A. Repository Visibility / 仓库可见性

- Repository should be **private**.
- Why private is safer: course projects may contain unfinished code, local test configurations, and security-sensitive implementation details.
- Private visibility reduces accidental leakage and lowers the risk of key/config exposure.

- 仓库应设置为**私有**。
- 私有仓库更安全：课程项目通常包含未完成代码、本地测试配置和安全相关实现细节。
- 私有可减少误公开风险，降低密钥或配置泄露概率。

## B. Branch Strategy / 分支策略

- `main`: stable branch for validated code.
- `develop`: integration branch for team testing.
- `feature/*`: personal or task-specific development branches.
- Do not work directly on `main`.
- Prefer pull requests for merging.

- `main`：稳定分支，仅放已验证内容。
- `develop`：集成分支，用于团队联调。
- `feature/*`：个人或任务分支。
- 不要直接在 `main` 开发。
- 合并优先使用 Pull Request。

## C. Sensitive Information Handling / 敏感信息处理

- Do not commit real API keys.
- Do not commit authorization headers.
- Do not commit `local.properties`.
- Do not commit `secrets.properties`.
- Keep local secrets only on local machines.
- Use `secrets.example.properties` as a placeholder template only.

- 不要提交真实 API key。
- 不要提交 Authorization Header。
- 不要提交 `local.properties`。
- 不要提交 `secrets.properties`。
- 本地密钥仅保存在本机。
- `secrets.example.properties` 仅作为占位模板。

## D. GitHub Settings Already Applied or Recommended / 已应用或建议的 GitHub 设置

Already applied or recommended (verify in repository settings):

- Branch protection for `main`.
- Branch protection for `develop`.
- Pull request review before merge.
- Avoid direct pushes when possible.
- Maintainer/admin should handle merge carefully.

已应用或建议（请在仓库设置中确认）：

- 对 `main` 启用分支保护。
- 对 `develop` 启用分支保护。
- 合并前执行 PR Review。
- 尽量避免直接 push。
- 维护者/管理员谨慎执行合并。

## E. Team Collaboration Rules / 团队协作规则

- Use clear and specific commit messages.
- Open PRs from `feature/*` branches to `develop`.
- Review changes before merge.
- Avoid sharing keys in commits, PR comments, docs, screenshots, or chat.

- 使用清晰具体的提交信息。
- 从 `feature/*` 向 `develop` 发起 PR。
- 合并前必须审查代码。
- 不要在提交、PR 评论、文档、截图或聊天中分享密钥。

## F. Submission Reminders / 提交提醒

- Final APK must not use a test key.
- App must communicate directly with the course server.
- Include source code + APK + build document in submission package.
- Declare any open-source obfuscators used.
- Do not use commercial obfuscators.

- 最终 APK 不能使用测试 key。
- App 必须直接与课程服务器通信。
- 提交内容应包含源码 + APK + 构建说明文档。
- 如使用开源混淆器，需要声明。
- 不可使用商业混淆器。

## G. Current Local Configuration Reminder / 当前本地配置提醒

- The project currently loads local auth config from `secrets.properties`.
- `secrets.properties` must stay local and be ignored by git.
- `secrets.example.properties` is template-only and safe for version tracking.

- 当前项目从 `secrets.properties` 读取本地认证配置。
- `secrets.properties` 必须仅保留本地并被 git 忽略。
- `secrets.example.properties` 仅为模板文件，可用于版本跟踪。
