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
  - `.github/workflows/release-phase-gate.yml`
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
| Delivery gate script | `scripts/run-phase-gates.sh --gate delivery` 会执行数据库脚本可执行检查、`mvn -B clean install`、覆盖率、Sonar、前端 lint/build 和仓库知识检查 | `scripts/run-phase-gates.sh`, `scripts/verify-db-scripts.sh` |
| Compliance gate script | `scripts/run-phase-gates.sh --gate compliance` 会调用 `scripts/verify_compliance_baseline.py`，并按参数执行 Kafka 配置校验或真实 Kafka runtime gate，对 auth/access-control、审计 schema、加密基线、恢复基线和消息运行证据做机器校验 | `scripts/run-phase-gates.sh`, `scripts/verify_compliance_baseline.py`, `scripts/verify_kafka_runtime_config.py`, `scripts/run-kafka-runtime-gate.sh` |
| Manual GitHub Actions gate | `Phase Gate` workflow 可通过 `workflow_dispatch` 选择 `entry|delivery|compliance|full`；`delivery/full` 默认强制 Sonar，`compliance/full` 可启用真实 Kafka gate；workflow 已把 `SONAR_*` 环境显式注入 `run-phase-gates.sh` | `.github/workflows/phase-gate.yml` |
| Dedicated Kafka workflow | 新增独立 `Kafka Runtime Gate` workflow，在真实 Kafka 模式下执行 bootstrap/security 参数校验、连通性检查与恢复 smoke | `.github/workflows/kafka-runtime-gate.yml`, `scripts/run-kafka-runtime-gate.sh` |
| Automated release gate | 新增 `Release Phase Gate` workflow，在 `checkpoint/*` tag push 与 `release.published` 上自动执行 `--gate full --coverage-phase phase1plus --require-sonar --run-real-kafka-gate`，并把 release metadata 作为 artifact 留档；workflow job 绑定到 GitHub Actions environment `quality-gate` 以读取正式发布链所需 Sonar 配置 | `.github/workflows/release-phase-gate.yml`, `scripts/run-phase-gates.sh`, `docs/deployments/sonar-quality-gate-provisioning.md` |

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
- 当前仓库的 `phase1plus` 覆盖率已达到 `86.9763%` 并满足 85% 门槛；当前 full delivery gate 的主要残余阻断项已收敛为 Sonar 外部 secrets / environment provisioning。
- 但只要显式运行 `Phase Gate` workflow，当前脚本已经具备非零退出码阻断能力。

### Automatically Blocking On Release Metadata

以下检查会在正式 release metadata 进入仓库发布路径时自动触发：

1. `checkpoint/*` tag push
2. GitHub `release.published`

当前自动化语义：

- 自动执行 `bash scripts/run-phase-gates.sh --gate full --coverage-phase phase1plus --require-sonar --run-real-kafka-gate`
- 自动消费 GitHub 事件中的 tag / release 元数据，并上传为 `release-phase-gate-metadata` artifact
- 若覆盖率低于 `85%` 或 Sonar secrets 缺失，会在发布链上直接阻断，而不是留到人工提醒阶段

## Current Gaps

以下缺口在 `F-TASK-005` 完成后仍然存在：

1. `Release Phase Gate` 已自动绑定到 `checkpoint/*` tag / `release.published`，但 ad hoc 阶段切换仍主要依赖 `workflow_dispatch`。
2. `phase1plus` 覆盖率当前实测为 `86.9763%`，已高于 85% 门槛，coverage blocker 不再是 release gate 的残余缺口。
3. Sonar 仍受 secrets / environment 是否配置影响；若自动或手工门禁要求 `--require-sonar` 但 GitHub Settings 中未补齐 `SONAR_HOST_URL` / `SONAR_TOKEN`，门禁会失败。
4. 自动 release gate 已消费发布元数据，但当前还未把 foreman 的 delivery write-back 记录直接反向注入 workflow 输入。
5. `R-118` 虽已补入恢复基线、观测基线、Kafka gate 和脚本存在性校验，但仍不替代真实环境中的身份、授权、审计、加密、备份恢复演练。

## Follow-Up Mapping

| Next task | Recommended follow-up |
|:---|:---|
| `F-TASK-027` | 已完成：数据库脚本 gate、Sonar-required delivery mode、真实 Kafka compliance gate 与 R-118 基线证据已接入 |
| `F-TASK-029` | 稳定 coverage 入口、修复导致 phase gate 误报的测试稳定性问题，并把 release metadata 自动触发链接入正式发布路径 |
| Later hardening | 当前自动发布阻断已接入，coverage 阈值已达标；剩余硬化项收敛为 Sonar 外部 secrets / environment provisioning 与更细粒度的 delivery write-back 元数据联动 |

## Related Documents

- [CI Capability Baseline](/models/project/codex/SQLForge/docs/deployments/ci-capability-baseline.md)
- [Validation Rules](/models/project/codex/SQLForge/docs/quality/validation-rules.md)
- [Local Development](/models/project/codex/SQLForge/docs/operations/local-development.md)
- [Sonar Quality Gate Provisioning](/models/project/codex/SQLForge/docs/deployments/sonar-quality-gate-provisioning.md)
