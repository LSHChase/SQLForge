# SQLForge Master Execution Plan

## 1. Summary

- 目标：在不改写既有规则语义的前提下，基于当前仓库事实、`docs/` 全量文档、规则库、ADR 索引、原始资料归档与现有代码，形成可直接执行的全量主计划。
- 计划方法：严格遵循 Harness Engineering，先固化目标、约束、接口边界、验证方案、依赖关系和人工确认项，再拆解为阶段、Epic、Story、Task。
- 计划边界：
  - 本文档是当前执行主控文档。
  - [phase-0-plan.md](./phase-0-plan.md) 作为历史阶段计划保留，不覆盖其历史语义。
  - 本文档不把“目标架构”写成“已实现事实”，所有状态均按仓库现状和已执行检查标注。

## 2. Evidence Basis

### 2.1 Repository-wide document set

- 主执行计划直接消费的权威文档：
  - `docs/README.md`
  - `docs/architecture/init.md`
  - `docs/rules/codex-rules.md`
  - `docs/quality/validation-rules.md`
  - `docs/architecture/messaging-abstraction.md`
  - `docs/frontend/design-system.md`
  - `docs/quality/alibaba-java-guidelines.md`
  - `docs/quality/frontend-backend-separation-baseline.md`
  - `docs/security/compliance.md`
  - `docs/security/access-control-spec.md`
  - `docs/adr/README.md`
  - `docs/deployments/local-setup.md`
  - `docs/deployments/offline-setup.md`
  - `docs/deployments/huawei-cloud-setup.md`
  - `docs/references/human-constraint-history.md`
  - `docs/deliveries/init-completion.md`
  - `docs/plans/README.md`
  - `docs/plans/phase-0-plan.md`
- 全量 `docs/` 文件盘点、角色分类和消费状态见：
  - [document-coverage-matrix.md](./document-coverage-matrix.md)
- 全部 59 个 Task 的 Harness 10 字段补全集见：
  - [task-spec-matrix.md](./task-spec-matrix.md)

### 2.2 Repository facts already verified

- `node scripts/lint-repository-knowledge.js` passed
- `node scripts/check-frontend-backend-separation.js` passed
- `mvn -B test` passed
- `npm run build` passed
- `npm run lint` passed

### 2.3 Current implementation facts

- 当前仓库存在根级前端工程，入口为 `package.json`、`vite.config.js`、`src/`
- 当前 Maven 聚合工程包含 `sqlforge-common/` 与 `governance-service/`
- `sqlforge-common/` 当前只有 `pom.xml`，尚无源码
- `governance-service/` 已有基础应用、controller、application service、最小租户校验能力、MyBatis XML、配置文件和基础测试
- `docs/adr/` 已按 `ADR-001` 至 `ADR-013` 补齐实体文件
- `docs/deployments/` 已补齐 `huawei-cloud-setup.md`

### 2.4 Decisions confirmed after initial plan publication

- `HC-001` 已确认：最终服务目标固定为 4 个微服务
- `HC-003` 已确认并执行：补齐 `ADR-001` 至 `ADR-013`
- `HC-005` 已确认并执行：补齐华为云部署文档，并与 `R-144` 生产 Kafka 模式对齐
- `HC-006` 已确认并执行：访问控制文档补全为完整规格

## 3. Planning Rules

- 所有阶段、Epic、Story、Task 必须符合 Harness Engineering。
- 所有 Task 必须包含：
  - `任务ID与名称`
  - `关联ADR编号`
  - `关联规则编号`
  - `上下文引用`
  - `接口契约`
  - `技术约束`
  - `分层定位`
  - `测试策略`
  - `依赖声明`
  - `环境要求`
- 所有 Task 必须在实现前写出验证方案，验证必须真实执行并回写结果。
- 所有跨步骤、跨前后端、跨领域工作必须先更新本计划或对应阶段计划。
- 所有不确定、冲突、缺失项必须先进入 `Human Confirmation Ledger`，未经人工确认不得擅自改变规则、边界或历史语义。

## 4. Phase Overview

