# Repository Structure / 仓库结构说明

This document explains the main directories in the current repository.  
本文件说明当前仓库中主要目录的用途。

## Main Directories / 主要目录

- `app/`  
  Android application source code, resources, manifest, and app-level Gradle config.  
  Android 主工程代码目录，包含源码、资源、清单及 app 级构建配置。

- `gradle/`  
  Gradle wrapper files used by Android Studio/CLI builds.  
  Gradle Wrapper 相关文件，供 Android Studio/命令行构建使用。

- `doc/`  
  Original documentation copied from the legacy project. Keep as source historical docs.  
  从旧项目复制来的原始文档目录，作为历史文档保留。

- `docs/build/`  
  Team build/process documents (for setup, structure, and build notes).  
  团队构建与流程文档目录（构建说明、结构说明、流程备注等）。

- `docs/fortify/`  
  Notes and artifacts for Fortify-related tasks.  
  Fortify 相关记录与产物目录。

- `docs/attack/`  
  Notes and records for attack/safety testing tasks.  
  攻防测试相关记录目录。

- `docs/submission/`  
  Final submission and collaboration policy documents.  
  最终提交说明与团队协作规范文档目录。

- `security/`  
  Security operation guides (for local secret handling and key hygiene).  
  安全操作说明目录（如本地密钥管理规范）。

- `notes/`  
  General team notes, meeting points, and temporary coordination records.  
  团队通用笔记目录（会议记录、协作备忘等）。

## Important Notes / 重要说明

- `app/` is the core Android project and should remain the primary build target.  
  `app/` 是 Android 主构建目标，保持不变。

- `doc/` is preserved from the old project and should not be removed.  
  `doc/` 为旧项目文档副本，应保留不删除。

- `docs/` is the new collaboration document area and complements `doc/`.  
  `docs/` 是新增协作文档区，用于补充 `doc/`。

- Newly added documentation directories are support-only and should not affect Android build behavior.  
  新增目录仅用于文档协作，不应影响 Android 构建行为。
