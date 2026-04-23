# SQLForge Phase Gate Baseline

## Purpose

本文件作为 `F-TASK-005` 的当前权威落点，用于说明 SQLForge 已经把哪些阶段门禁脚本化接入 GitHub Actions，以及当前如何把门禁拆成 `repo-closed` 主路径与 `environment-backed` fallback。

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
  - `scripts/run-env-smoke.sh`
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
| Delivery gate script | `scripts/run-phase-gates.sh --gate delivery` 会执行数据库脚本可执行检查、`mvn -B clean install`、覆盖率、可选 Sonar fallback、前端 lint/build 和仓库知识检查 | `scripts/run-phase-gates.sh`, `scripts/verify-db-scripts.sh` |
| Compliance gate script | `scripts/run-phase-gates.sh --gate compliance` 会调用 `scripts/verify_compliance_baseline.py`，默认执行 Kafka 配置校验；仅在显式传参时追加真实 Kafka runtime gate，对 auth/access-control、审计 schema、加密基线、恢复基线和消息运行证据做机器校验 | `scripts/run-phase-gates.sh`, `scripts/verify_compliance_baseline.py`, `scripts/verify_kafka_runtime_config.py`, `scripts/run-kafka-runtime-gate.sh` |
| Manual GitHub Actions gate | `Phase Gate` workflow 可通过 `workflow_dispatch` 选择 `entry|delivery|compliance|full`；默认执行 repo-closed 路径，可按输入显式启用 Sonar 或真实 Kafka fallback；workflow 继续把 `SONAR_*` 环境注入 `run-phase-gates.sh` | `.github/workflows/phase-gate.yml` |
| Dedicated Kafka workflow | 新增独立 `Kafka Runtime Gate` workflow，在真实 Kafka 模式下执行 bootstrap/security 参数校验、连通性检查与恢复 smoke | `.github/workflows/kafka-runtime-gate.yml`, `scripts/run-kafka-runtime-gate.sh` |
| Automated release gate | 新增 `Release Phase Gate` workflow，在 `checkpoint/*` tag push 与 `release.published` 上自动执行 `--gate full --coverage-phase phase1plus`，并把 release metadata 作为 artifact 留档；默认不绑定 `quality-gate` environment，Sonar 与真实 Kafka 保留为独立 fallback 能力，不再作为默认附加参数 | `.github/workflows/release-phase-gate.yml`, `scripts/run-phase-gates.sh`, `docs/deployments/sonar-quality-gate-provisioning.md` |

## Gate Semantics

### Dual-Layer Model

当前仓库门禁分为两层：

1. `repo-closed`
   - 以仓库内可证明、可复跑的 build/test/lint、coverage、db-script、runtime smoke、knowledge lint、task audit、compliance baseline 为主路径。
   - 这是当前 closeout 与主线发布默认依赖的门禁层。
2. `environment-backed`
  - 包括 Sonar、真实 Kafka gate、外部测试环境 CI/CD 及其部署后 minimal smoke 等依赖外部环境或额外 provisioning 的增强项。
   - 这些能力继续保留，但默认不替代也不阻断 `repo-closed` 主路径；仅 provisioning 不会自动把它们升级回默认阻断。

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
4. Sonar fallback：仅在显式要求 `require_sonar=true` 或直接传 `--require-sonar` 时启用
5. 真实 Kafka fallback：仅在显式要求 `run_real_kafka_gate=true` 或直接传 `--run-real-kafka-gate` 时启用

设计原因：

- 阶段切换不是每次 push / PR 都发生。
- 当前仓库的 `phase1plus` 覆盖率已达到 `86.9763%` 并满足 85% 门槛；仓库主线阻断继续以内建 repo gate 为准。
- Sonar 与真实 Kafka 仍可在环境具备时单独拉起，但默认不再被 workflow 自动强绑；Sonar 还需显式 enable 才会重新进入默认 CI / release wiring。
- 外部测试环境 CI/CD 即使已有独立流水线，也只能在额外调用 `bash scripts/run-env-smoke.sh` 并保留部署后证据后，才算具备最小 smoke；它仍不能替代仓库 `repo-closed` 主路径。

