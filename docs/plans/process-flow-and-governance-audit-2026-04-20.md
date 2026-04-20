# SQLForge Process Flow And Governance Audit 2026-04-20

## 1. Summary

本文基于当前仓库 `docs/` 全量文档、ADR、计划、规则、运维与交付记录，对 SQLForge 的“功能实现全流程”进行正式整理，并从 Harness Engineering 视角审查流程是否闭口、是否存在不合理约束、是否缺少步骤或规则。

本文目标：

- 给出当前仓库可执行的正式实现流程说明
- 为每个流程节点标注必须阅读的文档、必须遵守的规则、验证锚点与主要约束
- 区分“工程交付流程”与“产品运行流程”
- 输出流程缺陷清单、整改建议与优先级路线图
- 逐条指出相关文档应如何修订

本文定位：

- 属于治理审计与执行说明文档
- 不替代现有权威规则、ADR 或主执行计划
- 当前执行仍以 `docs/README.md`、`document-truth-baseline.md`、`codex-rules.md`、`validation-rules.md`、`master-execution-plan.md` 为权威入口

## 2. Evidence Basis

本次扫描直接消费的核心文档包括：

- `docs/README.md`
- `docs/plans/document-truth-baseline.md`
- `docs/architecture/init.md`
- `docs/rules/codex-rules.md`
- `docs/quality/validation-rules.md`
- `docs/plans/implementation-readiness.md`
- `docs/plans/phase-prerequisite-matrix.md`
- `docs/plans/master-execution-plan.md`
- `docs/plans/task-spec-matrix.md`
- `docs/plans/task-governance-extension-matrix.md`
- `docs/plans/document-gap-matrix.md`
- `docs/operations/*.md`
- `docs/security/*.md`
- `docs/architecture/service-capability-map.md`
- `docs/architecture/service-interface-contract-baseline.md`
- `docs/architecture/messaging-abstraction.md`
- `docs/frontend/design-system.md`
- `docs/deployments/*.md`
- `docs/deliveries/init-completion.md`
- `docs/references/human-constraint-history.md`
- `docs/adr/*.md`
- `tasks.md`
- `tasks-done.md`
- `INBOX.md`

## 3. Current Truth Snapshot

当前仓库已确认事实：

- 根级前端工程已存在并可构建
- Maven 聚合工程当前只包含 `sqlforge-common/` 与 `governance-service/`
- `governance-service` 当前仅代表公共管理服务的阶段性基线，不代表 4 微服务已全部落地
- 查询执行服务、SQL 优化服务、压测引擎服务仍未形成独立代码模块
- 访问控制、审计、加密、备份恢复已形成文档规格，但代码尚未完整落地

当前目标边界：

- 最终服务固定为 4 个微服务
- 交付顺序以 `Phase-A` 到 `Phase-F` 及 `Wave 1` 到 `Wave 7` 为当前执行主线
- 历史 `阶段0-阶段3 / 11 个原始服务` 只保留为初始化来源与历史记录，不再作为当前实现口径

## 4. End-To-End Engineering Delivery Flow

以下流程描述“团队如何把一个功能从任务输入推进到实现、验证、归档和部署”。

### Node-01 任务进入与上下文建立

- 目标：
  - 确认任务来源、运行台账真值与阅读入口
- 必读文档：
  - `AGENTS.md`
  - `README.md`
  - `docs/README.md`
  - `docs/operations/foreman-workflow.md`
  - `tasks.md`
  - `tasks-done.md`
  - `INBOX.md`
- 必守规则：
  - `R-006`
  - `R-014`
  - `R-156` 至 `R-161`
- 主要要求与约束：
  - 活动任务只能以 `tasks.md` 为真值
  - 已完成任务不得留在 `tasks.md`
  - 人工判断事项放 `INBOX.md`
  - 不允许跳过入口文档直接开始实现
- 验证锚点：
  - `R-156`
  - `R-158`
  - `R-159`
  - `R-160`

### Node-02 当前真值校准

- 目标：
  - 先判断“仓库现在有什么”，再谈“目标应该是什么”
