# SQLForge CI Capability Baseline

## Purpose

本文件作为 `F-TASK-004` 的当前权威落点，用于盘点仓库现有 CI 流水线实际覆盖了哪些 lint / build / test / scan 动作，以及哪些能力目前仍停留在本地脚本或手工执行阶段。

本文只写两类内容：

- 已被 `.github/workflows/ci.yml`、脚本和仓库配置证明的当前 CI 事实
- 基于当前事实尚未接入 CI、需要后续任务补齐的缺口

凡尚未被 GitHub Actions 工作流调用的脚本或命令，不得写成“已进入 CI”。

## Scope

- CI workflow:
  - `.github/workflows/ci.yml`
- Supporting entry points:
  - `Makefile`
  - `scripts/run-coverage.sh`
  - `scripts/run-env-smoke.sh`
  - `scripts/run-sonar.sh`
  - `scripts/lint-repository-knowledge.js`
  - `scripts/check-frontend-backend-separation.js`
  - `scripts/task_audit.py`
  - `scripts/foreman.py`
  - `scripts/validate_codex_runtime.py`
- Related rules:
  - `R-075`
  - `R-117`
  - `R-148`
  - `R-151`
  - `R-152`

## Current Workflow Topology

| Area | Current implementation | Evidence |
|:---|:---|:---|
| Workflow count | 当前仓库有四个 GitHub Actions 工作流：`.github/workflows/ci.yml`、`.github/workflows/phase-gate.yml`、`.github/workflows/kafka-runtime-gate.yml` 与 `.github/workflows/release-phase-gate.yml` | `.github/workflows/ci.yml`, `.github/workflows/phase-gate.yml`, `.github/workflows/kafka-runtime-gate.yml`, `.github/workflows/release-phase-gate.yml` |
| Trigger policy | 主 CI 在 `main` / `master` / `develop` 的 `push` 以及所有 `pull_request` 上触发；release gate 绑定到 `checkpoint/*` tag push 与 GitHub `release.published` 元数据 | `.github/workflows/ci.yml`, `.github/workflows/release-phase-gate.yml` |
| Job topology | 当前只有一个 job：`build-and-test`，运行环境为 `ubuntu-latest` | `.github/workflows/ci.yml` |
| Runtime setup | workflow 会安装 Zulu JDK 8u112 和 Node.js 20 | `.github/workflows/ci.yml` |
| Backend static checks | workflow 会分步执行 `mvn -B validate -DskipTests`、`mvn -B pmd:pmd -DskipTests`、`mvn -B checkstyle:checkstyle -DskipTests`、`mvn -B checkstyle:check -DskipTests` | `.github/workflows/ci.yml` |
| Java scan artifacts | workflow 会校验并上传各模块的 `target/pmd.xml`、`target/site/pmd.html`、`target/checkstyle-result.xml`、`target/site/checkstyle.html` | `.github/workflows/ci.yml`, `scripts/verify_java_quality_reports.py` |
| Backend tests | workflow 会执行 `mvn -B test` | `.github/workflows/ci.yml` |
| Coverage integration | 主 CI 继续调用 `bash scripts/run-coverage.sh --phase report-only` 生成覆盖率报告；阶段切换阻断改由 `Phase Gate` workflow 显式运行 `phase0|phase1plus` | `.github/workflows/ci.yml`, `.github/workflows/phase-gate.yml`, `scripts/run-coverage.sh` |
| Runtime smoke integration | 主 CI 会显式执行 compose 语法检查、基础依赖启动、`governance`、`query-execution`、`sql-optimization`、`benchmark-engine` 与前端 dev server，并串联 `query-execution -> governance`、`sql-optimization -> governance`、`benchmark-engine -> governance` 业务 smoke、审计补偿验证、消息队列 smoke，以及浏览器驱动的前端真实业务请求与治理修复动作 smoke | `.github/workflows/ci.yml`, `scripts/run-runtime-smoke.sh`, `scripts/health-check.sh`, `scripts/manual-query-governance-smoke.sh`, `scripts/manual-sql-optimization-governance-smoke.sh`, `scripts/manual-benchmark-governance-smoke.sh`, `scripts/manual-message-queue-smoke.sh`, `scripts/frontend-runtime-smoke.mjs` |
| Database script executability | 主 CI 会在干净 MySQL 上重放 `sql/init-schema.sql`、`sql/init-data.sql` 并顺序执行所有 migration，确保 schema/init/migration 组合可执行 | `.github/workflows/ci.yml`, `scripts/verify-db-scripts.sh` |
| Sonar integration | 主 CI 仅在 `SONAR_ENABLE_DEFAULT=true` 且仓库 secrets 存在时执行 `bash scripts/run-sonar.sh --require-config`；`Phase Gate` workflow 保留 `require_sonar` opt-in 输入；`Release Phase Gate` 默认不绑定 `quality-gate` environment，并仅在显式 enable 时透传 Sonar 配置，整体语义保持为 environment-backed fallback | `.github/workflows/ci.yml`, `.github/workflows/phase-gate.yml`, `.github/workflows/release-phase-gate.yml`, `scripts/run-sonar.sh`, `docs/deployments/sonar-quality-gate-provisioning.md` |
| Boundary lint | workflow 会执行 `node scripts/check-frontend-backend-separation.js` | `.github/workflows/ci.yml` |
| Frontend lint/build | workflow 会执行 `npm install`、`npm run lint`、`npm run build` | `.github/workflows/ci.yml`, `package.json` |
| Repository knowledge lint | workflow 会执行 `node scripts/lint-repository-knowledge.js`，并补充校验 `README.md`、`AGENTS.md`、`.gitignore`、`.editorconfig` 存在 | `.github/workflows/ci.yml` |
| Governance gate in CI | 主 CI 已接入 `python3 scripts/task_audit.py --check` 与 `python3 scripts/foreman.py compile-governance --check` | `.github/workflows/ci.yml` |
| Manual phase gate workflow | `Phase Gate` workflow 通过 `workflow_dispatch` 执行 `entry|delivery|compliance|full` 阶段门禁；默认执行 repo-closed 路径，可按输入显式开启 Sonar 或真实 Kafka fallback | `.github/workflows/phase-gate.yml`, `scripts/run-phase-gates.sh` |
| Dedicated real Kafka workflow | 独立 `Kafka Runtime Gate` workflow 可显式拉起 MySQL + Kafka + governance，执行真实 Kafka 配置检查与成功/失败恢复 smoke | `.github/workflows/kafka-runtime-gate.yml`, `scripts/run-kafka-runtime-gate.sh`, `scripts/verify_kafka_runtime_config.py` |
| Automated release phase gate | `Release Phase Gate` workflow 会在 `checkpoint/*` tag push 与 `release.published` 上自动执行 `bash scripts/run-phase-gates.sh --gate full --coverage-phase phase1plus`，上传 release metadata artifact；默认不绑定 `quality-gate` environment，Sonar / 真实 Kafka 继续保留为独立 fallback 入口而非默认强绑 | `.github/workflows/release-phase-gate.yml`, `scripts/run-phase-gates.sh` |