### Automatically Replaying Repo-Closed Gate On Release Metadata

以下检查会在正式 release metadata 进入仓库发布路径时自动触发：

1. `checkpoint/*` tag push
2. GitHub `release.published`

当前自动化语义：

- 自动执行 `bash scripts/run-phase-gates.sh --gate full --coverage-phase phase1plus`
- 自动消费 GitHub 事件中的 tag / release 元数据，并上传为 `release-phase-gate-metadata` artifact
- 若 repo-closed 主路径失败，会在发布链上直接阻断
- `release-phase-gate.yml` 默认不再绑定 `quality-gate` environment；Sonar / 真实 Kafka 是否额外执行，改由 fallback 入口和环境恢复项管理，而不是默认附带到 release metadata 路径

## Current Gaps

以下缺口在 `F-TASK-005` 完成后仍然存在：

1. `Release Phase Gate` 已自动绑定到 `checkpoint/*` tag / `release.published`，但 ad hoc 阶段切换仍主要依赖 `workflow_dispatch`。
2. `phase1plus` 覆盖率当前实测为 `86.9763%`，已高于 85% 门槛，coverage blocker 不再是 release gate 的残余缺口。
3. Sonar 仍受 secrets / vars / 可选 environment 是否配置影响；但当前必须先完成 provisioning，再显式 enable 或显式要求 fallback，才会影响默认 workflow 行为。
4. 自动 release gate 已消费发布元数据，但当前还未把 foreman 的 delivery write-back 记录直接反向注入 workflow 输入。
5. 真实 Kafka gate 仍依赖外部 broker、Docker 资源与安全参数配置，当前继续保留为 environment-backed 增强项。
6. `R-118` 虽已补入恢复基线、观测基线、Kafka gate 和脚本存在性校验，但仍不替代真实环境中的身份、授权、审计、加密、备份恢复演练。
7. 仓库现已提供 `bash scripts/run-env-smoke.sh` 作为测试环境部署后 minimal smoke 入口，但外部测试环境 CI/CD 仍需由独立 owner 显式接入并保留证据；在此之前，不能被记为完整替代门禁。

## Follow-Up Mapping

| Next task | Recommended follow-up |
|:---|:---|
| `F-TASK-027` | 已完成：数据库脚本 gate、coverage gate、真实 Kafka fallback 与 R-118 基线证据已接入 |
| `F-TASK-029` | 已完成：稳定 coverage 入口、修复导致 phase gate 误报的测试稳定性问题，并把 release metadata 自动触发链接入正式发布路径 |
| `F-TASK-031` | 已完成：把 Sonar / 环境级门禁降级为 fallback，显式建立 repo-closed / environment-backed 双层门禁语义 |
| `F-TASK-032` | 已完成：去除 Sonar fallback 的隐性自动恢复接线，显式分离 provisioning / enable 语义，避免 secrets / environment 一旦具备就自动回到默认硬阻断 |
| `F-TASK-033` | 已完成：新增环境无关的测试环境 minimal smoke 入口，明确外部测试环境部署后验证的最小脚本入口与证据边界 |
| Later hardening | 若未来要恢复 Sonar 或真实 Kafka 的默认强制语义，应通过新的环境恢复任务显式重启，不得直接覆写当前 repo-closed 真值 |

## Related Documents

- [CI Capability Baseline](/models/project/codex/SQLForge/docs/deployments/ci-capability-baseline.md)
- [Validation Rules](/models/project/codex/SQLForge/docs/quality/validation-rules.md)
- [Local Development](/models/project/codex/SQLForge/docs/operations/local-development.md)
- [Sonar Quality Gate Provisioning](/models/project/codex/SQLForge/docs/deployments/sonar-quality-gate-provisioning.md)
- [Test Environment Smoke Baseline](/models/project/codex/SQLForge/docs/deployments/test-environment-smoke-baseline.md)