- 必读文档：
  - `docs/plans/document-truth-baseline.md`
  - `docs/generated/repo-map.md`
  - 当前代码、脚本、构建结果与验证日志
- 必守规则：
  - `R-048`
  - `R-161`
- 主要要求与约束：
  - 只能用 `Implemented Fact` 判断现状
  - 不能把目标架构、历史文本或原始归档写成已实现事实
  - 初始化文档路径与当前路径不一致时，以真值基线漂移映射为准
- 验证锚点：
  - `R-133`
  - `document-coverage-matrix.md`

### Node-03 规则、验证与长期约束锁定

- 目标：
  - 确认当前任务受哪些长期规则、验证门禁和历史决策约束
- 必读文档：
  - `docs/rules/codex-rules.md`
  - `docs/quality/validation-rules.md`
  - `docs/references/human-constraint-history.md`
- 必守规则：
  - `R-001` 至 `R-164`
- 主要要求与约束：
  - 规则库是 append-only
  - 新约束必须进入规则、文档、测试或 lint
  - 迁移外部治理思想时只能迁移方法，不得照搬外部项目事实
- 验证锚点：
  - `R-131`
  - `R-135`
  - `R-140`
  - `R-161`

### Node-04 阶段准入检查

- 目标：
  - 确认当前任务属于哪个阶段，以及该阶段是否已经 ready
- 必读文档：
  - `docs/plans/master-execution-plan.md`
  - `docs/plans/phase-prerequisite-matrix.md`
  - `docs/plans/document-gap-matrix.md`
- 必守规则：
  - `R-007`
  - `R-116`
  - `R-117`
  - `R-118`
  - `R-163`
- 主要要求与约束：
  - 进入新阶段前必须核对输入文档、ADR、规则、验证、人工确认点
  - 若阶段依赖文档缺失，先补文档
  - 若阶段人工确认项未解，不得标记 ready
- 验证锚点：
  - `R-116`
  - `phase-prerequisite-matrix.md`

### Node-05 冲突与人工确认处理

- 目标：
  - 在改规则、改边界、改契约、改主语义前完成确认
- 必读文档：
  - `docs/plans/master-execution-plan.md`
  - `docs/plans/document-gap-matrix.md`
  - `docs/operations/human-collaboration.md`
- 必守规则：
  - `R-049`
  - `R-050`
  - `R-153`
  - `R-158`
- 主要要求与约束：
  - 删除规则、改边界、废弃主存储、改 API 契约等高风险动作必须先记录影响
  - 不确定项先进入 `Human Confirmation Ledger` 或 `INBOX.md`
  - 不得以“实现方便”为由绕过确认
- 验证锚点：
  - `R-153`
  - `R-158`

### Node-06 Task 成形与拆解

- 目标：
  - 把需求拆成可执行、可验证、可关闭的 Task
- 必读文档：
  - `docs/plans/task-spec-matrix.md`
  - `docs/plans/task-governance-extension-matrix.md`
  - `docs/plans/master-execution-plan.md`
- 必守规则：
  - `R-007`
  - `R-164`
- 主要要求与约束：
  - 每个 Task 必须具备 10 个 Harness 字段
  - 高风险 Task 还要写人工确认点、数据影响、回滚策略
  - task 内高内聚，task 间低耦合
- 验证锚点：
  - `task-spec-matrix.md`
  - `task-governance-extension-matrix.md`

### Node-07 服务归属与边界判定

- 目标：
  - 判断功能最终属于哪一个微服务，以及当前先落在哪个模块
- 必读文档：
  - `docs/architecture/service-capability-map.md`
  - `docs/adr/ADR-002-microservice-splitting-and-bounded-contexts.md`
  - `docs/plans/implementation-readiness.md`
- 必守规则：
  - `R-018`
  - `R-020`
  - `R-021`
  - `R-022`
  - `R-067`
  - `R-068`
- 主要要求与约束：
  - 任何新增模块必须标明其最终服务域
  - `governance-service` 只能向公共管理服务边界收敛
  - `sqlforge-common` 只能承载真正公共能力
  - 不允许把查询执行、异步优化、压测主流程长期堆进 `governance-service`
