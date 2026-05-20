# Local Development

## Current Entry Points

- 查看标准命令：`make help`
- MCP 治理手册：`docs/operations/codex-mcp-playbook.md`
- Codex / foreman 预检：`python3 scripts/foreman.py preflight`
- Codex / foreman 治理编译：`python3 scripts/foreman.py compile-governance`
  - 会同步产出 `.codex/policy/mcp-policy.json`
- Codex / foreman 审计包装：`python3 scripts/foreman.py audit --phase pre-closeout`
- Codex / foreman 运行时验证：`python3 scripts/validate_codex_runtime.py`
  - 当前会校验 MCP 基线文档、`mcp-policy.json` 和 repo-tracked runtime config 边界
- Codex / foreman 任务 closeout：`python3 scripts/foreman.py closeout <TASK_ID> --stage-path <FILE> ...`
- Codex / foreman delivery closeout：`python3 scripts/foreman.py delivery-closeout <TASK_ID> --tag <TAG> --writeback-file <FILE>`
- 启动本地流程：`bash scripts/local-start.sh`
- 启动后端四服务：`bash scripts/start-backend-services.sh`
- 停止本地流程：`bash scripts/local-stop.sh`
- 健康检查：`bash scripts/health-check.sh`
- 端口检查：`bash scripts/check-ports.sh`
- 治理授权矩阵 smoke：`bash scripts/manual-governance-authorization-smoke.sh --cleanup`
- 查询执行到治理链路 smoke：`bash scripts/manual-query-governance-smoke.sh --cleanup`
- SQL 优化到治理链路 smoke：`bash scripts/manual-sql-optimization-governance-smoke.sh --cleanup`
- 压测引擎到治理链路 smoke：`bash scripts/manual-benchmark-governance-smoke.sh --cleanup`
  - 当前会校验 benchmark 报告查询与 raw-data 下载审计已补齐 trace/export 链接键
- 测试环境最小 smoke 入口：`bash scripts/run-env-smoke.sh`
  - 可先执行 `bash scripts/run-env-smoke.sh --check-config`
  - 该入口供外部测试环境 CI/CD 在部署后调用，不替代本地 repo-closed runtime smoke
- 消息链路 smoke：`bash scripts/manual-message-queue-smoke.sh`
- 前端轻量 dev browser smoke：`npm run smoke:frontend-dev`
  - 该入口会临时拉起本地 Vite dev server，并在浏览器侧 mock `/api/*` 以验证 history 路由、Vue SFC 页面渲染和 `X-SQLForge-Dev-*` 调试提示头语义
  - 该入口用于本地 `repo-closed` 前端基线回归，不替代 `npm run smoke:frontend-runtime` 的多服务真实业务 smoke
  - 该入口不得被提升为默认 CI、`run-runtime-smoke.sh`、`run-phase-gates.sh` 或 release gate 的 browser runtime gate；它只服务于本地开发时的快速回归
- 改写治理 smoke：`npm run smoke:rewrite-governance`
  - 该入口串联推荐/历史页面契约、前端页面治理和 `PRW-012` 生产改写闭环 browser smoke
  - 该入口属于本地 `repo-closed` 任务级验证，不替代 full-stack runtime smoke，也不消费真实 Hetu / MRS environment-backed 证据
- 前端真实业务 smoke：`npm run smoke:frontend-runtime`
- 运行时 smoke 编排：`bash scripts/run-runtime-smoke.sh`
  - 如只需要后端服务，不需要前端 browser smoke，可直接使用 `bash scripts/start-backend-services.sh`
  - 默认会准备本地依赖、执行一次后端 Maven 安装、注入 `dev` 加密密钥，并等待 `8080`~`8083` 四个健康端点全部就绪

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
  - 仓库默认非阻断探测：`bash scripts/run-sonar.sh`
  - 环境恢复 / 显式强制验证：`bash scripts/run-sonar.sh --require-config`
  - Sonar 当前属于 `environment-backed` fallback，不是本地 `repo-closed` 主路径的默认硬前提；workflow 侧还要求显式 `SONAR_ENABLE_DEFAULT=true` 才会重新消费已 provision 的 Sonar 配置，具体恢复要求见 `docs/deployments/sonar-quality-gate-provisioning.md`
