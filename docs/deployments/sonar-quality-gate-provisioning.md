# Sonar Quality Gate Provisioning

## Purpose

本文件定义 `F-TASK-032` 收口后的 SonarQube 仓库侧接线真值，只描述仓库内已经实现的 workflow / script wiring，以及仍需在 GitHub Settings 中人工补齐的外部配置项。

## Repository Wiring

当前仓库的 SonarQube 入口继续保留为：

- 本地 / CI 脚本入口：`bash scripts/run-sonar.sh`
- 主 CI 可选扫描：`.github/workflows/ci.yml`
- 手工阶段门禁：`.github/workflows/phase-gate.yml`
- 自动 release 门禁：`.github/workflows/release-phase-gate.yml`

当前接线语义：

- `ci.yml`
  - 仅当仓库 variable `SONAR_ENABLE_DEFAULT=true` 且仓库 secrets 中存在 `SONAR_HOST_URL` 与 `SONAR_TOKEN` 时执行 Sonar 扫描
  - `SONAR_PROJECT_KEY`、`SONAR_PROJECT_NAME`、`SONAR_QUALITY_GATE_WAIT` 可从 GitHub Actions `vars` 注入；若为空，`scripts/run-sonar.sh` 使用脚本默认值
- `phase-gate.yml`
  - 默认不再自动传入 `--require-sonar`
  - 仅当显式设置 `require_sonar=true` 时才追加 Sonar-required fallback
  - workflow job 继续把 `SONAR_HOST_URL`、`SONAR_TOKEN`、`SONAR_PROJECT_KEY`、`SONAR_PROJECT_NAME`、`SONAR_QUALITY_GATE_WAIT` 注入到 `scripts/run-phase-gates.sh`
- `release-phase-gate.yml`
  - 保留 release metadata artifact，并仅在 `SONAR_ENABLE_DEFAULT=true` 时透传 Sonar 配置
  - 默认不再绑定 `quality-gate` environment，也不再因为 Sonar 已 provision 就自动恢复为阻断路径
  - release gate 默认执行 repo-closed full gate，不再自动附加 `--require-sonar`

## Current Semantics

- Sonar 继续保留在仓库内，作为 `environment-backed` fallback 入口存在。
- Sonar 不再是仓库 `repo-closed` 主路径的默认硬阻断。
- Sonar 当前显式分成两步：
  - provisioning：把 secrets / vars / 可选 environment 准备好，使能力可用
  - enable：通过治理批准显式打开 `SONAR_ENABLE_DEFAULT=true`，或使用 `Phase Gate` 的 `require_sonar=true`
- 若未来要恢复 Sonar 强制门禁，应通过新的环境恢复动作显式启用，而不是把当前 fallback 语义重新写成已强制。

## Environment Recovery Inputs

若未来需要恢复 Sonar 强制门禁，仓库外仍需人工在 GitHub Settings 中分两步补齐：

### Step 1: provisioning inputs

#### Required secrets

- `SONAR_HOST_URL`
- `SONAR_TOKEN`

#### Optional variables

- `SONAR_PROJECT_KEY`
- `SONAR_PROJECT_NAME`
- `SONAR_QUALITY_GATE_WAIT`

#### Optional environment

- GitHub Actions environment: `quality-gate`
  - 不再默认绑定到 `release-phase-gate.yml`
  - 仅在未来要恢复更严格的 release Sonar gate 时使用
  - 可在该 environment 中配置 `SONAR_HOST_URL` 与 `SONAR_TOKEN`

### Step 2: enable inputs

- GitHub Actions repository variable: `SONAR_ENABLE_DEFAULT`
  - 默认保持未设置或 `false`
  - 仅在具备人类批准并确认要让默认 CI / release fallback 路径重新消费 Sonar 配置时设置为 `true`

## Validation Path

当前推荐把 Sonar 分成两条验证路径：

1. 仓库默认非阻断路径：执行 `bash scripts/run-sonar.sh`
   - 若未配置 Sonar 环境变量，应输出 skip，而不影响仓库主线路径
2. 环境恢复 / 强制验证路径：执行 `bash scripts/run-sonar.sh --require-config`
3. 如需恢复 workflow 级默认启用路径，先完成 provisioning，再显式设置 `SONAR_ENABLE_DEFAULT=true`
4. 如需恢复阶段切换级强制门禁，再触发 `Phase Gate` 的 `require_sonar=true` 或通过新任务调整 release gate 恢复方案

## Current Limitation

仓库已经保留 Sonar 的脚本入口、phase gate 环境注入与 runbook；但真实 `SONAR_HOST_URL` / `SONAR_TOKEN` 仍属于 GitHub 外部环境数据，不会也不能以明文进入仓库。当前这些外部数据只代表 provisioning 已就绪；只有在显式 enable 后，才会影响默认 workflow 是否重新消费 Sonar fallback。
