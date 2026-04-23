# SQLForge Inbox

本文件只记录仍需人类判断、批准、优先级排序或任务塑形的问题。

- 已在 `tasks.md` 进度日志中完整记录的失败，不重复写入本文件
- 不记录纯审计型条目
- 不把已明确目标的实现任务先放入本文件
- 条目 ID 使用 `### INBOX-XXX: 标题` 形式，便于任务台账通过 `INBOX ref:` 回指
- 每个条目至少记录 `Status:`、`Needed decision:`，以及 `Task refs:` / `Plan refs:` 之一
- 若已有任务通过 `INBOX ref:` 指向该条目，则该条目必须在 `Task refs:` 中列出对应 task id；`Plan refs:` 只用于补充计划背景或尚未形成任务的事项
- 若某问题只保留在任务日志，则任务中必须写 `INBOX ref: task-log-only: <reason>`

## Open

### INBOX-001: F-TASK-030 Sonar 外部环境补齐

- Status: open
- Needed decision: 由具备 GitHub Settings 权限的人类确认并完成 `quality-gate` environment 或仓库级 Sonar secrets / vars provisioning，使 `Phase Gate` / `Release Phase Gate` 可在真实环境中跑通 `bash scripts/run-sonar.sh --require-config`
- Task refs: F-TASK-030
- Plan refs: docs/plans/master-execution-plan.md#F-TASK-030
