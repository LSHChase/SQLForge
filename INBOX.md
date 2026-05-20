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

### INBOX-007: 是否删除审计取证追踪告警运行门禁恢复演练

- Status: resolved
- Needed decision: 人类已选择 B）删除产品化辅助治理面和公开追踪 / 告警 API，但保留最小执行安全内核；同时确认废止 `R-111` 与 `R-115` 作为产品化页面 / 公开操作面的要求，保留 SQL 执行历史、真实改写历史和 runtime binding `ACTIVE` 后才允许自动改写 / 加速命中的安全约束，允许历史 `audit_log` / `alert_event` / trace 数据 drop，并确认 benchmark artifact recovery / cleanup 移出项目产品边界。
- Task refs: USER-CN-SIMPLIFY-ENGINE-REMOVE-OPS-SURFACES-20260520, USER-CN-IMPLEMENT-ENGINE-OPTION-B-20260520
- Plan refs: docs/plans/simplify-engine-remove-ops-surfaces-impact-analysis-2026-05-20.md

### INBOX-006: 激活/暂停折叠方案核心语义待确认

- Status: resolved
- Needed decision: 人类已选择 C）破坏性状态机迁移，确认 approval 不是 activate 背后的强制审计门禁，确认 `unpublish` 完全并入 `pause`，并确认 acceleration plan 的 `apply/verify/rollback` 纳入本轮迁移但保留原有证据内容，改为 activate/pause 的 `activationEvidence` / `pauseEvidence`。
- Task refs: USER-CN-SIMPLIFY-LIFECYCLE-ACTIVATE-PAUSE-20260520
- Plan refs: docs/plans/lifecycle-activation-pause-impact-analysis-2026-05-20.md

### INBOX-001: 恢复 Sonar 强制门禁的环境恢复项

- Status: open
- Needed decision: 若未来要把 Sonar 从当前 fallback 语义恢复为默认强制门禁，需由具备 GitHub Settings 权限的人类分两步决定恢复路径：先完成 provisioning（仓库级 secrets / vars，必要时再补 `quality-gate` environment），再决定是否显式 enable `SONAR_ENABLE_DEFAULT=true` 或通过新任务恢复更严格的 release 绑定；外部测试环境 CI/CD 因缺少仓库 smoke，不可作为替代仓库 repo-closed 门禁的理由
- Task refs: F-TASK-030, F-TASK-031, F-TASK-032
- Plan refs: docs/plans/master-execution-plan.md#F-TASK-030, docs/plans/master-execution-plan.md#F-TASK-031, docs/plans/master-execution-plan.md#F-TASK-032

### INBOX-002: 外部 Hetu/MRS 测试环境验证窗口待确认

- Status: open
- Needed decision: 确认真实 Win10 测试环境何时可执行 `scripts/run-hetu-env-smoke.sh`，采用 `JDBC` / `REST` / `CLIENT` 中哪一种 Hetu 模式先做留证，以及由谁负责保存真实返回日志/响应证据；在该决定落定前，仓库侧实现继续推进，不把外部环境等待视为主线编码阻塞。
- Task refs: HARN-016, HARN-088, D-TASK-017, D-TASK-018, HARN-013, HARN-014
- Plan refs: docs/plans/master-execution-plan.md#D-TASK-017, docs/plans/master-execution-plan.md#D-TASK-018

### INBOX-005: 生产规模压测外部证据 owner 与归档位置待确认

- Status: open
- Needed decision: 确认可用于真实生产规模留证的生产或准生产环境、执行窗口、10000 并发压测 runner、千万级日查询量证明来源、30PB 数据布局证明来源、24 小时 workload replay 来源、P95/P99/扫描字节/CPU/队列等待指标导出方式、成本账单来源、外部 artifacts 归档位置、谁提供 `provenance.json` 中的 `environmentId/environmentType/evidenceOwner/artifactArchiveRef/verifierOperator`、谁运行 `scripts/verify-benchmark-production-evidence.py`、谁复核 `provenance.json` 和 `evidenceFileDigests` 可按原始 evidence directory 复算 SHA-256，以及谁负责把通过校验且含来源元数据和摘要的 `scaleTargetEvidenceManifest` 提交到 benchmark 任务。
- Task refs: USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-EXTERNAL-ARTIFACTS-20260518
- Plan refs: docs/deployments/benchmark-production-evidence-runbook.md

### INBOX-003: 前端 Vue SFC 构建约束与依赖禁用策略冲突

- Status: resolved
- Needed decision: 人类已再次确认当前环境允许 `@vitejs/plugin-vue` 与 `@vue/compiler-sfc`，因此不再坚持非 SFC 架构；按 `E-TASK-014` 恢复根级前端 `.vue` 源文件与 SFC 构建链，同时保留此前已经交付的 portable 双产物、浏览器 smoke 和分包优化结果。
- Task refs: E-TASK-010, E-TASK-011, E-TASK-014

### INBOX-004: 本地后端重启 JDK 8u112 环境待确认

- Status: resolved
- Needed decision: 人类已确认安装本地 JDK `8u112`，用于解除 `U-TASK-006` 的后端重启运行时阻塞。
- Task refs: U-TASK-006