## Current CI Coverage Matrix

### Already In CI

| Capability | Current CI status | Command path | Notes |
|:---|:---|:---|:---|
| Java static checks | Enabled | `mvn -B validate -DskipTests` + `mvn -B pmd:pmd -DskipTests` + `mvn -B checkstyle:checkstyle -DskipTests` + `mvn -B checkstyle:check -DskipTests` | 已拆分为显式步骤，便于定位失败环节 |
| Java scan report retention | Enabled | `python3 scripts/verify_java_quality_reports.py` + `actions/upload-artifact@v4` | 保留 PMD / Checkstyle XML 与 HTML 报告，满足 `R-151` 可读报告要求 |
| Backend unit/integration tests | Enabled | `mvn -B test` | 未按模块拆分 |
| Compose syntax validation | Enabled | `bash scripts/run-runtime-smoke.sh --compose-check` | 通过统一脚本兼容 `docker compose` / `docker-compose` |
| Database script executability | Enabled | `bash scripts/verify-db-scripts.sh` | 在干净 MySQL 上校验 init schema、init data 与 migrations 的可执行性 |
| Runtime startup / health / queue smoke | Enabled | `bash scripts/run-runtime-smoke.sh --runtime-smoke` | 启动本地依赖，拉起 `governance`、`query-execution`、`sql-optimization`、`benchmark-engine` 与前端 dev server，执行多服务健康探针、`query-execution -> governance`、`sql-optimization -> governance`、`benchmark-engine -> governance` 成功链路、失败恢复与审计补偿 smoke、消息重试 smoke，以及浏览器驱动的前端 `sql-query` / `acceleration` / `benchmark` / `system` 真实业务请求与治理修复动作；脚本会为 `dev` profile 注入仓库内测试密钥，并为治理侧启用按 trace 前缀触发的定向审计路由失败注入，前端 smoke 优先复用系统 Chrome |
| Coverage report generation | Enabled | `bash scripts/run-coverage.sh --phase report-only` | 只生成报告，不做 phase threshold gate |
| Optional Sonar scan | Conditional | `bash scripts/run-sonar.sh --require-config` | 依赖 secrets + 显式 `SONAR_ENABLE_DEFAULT=true`；仅 provisioning 不会自动运行 |
| Frontend-backend separation | Enabled | `node scripts/check-frontend-backend-separation.js` | 已纳入 CI |
| Frontend lint | Enabled | `npm run lint` | 与 `npm install` 同步执行 |
| Frontend build | Enabled | `npm run build` | 与 `npm install` 同步执行 |
| Frontend dev browser smoke | Not in CI by design | `npm run smoke:frontend-dev` | 当前只作为本地 `repo-closed` 开发回归基线；不得替代 `npm run smoke:frontend-runtime` 或被提升为默认 CI/runtime gate |
| Rewrite governance smoke | Not in CI by design | `npm run smoke:rewrite-governance` | 本地任务级 repo-closed smoke，串联 PRW-012 生产改写闭环 browser smoke、推荐/历史/告警契约和文档 closeout 检查；不消费真实 Hetu/MRS environment-backed 证据 |
| Repository knowledge lint | Enabled | `node scripts/lint-repository-knowledge.js` | 已作为仓库级文档门禁 |
| Task audit | Enabled | `python3 scripts/task_audit.py --check` | 已进入主 CI |
| Governance compile drift check | Enabled | `python3 scripts/foreman.py compile-governance --check` | 已进入主 CI |
| Manual phase gate workflow | Enabled | `.github/workflows/phase-gate.yml` + `scripts/run-phase-gates.sh` | 默认执行 repo-closed 门禁；可通过 `require_sonar` / `run_real_kafka_gate` 输入显式启用 fallback |
| Dedicated real Kafka gate workflow | Enabled | `.github/workflows/kafka-runtime-gate.yml` + `scripts/run-kafka-runtime-gate.sh` | 真实 Kafka 模式验证与主 CI 分离，避免默认流水线强绑外部 broker |
| Automated release phase gate | Enabled | `.github/workflows/release-phase-gate.yml` + `scripts/run-phase-gates.sh --gate full --coverage-phase phase1plus` | 发布路径会自动消费 tag / release metadata，并把 metadata 作为 artifact 留档；Sonar / 真实 Kafka 不再是默认附加参数 |

