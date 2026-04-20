# Foreman Workflow

## Entry Order

Foreman 接手任务前按以下顺序建立上下文：

1. `AGENTS.md`
2. `README.md`
3. `docs/README.md`
4. `docs/architecture/init.md`
5. `docs/rules/codex-rules.md`
6. `docs/quality/validation-rules.md`
7. `docs/plans/master-execution-plan.md`

按需懒加载：

- Java 任务：`docs/quality/alibaba-java-guidelines.md`
- 消息任务：`docs/architecture/messaging-abstraction.md`
- 安全或租户任务：`docs/security/compliance.md`、`docs/security/access-control-spec.md`
- 前端任务：`docs/frontend/design-system.md`、`docs/quality/frontend-backend-separation-baseline.md`

## Task Selection

- 人类指定任务时，先读 `tasks.md` 中对应任务，再标记 `in_progress`
- 人类未指定任务时，只能选择 `todo` 且依赖全为 `done` 的任务
- 自动选择优先级：较小 `priority` 数字优先；并列时选 `updated_at` 最早的任务
- `done` 任务不得保留在 `tasks.md`

## Execution Loop

1. 调查当前事实，避免把目标架构写成已实现事实
2. 实现任务范围内的变更
3. 自审代码、规则、文档和边界影响
4. 执行适用验证并记录证据
5. 如行为、API、架构、配置、合规或文档入口变化，则同步更新文档
6. 回写 `tasks.md` 或 `tasks-done.md`
7. 运行 `python3 scripts/task_audit.py --check`
8. 只 stage 当前任务相关文件并创建单任务 commit

## Progress Log Requirements

每个重大节点都要写入任务 `Progress log`，至少包含：

- 日期
- 变更文件或模块
- 执行命令
- 结果和验证证据
- 下一步
- 升级状态

## Escalation

- 达到最大尝试次数或必须人工决策时，任务改为 `blocked`
- `blocked` 任务必须包含 `Next action:` 和 `Escalation:`
- 需要人类 code review 时，任务改为 `in_review` 并暂停
