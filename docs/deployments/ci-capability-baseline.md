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
| Workflow count | 当前仓库有两个 GitHub Actions 工作流：`.github/workflows/ci.yml` 与 `.github/workflows/phase-gate.yml` | `.github/workflows/ci.yml`, `.github/workflows/phase-gate.yml` |
| Trigger policy | 在 `main` / `master` / `develop` 的 `push` 以及所有 `pull_request` 上触发 | `.github/workflows/ci.yml` |
| Job topology | 当前只有一个 job：`build-and-test`，运行环境为 `ubuntu-latest` | `.github/workflows/ci.yml` |
| Runtime setup | workflow 会安装 Java 8 和 Node.js 20 | `.github/workflows/ci.yml` |
| Backend static checks | workflow 会分步执行 `mvn -B validate -DskipTests`、`mvn -B pmd:pmd -DskipTests`、`mvn -B checkstyle:checkstyle -DskipTests`、`mvn -B checkstyle:check -DskipTests` | `.github/workflows/ci.yml` |
| Java scan artifacts | workflow 会校验并上传各模块的 `target/pmd.xml`、`target/site/pmd.html`、`target/checkstyle-result.xml`、`target/site/checkstyle.html` | `.github/workflows/ci.yml`, `scripts/verify_java_quality_reports.py` |
| Backend tests | workflow 会执行 `mvn -B test` | `.github/workflows/ci.yml` |
| Coverage integration | 主 CI 继续调用 `bash scripts/run-coverage.sh --phase report-only` 生成覆盖率报告；阶段切换阻断改由 `Phase Gate` workflow 显式运行 `phase0|phase1plus` | `.github/workflows/ci.yml`, `.github/workflows/phase-gate.yml`, `scripts/run-coverage.sh` |
| Sonar integration | workflow 仅在 `SONAR_HOST_URL` 与 `SONAR_TOKEN` secrets 存在时执行 `bash scripts/run-sonar.sh --require-config` | `.github/workflows/ci.yml`, `scripts/run-sonar.sh` |
| Boundary lint | workflow 会执行 `node scripts/check-frontend-backend-separation.js` | `.github/workflows/ci.yml` |
| Frontend lint/build | workflow 会执行 `npm install`、`npm run lint`、`npm run build` | `.github/workflows/ci.yml`, `package.json` |
| Repository knowledge lint | workflow 会执行 `node scripts/lint-repository-knowledge.js`，并补充校验 `README.md`、`AGENTS.md`、`.gitignore`、`.editorconfig` 存在 | `.github/workflows/ci.yml` |
| Governance gate in CI | 主 CI 已接入 `python3 scripts/task_audit.py --check` 与 `python3 scripts/foreman.py compile-governance --check` | `.github/workflows/ci.yml` |
| Manual phase gate workflow | 新增 `Phase Gate` workflow，通过 `workflow_dispatch` 执行 `entry|delivery|compliance|full` 阶段门禁 | `.github/workflows/phase-gate.yml`, `scripts/run-phase-gates.sh` |

## Current CI Coverage Matrix

### Already In CI

| Capability | Current CI status | Command path | Notes |
|:---|:---|:---|:---|
| Java static checks | Enabled | `mvn -B validate -DskipTests` + `mvn -B pmd:pmd -DskipTests` + `mvn -B checkstyle:checkstyle -DskipTests` + `mvn -B checkstyle:check -DskipTests` | 已拆分为显式步骤，便于定位失败环节 |
| Java scan report retention | Enabled | `python3 scripts/verify_java_quality_reports.py` + `actions/upload-artifact@v4` | 保留 PMD / Checkstyle XML 与 HTML 报告，满足 `R-151` 可读报告要求 |
| Backend unit/integration tests | Enabled | `mvn -B test` | 未按模块拆分 |
| Coverage report generation | Enabled | `bash scripts/run-coverage.sh --phase report-only` | 只生成报告，不做 phase threshold gate |
| Optional Sonar scan | Conditional | `bash scripts/run-sonar.sh --require-config` | 依赖 secrets；缺少配置时不会运行 |
| Frontend-backend separation | Enabled | `node scripts/check-frontend-backend-separation.js` | 已纳入 CI |
| Frontend lint | Enabled | `npm run lint` | 与 `npm install` 同步执行 |
| Frontend build | Enabled | `npm run build` | 与 `npm install` 同步执行 |
| Repository knowledge lint | Enabled | `node scripts/lint-repository-knowledge.js` | 已作为仓库级文档门禁 |
| Task audit | Enabled | `python3 scripts/task_audit.py --check` | 已进入主 CI |
| Governance compile drift check | Enabled | `python3 scripts/foreman.py compile-governance --check` | 已进入主 CI |

### Available Locally But Not In CI

| Capability | Current local entry point | Why it is not counted as CI coverage |
|:---|:---|:---|
| Phase gate coverage thresholds | `bash scripts/run-coverage.sh --phase phase0|phase1plus` | workflow 只调用了 `report-only`，未启用阈值阻断 |
| Foreman task validation | `python3 scripts/foreman.py validate <TASK_ID>` | 当前 workflow 仍未做任务级 validate 编排 |
| Codex runtime validation | `python3 scripts/validate_codex_runtime.py` | 当前 workflow 未调用 |
| Compose syntax validation | `docker compose config` | 当前 workflow 未调用 |
| Local startup / health / smoke | `bash scripts/local-start.sh`, `bash scripts/health-check.sh`, `bash scripts/manual-message-queue-smoke.sh` | 当前 workflow 未启动服务，也未做 runtime smoke |

## Current Gaps

以下缺口属于 `F-TASK-004` 盘点结论，不是“已经接入”的事实：

1. `R-116` / `R-117` / `R-118` 已有脚本和 `workflow_dispatch` 接线，但尚未自动绑定到阶段切换事件。
2. 当前 workflow 仍未把 `python3 scripts/foreman.py validate <TASK>` 纳入通用 CI。
3. `Phase Gate` 的 `phase1plus` 覆盖率阈值当前仍可能阻断，因为仓库聚合覆盖率尚未稳定达到 85%。
4. Sonar 目前仍是“有 secrets 才能真正通过”的门禁项，不是无条件可运行。
5. 未在 CI 中执行 `docker compose config`、本地启动、健康检查或消息链路 smoke。
6. 当前 workflow 使用 `npm install`，尚未固化成更严格的缓存/锁文件策略说明。

## Recommended Follow-Up Mapping

| Next task | Recommended scope based on current inventory |
|:---|:---|
| `F-TASK-005` | 已完成：`task_audit`、`compile-governance --check` 与 `Phase Gate` workflow 已接入 |
| `F-TASK-006` | 已完成：把 Java 静态检查拆成显式 CI 步骤，并把 PMD / Checkstyle 报告留存为 artifact |

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
