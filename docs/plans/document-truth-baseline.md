# SQLForge Document Truth Baseline

## Summary

本文件用于把 SQLForge 当前仓库中的“已实现事实”“已确认目标”“历史记录”“归档原文”和“待实现缺口”分层，避免后续编码把目标架构、初始化占位和当前实现混写成同一层事实。

当前基线时间：`2026-04-21`

## Engineering Naming Mapping

| Historical engineering name | Current engineering name | Notes |
|:---|:---|:---|
| `governance-service` | `governance` | 公共管理服务模块；业务文案仍可写“governance 服务” |
| `query-execution-service` | `query-execution` | 查询执行服务模块；工程标识不再追加 `Service` |
| `sqlforge-common` | `sqlforge-shared` | 共享底座模块；保持只承载真正公共能力 |

本映射是当前仓库关于模块与工程命名的唯一权威映射。历史文档、台账、验证日志与脚本如已重写为当前工程命名，仍以上表作为旧名追溯基线。

## Truth Layers

| Layer | Meaning | Can be used as current fact | Typical sources |
|:---|:---|:---|:---|
| `Implemented Fact` | 已经被仓库文件、结构、脚本或测试证明的现状 | Yes | 代码、配置、脚本、构建结果、验证日志 |
| `Confirmed Target` | 已被人类确认的目标边界，但尚未全部实现 | No | `ADR`、主执行计划、确认台账 |
| `Historical Record` | 历史阶段计划、初始化描述、交付记录 | No | `phase-0-plan.md`、初始化文档、交付记录 |
| `Archive Source` | 原始资料、原始规范、快照与来源元数据 | No | `docs/references/raw-requirements/` |
| `Pending Gap` | 文档已声明但仓库尚未完整实现的能力 | No | 主执行计划、真值差异记录 |

## Verified Current Facts

- 根级前端工程存在且可构建：
  - `package.json`
  - `vite.config.js`
  - `src/`
- Maven 聚合工程存在五个后端模块：
  - `sqlforge-shared/`
  - `governance/`
  - `query-execution/`
  - `sql-optimization/`
  - `benchmark-engine/`
- `sqlforge-shared/` 已承载共享底座基线：
  - 统一错误码常量
  - 请求/租户上下文
  - 共享异常模型
  - 审计事件契约
  - 公共配置契约与通用工具
- `governance/` 已具备最小治理基线：
  - Spring Boot 应用入口
  - `application` 包域下的 controller / service / domain / infrastructure 分层骨架
  - 受保护接口的统一请求上下文拦截器，当前要求完整 `X-*` 请求上下文字段
  - `DATABASE` / `MOCK` 可运行的消息抽象基线，以及 `KAFKA` 客户端接入基线
  - 内部治理契约入口（租户范围、数据源访问、审计写入、调度扩展点）
  - `audit/write` 已同步落库到 `audit_log`，并支持可选 `configSnapshotId/resultId/historyId/exportId` 追溯键
  - header-based stateless auth 的 `LOGIN` / `LOGOUT` 审计落库基线
  - 共享 AES-256 敏感字段加密与脱敏组件
  - `GovernanceProtectedPersistenceService` 已把 `config_snapshot` / `execution_result` / `query_history` / `export_record` / `audit_log` / `system_config` 接到统一受保护写入入口
  - `system_config` 已补齐 `sensitive_flag/value_ciphertext/value_mask/encryption_*` 列基线，用于密码 / token / key 类配置的密文存储
  - 数据库消息管理接口（`retry` / `stats`），并限制在 `DATABASE` 模式下使用
  - `config_snapshot` / `execution_result` / `query_history` / `export_record` / `audit_log` 的核心追溯链 schema、Entity 与 MyBatis XML 骨架
  - `sql/migrations/V20260421_011__core_traceability_chain.sql` 增量脚本
  - `docs/architecture/persistence.md` 持久化权威文档
  - 多环境配置
  - 基础测试
