# SQLForge Task Specification Matrix

本文件用于把 [master-execution-plan.md](/models/project/codex/SQLForge/docs/plans/master-execution-plan.md) 中的 Task 补齐为 Harness Engineering 要求的 10 项字段。

## Context Aliases

- `Core`：`docs/README.md`, `docs/architecture/init.md`, `docs/rules/codex-rules.md`, `docs/plans/master-execution-plan.md`
- `Val`：`docs/quality/validation-rules.md`
- `ADR`：`docs/adr/README.md` 与对应 ADR 实体文件
- `Sec`：`docs/security/compliance.md`, `docs/security/access-control-spec.md`
- `Msg`：`docs/architecture/messaging-abstraction.md`
- `Front`：`docs/frontend/design-system.md`, `docs/quality/frontend-backend-separation-baseline.md`
- `Deploy`：`docs/deployments/local-setup.md`, `docs/deployments/offline-setup.md`, `docs/deployments/huawei-cloud-setup.md`
- `Delivery`：`docs/deliveries/init-completion.md`, `docs/plans/phase-0-plan.md`
- `Archive`：`docs/references/human-constraint-history.md`, `docs/references/raw-requirements/...`

## Tech / Layer Codes

- `DOCS`：Markdown/规则/计划/ADR 文档
- `JAVA-BE`：Java 8 + Spring Boot 2.x + MyBatis XML + Maven
- `VUE-FE`：Vue 3 + JavaScript + Element Plus + Vite
- `OPS`：Docker Compose / CI / Huawei Cloud / shell scripts
- `SQL`：MySQL schema / init scripts / XML
- `docs`：文档层
- `common`：公共层
- `application(controller/service)/domain/infrastructure`：后端强制分层，其中 `application` 仅作为 `controller`、`service` 等入站与编排代码的包域
- `frontend/router/views/styles`：前端层
- `deployments/ci/scripts`：运维与交付层

## Phase-A

| Task ID | Name | ADR | Rules | Context | Contract | Tech | Layer | Tests | Deps | Env |
|:---|:---|:---|:---|:---|:---|:---|:---|:---|:---|:---|
| `A-TASK-001` | 盘点 `docs/` 一级与二级目录 | N/A | `R-006`,`R-007`,`R-014` | `Core`,`Archive` | 输出完整文档目录清单，不改内容 | `DOCS` | `docs` | 文件清单与实际目录一致 | 无 | docs-only |
| `A-TASK-002` | 标注文档权威关系 | N/A | `R-006`,`R-009`,`R-010` | `Core`,`ADR`,`Sec`,`Deploy`,`Front` | 为每份文档标注 authority/index/archive 角色 | `DOCS` | `docs` | 路径、角色、引用关系检查 | `A-TASK-001` | docs-only |
| `A-TASK-003` | 标注文档缺失与重复声明 | N/A | `R-007`,`R-049`,`R-050` | `Core`,`Archive` | 产出缺失/冲突/重复声明清单 | `DOCS` | `docs` | 缺口进入确认台账 | `A-TASK-002` | docs-only |
| `A-TASK-004` | 盘点前端实现现状 | N/A | `R-015`~`R-030`,`R-088`~`R-098` | `Core`,`Front` | 输出前端现状与目标差异 | `VUE-FE` | `frontend/router/views/styles` | `npm run build`,`npm run lint` | 无 | dev |
| `A-TASK-005` | 盘点后端实现现状 | `ADR-002` | `R-018`~`R-022`,`R-065`~`R-068` | `Core`,`ADR`,`Sec`,`Msg` | 输出后端现状、分层和配置差异 | `JAVA-BE` | `application(controller/service)/domain/infrastructure` | `mvn -B test` | 无 | dev/test |
| `A-TASK-006` | 盘点脚本与部署现状 | `ADR-004` | `R-076`,`R-077`,`R-106`~`R-110`,`R-130` | `Core`,`Deploy`,`Msg` | 输出脚本/compose/SQL/部署文档现状 | `OPS`,`SQL` | `deployments/ci/scripts` | 脚本存在性、文档引用、一致性检查 | `A-TASK-005` | dev/prod-doc |
| `A-TASK-007` | 建立规则映射矩阵 | N/A | `R-001`~`R-155` | `Core`,`Val`,`Archive` | 每条规则至少映射到文档/代码/验证/缺口 | `DOCS` | `docs` | 规则连续性与落点检查 | `A-STORY-001`,`A-STORY-002` | docs-only |
| `A-TASK-008` | 建立冲突台账 | N/A | `R-049`,`R-050` | `Core`,`Archive` | 每个冲突具备范围/收益/破坏面/方案 | `DOCS` | `docs` | 冲突项编号和字段完整 | `A-TASK-007` | docs-only |
| `A-TASK-009` | 固化 Harness 计划模板 | N/A | `R-007`,`R-116`,`R-136` | `Core`,`Val` | 建立 Task 10 字段标准模板 | `DOCS` | `docs` | 对照 `11.2` 字段完整性检查 | `A-TASK-007` | docs-only |
| `A-TASK-010` | 主计划与运行台账对齐 | N/A | `R-006`,`R-007`,`R-133`,`R-156`,`R-163` | `Core`,`Val`,`Delivery`,`Archive` | 回补 post-publication 已执行任务、修正 active wave、清理已解决 INBOX | `DOCS` | `docs` | `task_audit`、knowledge lint、计划/矩阵/台账交叉检查 | `A-STORY-003` | docs-only |
| `A-TASK-011` | 主计划剩余任务对齐并修复跨服务鉴权审计缺口 | `ADR-002`,`ADR-009`,`ADR-012`,`ADR-013` | `R-006`,`R-007`,`R-111`~`R-115`,`R-128`,`R-133`,`R-156`,`R-163` | `Core`,`Val`,`Sec`,`Archive` | 对齐 active wave、剩余任务与事实完成度，并修复跨服务鉴权、租户归一化与审计元数据高优先缺口 | `DOCS`,`JAVA-BE` | `docs`,`application(controller/service)/domain/infrastructure` | `task_audit`、knowledge lint、计划/矩阵/台账交叉检查、跨服务测试与配置断言 | `A-TASK-010` | docs/dev/test/prod-doc |
| `A-TASK-012` | 抽取 shared 认证与治理客户端支撑 | `ADR-002`,`ADR-009` | `R-021`,`R-022`,`R-067`,`R-068`,`R-111`~`R-115`,`R-126`,`R-156` | `Core`,`Val`,`Sec` | 把重复的 header 鉴权建立、请求元数据透传和治理内部受保护请求支撑下沉到 `sqlforge-shared`，保持外部契约不变 | `JAVA-BE` | `common`,`application(controller/service)/domain/infrastructure` | `task_audit`、`mvn -B -pl query-execution,sql-optimization,benchmark-engine -am test -DskipITs`、共享支撑回归断言 | `A-TASK-011` | dev/test |

