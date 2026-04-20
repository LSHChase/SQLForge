# SQLForge Task Ledger

本文件是当前待办与执行中任务的唯一轻量运行台账。

- 状态只允许：`todo`、`in_progress`、`in_review`、`blocked`
- `done` 任务必须移入 `tasks-done.md`
- 任务台账不替代 `docs/plans/master-execution-plan.md`

## Todo

_No tasks._

## In Progress

_No tasks._

## In Review

### C-TASK-002: 建立 common 包结构

- Status: in_review
- Priority: 1
- Depends on: C-TASK-001
- Scope: 按“领域目录 + 分层子目录”与公共层边界建立 `sqlforge-common` 的目标包结构，不引入服务专属逻辑。
- Validation:
  - 目录结构符合 `R-021`、`R-022`、`R-067`
  - `mvn -B clean compile`
  - 包结构回归检查
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` after shared-capability inventory.
  - 2026-04-20: strict ledger reconciliation marked the task as partial; `sqlforge-common` 已形成 `async`、`audit`、`config`、`constants`、`context`、`exception`、`log`、`utils` 包结构，且当前验证链 `mvn clean compile`、`mvn test`、`mvn validate pmd:pmd checkstyle:check`、`node scripts/lint-repository-knowledge.js` 已通过，但单任务 git closeout 仍缺失，暂不归档。

### C-TASK-003: 迁移重复或散落能力

- Status: in_review
- Priority: 1
- Depends on: C-TASK-002
- Scope: 把共性能力迁移到 common，仅迁移共性能力，不破坏服务边界。
- Validation:
  - `mvn -B clean compile`
  - 消费方通过
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` after common package structure settled.
  - 2026-04-20: strict ledger reconciliation marked the task as partial; 旧 `com.company.common` 与治理服务内部重复 common 能力已被当前工作树迁移到 `com.company.sqlforge.common` 并由 `governance-service` 消费，当前验证链 `mvn clean compile`、`mvn test`、`mvn validate pmd:pmd checkstyle:check`、`node scripts/lint-repository-knowledge.js` 已通过，但单任务 git closeout 仍缺失，暂不归档。

### C-TASK-007: 对齐现有分层

- Status: in_review
- Priority: 2
- Depends on: none
- Scope: 对齐 `governance-service` 当前 controller/application/domain/infrastructure 分层，确保其继续向公共管理服务边界收敛。
- Validation:
  - `R-120` 分层结构回归
  - `mvn -B clean compile`
  - `mvn -B test`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` as the first governance-service hardening task.
  - 2026-04-20: strict ledger reconciliation marked the task as partial; 当前工作树已形成 `controller` / `application` / `domain` / `infrastructure` 分层、严格请求上下文校验与治理内部契约基线，并通过 `mvn clean compile`、`mvn test`、`mvn validate pmd:pmd checkstyle:check`、`node scripts/lint-repository-knowledge.js` 验证，但当前分层调整尚未完成单任务 git closeout，暂不归档。

## Blocked

_No tasks._