| Phase | Name | Goal | Entry Gate | Exit Gate |
|:---|:---|:---|:---|:---|
| `Phase-A` | 基线对齐与计划治理 | 建立主计划、追踪矩阵、确认台账、现状真值 | `R-116` | 文档与计划体系一致，待确认项收敛 |
| `Phase-B` | 阶段0修正与缺口补齐 | 修正阶段0真值、补齐强制文档与 ADR 缺口 | `R-116` | 阶段0状态、交付记录、必备文档一致 |
| `Phase-C` | 共享底座与公共治理服务 | 完善 common、公用配置、消息抽象、治理服务底座 | `R-116` | 后端共享底座和治理服务达到可扩展基线 |
| `Phase-D` | 核心业务服务实现 | 分阶段落地查询执行、SQL优化、压测、数据治理能力 | `R-116` | 核心接口和服务边界可验证 |
| `Phase-E` | 前端驾驶舱与业务页面 | 按任务流完成驾驶舱和业务页面分离 | `R-116` | 前后端分离与页面信息架构达标 |
| `Phase-F` | 部署、运维、生产就绪 | 完善部署、运维、合规、门禁与交付闭环 | `R-116` | `R-117`、`R-118`、CI、部署文档和演练达标 |

## 5. Traceability Matrix

| Source | Governs | Current repository target | Execution impact | Verification anchor |
|:---|:---|:---|:---|:---|
| `docs/architecture/init.md` | 总体架构、阶段、服务边界、任务拆解模板 | 全仓主基线 | 决定阶段拆解、服务边界、接口优先顺序 | `R-116`, `R-121` |
| `docs/rules/codex-rules.md` | 执行规则、工程约束、验证规则 | 全仓 | 决定可变更边界与验证门禁 | `R-117` 至 `R-154` |
| `docs/quality/validation-rules.md` | 阶段/Task/回归/环境验证 | 全仓 | 决定所有 Task 的验证格式 | `R-119` 至 `R-154` |
| `docs/security/compliance.md` | 等保与审计 | 后端、部署、运维 | 决定身份、租户、审计、加密、备份 | `R-111` 至 `R-118` |
| `docs/security/access-control-spec.md` | 完整访问控制规格与阶段实现基线 | `governance-service` 及后续 4 微服务 | 决定身份、角色、资源、租户、审计和失败处理边界 | `R-111` 至 `R-115` |
| `docs/architecture/messaging-abstraction.md` | 消息抽象与环境切换 | `governance-service`, SQL, 配置, 脚本 | 决定 `DATABASE/MOCK/KAFKA` 模式 | `R-144` |
| `docs/frontend/design-system.md` | 前端视觉与组件基线 | `src/` | 决定 Dashboard 和业务页视觉风格 | `R-023` 至 `R-030` |
| `docs/quality/alibaba-java-guidelines.md` | Java 代码规范治理 | `sqlforge-common/`, `governance-service/` | 决定 Java 实现方式与扫描要求 | `R-145` 至 `R-154` |
| `docs/quality/frontend-backend-separation-baseline.md` | 前后端边界 | 根级前端与 Maven 后端 | 决定目录和职责边界 | 分离检查脚本 |
| `docs/plans/phase-0-plan.md` | 历史阶段0任务顺序 | 阶段0基线对照 | 用于真值修正，不直接代表当前全部现状 | `HC-002` |
| `docs/deliveries/init-completion.md` | 阶段0交付记录 | 初始化交付基线 | 用于交付状态与 Tag 回写对齐 | `HC-007` |
| `docs/adr/README.md` | 决策索引 | `docs/adr/` | 决定 ADR 落地缺口与优先级 | `HC-003` |
| `docs/plans/document-coverage-matrix.md` | `docs/` 全量文件覆盖证明 | `docs/` | 证明所有文档与归档资料已被盘点和分类 | 文档全量覆盖验证 |
| `docs/plans/task-spec-matrix.md` | Harness Task 字段补全集 | 全部 59 个 Task | 为每个 Task 补齐 10 个必填字段 | Task 规格完整性验证 |

## 6. Execution Breakdown

### Phase-A 基线对齐与计划治理

#### Epic `A-EPIC-001` 主计划与知识追踪基线