## Phase-B

| Task ID | Name | ADR | Rules | Context | Contract | Tech | Layer | Tests | Deps | Env |
|:---|:---|:---|:---|:---|:---|:---|:---|:---|:---|:---|
| `B-TASK-001` | 对齐 `Task-001` 至 `Task-009` 真值 | N/A | `R-007`,`R-040`,`R-117` | `Core`,`Delivery` | 只回填阶段0状态与说明，不改历史目标 | `DOCS` | `docs` | 逐项证据核对 | `A-STORY-002` | docs-only |
| `B-TASK-002` | 补充状态判定标准 | N/A | `R-007`,`R-048` | `Core`,`Delivery` | 定义 Completed/Partial/Pending 判定口径 | `DOCS` | `docs` | 状态口径可复用且一致 | `B-TASK-001` | docs-only |
| `B-TASK-003` | 回填阶段0验证结果 | N/A | `R-040`,`R-117`,`R-119`,`R-124` | `Core`,`Val`,`Delivery` | 将真实构建/lint/test 结果回填阶段0 | `DOCS` | `docs` | 命令结果与文档一致 | `B-TASK-001` | dev/test |
| `B-TASK-004` | 补齐 ADR 实体文件清单与落地顺序 | `ADR-001`~`ADR-013` | `R-102`,`R-103`,`R-132` | `Core`,`ADR` | 建立并补齐 13 份 ADR 实体文件 | `DOCS` | `docs` | 编号、状态、索引、链接一致 | `A-STORY-001` | docs-only |
| `B-TASK-005` | 补齐华为云部署文档 | `ADR-004`,`ADR-002` | `R-109`,`R-110`,`R-130` | `Core`,`Deploy`,`Msg`,`Sec` | 新增华为云私有云部署规范 | `DOCS`,`OPS` | `docs`,`deployments/ci/scripts` | 文档存在、入口引用、配置语义检查 | `A-TASK-006` | prod-doc |
| `B-TASK-006` | 对齐交付记录 | N/A | `R-012`,`R-040`,`R-117` | `Core`,`Delivery` | 回填阶段0和修复批次交付元数据 | `DOCS` | `docs` | 交付记录与计划/tag一致 | `B-TASK-001` | docs-only |
| `B-TASK-007` | 记录 `R-101` 与 `R-062`/`R-154` 冲突 | N/A | `R-049`,`R-050`,`R-155` | `Core`,`Archive` | 保留冲突来源、历史语义和影响面 | `DOCS` | `docs` | 冲突项编号与来源完整 | `A-TASK-008` | docs-only |
| `B-TASK-008` | 形成归档目录澄清方案 | N/A | `R-101`,`R-062`,`R-154`,`R-155` | `Core`,`Archive` | 用追加规则澄清 raw-requirements 语义 | `DOCS` | `docs` | 规则、计划、lint 语义一致 | `B-TASK-007` | docs-only |
| `B-TASK-009` | 补全身份与角色模型 | `ADR-002`,`ADR-009` | `R-111`,`R-112`,`R-113` | `Core`,`Sec` | 定义身份上下文、角色与职责边界 | `DOCS` | `docs` | 字段、角色矩阵与场景完整 | `A-STORY-001` | prod-doc |
| `B-TASK-010` | 补全资源和数据范围控制 | `ADR-002`,`ADR-009` | `R-112`,`R-046`,`R-047` | `Core`,`Sec` | 定义资源模型、租户隔离和数据源授权顺序 | `DOCS` | `docs` | 资源模型和授权顺序检查 | `B-TASK-009` | prod-doc |
| `B-TASK-011` | 补全审计、敏感数据与失败处理 | `ADR-009` | `R-113`,`R-114`,`R-115`,`R-041` | `Core`,`Sec` | 定义审计字段、失败策略、敏感数据处理 | `DOCS` | `docs` | 验证场景与合规条目完整 | `B-TASK-010` | prod-doc |

## Phase-C

