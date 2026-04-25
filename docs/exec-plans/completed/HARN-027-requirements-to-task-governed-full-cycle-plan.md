# HARN-027 Requirements-To-Task Governed Full-Cycle Plan

## Summary

- Task: `HARN-027`
- Goal: 在 `HARN-026` 的 downstream full-auto 基础上，补齐“从无 task 开始”的上游治理自动化：先做 requirement normalization、candidate execution plan、candidate task pack 与 governance gate，再 formal materialize 成正式 task，并 handoff 给既有 full-auto 执行链。
- Scope: `docs/operations/requirements-to-task-playbook.md`、新的 task-shaping prompt 模板、candidate task pack 模板、`scripts/requirements_to_plan.sh`、`scripts/task_materialize.sh`、`scripts/governed_full_cycle.sh`，以及 docs 索引/coverage/master plan/matrix 同步。
- Non-scope: 业务主线功能、现有 `HARN-026` downstream full-auto 语义重写、INBOX 自动裁决、delivery closeout 自动化。

## Guardrails

- 仍然只有 Main Foreman 可以最终写 ledger、执行 validate / audit / closeout。
- candidate task pack 在 gate 通过前只能停留在 `.codex/state/task-shaping/`。
- formal materialization 前必须阻断 task id 冲突、story id 漂移、human confirmation 漏标和审计链绕过。
- downstream execution 仍然调用既有 `multi_agent_full_auto.sh`，不发明第二套执行链。

## Validation Sequence

1. `python3 scripts/foreman.py validate HARN-027`
2. `bash scripts/requirements_to_plan.sh --help`
3. `bash scripts/task_materialize.sh --help`
4. `bash scripts/governed_full_cycle.sh --help`
5. requirement-to-plan dry-run
6. real candidate pack generation under `.codex/state/task-shaping/`
7. task_materialize dry-run against the generated pack
8. governed_full_cycle dry-run
9. `python3 scripts/task_audit.py --check --phase pre-closeout`
10. `python3 scripts/foreman.py closeout HARN-027 --stage-path ...`
11. `python3 scripts/task_audit.py --check --phase post-closeout`