- 验证锚点：
  - `R-120`
  - `R-126`

### Node-08 接口、错误码、身份与审计契约锁定

- 目标：
  - 在编码前固定跨服务接口、错误码区间、上下文字段和审计字段
- 必读文档：
  - `docs/architecture/service-interface-contract-baseline.md`
  - `docs/security/access-control-spec.md`
  - `docs/security/compliance.md`
- 必守规则：
  - `R-041`
  - `R-046`
  - `R-057`
  - `R-111` 至 `R-115`
- 主要要求与约束：
  - 所有受保护请求必须形成统一身份上下文
  - 跨服务只能传 DTO、事件或统一错误响应
  - 错误码区间必须按服务域归属
  - 审计字段必须统一，且可追踪
- 验证锚点：
  - `R-121`
  - `R-118`

### Node-09 专项约束消费

- 目标：
  - 在编码前消费与任务主题相关的专项约束
- 典型文档：
  - Java：`docs/quality/alibaba-java-guidelines.md`
  - 消息：`docs/architecture/messaging-abstraction.md`
  - 前端：`docs/frontend/design-system.md`
  - 前后端边界：`docs/quality/frontend-backend-separation-baseline.md`
  - 部署：`docs/deployments/*.md`
- 必守规则：
  - `R-144`
  - `R-145` 至 `R-154`
  - `R-015`
  - `R-019`
  - `R-023` 至 `R-030`
  - `R-063`
- 主要要求与约束：
  - 本地消息默认不依赖 Kafka
  - Java 规范需通过文档与扫描工具共同落地
  - 前端必须遵守任务流信息架构和深色设计系统
  - 前端不得承载后端权威逻辑
- 验证锚点：
  - `R-124`
  - `R-144`
  - `R-151` 至 `R-154`

### Node-10 编码实施与执行波次

- 目标：
  - 按当前仓库可落地顺序推进实现
- 必读文档：
  - `docs/plans/implementation-readiness.md`
- 必守规则：
  - `Wave 1` 至 `Wave 7`
- 主要要求与约束：
  - Wave 1：做实 `sqlforge-common`
  - Wave 2：强化公共管理服务
  - Wave 3：建立查询执行服务骨架
  - Wave 4：建立 SQL 优化服务骨架
  - Wave 5：建立压测引擎服务骨架
  - Wave 6：前端驾驶舱与业务页
  - Wave 7：部署、门禁、合规与复盘
- 验证锚点：
  - `master-execution-plan.md`
  - `implementation-readiness.md`

### Node-11 Task 级验证

- 目标：
  - 每个 Task 完成后执行真实验证，而不是形式占位
- 必读文档：
  - `docs/quality/validation-rules.md`
  - `docs/operations/local-development.md`
- 必守规则：
  - `R-119` 至 `R-130`
  - `R-136` 至 `R-141`
- 主要要求与约束：
  - 后端至少编译或测试
  - 涉及接口要做契约验证
  - 涉及 MyBatis 要做 XML 与参数绑定验证
  - 涉及前端要做 build/lint/入口文件回归
  - 涉及配置、SQL、compose 要做对应环境验证
- 验证锚点：
  - `mvn -B clean compile`
  - `mvn -B test`
  - `npm run build`
  - `npm run lint`
  - `docker compose config`
  - `node scripts/lint-repository-knowledge.js`

### Node-12 文档、规则、索引同步

- 目标：
  - 让代码、文档、计划、规则、索引同步闭口
- 必读文档：
  - `docs/README.md`
  - `docs/plans/document-coverage-matrix.md`
  - `docs/quality/validation-log.md`
  - `docs/plans/document-gap-matrix.md`
- 必守规则：
  - `R-131`
  - `R-132`
  - `R-133`
  - `R-140`
  - `R-162`
- 主要要求与约束：
  - 新增或变更权威文档时，必须更新索引与 coverage
  - 新增规则时，规则库、历史账本、lint 要同步
  - 验证记录必须 append-only
  - 治理批次必须同时闭口到任务归档、验证日志、复盘与 git history
