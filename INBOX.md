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

### INBOX-002: 外部 Hetu/MRS 测试环境验证窗口待确认

- Status: open
- Needed decision: 确认真实 Win10 测试环境何时可执行 `scripts/run-hetu-env-smoke.sh`，采用 `JDBC` / `REST` / `CLIENT` 中哪一种 Hetu 模式先做留证，以及由谁负责保存真实返回日志/响应证据；在该决定落定前，仓库侧实现继续推进，不把外部环境等待视为主线编码阻塞。
- Task refs: HARN-016, D-TASK-017, D-TASK-018, HARN-013, HARN-014
- Plan refs: docs/plans/master-execution-plan.md#D-TASK-017, docs/plans/master-execution-plan.md#D-TASK-018

### INBOX-003: 前端 Vue SFC 构建约束与依赖禁用策略冲突

- Status: open
- Needed decision: 当前仓库前端以 `src/App.vue` 和多个路由级 `.vue` single-file components 为真值，`vite.config.js` 也直接依赖 `@vitejs/plugin-vue`；同时 `vue@3.5.13` 自身会携带 `@vue/compiler-sfc` 作为传递依赖。若环境或供应链策略确实不支持 `@vitejs/plugin-vue`，或不允许出现任何层级的 `@vue/compiler-sfc`，则需要人类决定是保留现有 Vue SFC 架构并接受例外，还是授权一个更大范围的前端迁移任务，把根组件、路由视图和构建链改成非 `.vue` 方案。
- Task refs: E-TASK-010