- `query-execution/` 已具备查询执行边界基线：
  - Spring Boot 应用入口与独立 Maven 模块
  - `application` 包域下的 controller / service 与 `domain` / `infrastructure` / `config` 分层骨架
  - 查询执行边界定义，显式固化路由、执行控制、轻量解析、轻量改写和已批准加速配置应用
  - `JDBC` / `REST` / `CLIENT` 三种 Hetu 访问模式的承载边界
  - 独立多环境配置与日志配置骨架
  - 基础单元测试
- `sql-optimization/` 已具备 SQL 优化提交/轮询骨架：
  - Spring Boot 应用入口与独立 Maven 模块
  - `application` 包域下的 controller / DTO / VO / service 与 `domain` / `infrastructure` / `config` 分层骨架
  - `PARSE` / `REWRITE` / `ACCELERATION_SUGGESTION` 三类异步优化任务实体
  - 生命周期状态、处理阶段流转、优先级、解析深度和加速建议类型的领域模型
  - `POST /api/sql-optimization/tasks` 与 `GET /api/sql-optimization/tasks/{taskId}` skeleton
  - in-memory placeholder repository、提交流程日志、失败路径和基础测试
  - `suggestion / failure` 结构化输出，当前已覆盖收益、成本、风险和任务类型差异
- `benchmark-engine/` 已具备压测任务与报告查询骨架：
  - Spring Boot 应用入口与独立 Maven 模块
  - `application` 包域下的 controller / DTO / VO / service 与 `domain` / `infrastructure` / `config` 分层骨架
  - `BASELINE` / `COMPARISON` / `REGRESSION_GUARD` 三类压测任务实体
  - 生命周期状态、处理阶段流转、影子环境模式、只读要求、脱敏要求和阈值模型
  - `POST /api/benchmark-engine/tasks` 与 `GET /api/benchmark-engine/tasks/{taskId}` skeleton
  - `GET /api/benchmark-engine/reports/{reportId}` skeleton，支持 `format=JSON|PDF|HTML`
  - in-memory placeholder repository、提交流程日志、失败路径、占位报告落库、报告查询和基础测试
  - 引擎指标快照、阈值判定结果、趋势图表、建议输出和报告实体
  - 基础模型测试、应用服务测试与控制器测试
- 当前已验证通过：
  - `mvn clean compile`
  - `mvn test`
  - `mvn validate pmd:pmd checkstyle:check`
  - `node scripts/lint-repository-knowledge.js`
  - `node scripts/check-frontend-backend-separation.js`
  - `npm run build`
  - `npm run lint`

## Confirmed Targets

- 最终服务目标固定为 `4` 个微服务，而不是初始化来源中的 `11` 个原始服务。
- 当前 `governance` 只代表“公共管理服务”的阶段性实现基线，不代表其他服务已经实现。
- `sqlforge-shared` 只能承载跨服务公共能力，不能演化成承载业务逻辑的杂项仓。
- 访问控制、租户隔离、审计、敏感信息加密和备份恢复已经形成完整文档规格，但代码尚未全部落地。

## Historical Records That Must Not Be Misread

- `docs/architecture/init.md` 是初始化总基线，保留了来源服务映射、目标架构和阶段0要求；它不是“当前全部已实现状态”。
- `docs/plans/phase-0-plan.md` 是历史阶段计划，用于对照，不应替代主执行计划。
- `docs/deliveries/init-completion.md` 记录的是初始化交付闭环，不等于后续阶段已交付。
- `docs/references/raw-requirements/` 中的资料用于追溯原始来源，不直接替代当前权威口径。

## Drift Matrix

下表记录初始化文档中的“目标落点”与当前仓库中“实际权威文档”的差异。该差异不改变规则语义，只用于当前执行时的正确消费。