| Task ID | Name | ADR | Rules | Context | Contract | Tech | Layer | Tests | Deps | Env |
|:---|:---|:---|:---|:---|:---|:---|:---|:---|:---|:---|
| `C-TASK-001` | 盘点应抽取的公共能力 | `ADR-002` | `R-067`,`R-068`,`R-126` | `Core`,`ADR`,`Sec`,`Msg` | 列出 common 应承载的共性能力 | `JAVA-BE` | `common` | 清单与现有代码对照 | `Phase-B` | dev/test |
| `C-TASK-002` | 建立 common 包结构 | `ADR-002` | `R-021`,`R-022`,`R-067` | `Core`,`ADR` | 建立 common 包结构与边界 | `JAVA-BE` | `common` | 目录结构回归检查 | `C-TASK-001` | dev |
| `C-TASK-003` | 迁移重复或散落能力 | `ADR-002` | `R-067`,`R-068`,`R-126` | `Core`,`ADR` | 把共性能力迁移到 common | `JAVA-BE` | `common`,`application(controller/service)/domain/infrastructure` | `mvn clean compile` | `C-TASK-002` | dev/test |
| `C-TASK-004` | 对齐所有 `application-*.yml` 职责 | N/A | `R-066`,`R-128` | `Core`,`Msg`,`Sec`,`Deploy` | 明确 dev/test/prod 配置职责 | `JAVA-BE` | `application(controller/service)/domain/infrastructure` | `R-128` 四项检查 | `Phase-B` | dev/test/prod |
| `C-TASK-005` | 固化消息抽象接口实现路线 | `ADR-012` | `R-066`,`R-144` | `Core`,`Msg`,`ADR` | 统一消息接口、配置和实现切换 | `JAVA-BE` | `application(controller/service)/domain/infrastructure`,`common` | mock/database/kafka 契约测试 | `C-TASK-004` | dev/test/prod |
| `C-TASK-006` | 完成消息流管理接口验证 | `ADR-012` | `R-121`,`R-144` | `Core`,`Msg`,`Deploy` | 验证 retry/stats/manual smoke 管理接口 | `JAVA-BE`,`SQL`,`OPS` | `application(controller/service)/domain/infrastructure` | 管理接口、消息表、smoke | `C-TASK-005` | dev/test |
| `C-TASK-007` | 对齐现有分层 | `ADR-002` | `R-021`,`R-022`,`R-120` | `Core`,`ADR` | 修正治理服务到目标分层 | `JAVA-BE` | `application(controller/service)/domain/infrastructure` | 分层结构回归 | `Phase-B` | dev |
| `C-TASK-008` | 落实租户配置与访问占位能力 | `ADR-002`,`ADR-009` | `R-111`,`R-112`,`R-121` | `Core`,`Sec` | 保持 phase 0 最小租户校验闭环 | `JAVA-BE` | `application(controller/service)/domain/infrastructure` | 正常/异常/越权占位测试 | `C-TASK-007` | dev/test |
| `C-TASK-009` | 规划审计、数据源、调度扩展点 | `ADR-002`,`ADR-009` | `R-020`,`R-034`,`R-113` | `Core`,`ADR`,`Sec` | 定义管理服务扩展契约与骨架 | `JAVA-BE`,`DOCS` | `application(controller/service)/domain/infrastructure`,`docs` | 接口文档、错误码一致性 | `C-TASK-008` | dev/prod-doc |

## Phase-D

