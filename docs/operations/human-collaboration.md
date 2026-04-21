# Human Collaboration

## Shared Responsibilities

- 人类负责目标、优先级、凭据、环境变量、基础设施和 merge / push / publish 审批
- Foreman 负责调查、实现、验证、台账、归档和建议
- `tasks.md` 是活动任务真值，`tasks-done.md` 是完成历史真值

## Default Strict Mode

- Foreman 执行任务时默认始终采用严格模式，不得静默丢失需求、约束、证据、历史记录、任务状态或文档同步项。
- 只要出现不确定、冲突、缺失、无法证明或可能造成语义丢失的内容，就必须暂停相关拍板动作，转为记录并等待人类决定或补充。
- 若问题仍需人类判断、批准、优先级排序或任务塑形，必须进入 `INBOX.md` 或对应任务/计划日志；不得自行替人类做最终决定。

## Context Commands

- 项目要求的是任务完成后的“上下文收缩”和“上下文清理”，不是强制绑定某个 Codex 客户端命令。
- 若当前运行环境支持 `/contract`、`/clear` 或等价命令，可以使用；若不支持，Foreman 仍必须通过台账回写、验证日志、`INBOX.md` / 执行计划补录以及重新建立上下文完成等价动作。
- 人类若要把某个特定客户端命令设为必选项，必须先确认该命令在当前执行环境稳定可用，再进入规则库；否则只能写成“可选实现方式”。

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