- 验证锚点：
  - `R-131`
  - `R-133`
  - `document-coverage-matrix.md`

### Node-13 任务关闭与审计链固化

- 目标：
  - 形成单任务闭环和可追溯提交链
- 必读文档：
  - `docs/operations/git-and-task-closeout.md`
  - `tasks.md`
  - `tasks-done.md`
- 必守规则：
  - `R-012`
  - `R-157`
  - `R-159`
  - `R-160`
- 主要要求与约束：
  - 一个已验证任务对应一个 commit
  - stage 只覆盖当前任务相关文件
  - 关闭前必须执行 task audit
  - 不自动 push，不改写已发布历史
- 验证锚点：
  - `python3 scripts/task_audit.py --check`
  - Git history

### Node-14 部署、环境提醒与复盘

- 目标：
  - 将本地交付推进到环境就绪、生产就绪与持续治理
- 必读文档：
  - `docs/deployments/local-setup.md`
  - `docs/deployments/offline-setup.md`
  - `docs/deployments/huawei-cloud-setup.md`
  - `docs/plans/retrospective-template.md`
- 必守规则：
  - `R-004`
  - `R-108`
  - `R-115`
  - `R-130`
  - `R-148`
  - `R-162`
- 主要要求与约束：
  - 本地默认 `DATABASE` 消息模式
  - 生产环境切到 `KAFKA`
  - 必须提醒独立 MySQL/TDSQL、真实鉴权、审计、加密和备份恢复
  - 每轮复杂交付后应留复盘
- 验证锚点：
  - 环境 smoke
  - 部署文档一致性
  - 复盘文档

## 5. End-To-End Product Runtime Flow

以下流程描述“平台功能本身如何运行”。

### 5.1 联机查询主流程

- 触发条件：
  - BI 工具或用户界面提交 SQL
- 主要节点：
  1. 请求接入
  2. 合规检查
  3. 缓存检查
  4. 智能路由
  5. 轻量解析
  6. 轻量改写
  7. 执行引擎调用
  8. 结果返回
  9. 异步审计、落库、优化与加速分析
- 关键文档：
  - `docs/architecture/init.md`
  - `docs/architecture/service-capability-map.md`
  - `docs/security/access-control-spec.md`
  - `docs/security/compliance.md`
  - `docs/adr/ADR-004-hetu-integration-boundary.md`
  - `docs/adr/ADR-005-sql-parser-build-vs-buy-boundary.md`
  - `docs/adr/ADR-011-cache-consistency-with-hudi-timestamp.md`
  - `docs/adr/ADR-012-saga-plus-local-transaction.md`
- 关键规则与约束：
  - `R-041`, `R-042`, `R-045`, `R-046`, `R-060`, `R-064`
  - `R-111` 至 `R-115`
  - `R-034` 关联键可追溯
- 当前应执行的边界解释：
  - 轻量解析、轻量改写、执行控制属于查询执行服务
  - 深度解析、重写建议、加速建议不应长期留在查询执行服务

### 5.2 异步优化子流程

- 触发条件：
  - SQL 保存
  - 定时扫描
  - Schema 变更
- 主要节点：
  1. 获取待解析任务
  2. 深度解析
  3. 规则改写
  4. AI 推荐
  5. 结果反馈
  6. 异步落库
- 关键文档：
  - `docs/architecture/init.md`
  - `docs/architecture/service-capability-map.md`
  - `docs/adr/ADR-005-sql-parser-build-vs-buy-boundary.md`
  - `docs/adr/ADR-013-acceleration-service-and-materialized-view-strategy.md`
- 关键规则与约束：
  - `R-018`, `R-020`, `R-068`
  - `R-121`
  - `R-123`
- 当前应执行的边界解释：
  - 深度解析、收益评估、物化视图与加速建议归 SQL 优化服务
  - 不应继续把该流程长期视为查询执行服务内嵌能力

### 5.3 压测评估子流程

- 触发条件：
  - SQL 设计保存
  - 数据量显著变化
  - 周期性低峰压测