| Task ID | Name | ADR | Rules | Context | Contract | Tech | Layer | Tests | Deps | Env |
|:---|:---|:---|:---|:---|:---|:---|:---|:---|:---|:---|
| `D-TASK-001` | 固化查询执行服务边界 | `ADR-002`,`ADR-004`,`ADR-005`,`ADR-013` | `R-018`,`R-020`,`R-068` | `Core`,`ADR`,`Sec`,`Msg` | 定义查询执行服务职责与边界 | `JAVA-BE`,`DOCS` | `application(controller/service)/domain/infrastructure`,`docs` | 服务边界与 ADR 一致性 | 无 | dev/prod-doc |
| `D-TASK-002` | 定义联机查询接口 DTO/VO/错误码 | `ADR-004`,`ADR-005` | `R-041`,`R-057`,`R-121` | `Core`,`ADR`,`Sec` | 定义查询输入/输出/边界/错误码 | `JAVA-BE` | `application(controller/service)/domain/infrastructure` | 接口契约符合度 | `D-TASK-001` | dev/test |
| `D-TASK-003` | 实现最小同步执行闭环 | `ADR-004`,`ADR-005` | `R-042`,`R-045`,`R-119` | `Core`,`ADR`,`Sec`,`Msg` | 查询提交、执行、失败、降级最小闭环 | `JAVA-BE` | `application(controller/service)/domain/infrastructure` | 正常/超时/失败/降级测试 | `D-TASK-002` | dev/test |
| `D-TASK-004` | 增加异常回滚与运行日志 | `ADR-012` | `R-039`,`R-060`,`R-064`,`R-123` | `Core`,`ADR`,`Sec` | 增补异常回滚与关键日志 | `JAVA-BE` | `application(controller/service)/domain/infrastructure` | 入口/出口/异常日志抽查 | `D-TASK-003` | dev/test |
| `D-TASK-005` | 固化异步优化任务模型 | `ADR-005`,`ADR-013` | `R-020`,`R-068`,`R-121` | `Core`,`ADR` | 定义 parse/rewrite/acceleration suggestion 模型 | `JAVA-BE`,`DOCS` | `application(controller/service)/domain/infrastructure`,`docs` | 模型与契约一致 | 无 | dev/prod-doc |
| `D-TASK-006` | 实现任务提交与状态查询骨架 | `ADR-013` | `R-041`,`R-121`,`R-123` | `Core`,`ADR`,`Msg` | 异步优化任务提交与轮询接口 | `JAVA-BE` | `application(controller/service)/domain/infrastructure` | 提交/轮询/失败路径测试 | `D-TASK-005` | dev/test |
| `D-TASK-007` | 输出优化建议结构 | `ADR-013` | `R-121`,`R-057` | `Core`,`ADR` | 定义收益/成本/风险 VO | `JAVA-BE` | `application(controller/service)/domain/infrastructure` | 输出字段与文档一致 | `D-TASK-006` | dev/test |
| `D-TASK-008` | 定义压测任务与报告模型 | `ADR-007` | `R-045`,`R-121`,`R-127` | `Core`,`ADR`,`Sec` | 定义压测任务、阈值、报告模型 | `JAVA-BE`,`DOCS` | `application(controller/service)/domain/infrastructure`,`docs` | 模型与架构文档一致 | 无 | dev/prod-doc |
| `D-TASK-009` | 实现压测任务提交流程骨架 | `ADR-007` | `R-041`,`R-121`,`R-123` | `Core`,`ADR`,`Sec`,`Msg` | 异步压测提交流程 | `JAVA-BE` | `application(controller/service)/domain/infrastructure` | 提交/取消/失败测试 | `D-TASK-008` | dev/test |
| `D-TASK-010` | 实现报告查询接口 | `ADR-007` | `R-121`,`R-057` | `Core`,`ADR` | 定义 JSON/PDF/HTML 报告查询输出 | `JAVA-BE` | `application(controller/service)/domain/infrastructure` | 输出字段与契约一致 | `D-TASK-009` | dev/test |
| `D-TASK-011` | 补齐核心表与关联键设计 | `ADR-001`,`ADR-009`,`ADR-012` | `R-034`,`R-106`,`R-129` | `Core`,`ADR`,`Sec`,`Msg` | 定义 config/result/history/export/audit 关联键 | `SQL`,`JAVA-BE` | `application(controller/service)/domain/infrastructure`,`common` | schema/sql/entity 映射检查 | `Phase-C` | dev/test/prod |
| `D-TASK-012` | 落实审计日志链路 | `ADR-009`,`ADR-012` | `R-113`,`R-144` | `Core`,`ADR`,`Sec`,`Msg` | 建立 SQL/登录/权限变更审计链路 | `JAVA-BE`,`SQL` | `application(controller/service)/domain/infrastructure` | `R-113` 场景测试 | `D-TASK-011` | dev/test/prod |
| `D-TASK-013` | 落实敏感字段加密 | `ADR-009` | `R-114`,`R-128` | `Core`,`Sec`,`Deploy` | 定义并实现密码/token/key 加密策略 | `JAVA-BE`,`SQL`,`OPS` | `application(controller/service)/domain/infrastructure`,`common` | 库/日志/导出无明文 | `D-TASK-011` | test/prod |
| `D-TASK-014` | 收口异步服务鉴权、占位执行与审计兜底 | `ADR-009`,`ADR-012`,`ADR-013` | `R-041`,`R-111`~`R-115`,`R-123`,`R-144` | `Core`,`ADR`,`Sec`,`Msg` | 为异步服务补齐鉴权、租户隔离、占位执行与治理审计兜底 | `JAVA-BE`,`SQL` | `application(controller/service)/domain/infrastructure` | sql-optimization/benchmark/governance 模块测试与 task audit | `D-TASK-013` | dev/test/prod |
| `D-TASK-015` | 补完 `query-execution` 真实执行适配与结果聚合基线 | `ADR-004`,`ADR-005`,`ADR-012`,`ADR-013` | `R-018`,`R-020`,`R-041`,`R-042`,`R-045`,`R-121`,`R-123`,`R-126` | `Core`,`ADR`,`Sec`,`Msg` | 落实 Hetu 多模式执行适配、模式选择、结果聚合与审计证据 | `JAVA-BE` | `application(controller/service)/domain/infrastructure` | 模块测试、跨模式适配测试、runtime smoke | `D-TASK-014` | dev/test |
| `D-TASK-016` | 收口治理授权矩阵并下沉统一授权入口 | `ADR-002`,`ADR-009`,`ADR-012`,`ADR-013` | `R-018`,`R-020`,`R-041`,`R-046`,`R-111`~`R-115`,`R-121`,`R-123`,`R-126`,`R-144` | `Core`,`ADR`,`Sec`,`Msg` | 在 governance 落地角色矩阵、资源模型、数据源授权矩阵，把真实授权决策下沉为 query/sql-optimization/benchmark 统一入口，并补齐授权成功/拒绝/跨租户/吊销后访问与权限变更审计 | `JAVA-BE`,`SQL`,`OPS`,`DOCS` | `application(controller/service)/domain/infrastructure`,`deployments/ci/scripts`,`docs` | governance/三服务模块测试、runtime smoke、task audit、契约/安全文档同步 | `D-TASK-015` | dev/test/prod |
| `D-TASK-017` | 落实 `query-execution` 真实 Hetu 集成与 smoke 分层 | `ADR-004`,`ADR-005`,`ADR-012`,`ADR-013` | `R-018`,`R-020`,`R-041`,`R-042`,`R-045`,`R-046`,`R-111`~`R-115`,`R-121`,`R-123`,`R-126`,`R-128`,`R-141` | `Core`,`ADR`,`Sec`,`Msg`,`Deploy` | 让 HETU 主路径切到真实 JDBC/REST/CLIENT 接入，补齐 JDBC 驱动接线、Hetu client 协议执行、严格路由与错误语义，并提供 repo-closed runtime smoke 与 environment-backed Hetu/MRS smoke 入口 | `JAVA-BE`,`OPS`,`DOCS` | `application(controller/service)/domain/infrastructure`,`deployments/ci/scripts`,`docs` | query-execution 模块测试、跨模式适配测试、runtime smoke、Hetu env smoke 脚本/文档、task audit | `D-TASK-016` | dev/test/prod/test-env |

