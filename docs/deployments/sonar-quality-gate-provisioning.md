# Sonar Quality Gate Provisioning

## Purpose

本文件定义 `F-TASK-031` 收口后的 SonarQube 仓库侧接线真值，只描述仓库内已经实现的 workflow / script wiring，以及仍需在 GitHub Settings 中人工补齐的外部配置项。

## Repository Wiring

当前仓库的 SonarQube 入口继续保留为：

- 本地 / CI 脚本入口：`bash scripts/run-sonar.sh`
- 主 CI 可选扫描：`.github/workflows/ci.yml`
- 手工阶段门禁：`.github/workflows/phase-gate.yml`
- 自动 release 门禁：`.github/workflows/release-phase-gate.yml`

当前接线语义：

- `ci.yml`
  - 仅当仓库 secrets 中存在 `SONAR_HOST_URL` 与 `SONAR_TOKEN` 时执行 Sonar 扫描
  - `SONAR_PROJECT_KEY`、`SONAR_PROJECT_NAME`、`SONAR_QUALITY_GATE_WAIT` 可从 GitHub Actions `vars` 注入；若为空，`scripts/run-sonar.sh` 使用脚本默认值
- `phase-gate.yml`
  - 默认不再自动传入 `--require-sonar`
  - 仅当显式设置 `require_sonar=true` 时才追加 Sonar-required fallback
  - workflow job 继续把 `SONAR_HOST_URL`、`SONAR_TOKEN`、`SONAR_PROJECT_KEY`、`SONAR_PROJECT_NAME`、`SONAR_QUALITY_GATE_WAIT` 注入到 `scripts/run-phase-gates.sh`
- `release-phase-gate.yml`
  - 保留 Sonar 环境透传与 release metadata artifact
  - release gate 默认执行 repo-closed full gate，不再自动附加 `--require-sonar`

## Current Semantics

- Sonar 继续保留在仓库内，作为 `environment-backed` fallback 入口存在。
- Sonar 不再是仓库 `repo-closed` 主路径的默认硬阻断。
- 若未来要恢复 Sonar 强制门禁，应通过新的环境恢复动作显式启用，而不是把当前 fallback 语义重新写成已强制。

## Environment Recovery Inputs

若未来需要恢复 Sonar 强制门禁，仓库外仍需人工在 GitHub Settings 中补齐以下项：

### Required secrets

- `SONAR_HOST_URL`
- `SONAR_TOKEN`

### Optional variables

- `SONAR_PROJECT_KEY`
- `SONAR_PROJECT_NAME`
- `SONAR_QUALITY_GATE_WAIT`

### Recommended environment

- GitHub Actions environment: `quality-gate`
  - 供恢复更严格的 release Sonar gate 时使用
  - 建议在该 environment 中配置 `SONAR_HOST_URL` 与 `SONAR_TOKEN`

## Validation Path

当前推荐把 Sonar 分成两条验证路径：

1. 仓库默认非阻断路径：执行 `bash scripts/run-sonar.sh`
   - 若未配置 Sonar 环境变量，应输出 skip，而不影响仓库主线路径
2. 环境恢复 / 强制验证路径：执行 `bash scripts/run-sonar.sh --require-config`
3. 如需恢复 workflow 级强制门禁，再触发 `Phase Gate` 的 `require_sonar=true` 或调整 release gate 恢复方案

## Current Limitation

仓库已经保留 Sonar 的脚本入口、phase gate 环境注入与 runbook；但真实 `SONAR_HOST_URL` / `SONAR_TOKEN` 仍属于 GitHub 外部环境数据，不会也不能以明文进入仓库。当前这些外部数据只影响 fallback / 环境恢复路径，不再代表仓库主线阻塞。