- 目标：建立全局主计划、计划索引、规则/文档/代码/验证追踪矩阵与人工确认台账。
- 成功标准：
  - 主计划和计划索引入库
  - 每个阶段都有明确目标、入口门禁、退出门禁
  - 每个 Story/Task 都具有验证要求
  - 现有事实、目标能力、冲突项分离记录
- 依赖：无

##### Story `A-STORY-001` 文档全量盘点

- 目标：盘点 `docs/` 内所有已存在文档与原始资料归档。
- 输出：文档清单、文档角色、文档权威来源、缺失项列表。
- 验证：文档清单与仓库文件逐项一致，引用路径可打开。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `A-TASK-001` | 盘点 `docs/` 一级与二级目录 | 仅文档清单，不改语义 | 无 | 目录清单与实际文件一致 |
| `A-TASK-002` | 标注文档权威关系 | `README` / `init` / 规则 / 计划 / 部署 / 合规 | `A-TASK-001` | 任一文档能追溯到权威入口 |
| `A-TASK-003` | 标注文档缺失与重复声明 | 仅记录，不做规则改写 | `A-TASK-002` | 缺失项和冲突项进入确认台账 |

##### Story `A-STORY-002` 仓库事实基线

- 目标：把当前仓库已实现状态和未实现状态分开。
- 输出：代码现状、测试现状、构建现状、模块现状。
- 验证：构建和测试命令真实执行并记录结果。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `A-TASK-004` | 盘点前端实现现状 | 路由、页面、主题、i18n | 无 | `npm run build`、`npm run lint` |
| `A-TASK-005` | 盘点后端实现现状 | common、governance-service、测试、配置 | 无 | `mvn -B test` |
| `A-TASK-006` | 盘点脚本与部署现状 | `scripts/`、`docker-compose*`、SQL | `A-TASK-005` | 脚本/编排存在性与文档引用一致 |

##### Story `A-STORY-003` 追踪矩阵与确认台账

- 目标：建立规则、文档、实现、验证之间的映射与未决事项台账。
- 输出：主计划中的追踪矩阵与 `Human Confirmation Ledger`
- 验证：所有高风险冲突均有编号、影响面和推荐方案。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `A-TASK-007` | 建立规则映射矩阵 | `R-001` 至当前最大规则 | `A-STORY-001`,`A-STORY-002` | 每条规则至少有落点或缺口记录 |
| `A-TASK-008` | 建立冲突台账 | 只记录不改规则 | `A-TASK-007` | 每项冲突包含影响范围/推荐方案 |
| `A-TASK-009` | 固化 Harness 计划模板 | Epic/Story/Task 标准字段 | `A-TASK-007` | 模板覆盖 `11.2` 全字段 |

### Phase-B 阶段0修正与缺口补齐

#### Epic `B-EPIC-001` 阶段0真值与强制文件修正

- 目标：把阶段0从“部分历史描述”修正为“与仓库事实一致的状态”，并补齐强制缺口。
- 成功标准：
  - 阶段0计划与现状一致
  - 交付记录与现状一致
  - 强制文档和缺失项进入实现或确认闭环
- 依赖：`Phase-A`

##### Story `B-STORY-001` 阶段0状态回填

- 目标：基于当前仓库事实修正阶段0任务状态。
- 验证：阶段0任务状态与文件存在性、测试结果、修复记录一致。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `B-TASK-001` | 对齐 `Task-001` 至 `Task-009` 真值 | 只改计划状态与说明 | `A-STORY-002` | 逐项有文件或结果证据 |
| `B-TASK-002` | 补充“已完成/部分完成/待补强”判定标准 | 不改原始历史目标 | `B-TASK-001` | 判定标准可重复使用 |
| `B-TASK-003` | 回填阶段0验证结果 | 文档、构建、测试、lint | `B-TASK-001` | 结果有命令与通过状态 |

##### Story `B-STORY-002` 强制文档与 ADR 缺口

- 目标：补齐 `R-070` 至 `R-110` 中当前缺失的计划级阻塞项。
- 验证：强制文件清单与仓库零遗漏，或全部进入确认台账。
- 当前进展：`ADR-001` 至 `ADR-013` 和 `docs/deployments/huawei-cloud-setup.md` 已落地。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `B-TASK-004` | 补齐 ADR 实体文件清单与落地顺序 | `ADR-001` 至 `ADR-013` | `A-STORY-001` | 编号、状态、索引一致 |
| `B-TASK-005` | 补齐华为云部署文档 | `docs/deployments/huawei-cloud-setup.md` | `A-TASK-006` | 文档存在且被入口引用 |
| `B-TASK-006` | 对齐交付记录 | `docs/deliveries/init-completion.md` | `B-TASK-001` | 交付记录与阶段计划一致 |

