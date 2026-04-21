# SQLForge Task Specification Matrix

本文件用于把 [master-execution-plan.md](/models/project/codex/SQLForge/docs/plans/master-execution-plan.md) 中的 59 个 Task 补齐为 Harness Engineering 要求的 10 项字段。

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

## Completeness Statement

- 当前主计划中的 59 个 Task 已全部补齐 10 个 Harness 字段。
- 若后续新增 Task，必须先在主计划新增，再同步补充到本矩阵。
- 若任务需要显式维护人工确认点、数据影响和回滚策略，继续同步更新 `docs/plans/task-governance-extension-matrix.md`。