- 主要节点：
  1. 压测任务入队
  2. 多引擎并发执行
  3. 性能基线生成
  4. 阈值判定
  5. 报告生成
- 关键文档：
  - `docs/architecture/init.md`
  - `docs/security/compliance.md`
  - `docs/adr/ADR-007-benchmark-engine-production-isolation.md`
- 关键规则与约束：
  - `R-039`
  - `R-045`
  - `R-111` 至 `R-115`
- 当前应执行的边界解释：
  - 压测服务必须独立隔离，不得污染生产链路
  - 压测与查询执行服务之间只能通过受控接口协同

### 5.4 加速与物化视图子流程

- 触发条件：
  - 用户申请加速
  - 系统识别性能瓶颈
  - 数据模型变更导致失效
- 主要节点：
  1. 加速建议生成
  2. 用户确认
  3. 配置审批与生效
  4. 查询执行时应用
  5. 效果监控与失效回滚
- 关键文档：
  - `docs/architecture/init.md`
  - `docs/adr/ADR-013-acceleration-service-and-materialized-view-strategy.md`
  - `docs/architecture/service-capability-map.md`
- 关键规则与约束：
  - `R-020`
  - `R-034`
  - `R-041`
  - `R-044`
  - `R-068`
- 当前应执行的边界解释：
  - 建议生成与物化视图管理归 SQL 优化服务
  - 查询执行服务只消费已批准、已生效配置
  - 公共管理服务负责审批、审计与配置主数据

### 5.5 异常回滚与补偿流程

- 触发条件：
  - 联机查询、异步优化、压测或配置应用任一环节失败
- 主要节点：
  1. 局部失败识别
  2. 重试
  3. 降级
  4. 清理副作用
  5. 审计留痕
  6. 补偿或回放
- 关键文档：
  - `docs/architecture/init.md`
  - `docs/adr/ADR-012-saga-plus-local-transaction.md`
  - `docs/architecture/messaging-abstraction.md`
- 关键规则与约束：
  - `R-034`
  - `R-036`
  - `R-039`
  - `R-042`
  - `R-044`
  - `R-144`
- 当前应执行的边界解释：
  - 补偿和回滚必须可审计
  - 最终一致性通过 Saga + 本地事务达成，不引入复杂分布式事务框架

### 5.6 合规与审计贯穿流程

- 触发条件：
  - 任何受保护操作
- 主要节点：
  1. 身份鉴别
  2. 租户与资源范围校验
  3. 风险检查
  4. 审计写入
  5. 敏感数据保护
  6. 归档与恢复
- 关键文档：
  - `docs/security/access-control-spec.md`
  - `docs/security/compliance.md`
  - `docs/architecture/service-interface-contract-baseline.md`
- 关键规则与约束：
  - `R-111` 至 `R-115`
  - `R-041`
  - `R-046`
- 当前应执行的边界解释：
  - 合规不是收尾动作，而是请求入口、资源授权、日志留痕、恢复策略的贯穿层

## 6. Completeness Assessment

### 6.1 已经较完整的部分

- 文档入口、真值分层、规则库、验证规则、ADR 索引已形成稳定体系
- 阶段前置条件矩阵、Task 字段矩阵、Task 扩展治理矩阵已补齐
- 活动任务、完成任务、人工决策入口、验证日志和复盘模板已建立
- 服务能力分配图和接口契约基线已把“4 微服务目标”和“当前仓库事实”分开

### 6.2 仍然部分完成的部分

- `sqlforge-common` 共享层做实
- `governance-service` 向公共管理服务收敛
- 访问控制完整代码化
- 前端驾驶舱与五大业务页真实承载已交付能力
- 生产级部署、门禁、监控与演练

### 6.3 仍存在断层或未完全闭口的部分

- 历史初始化正文与当前权威边界之间仍有少量正文冲突
- 部分阶段门禁要求在文档中存在，但没有对应执行命令或脚本
- 主计划与当前运行台账之间存在“已规划但未实例化为活动任务”的断层
- 个别规则过于严格，会把常规实现任务误判为必须先人工审批

## 7. Process Defects And Remediation Recommendations

### D-001 高优先级：审计保留期口径冲突