## Phase-E

| Task ID | Name | ADR | Rules | Context | Contract | Tech | Layer | Tests | Deps | Env |
|:---|:---|:---|:---|:---|:---|:---|:---|:---|:---|:---|
| `E-TASK-001` | 建立驾驶舱路由与导航骨架 | N/A | `R-023`,`R-025`,`R-030`,`R-124` | `Core`,`Front` | 建立 dashboard 主路径与导航壳层 | `VUE-FE` | `frontend/router/views/styles` | build、路由可达、i18n | `Phase-C` | dev |
| `E-TASK-002` | 落地项目全景/架构设计/进度管理板块 | N/A | `R-023`,`R-024`,`R-028` | `Core`,`Front`,`Delivery` | 驾驶舱展示项目与计划信息 | `VUE-FE` | `frontend/router/views/styles` | 页面结构与 IA 对照 | `E-TASK-001` | dev |
| `E-TASK-003` | 落地合规中心与规则库板块 | N/A | `R-028`,`R-029`,`R-111`~`R-115` | `Core`,`Front`,`Sec` | 只读展示规则与合规信息 | `VUE-FE` | `frontend/router/views/styles` | 可见性、只读性、构建 | `E-TASK-002` | dev |
| `E-TASK-004` | 建立五大业务页路由骨架 | N/A | `R-023`,`R-025`,`R-030` | `Core`,`Front` | 建立 5 个独立业务路由 | `VUE-FE` | `frontend/router/views/styles` | 每页独立入口、build | `E-TASK-001` | dev |
| `E-TASK-005` | 接入已存在治理接口能力 | `ADR-002` | `R-018`,`R-028`,`R-121` | `Core`,`Front`,`Sec` | 页面消费已交付后端能力 | `VUE-FE`,`JAVA-BE` | `frontend/router/views/styles` | UI/API 契约、可见性 | `E-TASK-004`,`Phase-C` | dev/test |
| `E-TASK-006` | 深色设计系统组件化 | N/A | `R-027`,`R-063`,`R-097`,`R-124` | `Core`,`Front` | 把主题 token 和组件规则转成实现 | `VUE-FE` | `frontend/router/views/styles` | token 生效、build、lint | `E-TASK-004` | dev |
| `E-TASK-007` | 扩展分离检查清单 | N/A | `R-015`,`R-018`,`R-019` | `Core`,`Front` | 强化前后端分离校验 | `DOCS`,`OPS` | `docs`,`deployments/ci/scripts` | 分离检查脚本通过 | `Phase-C` | dev/ci |
| `E-TASK-008` | 清理潜在越界逻辑 | N/A | `R-018`,`R-019`,`R-020` | `Core`,`Front`,`Sec` | 清理前端中的权威业务判断 | `VUE-FE`,`JAVA-BE` | `frontend/router/views/styles`,`application(controller/service)/domain/infrastructure` | 边界抽查、build、lint | `E-TASK-007` | dev/test |
| `E-TASK-009` | 建立临时 AI 交付进度页路由与展示骨架 | N/A | `R-023`,`R-024`,`R-029`,`R-166` | `Core`,`Front`,`Delivery` | 建立 `/delivery-progress` 临时只读页面，展示 AI 任务进度且与 `/dashboard` 分离 | `VUE-FE` | `frontend/router/views/styles` | 非生产路由可达、生产默认隐藏、展示源仅来自权威台账、build | `E-TASK-001`,`E-TASK-002`,`Phase-C` | dev/test/prod-switch |

## Phase-F