##### Story `B-STORY-003` 规则冲突与原始资料治理

- 目标：收敛 `raw-requirements` 空目录要求与原始资料归档要求的冲突。
- 验证：规则、lint、计划和阶段验收口径对 `raw-requirements/` 的语义保持一致。
- 当前进展：已通过 `R-155` 完成语义澄清，当前任务转为一致性维护而非等待确认。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `B-TASK-007` | 记录 `R-101` 与 `R-062`/`R-154` 冲突 | 仅台账 | `A-TASK-008` | 冲突项编号化、可追踪 |
| `B-TASK-008` | 形成备选方案 | 空目录改为保留目录/子目录约束等 | `B-TASK-007` | 方案具收益与破坏面 |

##### Story `B-STORY-004` 访问控制与合规规格补全

- 目标：把访问控制从 placeholder 补全为可执行的完整规格，并与等保规则一致。
- 验证：身份、角色、资源、租户、审计、敏感数据和失败处理都具有明确规则与验证场景。
- 当前进展：`docs/security/access-control-spec.md` 已补全为完整规格文档。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `B-TASK-009` | 补全身份与角色模型 | 文档规范 | `A-STORY-001` | 角色、身份字段、权限边界完整 |
| `B-TASK-010` | 补全资源和数据范围控制 | 文档规范 | `B-TASK-009` | 资源模型和授权顺序完整 |
| `B-TASK-011` | 补全审计、敏感数据与失败处理 | 文档规范 | `B-TASK-010` | 验证场景可执行 |

### Phase-C 共享底座与公共治理服务

#### Epic `C-EPIC-001` Backend Shared Foundation

- 目标：完成后端共享底座、配置治理、消息抽象和公共治理服务扩展基线。
- 成功标准：
  - `sqlforge-common` 具备公共源码
  - `governance-service` 满足当前治理底座职责
  - 配置、消息、日志、异常、审计、上下文遵循统一约束
- 依赖：`Phase-B`

##### Story `C-STORY-001` Common 模块补齐

- 目标：落实 `R-067` 公共层。
- 验证：公共代码被服务复用，跨服务编译通过。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `C-TASK-001` | 盘点应抽取的公共能力 | constants/exception/context/audit/utils 等 | `Phase-B` | 清单与现有代码对照完整 |
| `C-TASK-002` | 建立 common 包结构 | 高内聚低耦合 | `C-TASK-001` | 目录结构符合 `R-021`/`R-022` |
| `C-TASK-003` | 迁移重复或散落能力 | 仅迁移共性能力 | `C-TASK-002` | `mvn clean compile`，消费方通过 |

##### Story `C-STORY-002` 多环境配置与消息模式

- 目标：巩固 `R-066`、`R-128`、`R-144`。
- 验证：dev/test/prod 与 DATABASE/MOCK/KAFKA 配置契约一致。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `C-TASK-004` | 对齐所有 `application-*.yml` 职责 | 只处理配置 | `Phase-B` | `R-128` 四项检查通过 |
| `C-TASK-005` | 固化消息抽象接口实现路线 | interface/config/impl | `C-TASK-004` | 本地数据库模式与测试 mock 模式通过 |
| `C-TASK-006` | 完成消息流管理接口验证 | retry/stats/manual smoke | `C-TASK-005` | 管理接口与消息表验证通过 |

##### Story `C-STORY-003` 公共治理服务增强

- 目标：把 `governance-service` 明确为公共管理服务的当前实现基线。
- 验证：接口契约、tenant 校验、错误码、MyBatis XML、日志满足现有规则。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `C-TASK-007` | 对齐现有 controller/application/domain/infrastructure 分层 | 不扁平化 | `Phase-B` | `R-120` 检查通过 |
| `C-TASK-008` | 落实租户配置与访问占位能力 | 当前 phase 0 允许 placeholder | `C-TASK-007` | 测试覆盖正常/异常/越权占位路径 |
| `C-TASK-009` | 规划审计、数据源、调度扩展点 | 只补契约和骨架 | `C-TASK-008` | 接口文档和错误码一致 |

