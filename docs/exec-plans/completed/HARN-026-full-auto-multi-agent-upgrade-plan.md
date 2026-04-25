# HARN-026 Full-Auto Multi-Agent Upgrade Plan

## Summary

- Task: `HARN-026`
- Goal: 把 SQLForge 多 agent 能力从 `HARN-025` 的半自动模式升级为“需求输入 -> exec plan/manifest 生成 -> prepare/launch/collect -> autonomous Main Foreman 收口”的全自动主路径。
- Scope: `docs/operations/multi-agent-playbook.md`, `docs/agent-prompts/auto-planner.md`, `docs/agent-prompts/auto-foreman.md`, docs 索引与 coverage，同步 `scripts/multi_agent_autoplan.sh`, `scripts/multi_agent_full_auto.sh`，以及对既有 prepare 脚本的 full-auto 兼容改造。
- Non-scope: 业务主线功能、数据库 schema、服务接口、delivery tag/write-back 语义。

## Guardrails

- 仍然只有 Main Foreman 可以最终写台账、执行 validate / audit / closeout。
- 全自动路径不得引入第二套长期真值。
- auto-planner 只生成 exec plan 和 manifest，不直接归档任务。
- auto-foreman 仍必须走 `foreman.py` 标准动作和显式 `--stage-path` closeout。

## Validation Sequence

1. `python3 scripts/foreman.py validate HARN-026`
2. `python3 scripts/task_audit.py --check --phase pre-closeout`
3. `python3 scripts/foreman.py closeout HARN-026 --stage-path ...`
4. `python3 scripts/task_audit.py --check --phase post-closeout`