| Initialization target path | Current authority | Current interpretation |
|:---|:---|:---|
| `docs/references/codex-rules.md` | `docs/rules/codex-rules.md` | 规则库已迁移到 `docs/rules/`，后续统一以现路径为准 |
| `docs/architecture/deployment.md` | `docs/deployments/local-setup.md`, `docs/deployments/offline-setup.md`, `docs/deployments/huawei-cloud-setup.md` | 部署文档已拆为多场景文档 |
| `docs/tech-stack.md` | `docs/architecture/init.md`, `docs/references/human-constraint-history.md` | 技术栈已在初始化文档和历史账本中固化，尚未单拆独立文档 |
| `docs/architecture/backend-layers.md` | `docs/architecture/init.md` | 后端分层规则当前仍以初始化文档为权威 |
| `docs/architecture/package-structure.md` | `docs/architecture/init.md` | 包结构规则当前仍以内嵌规则表为权威 |
| `docs/frontend/page-layout.md` | `docs/frontend/design-system.md`, `docs/architecture/init.md` | 页面信息架构已在设计系统和初始化规则中落地 |
| `docs/frontend/data-display.md` | `docs/frontend/design-system.md`, `docs/architecture/init.md` | 数据展示原则已合并到现有设计文档 |
| `docs/architecture/persistence.md` | `docs/architecture/persistence.md` | 持久化与保留策略已有独立权威文档 |
| `docs/api/versioning.md` | `docs/architecture/init.md` | API 版本控制规则已在初始化文档中固化，尚未单拆 |
| `docs/git/commit-convention.md` | `docs/operations/git-and-task-closeout.md`, `docs/rules/codex-rules.md` | Git closeout 与 Conventional Commits 由运维文档和规则库共同约束 |

## Consumption Rules

1. 需要判断“仓库现在有什么”时，只能使用 `Implemented Fact` 层。
2. 需要判断“项目最终应演进到什么边界”时，使用 `Confirmed Target` 层。
3. 需要追溯“为什么会有这条规则或这段文本”时，回看 `Historical Record` 与 `Archive Source` 层。
4. 如果初始化文档中的目标落点与当前仓库文档路径不一致，优先采用本文件中的 `Current authority` 映射。
5. 任何尚未被代码、测试、脚本或仓库结构证明的内容，都必须明确标为 `Pending Gap`，不得写成已交付事实。

## Immediate Pending Gaps

- 压测引擎服务已建立独立 `benchmark-engine` 模块与提交/轮询/报告查询 API skeleton，但真实执行链路、真实导出链路和跨服务协同仍待 `Phase-D` 后续任务补齐。
- 查询执行服务已建立独立模块骨架、公共 HTTP DTO/VO/错误码、最小同步执行闭环，以及本地流程日志与 timeout/fallback 恢复标记；真实治理调用、真实引擎适配器和跨服务审计补偿仍待 `Phase-D` 后续任务补齐。
- SQL 优化服务已建立独立模块与提交/轮询 API skeleton，但 MySQL 持久化、队列调度、回调通知和建议结果明细仍待 `Phase-D` 后续任务补齐。
- Phase-D 核心追溯链已在 `governance` 内完成 schema、migration、entity 与 mapper XML 固化，且 `audit/write` 与 header-based stateless auth 已接入真实 `audit_log` 落库；当前敏感字段加密基线已进入共享组件和治理受保护持久化入口，但查询执行、SQL 优化、压测引擎等其他服务的主动上报链仍待后续任务补齐。
- 访问控制当前仍是“最小租户校验基线”，尚未形成完整角色矩阵和数据源授权实现。
- `KAFKA` 模式虽已接入真实客户端，但尚未沉淀真实 Kafka 集群运行验证、鉴权和安全参数配置证据。
- 治理扩展点当前仍以租户范围、数据源访问、审计写入、调度状态契约为主，未演进为完整治理中心能力。
- 前端当前仍以基础壳层为主，尚未落成完整驾驶舱与五大业务页。

## Related Documents

- `docs/architecture/init.md`
- `docs/adr/ADR-002-microservice-splitting-and-bounded-contexts.md`
- `docs/plans/master-execution-plan.md`
- `docs/references/human-constraint-history.md`