### Phase-D 核心业务服务实现

#### Epic `D-EPIC-001` 服务边界落地

- 目标：按确认后的服务目标逐步落地查询执行、SQL优化、压测、数据治理能力。
- 成功标准：
  - 服务边界、接口契约、依赖关系、阶段顺序被锁定
  - 各核心 Story 有可执行 Task 树
  - 每个 Task 有真实验证路径
- 依赖：`Phase-C`

##### Story `D-STORY-001` 查询执行服务

- 目标：实现联机查询主流程的服务骨架与最小闭环。
- 验证：接口契约、错误码、状态流转、日志、回滚路径可测。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `D-TASK-001` | 固化查询执行服务边界 | 路由/执行/轻量解析/轻量改写/加速应用 | 无 | 服务边界与 ADR 一致 |
| `D-TASK-002` | 定义联机查询接口 DTO/VO/错误码 | 契约优先 | `D-TASK-001` | `R-121` 四项检查通过 |
| `D-TASK-003` | 实现最小同步执行闭环 | 不执行任意 SQL 越界能力 | `D-TASK-002` | 正常/超时/失败/降级路径测试 |
| `D-TASK-004` | 增加异常回滚与运行日志 | 入口/出口/异常/状态变更 | `D-TASK-003` | `R-123` 抽查通过 |

##### Story `D-STORY-002` SQL 优化服务

- 目标：实现异步优化、加速建议与物化视图管理路线。
- 验证：异步任务、建议输出、风险标记、回调路径可测。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `D-TASK-005` | 固化异步优化任务模型 | parse/rewrite/acceleration suggestion | 无 | 模型与接口契约一致 |
| `D-TASK-006` | 实现任务提交与状态查询骨架 | 异步接口 | `D-TASK-005` | 提交/轮询/失败路径测试 |
| `D-TASK-007` | 输出优化建议结构 | 成本、收益、风险 | `D-TASK-006` | VO 与文档一致 |

##### Story `D-STORY-003` 压测引擎服务

- 目标：实现压测任务提交、只读隔离和报告输出路线。
- 验证：提交、执行状态、只读安全约束、报告查询可测。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `D-TASK-008` | 定义压测任务与报告模型 | DAG/阈值/只读标记 | 无 | 模型与架构文档一致 |
| `D-TASK-009` | 实现压测任务提交流程骨架 | 异步任务 | `D-TASK-008` | 提交/取消/失败路径测试 |
| `D-TASK-010` | 实现报告查询接口 | JSON/PDF/HTML 契约 | `D-TASK-009` | 输出字段与契约一致 |

##### Story `D-STORY-004` 数据与合规底座

- 目标：让历史、审计、导出、配置、结果和审计具备可追溯关联键，并落实等保要求。
- 验证：SQL 脚本、实体、Mapper XML、日志、加密策略一致。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `D-TASK-011` | 补齐核心表与关联键设计 | config/result/history/export/audit | `Phase-C` | `R-129` 四项检查通过 |
| `D-TASK-012` | 落实审计日志链路 | SQL 操作/登录/权限变更 | `D-TASK-011` | `R-113` 场景测试 |
| `D-TASK-013` | 落实敏感字段加密 | 密码/token/key | `D-TASK-011` | 库、日志、导出无明文 |

### Phase-E 前端驾驶舱与业务页面

#### Epic `E-EPIC-001` Frontend Information Architecture

- 目标：按既有设计系统和页面组织规则交付研发驾驶舱与业务页面。
- 成功标准：
  - Dashboard/驾驶舱与业务主路径分离
  - 页面组织遵循任务流
  - 后端已交付能力在页面可见
- 依赖：`Phase-C`、`Phase-D`

##### Story `E-STORY-001` 研发驾驶舱

