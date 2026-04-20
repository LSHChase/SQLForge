# Local Development

## Current Entry Points

- 查看标准命令：`make help`
- 启动本地流程：`bash scripts/local-start.sh`
- 停止本地流程：`bash scripts/local-stop.sh`
- 健康检查：`bash scripts/health-check.sh`
- 端口检查：`bash scripts/check-ports.sh`
- 消息链路 smoke：`bash scripts/manual-message-queue-smoke.sh`

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
- 任务台账审计：`python3 scripts/task_audit.py --check`

## Environment Rules

- 后端固定 Java 8 + Spring Boot 2.x
- 多环境配置通过 `application-{profile}.yml`
- 本地脚本和 compose 编排以仓库当前文件为准，不引入外部项目的服务顺序或端口口径
- `docs/generated/repo-map.md` 只作为仓库结构快照，不替代源码和规范文档
