# Sonar Quality Gate Provisioning

## Purpose

本文件定义 `F-TASK-030` 收口后的 SonarQube 仓库侧接线真值，只描述仓库内已经实现的 workflow / script wiring，以及仍需在 GitHub Settings 中人工补齐的外部配置项。

## Repository Wiring

当前仓库的 SonarQube 入口已经固定为：

- 本地 / CI 脚本入口：`bash scripts/run-sonar.sh`
- 主 CI 可选扫描：`.github/workflows/ci.yml`
- 手工阶段门禁：`.github/workflows/phase-gate.yml`
- 自动 release 门禁：`.github/workflows/release-phase-gate.yml`

当前接线语义：

- `ci.yml`
  - 仅当仓库 secrets 中存在 `SONAR_HOST_URL` 与 `SONAR_TOKEN` 时执行 Sonar 扫描
  - `SONAR_PROJECT_KEY`、`SONAR_PROJECT_NAME`、`SONAR_QUALITY_GATE_WAIT` 可从 GitHub Actions `vars` 注入；若为空，`scripts/run-sonar.sh` 使用脚本默认值
- `phase-gate.yml`
  - `delivery` / `full` 模式会显式传入 `--require-sonar`
  - workflow job 已把 `SONAR_HOST_URL`、`SONAR_TOKEN`、`SONAR_PROJECT_KEY`、`SONAR_PROJECT_NAME`、`SONAR_QUALITY_GATE_WAIT` 注入到 `scripts/run-phase-gates.sh`
- `release-phase-gate.yml`
  - job 绑定到 GitHub Actions environment `quality-gate`
  - release gate 会从该 environment / workflow 上下文读取 Sonar 配置，并在 `checkpoint/*` tag push 与 `release.published` 上自动执行 full gate

## Required External Provisioning

仓库外仍需人工在 GitHub Settings 中补齐以下项：

### Required secrets

- `SONAR_HOST_URL`
- `SONAR_TOKEN`

### Optional variables

- `SONAR_PROJECT_KEY`
- `SONAR_PROJECT_NAME`
- `SONAR_QUALITY_GATE_WAIT`

### Recommended environment

- GitHub Actions environment: `quality-gate`
  - 供 `.github/workflows/release-phase-gate.yml` 的正式发布链使用
  - 建议在该 environment 中配置 `SONAR_HOST_URL` 与 `SONAR_TOKEN`

## Validation Path

完成外部配置后，推荐按以下顺序验证：

1. 本地或 runner 环境执行 `bash scripts/run-sonar.sh --require-config`
2. 手工触发 `Phase Gate` workflow，选择 `delivery` 或 `full`
3. 推送 `checkpoint/*` tag 或发布 GitHub Release，验证 `Release Phase Gate`

## Current Limitation

仓库已经补齐 Sonar 的脚本入口、phase gate 环境注入、release gate environment 接线与 runbook；但真实 `SONAR_HOST_URL` / `SONAR_TOKEN` 仍属于 GitHub 外部环境数据，不会也不能以明文进入仓库。
