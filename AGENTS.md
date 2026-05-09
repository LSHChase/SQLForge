# SQLForge Codex Contract

本仓库的长期真值在 `docs/` 与任务台账中；Codex 执行必须以后述入口、标准动作和审计链为准，不得仅依赖记忆、prompt 摘要或临时推断推进实现。

## Hard Requirements

- 非 trivial 任务在开始实现前，必须先通过 `python3 scripts/foreman.py preflight` 建立上下文。
- `docs/README.md`、`docs/plans/document-truth-baseline.md`、`docs/architecture/init.md`、`docs/rules/codex-rules.md`、`docs/quality/validation-rules.md` 是默认必读入口。
- 完整的 Karpathy 风格编码说明见 `docs/rules/karpathy-guidelines.md`；`AGENTS.md` 只保留入口和仓库级执行约束。
- 后端 Java 编译、测试、本地运行、CI 与交付环境固定使用 JDK 8u112；不得以泛化 `Java 8` 或其他 8u 版本替代，除非人类确认并更新规则。
- 非 trivial 任务不得绕过 `tasks.md` / `tasks-done.md` / `INBOX.md` 的运行台账体系。
- 任务实现、验证、归档、closeout 与交付收尾必须通过 `python3 scripts/foreman.py ...` 标准动作执行。
- 禁止跳过 `python3 scripts/task_audit.py --check --phase pre-closeout|post-closeout`。
- 普通任务要求单任务单 commit；只有 delivery 类任务才进入 tag / write-back。
- `turn stop` 不等于 `task finish`。只有进入 `done_ready` 或显式 closeout 时才允许任务归档与提交。
- 禁止使用 `git add .`、`git add -A`、`git commit -a`、`git reset --hard` 或其他破坏审计链的命令。
- 若 `.codex/` hooks 未加载，仍必须手动执行同等流程；不得因 hooks 缺失跳过治理。

## Authority Entry

1. `docs/README.md`
2. `docs/plans/document-truth-baseline.md`
3. `docs/architecture/init.md`
4. `docs/rules/codex-rules.md`
5. `docs/quality/validation-rules.md`
6. 任务相关专项文档
7. `docs/plans/master-execution-plan.md`
8. `docs/plans/phase-prerequisite-matrix.md`
9. `docs/plans/task-spec-matrix.md`
10. `docs/plans/task-governance-extension-matrix.md`
11. `tasks.md` / `tasks-done.md` / `INBOX.md`

## Standard Action Entry Points

- Preflight: `python3 scripts/foreman.py preflight`
- Instantiate task: `python3 scripts/foreman.py instantiate <TASK_ID>`
- Validate: `python3 scripts/foreman.py validate <TASK_ID>`
- Closeout: `python3 scripts/foreman.py closeout <TASK_ID>`
- Delivery closeout: `python3 scripts/foreman.py delivery-closeout <TARGET>`

## Audit Chain

- `tasks.md`
- `tasks-done.md`
- `INBOX.md`
- `docs/quality/validation-log.md`
- Git history
- Delivery write-back records

## Fallback Rule

如果当前 Codex 运行环境未加载 `.codex/config.toml` 或 `.codex/hooks.json`，则上述 CLI 与审计链仍然是强制工程要求。
