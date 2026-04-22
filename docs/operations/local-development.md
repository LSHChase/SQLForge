# Local Development

## Current Entry Points

- 查看标准命令：`make help`
- Codex / foreman 预检：`python3 scripts/foreman.py preflight`
- Codex / foreman 治理编译：`python3 scripts/foreman.py compile-governance`
- Codex / foreman 审计包装：`python3 scripts/foreman.py audit --phase pre-closeout`
- Codex / foreman 运行时验证：`python3 scripts/validate_codex_runtime.py`
- Codex / foreman 任务 closeout：`python3 scripts/foreman.py closeout <TASK_ID> --stage-path <FILE> ...`
- Codex / foreman delivery closeout：`python3 scripts/foreman.py delivery-closeout <TASK_ID> --tag <TAG> --writeback-file <FILE>`
- 启动本地流程：`bash scripts/local-start.sh`
- 停止本地流程：`bash scripts/local-stop.sh`
- 健康检查：`bash scripts/health-check.sh`
- 端口检查：`bash scripts/check-ports.sh`
- 消息链路 smoke：`bash scripts/manual-message-queue-smoke.sh`
- 运行时 smoke 编排：`bash scripts/run-runtime-smoke.sh`

## Validation Baseline

- 仓库知识检查：`node scripts/lint-repository-knowledge.js`
- 前后端分离检查：`node scripts/check-frontend-backend-separation.js`
- 后端测试：`mvn -B test`
- 后端编译：`mvn -B clean compile`
- Java 规范扫描：`mvn -B validate pmd:pmd checkstyle:check`
- 覆盖率报告：`bash scripts/run-coverage.sh --phase report-only`
- 阶段门禁覆盖率检查：
  - 阶段 0：`bash scripts/run-coverage.sh --phase phase0`
  - 阶段 1+：`bash scripts/run-coverage.sh --phase phase1plus`
- SonarQube 扫描：
  - 本地或 CI 已配置环境变量时：`bash scripts/run-sonar.sh --require-config`
  - 尚未配置环境变量时可先执行：`bash scripts/run-sonar.sh`
- 前端构建：`npm run build`
- 前端 lint：`npm run lint`
- Compose 语法检查：`docker compose config`
- 运行时 smoke 门禁：`bash scripts/run-runtime-smoke.sh --compose-check`、`bash scripts/run-runtime-smoke.sh --runtime-smoke`
  - 如未预先设置 `SQLFORGE_DEV_CRYPTO_KEY_BASE64`，脚本会回落到仓库测试使用的开发密钥，只用于本地 / CI `dev` smoke
- 任务台账审计：`python3 scripts/task_audit.py --check`
- Codex 运行态状态目录：`.codex/state/`
- Codex 项目级 hook 编排：`.codex/hooks.json`
- 真实 Codex 项目验证：`codex exec --json "Reply with OK only."`

## Environment Rules

- 后端固定 Java 8 + Spring Boot 2.x
- 多环境配置通过 `application-{profile}.yml`
- 本地脚本和 compose 编排以仓库当前文件为准，不引入外部项目的服务顺序或端口口径
- `docs/generated/repo-map.md` 只作为仓库结构快照，不替代源码和规范文档
- `.codex/` 中的配置、policy 和 state 用于接线 Codex 执行流，不替代 `docs/` 真值，也不允许成为新的长期规则来源
