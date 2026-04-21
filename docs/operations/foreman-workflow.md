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
6. 进入 closeout 前先完成“上下文收缩”：把实现结果、验证证据、剩余风险、未决项和下一步写回权威来源，并为当前任务写定最终 `Commit subject`
7. 将当前完成任务从 `tasks.md` 移入 `tasks-done.md`，确保 `Context closeout` 已完整，并把最新归档任务追加到 `## Done` 顶部
8. 运行 `python3 scripts/task_audit.py --check --phase pre-closeout`
9. 只 stage 当前任务相关文件并创建单任务 commit
10. commit 完成后立即运行 `python3 scripts/task_audit.py --check --phase post-closeout` 与必要知识校验，确认当前任务的 `Commit subject` 已进入 Git history，再执行“上下文清理”后进入下一任务；若当前 Codex 运行环境支持 `/contract`、`/clear` 或等价命令，可以使用，但它们只是可选实现方式，不是唯一规范动作

## Context Hygiene

- Harness Engineering 要求的是“上下文收缩”和“上下文清理”的结果，不是强绑定某个客户端斜杠命令。
- 若当前运行环境不支持 `/contract`、`/clear`，必须通过权威台账回写、验证日志补录、`INBOX.md` / 执行计划记录和重新按 Entry Order 建立上下文来完成等价动作。
- 下一任务开始前，必须重新核对权威来源；不得把上一任务中的临时判断直接沿用为下一任务事实。

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
- `blocked` 任务必须包含 `Next action:`、`Escalation:` 和 `Human decision:`
- 需要人类 code review 时，任务改为 `in_review` 并暂停
- `in_review` 任务必须包含 `Review reason:` 和 `Human decision:`
