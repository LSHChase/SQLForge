# Phase 0 Plan

## 目标

阶段 0 的目标是在不偏离既定技术栈和分层规则的前提下，完成项目基础设施初始化，使仓库具备文档入口、父工程、公共模块、首个服务模板、前端框架、初始化脚本、知识检查脚本、交付记录和环境提醒能力。

## 任务顺序

1. `Task-001` 初始化仓库根目录文件
2. `Task-002` 初始化文档体系
3. `Task-003` 初始化后端父 POM 与公共模块
4. `Task-004` 初始化平台治理服务骨架
5. `Task-005` 初始化前端框架
6. `Task-006` 初始化数据库脚本
7. `Task-007` 初始化检查脚本
8. `Task-008` Git 提交与标记
9. `Task-009` 环境部署提醒

## 依赖关系

- `Task-001`：无前置依赖。
- `Task-002`：依赖 `Task-001` 提供根目录基础文件。
- `Task-003`：依赖 `Task-001` 与 `Task-002`，因为父 POM 与公共模块需要已存在的仓库规范与文档基线。
- `Task-004`：依赖 `Task-003`，以复用父 POM 与公共模块，并对齐分层模板。
- `Task-005`：依赖 `Task-001` 与 `Task-002`，技术栈与页面规则已在文档中锁定。
- `Task-006`：依赖 `Task-002`，表结构与合规要求以架构和等保文档为准。
- `Task-007`：依赖 `Task-002`，因为检查脚本必须验证文档体系完整性。
- `Task-008`：依赖 `Task-001` 至 `Task-007` 全部完成。
- `Task-009`：依赖 `Task-008` 产出交付 Tag。

## 当前状态

| Task | 状态 | 说明 |
|:---|:---|:---|
| `Task-001` | Completed | 根目录基础文件已初始化并完成关键内容校验 |
| `Task-002` | Completed | 文档体系已入仓，架构总文档、规则库、ADR 实体文件、计划、部署与合规文档已建立 |
| `Task-003` | Completed | 父 POM 与 `sqlforge-common/` 已建立；当前 `sqlforge-common/` 仍需后续补齐源码 |
| `Task-004` | Completed | `governance-service/` 已建立并完成阶段性修复与基础测试 |
| `Task-005` | Completed | 前端框架、路由、主题、国际化与基础页面骨架已建立 |
| `Task-006` | Completed | `sql/init-schema.sql` 与 `sql/init-data.sql` 已建立，并已覆盖本地消息表初始化 |
| `Task-007` | Completed | 仓库知识检查脚本与前后端分离检查脚本已建立并通过验证 |
| `Task-008` | Completed | 历史初始化提交与标签已存在；本轮文档回填另按 repair record 追加闭环 |
| `Task-009` | Completed | 环境部署提醒已写入交付记录与部署文档，独立 MySQL/TDSQL 环境部署要求已显式提醒 |

## 已验证事实

- `node scripts/lint-repository-knowledge.js`：通过
- `node scripts/check-frontend-backend-separation.js`：通过
- `mvn -B test`：通过
- `npm run build`：通过
- `npm run lint`：通过
- `docs/adr/ADR-001` 至 `ADR-013`：已补齐
- `docs/deployments/huawei-cloud-setup.md`：已补齐

## 环境部署提醒

- 本地阶段完成后，必须继续准备独立 MySQL/TDSQL 环境，不得长期依赖本地 Docker MySQL 作为唯一运行环境。
- 生产或仿真环境必须切换到独立 Kafka 集群和 `messaging.mode=KAFKA`，不得沿用本地 `DATABASE` 模式。
- 生产环境必须启用真实鉴权、租户隔离、审计日志保留、敏感配置加密和备份恢复策略。
- 华为云私有云部署以 [huawei-cloud-setup.md](/models/project/codex/SQLForge/docs/deployments/huawei-cloud-setup.md) 为准。

## 验收口径

- 文档结构必须满足 `R-099` 至 `R-104`。
- `docs/architecture/init.md` 必须完整包含 115 条规则、11 个服务、13 个 ADR、4 阶段里程碑、等保规则、加速服务与异常回滚。
- 规则库必须具备 `R-001` 至 `R-115` 的连续索引。
- `docs/references/raw-requirements/` 必须存在；其“保持为空目录”的历史口径与原始资料归档要求存在冲突，见主执行计划 `HC-004`。
- 所有文档均采用 UTF-8 与 LF。
