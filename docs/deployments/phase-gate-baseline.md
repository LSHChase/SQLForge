# SQLForge Phase Gate Baseline

## Purpose

本文件作为 `F-TASK-005` 的当前权威落点，用于说明 SQLForge 已经把哪些阶段门禁脚本化接入 GitHub Actions，以及哪些门禁目前仍需要显式触发或依赖环境配置才能成为真正的 release blocker。

本文只写两类内容：

- 已被 workflow、脚本和仓库事实证明的当前 phase gate 接线
- 仍待后续任务或环境补齐的阻断缺口

## Scope

- GitHub Actions workflows:
  - `.github/workflows/ci.yml`
  - `.github/workflows/phase-gate.yml`
- Supporting scripts:
  - `scripts/run-phase-gates.sh`
  - `scripts/verify_compliance_baseline.py`
  - `scripts/task_audit.py`
  - `scripts/foreman.py`
  - `scripts/run-coverage.sh`
  - `scripts/run-sonar.sh`
- Related rules:
  - `R-116`
  - `R-117`
  - `R-118`

## Implemented Gate Wiring

| Gate area | Current implementation | Evidence |
|:---|:---|:---|
| Main CI governance checks | 主 CI 已新增 `task_audit` 与 `foreman compile-governance --check`，让台账结构和 `.codex/policy` 漂移进入默认流水线 | `.github/workflows/ci.yml` |
| Entry gate script | `scripts/run-phase-gates.sh --gate entry` 会执行 `task_audit`、`compile-governance --check`、`lint-repository-knowledge.js` | `scripts/run-phase-gates.sh` |
| Delivery gate script | `scripts/run-phase-gates.sh --gate delivery` 会执行 `mvn -B clean install`、覆盖率、Sonar、前端 lint/build 和仓库知识检查 | `scripts/run-phase-gates.sh` |
| Compliance gate script | `scripts/run-phase-gates.sh --gate compliance` 会调用 `scripts/verify_compliance_baseline.py`，对 auth/access-control、审计 schema、加密基线和备份恢复文档做最小机器校验 | `scripts/run-phase-gates.sh`, `scripts/verify_compliance_baseline.py` |
| Manual GitHub Actions gate | 新增 `Phase Gate` workflow，可通过 `workflow_dispatch` 选择 `entry|delivery|compliance|full`，并显式指定 coverage phase 与 Sonar 是否必需 | `.github/workflows/phase-gate.yml` |

## Gate Semantics

### Already Blocking In Main CI

以下检查现在已经进入默认 `CI` workflow：

1. `python3 scripts/task_audit.py --check`
2. `python3 scripts/foreman.py compile-governance --check`
3. 原有的 Maven 静态检查、后端测试、覆盖率报告、前后端分离检查、前端 lint/build、repository knowledge lint

### Blocking On Demand For Stage Switching

以下检查通过 `Phase Gate` workflow 显式触发：

1. `R-116` 入口门禁：`--gate entry`
2. `R-117` 交付门禁：`--gate delivery` 或 `--gate full`
3. `R-118` 渐进等保门禁：`--gate compliance` 或 `--gate full`

设计原因：

- 阶段切换不是每次 push / PR 都发生。
- 当前仓库的 `phase1plus` 覆盖率阈值与 Sonar secrets 仍可能让 full delivery gate 失败，因此不应在未补齐前直接把所有 PR 变成常红。
- 但只要显式运行 `Phase Gate` workflow，当前脚本已经具备非零退出码阻断能力。

## Current Gaps

以下缺口在 `F-TASK-005` 完成后仍然存在：

1. `Phase Gate` workflow 当前是 `workflow_dispatch` 手动触发，不是自动绑定到阶段切换元数据。
2. `R-117` 里的数据库脚本可执行检查尚未形成专用脚本步骤。
3. `phase1plus` 覆盖率目前仍低于 85% 门槛，full delivery gate 在严格 `phase1plus` 模式下预期会阻断。
4. Sonar 仍受 secrets 是否配置影响；若显式要求 `--require-sonar` 但 secrets 缺失，门禁会失败。
5. `R-118` 目前是最小机器校验，不替代真实环境中的身份、授权、审计、加密、备份恢复演练。

## Follow-Up Mapping

| Next task | Recommended follow-up |
|:---|:---|
| `F-TASK-006` | 让 Java 规范扫描在 CI 中变得更显式、可追溯，并保留扫描结果 |
| Later hardening | 若要把 phase gate 从手动 workflow 变成自动发布阻断，需要先补齐 Sonar 配置、覆盖率阈值与数据库脚本检查 |

## Related Documents

- [CI Capability Baseline](/models/project/codex/SQLForge/docs/deployments/ci-capability-baseline.md)
- [Validation Rules](/models/project/codex/SQLForge/docs/quality/validation-rules.md)
- [Local Development](/models/project/codex/SQLForge/docs/operations/local-development.md)