- 现象：
  - `docs/architecture/init.md` 联机查询主流程写“审计日志写入（Kafka，保留30天）”
  - 同文件合规流程和 `docs/security/compliance.md` 又要求审计日志保留不少于 180 天
- 风险：
  - 合规实现、存储规划、审计归档和运维策略会产生冲突
- 建议：
  - 明确拆分“消息链路保留期”和“权威审计存储保留期”
  - 统一为：
    - Kafka/消息 Topic：短期缓冲和补偿保留期
    - 审计主存储：不少于 180 天

### D-002 高优先级：查询执行服务与 SQL 优化服务职责重叠

- 现象：
  - 初始化正文仍把“深度解析、规则+AI 改写、加速建议”写入查询执行服务
  - 当前权威能力图与 ADR 已将这些能力划归 SQL 优化服务
- 风险：
  - 实现者可能继续把优化主流程堆进查询执行服务或 `governance-service`
- 建议：
  - 用显式勘误修订 `init.md` 第 4 部分与第 5 部分正文
  - 保留历史来源说明，但在当前执行口径中明确：
    - 查询执行服务：轻量解析、轻量改写、执行控制、运行时应用已批准加速配置
    - SQL 优化服务：深度解析、异步改写建议、加速建议、物化视图治理

### D-003 中优先级：`R-153` 适用范围过宽，存在流程锁死风险

- 现象：
  - 当前规则写法要求修改既有规则、文档、脚本、代码、CI、部署文件前都先人工确认
- 风险：
  - 几乎所有非 trivial 任务都将被阻塞为人工审批
  - 与主计划中“只有不确定/冲突/破坏式变更才先确认”的执行口径冲突
- 建议：
  - 把 `R-153` 缩窄为：
    - 规则语义变更
    - 主文档权威变更
    - 破坏式接口/部署/安全口径变更
    - 高风险既有文件修改
  - 普通实现类变更改为要求“列出改动范围并在任务日志留痕”

### D-004 中优先级：阶段模型存在双轨词汇

- 现象：
  - 历史初始化文档保留“阶段0-3 / 11个服务”
  - 当前主计划使用 `Phase-A` 到 `Phase-F` / 4 微服务
- 风险：
  - 新接手工程师要在两套阶段模型间切换
- 建议：
  - 在 `init.md` 第十二部分前追加显式说明：
    - 该部分为历史初始化计划
    - 当前执行阶段以 `master-execution-plan.md` 为准

### D-005 中优先级：阶段门禁含不可直接执行项

- 现象：
  - 阶段门禁要求 SonarQube 和覆盖率阈值
  - 当前本地开发入口未提供 Sonar 或 coverage 的统一命令、脚本或 runbook
- 风险：
  - 规则有了，执行闭环缺失
  - 阶段准出时会出现“文档要求存在，但无人能复现”
- 建议：
  - 在本地开发文档和 CI 文档中增加统一执行方式
  - 若当前尚未接入工具，应明确“暂以占位要求存在，进入 Phase-F 前必须工具化”

### D-006 中优先级：主计划到活动任务台账存在断层

- 现象：
  - 主计划明确下一步为 `Phase-C` / `Phase-D`
  - 当前 `tasks.md` 为空
- 风险：
  - 计划存在，但执行节奏不能直接驱动
- 建议：
  - 从 `Phase-C` 中实例化首批活动任务到 `tasks.md`
  - 至少补：
    - `C-TASK-001`
    - `C-TASK-002`
    - `C-TASK-004`
    - `C-TASK-007`

### D-007 低优先级：C4 图要求存在，但未见独立图工件

- 现象：
  - 文档同步验证要求 C4 描述同步更新
  - 初始化文档只描述了 C4 层级，没有独立图工件或标准更新位置
- 风险：
  - `R-133` 在架构图项上难以执行
- 建议：
  - 明确“当前以文字架构图描述代替独立图”
  - 或新增 `docs/architecture/c4-overview.md`

## 8. P0 / P1 / P2 Remediation Roadmap

### P0

