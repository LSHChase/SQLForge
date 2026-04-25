# HARN-028 Governed Full-Cycle V2 Hardening Plan

## Summary

- Task: `HARN-028`
- Goal: 在不改写 `HARN-027` 既有“从无 task 起步”治理真值的前提下，补齐 V2 加固：governed intake/healthcheck、machine-readable run summary 细化、materialization rollback，以及 closeout 后不得留下 tracked `validation-log` residue 的仓库级修复。
- Scope: `scripts/governed_v2_support.py`、`scripts/requirements_to_plan.sh`、`scripts/task_materialize.sh`、`scripts/governed_intake.sh`、`scripts/governed_healthcheck.py`、`scripts/foreman.py`，以及相关 playbook / closeout 文档和 docs 索引同步。
- Non-scope: 业务主线功能、绕过 `foreman`/`task_audit` 的黑盒自动执行、worker 直接改台账、交付型 tag/write-back 流程改写。

## Guardrails

- Main Foreman 仍是唯一最终收口点。
- intake 只能生成 candidate brief / summary，并等待显式 `--confirm-run`。
- worker / healthcheck 不得自动修改 `tasks.md` / `tasks-done.md` / `INBOX.md` / `docs/quality/validation-log.md`。
- closeout 修复只能消除 tracked residue，不能降低 post-closeout audit 或知识校验要求。

## Validation Sequence

1. `bash -n scripts/requirements_to_plan.sh`
2. `bash -n scripts/task_materialize.sh`
3. `bash -n scripts/governed_intake.sh`
4. `python3 -m py_compile scripts/governed_v2_support.py scripts/governed_healthcheck.py`
5. `bash scripts/governed_intake.sh --help`
6. `python3 scripts/governed_healthcheck.py --check`
7. `bash scripts/requirements_to_plan.sh --run-id harn028-smoke --task-prefix HARN --prompt "<smoke requirement>"`
8. `bash scripts/task_materialize.sh --task-pack .codex/state/task-shaping/harn028-smoke/candidate-task-pack.json --dry-run`
9. `bash scripts/governed_intake.sh --run-id harn028-intake-smoke --task HARN-028`
10. `python3 scripts/foreman.py validate HARN-028`
11. `python3 scripts/task_audit.py --check --phase pre-closeout`
12. `python3 scripts/foreman.py closeout HARN-028 --stage-path ...`
13. `python3 scripts/task_audit.py --check --phase post-closeout`
