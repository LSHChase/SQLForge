# Human Collaboration

## Shared Responsibilities

- 人类负责目标、优先级、凭据、环境变量、基础设施和 merge / push / publish 审批
- Foreman 负责调查、实现、验证、台账、归档和建议
- `tasks.md` 是活动任务真值，`tasks-done.md` 是完成历史真值

## Stop And Continue

- 人类可以通过 `stop` 暂停 foreman
- 人类可以通过 `continue` 恢复
- stop 之后，先稳定当前工作树，再继续下一步

## Dirty Worktree Recovery

1. `stop`
2. 检查未提交变化
3. 决定保留、回滚或转为新任务
4. 恢复稳定状态后再 `continue`

## Git And Topology Safety

- 根目录 `/models/project/codex/SQLForge` 是唯一规范 Git 边界
- 若发现 nested `.git`、冲突或拓扑漂移，先停止任务，再由人类确认处理方式
- Foreman 活动期间，人类不应静默改写 branch、force push 或直接改 `.agent/config.json`

## Inbox Usage

- `INBOX.md` 只用于仍需人类判断、批准、优先级排序或任务塑形的问题
- 已在任务日志完整记录的失败，不重复写入 INBOX
- 已明确目标的 harness 或实现变更，直接创建任务，不先放 INBOX