- 目标：交付 `10.1` 定义的驾驶舱 IA。
- 验证：板块完整、导航清晰、深色主题一致。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `E-TASK-001` | 建立驾驶舱路由与导航骨架 | dashboard only | `Phase-C` | `npm run build`、路由可达 |
| `E-TASK-002` | 落地项目全景/架构设计/进度管理板块 | 文字与状态展示 | `E-TASK-001` | 页面结构与 `10.1` 一致 |
| `E-TASK-003` | 落地合规中心与规则库板块 | 只读展示 | `E-TASK-002` | 合规/规则入口存在且可见 |

##### Story `E-STORY-002` 业务页面拆分

- 目标：保留 `SQL 查询`、`解析记录`、`压测报告`、`加速配置`、`系统管理` 独立页面。
- 验证：不堆叠长页面，能力可见，前后端边界清晰。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `E-TASK-004` | 建立五大业务页路由骨架 | 独立路由 | `E-TASK-001` | 每页有独立入口 |
| `E-TASK-005` | 接入已存在治理接口能力 | 先接已交付能力 | `E-TASK-004`,`Phase-C` | `R-028` 检查通过 |
| `E-TASK-006` | 深色设计系统组件化 | token/component/layout | `E-TASK-004` | 设计 token 实际生效 |

##### Story `E-STORY-003` 前后端分离持续治理

- 目标：确保前端不承载后端权威逻辑。
- 验证：专项脚本、代码审查和边界抽查通过。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `E-TASK-007` | 扩展分离检查清单 | 文档+脚本 | `Phase-C` | 分离检查脚本通过 |
| `E-TASK-008` | 清理潜在越界逻辑 | 只处理边界问题 | `E-TASK-007` | 前端仅保留编排/预校验/展示 |

### Phase-F 部署、运维、生产就绪

#### Epic `F-EPIC-001` Delivery, Compliance and Operations

- 目标：补齐部署、运维、监控、备份、CI 门禁和交付闭环。
- 成功标准：
  - 本地/离线/云部署文档齐全
  - 阶段门禁接入 CI
  - 合规和备份恢复可验证
- 依赖：`Phase-D`、`Phase-E`

##### Story `F-STORY-001` 部署文档与编排

- 目标：统一本地、离线、华为云部署说明。
- 验证：文档存在、编排语法检查通过、入口索引正确。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `F-TASK-001` | 补齐华为云部署文档 | 私有云拓扑、环境、服务部署 | `B-TASK-005` | 文档存在且入口可达 |
| `F-TASK-002` | 对齐 compose 与脚本说明 | 本地/离线/可选服务 | `F-TASK-001` | `docker compose config` |
| `F-TASK-003` | 补齐环境提醒与恢复指引 | MySQL 独立环境、备份恢复 | `F-TASK-002` | 文档与规则一致 |

##### Story `F-STORY-002` CI 与质量门禁

- 目标：让规则、测试、构建、扫描、分离检查进入 CI。
- 验证：CI 覆盖仓库知识 lint、Java 扫描、前端 lint/build、后端 test。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `F-TASK-004` | 盘点现有 CI 能力 | `.github/workflows/ci.yml` | `Phase-C`,`Phase-E` | CI 清单完整 |
| `F-TASK-005` | 接入阶段门禁脚本化验证 | `R-116`/`R-117`/`R-118` | `F-TASK-004` | 阶段切换可阻断 |
| `F-TASK-006` | 接入 Java 规范扫描 | `pmd`/`checkstyle` | `F-TASK-004` | `R-151` 可追溯 |

##### Story `F-STORY-003` 运维、审计与恢复

- 目标：完成日志、监控、备份恢复、审计保留的交付闭环。
- 验证：合规清单齐全，恢复责任人和演练记录可追溯。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `F-TASK-007` | 补齐监控与日志规范落地清单 | logs/metrics/alerts | `Phase-D` | 规范与实现映射完整 |
| `F-TASK-008` | 补齐备份恢复策略与演练记录模板 | RPO/RTO/加密 | `F-TASK-007` | `R-115` 验证项可追溯 |
| `F-TASK-009` | 阶段交付回写闭环 | 完成记录/commit/tag/tag回写 | `F-TASK-008` | 交付记录模板闭环 |

## 7. Verification Matrix

### 7.1 Global commands

- `node scripts/lint-repository-knowledge.js`
- `node scripts/check-frontend-backend-separation.js`
- `mvn -B test`
- `mvn -B validate pmd:pmd checkstyle:check`
- `npm run build`
- `npm run lint`

