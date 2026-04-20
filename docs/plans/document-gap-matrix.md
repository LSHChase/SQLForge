# SQLForge Document Gap Matrix

## Summary

本文件把本轮文档治理中已经识别的冲突、漂移和缺失项显式矩阵化，避免它们只散落在主计划、真值基线和复盘中。

适用范围：

- 文档治理专项复验
- 后续批次接手前的闭口检查
- 判断“哪些缺口已经显式收口，哪些仍待实现”

## 1. Conflict Matrix

| ID | Conflict topic | Historical statement | Current repository fact | Closure artifact | Status |
|:---|:---|:---|:---|:---|:---|
| `CFG-001` | 初始化目标落点路径与当前文档路径不一致 | 初始化文档内含多个规划落点路径 | 当前仓库以 `docs/rules/`、`docs/deployments/`、`docs/plans/` 等为真实入口 | `docs/plans/document-truth-baseline.md` 漂移映射 | Closed |
| `CFG-002` | 初始化基线与追加规则消费边界不够显式 | `R-001` 至 `R-115` 来源于初始化文档，后续规则分散追加 | 当前规则库已成为统一规则入口 | `docs/rules/codex-rules.md` 当前消费说明 | Closed |
| `CFG-003` | 历史计划与当前事实容易混写 | `phase-0-plan.md`、初始化文档保留历史目标和阶段状态 | 当前仓库事实应由代码、结构、验证日志证明 | `docs/plans/document-truth-baseline.md` + `docs/plans/master-execution-plan.md` | Closed |
| `CFG-004` | 治理批次证据散落导致批次一致性不清 | 验证日志、复盘、任务归档和 git closeout 并非同一处可见 | 当前已形成任务归档 + 复盘 + 验证日志 + git 提交链路 | `tasks-done.md`、治理复盘、git history | Closed |

## 2. Drift Matrix

以下漂移项已被识别并重新固定消费方式：

| ID | Drift topic | Primary closure artifact | Current status |
|:---|:---|:---|:---|
| `DRF-001` | 初始化文档中的规划落点与当前实际文档落点漂移 | `docs/plans/document-truth-baseline.md` | Closed |
| `DRF-002` | 主阅读顺序未纳入新增治理文档 | `docs/README.md`, `docs/plans/README.md` | Closed |
| `DRF-003` | 主计划证据集与主追踪矩阵未纳入新增治理文档 | `docs/plans/master-execution-plan.md` | Closed |
| `DRF-004` | coverage matrix 与实际 `docs/` 文件可能失配 | `docs/plans/document-coverage-matrix.md` + `scripts/lint-repository-knowledge.js` | Closed |
| `DRF-005` | 本轮治理只留模板未留实例复盘 | `docs/plans/document-governance-retrospective-2026-04-20.md` + `docs/plans/document-governance-repair-retrospective-2026-04-20.md` | Closed |

## 3. Missing Matrix

| ID | Missing item from strict audit | Before closure | Closure artifact | Status |
|:---|:---|:---|:---|:---|
| `MIS-001` | 显式冲突矩阵 / 缺失矩阵 | 仅分散在主计划、真值基线和复盘中 | 本文件 | Closed |
| `MIS-002` | 显式实现前置条件矩阵 | 仅由 `Phase Overview`、`Traceability Matrix`、`implementation-readiness` 间接表达 | `docs/plans/phase-prerequisite-matrix.md` | Closed |
| `MIS-003` | 接口级统一契约表 | 身份/租户/审计字段已有，但错误码归属和服务间 DTO/事件边界未显式成表 | `docs/architecture/service-interface-contract-baseline.md` | Closed |
| `MIS-004` | 逐 Task 的人工确认点 / 数据影响 / 回滚字段 | 仅有核心 10 字段，扩展治理字段未单独成表 | `docs/plans/task-governance-extension-matrix.md` | Closed |
| `MIS-005` | `DOC-GOV-001` 任务归档 | 验证日志已有，任务归档缺失 | `tasks-done.md` | Closed |
| `MIS-006` | `DOC-GOV-002` 任务归档与 repair 复盘 | repair 验证日志存在，但归档和复盘缺失 | `tasks-done.md` + `docs/plans/document-governance-repair-retrospective-2026-04-20.md` | Closed |
| `MIS-007` | 治理批次 git closeout | 治理内容未形成任务级 commit 记录 | `docs(governance): DOC-GOV-001 ...` 与 `docs(governance): DOC-GOV-002 ...` 提交链 | Closed |

## 4. Residual Implementation Gaps

以下缺口不是文档治理未闭口，而是已被显式记录的后续实现缺口：

| ID | Topic | Current status | Execution owner |
|:---|:---|:---|:---|
| `IMP-002` | 查询执行服务独立模块 | Pending | `Phase-D` |
| `IMP-003` | SQL 优化服务独立模块 | Pending | `Phase-D` |
| `IMP-004` | 压测引擎服务独立模块 | Pending | `Phase-D` |
| `IMP-005` | 访问控制完整代码化落地 | Partial | `Phase-C` / `Phase-D` |
| `IMP-006` | 真实 Kafka 集群运行验证与环境安全参数落地 | Partial | `Phase-C` / `Phase-F` |
| `IMP-007` | 治理扩展点从契约骨架演进为完整能力 | Partial | `Phase-C` / `Phase-D` |

## Related Documents

- `docs/plans/document-truth-baseline.md`
- `docs/plans/phase-prerequisite-matrix.md`
- `docs/plans/task-governance-extension-matrix.md`
- `docs/architecture/service-interface-contract-baseline.md`
- `docs/plans/document-governance-retrospective-2026-04-20.md`