| Task ID | Name | ADR | Rules | Context | Contract | Tech | Layer | Tests | Deps | Env |
|:---|:---|:---|:---|:---|:---|:---|:---|:---|:---|:---|
| `F-TASK-001` | 补齐华为云部署文档 | `ADR-004`,`ADR-002` | `R-109`,`R-110`,`R-130` | `Core`,`Deploy`,`Sec`,`Msg` | 完整描述华为云部署拓扑和生产切换 | `DOCS`,`OPS` | `docs`,`deployments/ci/scripts` | 文档存在、入口索引、语义检查 | `B-TASK-005` | prod-doc |
| `F-TASK-002` | 对齐 compose 与脚本说明 | `ADR-004` | `R-076`,`R-130`,`R-144` | `Core`,`Deploy`,`Msg` | 对齐本地/离线/可选 Kafka 说明 | `OPS`,`DOCS` | `deployments/ci/scripts`,`docs` | `docker compose config`、文档一致性 | `F-TASK-001` | dev/prod-doc |
| `F-TASK-003` | 补齐环境提醒与恢复指引 | `ADR-009` | `R-004`,`R-108`,`R-115` | `Core`,`Deploy`,`Sec`,`Delivery` | 输出独立 MySQL/Kafka/恢复提醒 | `DOCS`,`OPS` | `docs`,`deployments/ci/scripts` | 文档与规则一致 | `F-TASK-002` | prod-doc |
| `F-TASK-004` | 盘点现有 CI 能力 | N/A | `R-075`,`R-117`,`R-148` | `Core`,`Val`,`Deploy` | 盘点 CI 对 lint/build/test 的覆盖 | `OPS`,`DOCS` | `deployments/ci/scripts`,`docs` | CI 清单完整性检查 | `Phase-C`,`Phase-E` | ci |
| `F-TASK-005` | 接入阶段门禁脚本化验证 | N/A | `R-116`,`R-117`,`R-118`,`R-141` | `Core`,`Val`,`Deploy` | 让阶段切换可阻断 | `OPS`,`DOCS` | `deployments/ci/scripts`,`docs` | 门禁阻断验证 | `F-TASK-004` | ci |
| `F-TASK-006` | 接入 Java 规范扫描 | N/A | `R-148`,`R-151`,`R-152` | `Core`,`Val`,`ADR` | 让 pmd/checkstyle 进入 CI | `OPS`,`JAVA-BE` | `deployments/ci/scripts`,`common` | `mvn validate pmd:pmd checkstyle:check` | `F-TASK-004` | ci/test |
| `F-TASK-007` | 补齐监控与日志规范落地清单 | `ADR-009` | `R-039`,`R-060`,`R-064` | `Core`,`Deploy`,`Sec` | 输出 logs/metrics/alerts 落地清单 | `DOCS`,`OPS` | `docs`,`deployments/ci/scripts` | 清单完整、映射一致 | `Phase-D` | prod-doc |
| `F-TASK-008` | 补齐备份恢复策略与演练记录模板 | `ADR-009`,`ADR-010` | `R-115`,`R-118` | `Core`,`Deploy`,`Sec`,`Delivery` | 定义 RPO/RTO/演练记录模板 | `DOCS`,`OPS` | `docs`,`deployments/ci/scripts` | 备份恢复模板可追溯 | `F-TASK-007` | prod-doc |
| `F-TASK-009` | 阶段交付回写闭环 | N/A | `R-012`,`R-040`,`R-117` | `Core`,`Delivery`,`Archive` | 完成记录、commit、tag、回写闭环 | `DOCS`,`OPS` | `docs`,`deployments/ci/scripts` | 交付记录与 git 元数据一致 | `F-TASK-008` | docs/ci |
| `F-TASK-010` | 接入运行时 smoke 到默认 CI | `ADR-012` | `R-075`,`R-117`,`R-130`,`R-141` | `Core`,`Val`,`Deploy`,`Msg` | 把 compose、启动探针和消息队列 smoke 接入默认 CI | `OPS`,`DOCS` | `deployments/ci/scripts`,`docs` | compose/runtime smoke/CI 通过 | `F-TASK-005`,`F-TASK-006` | ci/dev |
| `F-TASK-011` | 扩展多服务运行时 smoke 门禁 | `ADR-002`,`ADR-012` | `R-117`,`R-123`,`R-141` | `Core`,`Val`,`Deploy`,`Front` | 把多服务启动探针并入统一 runtime smoke | `OPS`,`DOCS` | `deployments/ci/scripts`,`docs` | multi-service runtime smoke | `F-TASK-010` | ci/dev |
| `F-TASK-012` | 扩展跨服务业务级 runtime smoke 门禁 | `ADR-002`,`ADR-012` | `R-117`,`R-123`,`R-126`,`R-141` | `Core`,`Val`,`Sec`,`Msg` | query-execution 到治理服务的业务级成功/失败/补偿门禁 | `OPS`,`JAVA-BE` | `deployments/ci/scripts`,`application(controller/service)/domain/infrastructure` | query-governance business smoke | `F-TASK-011` | ci/dev |
| `F-TASK-013` | 扩展优化与压测业务级 runtime smoke 门禁 | `ADR-002`,`ADR-012`,`ADR-013` | `R-117`,`R-123`,`R-126`,`R-141`,`R-144` | `Core`,`Val`,`Sec`,`Msg` | sql-optimization/benchmark-engine 到治理服务的业务级成功/失败/补偿门禁 | `OPS`,`JAVA-BE` | `deployments/ci/scripts`,`application(controller/service)/domain/infrastructure` | optimization/benchmark smoke | `F-TASK-012` | ci/dev |
| `F-TASK-014` | 扩展前端真实业务 runtime smoke 门禁 | N/A | `R-015`,`R-023`~`R-030`,`R-124`,`R-141` | `Core`,`Front`,`Val` | 浏览器驱动真实业务页并入默认 runtime gate | `VUE-FE`,`OPS` | `frontend/router/views/styles`,`deployments/ci/scripts` | lint/build/browser smoke | `F-TASK-013` | ci/dev |
| `F-TASK-015` | 推进 `sql-optimization` 真实持久化与调度链路 | `ADR-013` | `R-020`,`R-033`,`R-041`,`R-123`,`R-129` | `Core`,`ADR`,`Sec`,`Msg` | MySQL 任务表、状态流转、scheduled worker 与治理 smoke | `JAVA-BE`,`SQL` | `application(controller/service)/domain/infrastructure` | 模块测试与 manual smoke | `F-TASK-013` | dev/test |
| `F-TASK-016` | 推进 `benchmark-engine` 真实持久化与调度链路 | `ADR-007`,`ADR-012` | `R-020`,`R-033`,`R-041`,`R-123`,`R-129` | `Core`,`ADR`,`Sec`,`Msg` | MySQL 任务/报告表、scheduled worker 与治理 smoke | `JAVA-BE`,`SQL` | `application(controller/service)/domain/infrastructure` | 模块测试与 manual smoke | `F-TASK-015` | dev/test |
| `F-TASK-017` | 扩展前端失败恢复与审计补偿 runtime gate | N/A | `R-023`~`R-030`,`R-124`,`R-141` | `Core`,`Front`,`Val`,`Sec` | 把失败恢复和审计补偿可视化纳入浏览器门禁 | `VUE-FE`,`OPS` | `frontend/router/views/styles`,`deployments/ci/scripts` | browser runtime smoke | `F-TASK-016` | dev/test |
| `F-TASK-018` | 扩展 `system` 治理管理页 browser runtime gate | N/A | `R-023`,`R-028`,`R-124`,`R-141` | `Core`,`Front`,`Sec` | `system` 页接入 tenant-config、message stats、retry | `VUE-FE`,`JAVA-BE` | `frontend/router/views/styles`,`application(controller/service)/domain/infrastructure` | lint/build/browser smoke | `F-TASK-017` | dev/test |
| `F-TASK-019` | 加固前端补偿信号稳定性 | N/A | `R-024`,`R-028`,`R-124`,`R-141` | `Core`,`Front`,`Val` | 稳定 pending/total 双信号并收口残余验证日志 | `VUE-FE` | `frontend/router/views/styles` | lint/build/browser smoke | `F-TASK-018` | dev/test |
| `F-TASK-020` | 扩展治理历史页 browser runtime gate | N/A | `R-023`,`R-028`,`R-124`,`R-141` | `Core`,`Front`,`Sec` | `/parse-record` 真实历史诊断链路 | `VUE-FE`,`JAVA-BE` | `frontend/router/views/styles`,`application(controller/service)/domain/infrastructure` | governance test + browser smoke | `F-TASK-019` | dev/test |
| `F-TASK-021` | 扩展治理历史修复追溯页 browser runtime gate | N/A | `R-023`,`R-028`,`R-124`,`R-141` | `Core`,`Front`,`Sec` | `/repair-evidence` 修复追溯链路 | `VUE-FE`,`JAVA-BE` | `frontend/router/views/styles`,`application(controller/service)/domain/infrastructure` | governance test + browser smoke | `F-TASK-020` | dev/test |
| `F-TASK-022` | 升级治理长期历史反查与分页追溯 | `ADR-009` | `R-033`,`R-038`,`R-121`,`R-129`,`R-141` | `Core`,`Sec`,`Val` | indexed history query、分页与旧窗口追溯 | `JAVA-BE`,`SQL`,`VUE-FE` | `application(controller/service)/domain/infrastructure`,`frontend/router/views/styles` | governance test + browser smoke | `F-TASK-021` | dev/test |
| `F-TASK-023` | 扩展历史诊断与审计取证分页链路 | `ADR-009` | `R-033`,`R-038`,`R-121`,`R-141` | `Core`,`Sec`,`Front` | `/audit-forensics` 与分页 drill-through | `JAVA-BE`,`VUE-FE` | `application(controller/service)/domain/infrastructure`,`frontend/router/views/styles` | governance test + browser smoke | `F-TASK-022` | dev/test |
| `F-TASK-024` | 新增审计故障处置与修复决策页 | `ADR-009`,`ADR-012` | `R-023`,`R-028`,`R-121`,`R-141` | `Core`,`Front`,`Sec`,`Msg` | remediation decision page 与修复动作运行链 | `VUE-FE`,`JAVA-BE` | `frontend/router/views/styles`,`application(controller/service)/domain/infrastructure` | governance test + browser smoke | `F-TASK-023` | dev/test |
| `F-TASK-025` | 补齐治理归档历史窗口与深分页链路 | `ADR-009` | `R-033`,`R-038`,`R-121`,`R-129`,`R-141` | `Core`,`Sec`,`Front` | archival-window query、深分页与跨页 drill-through | `JAVA-BE`,`SQL`,`VUE-FE` | `application(controller/service)/domain/infrastructure`,`frontend/router/views/styles` | governance test + runtime smoke | `F-TASK-024` | dev/test |
| `F-TASK-026` | 接入真实 Kafka 运行验证与环境安全参数门禁 | `ADR-012` | `R-117`,`R-118`,`R-141`,`R-144` | `Core`,`Msg`,`Deploy`,`Sec` | Kafka bootstrap/security 校验、成功/恢复 smoke | `OPS`,`JAVA-BE` | `deployments/ci/scripts`,`application(controller/service)/domain/infrastructure` | Kafka runtime gate | `F-TASK-025` | ci/dev |
| `F-TASK-027` | 收口 Phase-F 退出门禁缺口 | `ADR-009`,`ADR-012` | `R-117`,`R-118`,`R-130`,`R-141`,`R-151` | `Core`,`Val`,`Deploy`,`Sec` | DB script、coverage、Sonar 与 R-118 证据收口 | `OPS`,`DOCS`,`JAVA-BE` | `deployments/ci/scripts`,`docs`,`common` | phase gate / DB / compliance baseline | `F-TASK-026` | ci/dev/prod-doc |
| `F-TASK-028` | 拆分主线业务与治理运维页面路径 | N/A | `R-023`~`R-030`,`R-124`,`R-141` | `Core`,`Front` | 主业务路由与治理 history/ops namespace 拆分 | `VUE-FE` | `frontend/router/views/styles` | lint/build/browser routing | `F-TASK-024` | dev/test |
| `F-TASK-029` | 收口 release automation 与门禁稳定性 | `ADR-009`,`ADR-012` | `R-012`,`R-075`,`R-117`,`R-118`,`R-141`,`R-151` | `Core`,`Val`,`Deploy`,`Delivery` | 稳定 coverage 入口、明确 Sonar 强制约束、并把 phase gate 绑定到 release metadata 自动触发链 | `OPS`,`DOCS`,`JAVA-BE` | `deployments/ci/scripts`,`docs`,`application(controller/service)/domain/infrastructure` | release gate workflow、`bash scripts/run-coverage.sh --phase phase1plus`、Sonar-required gate 路径与 CI/workflow 验证 | `F-TASK-027`,`F-TASK-028` | ci/dev/prod-doc |
| `F-TASK-030` | 提升覆盖率并补齐 Sonar 发布环境 | `ADR-009`,`ADR-012` | `R-075`,`R-117`,`R-118`,`R-141`,`R-151`,`R-157` | `Core`,`Val`,`Deploy`,`Delivery` | 把 phase1plus 聚合覆盖率提升到 85%+，补齐 Sonar 所需 secrets / 发布环境接线，并验证自动 release gate 可稳定放行 | `OPS`,`DOCS`,`JAVA-BE` | `deployments/ci/scripts`,`docs`,`application(controller/service)/domain/infrastructure` | `bash scripts/run-coverage.sh --phase phase1plus`、`bash scripts/run-sonar.sh --require-config`、release gate workflow / phase gate 验证 | `F-TASK-029` | ci/dev/prod-doc |
| `F-TASK-031` | 将 Sonar 与环境级门禁降级为 fallback，并建立双层门禁语义 | `ADR-009`,`ADR-012` | `R-075`,`R-117`,`R-118`,`R-141`,`R-144`,`R-156`,`R-157` | `Core`,`Val`,`Deploy`,`Delivery`,`Msg` | 把 repo-closed 主路径与 environment-backed 增强项显式拆层，修正 Sonar / 真实 Kafka / release gate 默认语义，同时保留独立脚本与 workflow 作为 fallback 入口 | `OPS`,`DOCS` | `deployments/ci/scripts`,`docs` | `python3 scripts/foreman.py validate F-TASK-031`、`bash scripts/run-phase-gates.sh --gate entry`、`bash scripts/run-phase-gates.sh --gate delivery --coverage-phase phase1plus`、`bash scripts/run-phase-gates.sh --gate compliance`、`bash scripts/run-sonar.sh`、`bash scripts/run-sonar.sh --require-config`、`node scripts/lint-repository-knowledge.js` | `F-TASK-030` | ci/dev/prod-doc |
| `F-TASK-032` | 去除 Sonar fallback 的隐性自动恢复接线，并分离 provisioning / enable 语义 | `ADR-009`,`ADR-012` | `R-075`,`R-117`,`R-118`,`R-141`,`R-156`,`R-157` | `Core`,`Val`,`Deploy`,`Delivery` | 去除 release workflow 默认 `quality-gate` environment 绑定，给主 CI 增加显式 Sonar enable 条件，并同步 Sonar runbook、INBOX 与部署基线，使 provisioning 不再等于自动恢复强制 Sonar | `OPS`,`DOCS` | `deployments/ci/scripts`,`docs` | `python3 scripts/foreman.py validate F-TASK-032`、`python3 scripts/task_audit.py --check --phase pre-closeout`、`bash scripts/run-sonar.sh`、`bash scripts/run-sonar.sh --require-config`、`node scripts/lint-repository-knowledge.js`、workflow / docs 语义核对 | `F-TASK-031` | ci/prod-doc |
| `F-TASK-033` | 补齐测试环境最小 smoke 门禁 | `ADR-002`,`ADR-012` | `R-075`,`R-117`,`R-123`,`R-126`,`R-141`,`R-156`,`R-157` | `Core`,`Val`,`Deploy`,`Sec`,`Msg` | 提供环境无关的最小 smoke 入口，供外部测试环境 CI/CD 在部署后执行四个后端 health、前端可达性、query/sql-optimization/benchmark 到 governance 的最小业务链路，以及受保护请求头有效性验证；本地 runtime smoke 契约保持不变 | `OPS`,`DOCS` | `deployments/ci/scripts`,`docs` | `python3 scripts/foreman.py validate F-TASK-033`、`python3 scripts/task_audit.py --check --phase pre-closeout`、`bash scripts/run-env-smoke.sh --help`、`bash scripts/run-env-smoke.sh --check-config`、`bash scripts/run-runtime-smoke.sh --compose-check`、`node scripts/lint-repository-knowledge.js` | `F-TASK-032` | dev/test-env/prod-doc |

## Completeness Statement

- 当前主计划中的 Task 已全部补齐 10 个 Harness 字段。
- 若后续新增 Task，必须先在主计划新增，再同步补充到本矩阵。
- 若任务需要显式维护人工确认点、数据影响和回滚策略，继续同步更新 `docs/plans/task-governance-extension-matrix.md`。