### Available Locally But Not In CI

| Capability | Current local entry point | Why it is not counted as CI coverage |
|:---|:---|:---|
| Phase gate coverage thresholds | `bash scripts/run-coverage.sh --phase phase0|phase1plus` | workflow 只调用了 `report-only`，未启用阈值阻断 |
| Foreman task validation | `python3 scripts/foreman.py validate <TASK_ID>` | 当前 workflow 仍未做任务级 validate 编排 |
| Codex runtime validation | `python3 scripts/validate_codex_runtime.py` | 当前 workflow 未调用 |
| Frontend dev browser smoke | `npm run smoke:frontend-dev` | 该入口被刻意保留为本地 `repo-closed` 开发回归基线，不属于默认 CI/browser runtime gate 覆盖 |
| Rewrite governance smoke | `npm run smoke:rewrite-governance` | 该入口是改写治理任务级 repo-closed 收口检查，并包含 PRW-012 生产改写闭环 browser smoke；Not in CI by design，若要升级为默认 CI / release gate 必须另立任务并人工确认 |
| Test-environment minimal smoke | `bash scripts/run-env-smoke.sh` | 供外部测试环境独立 CI/CD 在部署后调用；当前仓库内 GitHub Actions 未直接触发 |
## Current Gaps

以下缺口属于当前 CI 基线的残余事实，不是“已经接入”的事实：

