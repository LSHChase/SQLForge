# HARN-025 Semi-Auto Multi-Agent Foundation Plan

## Summary

- Task: `HARN-025`
- Goal: 在不改变业务主线真值、不引入第二套长期真值、不依赖隐式 subagent 的前提下，把 SQLForge 的半自动多 agent 协作基础设施正式落为仓库治理/工具能力。
- Scope: `docs/operations/`, `docs/agent-prompts/`, `docs/exec-plans/templates/`, docs 索引与 coverage 矩阵，`scripts/multi_agent_prepare.sh`, `scripts/multi_agent_launch.sh`, `scripts/multi_agent_collect.sh`。
- Non-scope: 业务主线功能、服务接口、数据库 schema、业务页面行为、默认 closeout 语义。

## Required Deliverables

1. `docs/operations/multi-agent-playbook.md`
2. `docs/agent-prompts/*.md`
3. `scripts/multi_agent_prepare.sh`
4. `scripts/multi_agent_launch.sh`
5. `scripts/multi_agent_collect.sh`
6. `docs/exec-plans/templates/multi-agent-run.template.json`
7. docs 索引与 coverage 矩阵同步
8. 一个 SQLForge 场景的 demo runbook

## Guardrails

- Main Foreman 是唯一收口点。
- worker 不得修改 `tasks.md`、`tasks-done.md`、`INBOX.md`、`docs/quality/validation-log.md`。
- 多 agent 默认技术载体是多个 `codex exec` 会话与多个 `git worktree`，而不是隐式 subagent。
- 最终验证、审计、closeout 仍只走 `python3 scripts/foreman.py validate <TASK_ID>`、`python3 scripts/task_audit.py --check --phase pre-closeout`、`python3 scripts/foreman.py closeout <TASK_ID>`、`python3 scripts/task_audit.py --check --phase post-closeout`。

## Validation Sequence

1. `python3 scripts/foreman.py validate HARN-025`
2. `python3 scripts/task_audit.py --check --phase pre-closeout`
3. `python3 scripts/foreman.py closeout HARN-025 --stage-path ...`
4. `python3 scripts/task_audit.py --check --phase post-closeout`
