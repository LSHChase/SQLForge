# SQLForge Document Truth Baseline

## Summary

本文件用于把 SQLForge 当前仓库中的“已实现事实”“已确认目标”“历史记录”“归档原文”和“待实现缺口”分层，避免后续编码把目标架构、初始化占位和当前实现混写成同一层事实。

当前基线时间：`2026-04-20`

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
- Maven 聚合工程存在三个后端模块：
  - `sqlforge-common/`
  - `governance-service/`
  - `query-execution-service/`
- `sqlforge-common/` 已承载共享底座基线：
  - 统一错误码常量
  - 请求/租户上下文
  - 共享异常模型
  - 审计事件契约
  - 公共配置契约与通用工具
- `governance-service/` 已具备最小治理基线：
  - Spring Boot 应用入口
  - controller / application service / domain / infrastructure 分层骨架
  - 受保护接口的统一请求上下文拦截器，当前要求完整 `X-*` 请求上下文字段
  - `DATABASE` / `MOCK` 可运行的消息抽象基线，以及 `KAFKA` 客户端接入基线
  - 内部治理契约入口（租户范围、数据源访问、审计写入、调度扩展点）
  - 数据库消息管理接口（`retry` / `stats`），并限制在 `DATABASE` 模式下使用
  - MyBatis XML
  - 多环境配置
  - 基础测试
- `query-execution-service/` 已具备查询执行边界基线：
  - Spring Boot 应用入口与独立 Maven 模块
  - `application` / `domain` / `infrastructure` / `config` 分层骨架
  - 查询执行边界定义，显式固化路由、执行控制、轻量解析、轻量改写和已批准加速配置应用
  - `JDBC` / `REST` / `CLIENT` 三种 Hetu 访问模式的承载边界
  - 独立多环境配置与日志配置骨架
  - 基础单元测试
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
- 当前 `governance-service` 只代表“公共管理服务”的阶段性实现基线，不代表其他服务已经实现。
- `sqlforge-common` 只能承载跨服务公共能力，不能演化成承载业务逻辑的杂项仓。
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
| `docs/architecture/persistence.md` | `docs/architecture/init.md`, `docs/adr/ADR-001-order-service-database-selection-example.md`, `docs/adr/ADR-009-data-retention-and-destruction.md` | 持久化与保留策略以当前组合文档消费 |
| `docs/api/versioning.md` | `docs/architecture/init.md` | API 版本控制规则已在初始化文档中固化，尚未单拆 |
| `docs/git/commit-convention.md` | `docs/operations/git-and-task-closeout.md`, `docs/rules/codex-rules.md` | Git closeout 与 Conventional Commits 由运维文档和规则库共同约束 |

## Consumption Rules

1. 需要判断“仓库现在有什么”时，只能使用 `Implemented Fact` 层。
2. 需要判断“项目最终应演进到什么边界”时，使用 `Confirmed Target` 层。
3. 需要追溯“为什么会有这条规则或这段文本”时，回看 `Historical Record` 与 `Archive Source` 层。
4. 如果初始化文档中的目标落点与当前仓库文档路径不一致，优先采用本文件中的 `Current authority` 映射。
5. 任何尚未被代码、测试、脚本或仓库结构证明的内容，都必须明确标为 `Pending Gap`，不得写成已交付事实。

## Immediate Pending Gaps

- SQL 优化服务、压测引擎服务仍未建立独立代码模块。
- 查询执行服务已建立独立模块骨架、公共 HTTP DTO/VO/错误码，并已落地最小同步执行闭环；真实治理调用、真实引擎适配器、异常回滚和运行日志仍待 `Phase-D` 后续任务补齐。
- 访问控制当前仍是“最小租户校验基线”，尚未形成完整角色矩阵和数据源授权实现。
- `KAFKA` 模式虽已接入真实客户端，但尚未沉淀真实 Kafka 集群运行验证、鉴权和安全参数配置证据。
- 治理扩展点当前仍以租户范围、数据源访问、审计写入、调度状态契约为主，未演进为完整治理中心能力。
- 前端当前仍以基础壳层为主，尚未落成完整驾驶舱与五大业务页。

## Related Documents

- `docs/architecture/init.md`
- `docs/adr/ADR-002-microservice-splitting-and-bounded-contexts.md`
- `docs/plans/master-execution-plan.md`
- `docs/references/human-constraint-history.md`
