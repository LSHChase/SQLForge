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

### INBOX-001: 恢复 Sonar 强制门禁的环境恢复项

- Status: open
- Needed decision: 若未来要把 Sonar 从当前 fallback 语义恢复为默认强制门禁，需由具备 GitHub Settings 权限的人类分两步决定恢复路径：先完成 provisioning（仓库级 secrets / vars，必要时再补 `quality-gate` environment），再决定是否显式 enable `SONAR_ENABLE_DEFAULT=true` 或通过新任务恢复更严格的 release 绑定；外部测试环境 CI/CD 因缺少仓库 smoke，不可作为替代仓库 repo-closed 门禁的理由
- Task refs: F-TASK-030, F-TASK-031, F-TASK-032
- Plan refs: docs/plans/master-execution-plan.md#F-TASK-030, docs/plans/master-execution-plan.md#F-TASK-031, docs/plans/master-execution-plan.md#F-TASK-032