- 统一审计保留期口径，修复 30 天与 180 天冲突
- 修正查询执行服务与 SQL 优化服务的职责边界正文漂移
- 缩窄 `R-153` 的适用范围，避免流程锁死
- 在 `init.md` 显式标注历史阶段模型与当前执行阶段模型的切换关系

### P1

- 为 SonarQube、coverage、阶段门禁补齐可执行入口或明确占位语义
- 把 `Phase-C` 的首批任务实例化到 `tasks.md`
- 在主计划或执行计划中增加“当前活跃波次”或“next executable tasks”区块
- 明确架构图更新落点，解决 `R-133` 中 C4 要求的执行歧义

### P2

- 细化各服务的 DTO 命名、事件版本与错误码枚举清单
- 为前端驾驶舱、五大业务页、公共管理服务和 3 个未建模块补具体执行计划
- 将部署、恢复、监控、演练进一步工具化并纳入 CI / phase gate

## 9. Document-By-Document Change Recommendations

### `docs/architecture/init.md`

- 修改点：
  - 第 4.1 节审计保留期表述
  - 第 5.1 节服务职责分工
  - 第 12 部分历史阶段说明
- 建议：
  - 把 Kafka 保留期和审计主存储保留期拆开写
  - 把深度解析、异步改写建议、加速建议从查询执行服务正文中移出，改为 SQL 优化服务
  - 在第十二部分前注明“以下为历史初始化阶段计划，当前执行以主计划为准”

### `docs/rules/codex-rules.md`

- 修改点：
  - `R-153`
- 建议：
  - 缩窄到“高风险既有文件 / 破坏式语义变更确认门禁”
  - 普通实现类变更不应默认触发人工确认

### `docs/quality/validation-rules.md`

- 修改点：
  - `R-117`
  - `R-133`
  - `R-153`
- 建议：
  - 给 SonarQube、coverage 增加可执行说明或阶段性占位语义
  - 明确 C4 图可以是独立图文件，也可以是权威文字架构说明
  - 同步缩窄 `R-153`

### `docs/operations/local-development.md`

- 修改点：
  - Validation Baseline
- 建议：
  - 补充 coverage 与 Sonar 的当前执行方式
  - 如果尚未接入，明确标注为“Phase-F 前必须接入”

### `docs/plans/master-execution-plan.md`

- 修改点：
  - `Phase-C` 到 `Phase-D` 的当前执行入口
- 建议：
  - 增加 “Next Executable Tasks” 或 “Current Active Wave” 区块
  - 与 `tasks.md` 同步当前准备开工的首批任务

### `tasks.md`

- 修改点：
  - 当前为空
- 建议：
  - 从主计划实例化下一批活动任务
  - 让主计划与运行台账重新联通

### `docs/architecture/service-capability-map.md`

- 修改点：
  - 可保持现状
- 建议：
  - 继续作为当前服务边界执行权威
  - 后续新增模块时同步更新 Current carrier / Current state

### `docs/architecture/service-interface-contract-baseline.md`

- 修改点：
  - 可保持现状
- 建议：
  - 后续进入实现阶段时继续细化 DTO、事件版本、错误码枚举

### `docs/README.md`

- 修改点：
  - 新增本审计文档入口
- 建议：
  - 放入计划治理或复盘治理索引，供后续接手人快速理解“当前流程全貌和缺陷”

### `docs/plans/README.md`

- 修改点：
  - 新增本审计文档入口
- 建议：
  - 作为治理分析与流程说明文档归档

## 10. Final Assessment

结论分三层：

- 从文档治理完整性看：
  - 当前仓库已经具备较完整的 Harness Engineering 治理骨架
- 从执行闭环看：
  - 流程大体完整，但仍存在少量权威正文冲突、规则过严和门禁未工具化问题
- 从可直接大规模实现看：
  - 还差 P0 与 P1 级修订，特别是边界统一、规则收窄和活动任务实例化

当前最准确的判断不是“流程不完善”，而是：

- 治理框架已基本完善
- 执行口径仍需做最后一轮收口
- 计划层强于实施层
- 文档已能指导实现，但还需要少量修订来避免误读和阻塞