1. `R-116` / `R-117` / `R-118` 已自动绑定到 `checkpoint/*` tag / `release.published` 发布路径，但日常的 ad hoc 阶段切换仍主要依赖 `workflow_dispatch`。
2. 当前 workflow 仍未把 `python3 scripts/foreman.py validate <TASK>` 纳入通用 CI。
3. `phase1plus` 聚合覆盖率已提升到 `86.9763%`，`Release Phase Gate` 与 `Phase Gate` 的 coverage blocker 已从“真实阻断项”转为“已达标门禁项”。
4. Sonar 已降为“仓库保留接线、外部 provisioning + 显式 enable 可恢复”的 fallback 项；缺少 `SONAR_HOST_URL` / `SONAR_TOKEN` 不再构成仓库默认发布阻断，且仅 provisioning 不会自动把 Sonar 升回主线阻断；若显式要求 `--require-config` 仍会失败并留下恢复证据。
5. 默认 browser runtime smoke 已覆盖前端 `sql-query`、`acceleration`、`benchmark`、`system` 与治理历史/修复链路的真实业务请求、失败恢复、审计补偿可视化与修复动作；剩余缺口已收敛为更多历史/取证页面尚未进入默认浏览器 smoke。
6. `npm run smoke:frontend-dev` 当前被明确保留为本地 `repo-closed` 开发基线，不属于默认 CI/browser runtime gate，也不应被后续任务无确认地提升为 release/phase gate 入口。
7. 真实 Kafka gate 已可运行，但仍依赖 runner 具备 Docker 资源、compose 拉镜像权限与可用端口，因此被保留为 environment-backed fallback，而不是 repo-closed 默认门禁。
8. 仓库已新增环境无关的 `bash scripts/run-env-smoke.sh` 作为测试环境 minimal smoke 入口，但外部测试环境 CI/CD 仍需由独立 owner 显式接入并保留执行证据；在此之前，测试环境仍不能被视为完整替代仓库闭环门禁。
9. 当前 workflow 继续使用 `npm install`，尚未固化成更严格的缓存/锁文件策略说明。

## Recommended Follow-Up Mapping

| Next task | Recommended scope based on current inventory |
|:---|:---|
| `F-TASK-005` | 已完成：`task_audit`、`compile-governance --check` 与 `Phase Gate` workflow 已接入 |
| `F-TASK-006` | 已完成：把 Java 静态检查拆成显式 CI 步骤，并把 PMD / Checkstyle 报告留存为 artifact |
| `F-TASK-010` | 已完成：把 compose 校验、`governance` 启动健康检查与消息队列 smoke 接入默认 CI |
| `F-TASK-011` | 已完成：把 `query-execution`、`sql-optimization`、`benchmark-engine` 与前端真实启动探针并入默认 runtime smoke 门禁 |
| `F-TASK-012` | 已完成：把 `query-execution -> governance` 真实业务 smoke、失败恢复路径与审计补偿验证并入默认 runtime gate |
| `F-TASK-013` | 已完成：把 `sql-optimization -> governance`、`benchmark-engine -> governance` 真实业务 smoke、失败恢复路径与审计补偿验证并入默认 runtime gate |
| `F-TASK-014` | 已完成：把前端 `sql-query`、`acceleration`、`benchmark` 真实业务请求与浏览器级 smoke 并入默认 runtime gate |
| `F-TASK-033` | 已完成：新增环境无关的测试环境 minimal smoke 入口，供外部测试环境 CI/CD 在部署后调用最小 health/API/前端可达性验证 |

## Exit Criteria For F-TASK-004

`F-TASK-004` 达成完成态时，必须满足：

1. 当前 `.github/workflows/ci.yml` 的真实覆盖范围已被完整盘点。
2. “本地可执行但未入 CI”的能力已与“已纳入 CI”的能力明确分层。
3. 后续 `F-TASK-005` / `F-TASK-006` 的接入范围已从盘点结论中自然导出。
4. 新文档已经进入 `docs/README.md`、真值基线与覆盖矩阵，不是孤立文件。

## Related Documents

- [Local Development](/models/project/codex/SQLForge/docs/operations/local-development.md)
- [Validation Rules](/models/project/codex/SQLForge/docs/quality/validation-rules.md)
- [Master Execution Plan](/models/project/codex/SQLForge/docs/plans/master-execution-plan.md)
- [Phase-F Story-003 Delivery Closeout](/models/project/codex/SQLForge/docs/deliveries/phase-f-story-003-ops-closeout.md)
- [Sonar Quality Gate Provisioning](/models/project/codex/SQLForge/docs/deployments/sonar-quality-gate-provisioning.md)
- [Test Environment Smoke Baseline](/models/project/codex/SQLForge/docs/deployments/test-environment-smoke-baseline.md)