### 7.2 Rule-driven verification

- Phase entry: `R-116`
- Phase delivery gate: `R-117`
- Progressive compliance: `R-118`
- Task compile/build: `R-119`
- Layering regression: `R-120`
- Interface contract: `R-121`
- MyBatis XML: `R-122`
- Flow logging: `R-123`
- Frontend validation: `R-124`
- Cross-service/config/db/doc regressions: `R-125` to `R-133`
- Rule and verification self-maintenance: `R-131` to `R-143`
- Messaging abstraction: `R-144`
- Java governance: `R-145` to `R-154`
- Archive semantics clarification: `R-155`

### 7.3 Task-level validation requirements

- 每个 Task 至少包含：
  - 正常场景验证
  - 异常场景验证
  - 边界条件验证
  - 文档/契约同步验证
  - 受影响脚本、配置或页面的回归验证
- 涉及后端代码：
  - `mvn clean compile`
  - `mvn test`
  - `mvn validate pmd:pmd checkstyle:check`
- 涉及前端代码：
  - `npm run build`
  - `npm run lint`
- 涉及 SQL/Mapper：
  - SQL 脚本可执行
  - XML namespace / 参数绑定 / 禁用 `${}` 验证
- 涉及部署：
  - 编排语法检查
  - 必要环境 smoke

### 7.4 Documentation coverage validation

- `docs/` 全量文件必须进入 [document-coverage-matrix.md](./document-coverage-matrix.md)
- 每份文档必须标注为 `Authority`、`Indexed` 或 `Archive`
- 主执行计划必须显式引用所有 `Authority` 文档
- 所有 Task 必须在 [task-spec-matrix.md](./task-spec-matrix.md) 中补齐 10 个 Harness 字段
- `raw-requirements/` 的验收语义必须与 `R-155` 保持一致

## 8. Human Confirmation Ledger

| ID | Topic | Current Fact | Impact | Recommended Option | Status |
|:---|:---|:---|:---|:---|:---|
| `HC-001` | 服务目标口径 | 已由人类确认最终主线为 4 个微服务 | 已消除阶段拆解口径冲突 | 4 个微服务作为唯一最终目标继续推进 | Resolved |
| `HC-002` | 阶段0状态冲突 | `phase-0-plan.md` 已按仓库事实回填状态，并区分 Completed / Partial / Pending | 已恢复阶段0真值 | 继续按当前状态推进后续阶段 | Resolved |
| `HC-003` | ADR 缺失 | `ADR-001` 至 `ADR-013` 已补齐实体文件 | 已恢复技术决策可追溯性 | 后续新增 ADR 继续 append-only 管理 | Resolved |
| `HC-004` | 原始资料目录规则冲突 | 已通过 `R-155` 澄清：目录必须存在，初始化可为空，归档发生后允许包含资料 | 已恢复规则一致性与 lint 语义 | 后续统一按归档根目录语义执行 | Resolved |
| `HC-005` | 华为云部署文档缺失 | `docs/deployments/huawei-cloud-setup.md` 已补齐 | 已补足部署文档完整性缺口 | 后续与实际部署脚本持续同步 | Resolved |
| `HC-006` | 合规目标与当前实现差距 | 访问控制文档已补全；代码实现仍需按阶段推进 | 文档缺口已关闭，后续转入实现缺口治理 | 以完整规格驱动后续实现与验证 | Resolved |
| `HC-007` | 交付闭环不完整 | 历史 `v0.1.0-init` 与本轮 checkpoint 提交/标签均已回写到交付记录 | 已恢复交付记录可信度 | 后续批次继续遵循完成记录 -> commit -> tag -> 回写流程 | Resolved |

## 9. Defaults

- 当前默认不修改既有规则语义，只新增计划导航与主执行计划。
- 当前默认把 `governance-service` 视为公共管理服务的现阶段实现基线，而不是全部目标能力已完成。
- 当前默认所有新增计划和后续任务都遵循“task 内高内聚、task 间低耦合、验证先行”。
- 当前默认每个阶段结束前必须回填：阶段状态、验证结果、剩余风险、待确认项状态、交付记录。