- 前端构建：`npm run build`
- 前端 lint：`npm run lint`
- 前端轻量 dev browser smoke：`npm run smoke:frontend-dev`
  - 自动拉起本地 Vite dev server，通过浏览器拦截 mock `/api/*`，检查 `dashboard`、`sql-query`、`sql-history`、`parse-record` 的 dev 路由与请求头语义
  - 用于快速确认当前 Vue SFC + Vite history 路由基线，不替代 full-stack runtime smoke
  - 当前只作为本地 `repo-closed` 开发验证基线，不计入默认 CI 或 phase/release runtime gate
- 改写治理 smoke：`npm run smoke:rewrite-governance`
  - 覆盖 repo-closed closeout 契约、`PRW-012` 推荐中心激活暂停到 SQL 历史追溯 mocked API browser smoke 与推荐中心 / SQL 历史静态契约
  - 真实 Hetu / MRS EXPLAIN、扫描量、P99、物化视图收益和外部装数证据仍归 `HARN-016` / `INBOX-002` environment-backed 链路
- 前端真实业务 smoke：`npm run smoke:frontend-runtime`
- Compose 语法检查：`docker compose config`
- 运行时 smoke 门禁：`bash scripts/run-runtime-smoke.sh --compose-check`、`bash scripts/run-runtime-smoke.sh --runtime-smoke`
  - `--runtime-smoke` 会启动本地依赖、`governance`、`query-execution`、`sql-optimization`、`benchmark-engine` 和前端 dev server，并串联健康探针、治理授权矩阵 smoke、`query-execution -> governance`、`sql-optimization -> governance`、`benchmark-engine -> governance` 业务 smoke、消息队列 smoke，以及浏览器驱动的前端 `sql-query` / `acceleration` / `benchmark` 真实业务请求 smoke
  - 如未预先设置 `SQLFORGE_DEV_CRYPTO_KEY_BASE64`，脚本会回落到仓库测试使用的开发密钥，并向需要敏感字段加密初始化的服务注入 dev key；仅用于本地 / CI `dev` smoke
  - 业务 smoke 会验证治理授权成功/拒绝/跨租户/吊销后访问与权限变更审计、查询执行成功链路、SQL 优化任务提交/轮询、压测任务提交/报告读取与 raw-data 下载审计链接、失败恢复路径、治理审计写入、前端浏览器端真实请求，以及按 trace 前缀触发的审计补偿队列兜底
  - 前端 smoke 优先复用系统 Chrome；如本机没有常见的 Chrome / Chromium 可执行文件，则会回退到 Playwright 默认浏览器解析逻辑
- 测试环境 minimal smoke：
  - `bash scripts/run-env-smoke.sh`
  - 通过环境变量接收各服务 URL 与受保护请求上下文
  - 默认只验证四个后端 health、前端可达性、三条最小业务链路和一条受保护治理接口有效性，不做完整浏览器回归或 DB 直查
  - 该入口属于外部测试环境部署后验证层，由外部环境 owner 调用；不能替代仓库 `repo-closed` 主路径
- 任务台账审计：`python3 scripts/task_audit.py --check`
- Codex 运行态状态目录：`.codex/state/`
- Codex 项目级 hook 编排：`.codex/hooks.json`
- 真实 Codex 项目验证：`codex exec --json "Reply with OK only."`

## Environment Rules

- 后端固定 JDK 8u112 + Spring Boot 2.x
- 多环境配置通过 `application-{profile}.yml`
- 本地脚本和 compose 编排以仓库当前文件为准，不引入外部项目的服务顺序或端口口径
- `docs/generated/repo-map.md` 只作为仓库结构快照，不替代源码和规范文档
- `.codex/` 中的配置、policy 和 state 用于接线 Codex 执行流，不替代 `docs/` 真值，也不允许成为新的长期规则来源
- 当前 MCP 基线只允许 4 类只读 category：观测/日志、部署证据、对象存储元数据、外部需求/工单检索；不得在本仓库率先接入可写云控、SSH、K8s、数据库执行型 MCP
- 真实 MCP server 凭据、token、endpoint 或 `mcp_profile` 不得在 repo-tracked 配置中落仓；如需本地使用，必须通过用户本地配置、环境变量或外部 secret store 提供
- 外部测试环境即使已有独立 CI/CD，只要缺少仓库口径的 smoke / runtime gate，就不能替代本仓库的 `repo-closed` 主路径
- 外部测试环境现在应调用 `bash scripts/run-env-smoke.sh` 完成最小部署后验证；若未调用并保留证据，仍不能被写成“已有 smoke 闭环”
