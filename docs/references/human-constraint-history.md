# Human Constraint History

本文件为 append-only 历史账本，用于记录人类明确表达的长期规则、边界和执行约束。

## 2026-04-18T00:00:00-06:00

- 初始化导入 SQLForge 架构初始化总文档，建立仓库内文档体系基线。
- 锁定当前规则索引范围为 `R-001` 至 `R-115`，以 `docs/architecture/init.md` 为权威来源。
- 锁定阶段0任务按顺序执行，单任务完成后必须停下等待下一条指令。
- 锁定技术栈：
  - 后端：Java 8、Spring Boot 2.7.x、MyBatis XML、Maven、Lombok、MapStruct
  - 前端：Vue 3、JavaScript、Element Plus 2.4+、Pinia、Vue Router 4、Vite
  - 基础设施：Nacos 2.2.x、Spring Cloud Gateway 3.1.x、Sentinel 1.8.x、XXL-JOB 2.4
  - 数据：MySQL 8.0/TDSQL、Redis 7.x、Kafka 3.6+、Hudi 0.14.0(COW)
  - 部署：华为云私有云，兼容 ARM64 + AMD64，UTF-8 + LF
- 锁定后端强制分层：
  - `controller -> service -> domain -> infrastructure`
  - 领域层无 Spring 注解
  - 包结构为“领域目录 + 分层子目录”
- 规则索引入口：`docs/rules/codex-rules.md`

## 2026-04-18T16:02:46-05:00

- 追加验证规则 `R-116` 至 `R-135` 到 `docs/rules/codex-rules.md`。
- 新增独立查阅文档 `docs/quality/validation-rules.md`，同步收录 `R-116` 至 `R-135`。
- 追加原因：将验证行为固化为规则，防止人工遗漏。

## 2026-04-18T16:02:47-05:00

- 事件：将“人类手动发送验证话术”固化为“Codex自动触发验证规则”。
- 新增规则：`R-136` 至 `R-143`。
- 影响：人类无需记忆验证话术，Codex 完成任务后自动输出验证报告。

## 2026-04-19T00:00:00-05:00

- 事件：追加 Kafka 本地开发抽象模式规则，固化 Kafka 相关开发默认不依赖本地 Kafka 服务。
- 新增规则：`R-144`。
- 约束：
  - Kafka 相关任务必须优先采用数据库模拟模式，保留接口转发与内存队列模式作为特定场景补充。
  - 业务层必须依赖消息抽象接口，禁止直接耦合 Kafka 具体实现。
  - 多环境配置必须支持 `messaging.mode` 切换，并同步维护消息抽象文档与接口契约。
- 文档落点：`docs/architecture/messaging-abstraction.md`。

## 2026-04-19T09:00:00-05:00

- 事件：将 Supabase-inspired 深色设计方向固化为 SQLForge 前端长期参考原则。
- 约束：
  - Dashboard 首页定位为“运营总览”，负责状态摘要、风险建议和五大主功能分发。
  - `SQL 查询`、`解析记录`、`压测报告`、`加速配置`、`系统管理` 继续保留为独立业务页面。
  - 前端默认采用 dark-mode-native 深色基线：`#171717` / `#0f0f0f`。
  - 品牌绿仅用于强调和识别，不用于大面积背景。
  - 页面深度以边框层级、透明表面和低阴影为主，不再沿用浅蓝玻璃风格。
  - 排版遵循低行高 Hero、默认 `400` 权重、monospace 技术标签的控制台表达。
  - 当前范围仅覆盖桌面端 Web，移动端规范延后。
- 文档落点：`docs/frontend/design-system.md`。

## 2026-04-19T23:59:00-05:00

- 事件：将阿里 Java 代码规范固化为 SQLForge 的长期 Java 开发参考基线。
- 新增规则：`R-145` 至 `R-150`。
- 约束：
  - Java 规范基线采用 `alibaba/p3c` 官方公开稳定版本“黄山版（2022-02-03）”。
  - 仓库内必须保留原始 PDF、README 快照与来源元数据。
  - Java 规范通过文档、规则和自动化检查共同落地。
  - 存量 Java 代码整改采用分批治理，禁止大面积无边界重写。
  - 既有规则、文档、内容、要求、需求、架构、设计、流程不得删减；修改既有文件需先确认。
- 备注：前后端分离检测与改造作为本次专项治理执行，不追加到长期规则库。

## 2026-04-19T12:00:00-05:00

- 事件：人类明确要求 SQLForge 的所有执行计划、阶段、Epic、Story、Task 必须符合 Harness Engineering。
- 约束：
  - 任何计划类文档都必须先固化目标、成功标准、约束、依赖、接口边界、验证方案，再拆分执行。
  - 任何 Task 必须具备独立且真实执行的验证计划，不允许仅写形式化占位验证。
  - 任何计划拆分必须保持 task 内高内聚、task 间低耦合，并明确前置依赖与人工确认项。
  - 任何不确定、冲突、缺失或无法从仓库与文档直接证明的内容，都必须进入待确认清单，不能擅自补写为既成事实。
- 文档落点：
  - `docs/plans/README.md`
  - `docs/plans/master-execution-plan.md`

## 2026-04-19T13:00:00-05:00

- 事件：人类对主执行计划中的关键待确认项给出明确决策，并要求继续执行。
- 决策：
  - `HC-001`：项目最终形态锁定为 4 个微服务，不再以 11 个独立服务作为最终交付口径。
  - `HC-003`：必须补齐 `ADR-001` 至 `ADR-013` 实体文件。
  - `HC-005`：必须补齐 `docs/deployments/huawei-cloud-setup.md`，并保持与消息抽象和生产 Kafka 切换方式一致。
  - `HC-006`：访问控制与合规要求必须补全为完整规格，不再保留仅有 placeholder 的文档状态。
- 影响：
  - 主执行计划中的 `HC-001`、`HC-003`、`HC-005`、`HC-006` 进入已确认状态。
  - 后续服务拆分、ADR、部署与安全文档必须以本次决策为准。

## 2026-04-19T14:00:00-05:00

- 事件：继续执行后，对 `docs/references/raw-requirements/` 的历史规则冲突进行语义澄清并收口。
- 结论：
  - `docs/references/raw-requirements/` 是长期归档根目录，必须持续存在。
  - 初始化阶段允许其先为空目录；一旦引入原始资料，允许并要求在该目录下保留文件和子目录。
  - 后续不能再把“必须保持为空目录”作为长期验收口径。
- 落点：
  - `docs/rules/codex-rules.md` 追加 `R-155`
  - `docs/plans/master-execution-plan.md`
  - `docs/plans/phase-0-plan.md`

## 2026-04-19T15:30:00-05:00

- 事件：人类要求按严格模式把外部项目的 Easy Engine 规则完整分析后迁移到 SQLForge，并在确认后落地实现。
- 约束：
  - 不得直接照搬外部项目的服务名、模块边界、HTTP 路由、端口、运行时拓扑、测试矩阵或 artifact 名称。
  - 必须完整分析每一类输入规则，并给出“迁移、适配、替换、仅迁移思想或判定为项目专属不迁移”的结论。
  - 任何删除、更新、修改操作都要先输出给人类确认，再执行。
  - SQLForge 新增 harness 层后，任务状态真值固定在 `tasks.md` 与 `tasks-done.md`，人工决策入口固定在 `INBOX.md`，机器参数固定在 `.agent/config.json`。
- 新增规则：`R-156` 至 `R-161`。
- 落点：
  - `docs/rules/codex-rules.md`
  - `docs/quality/validation-rules.md`
  - `docs/operations/`
  - `tasks.md`
  - `tasks-done.md`
  - `INBOX.md`

## 2026-04-20T01:03:22-05:00

- 事件：人类确认按“仅追加修订、不丢信息、先治理文档”的方式执行文档治理专项，并要求继续实现。
- 决策：
  - 新增当前仓库真值基线文档，明确区分已实现事实、已确认目标、历史记录和原始归档。
  - 新增实现就绪文档，固化编码前的文档消费顺序、主题权威和执行波次。
  - 新增服务能力分配图，把 4 微服务目标与当前 `governance`、`sqlforge-shared` 的过渡关系写清。
  - 新增复盘模板，为后续阶段性交付提供标准复盘骨架。
- 约束：
  - 本轮只做追加式勘误、索引增强和真值分层，不删除、不覆盖历史语义。
  - 后续编码默认先读真值基线，再读初始化文档和规则库。
  - 当前 `governance` 只能向“公共管理服务”边界收敛，不能继续承载其他三个目标服务的长期主流程。

## 2026-04-20T01:26:01-05:00

- 事件：对文档治理专项进行严格复验后，继续补齐主阅读顺序、主计划证据集、主追踪矩阵与实际复盘实例。
- 落点：
  - `docs/README.md`
  - `docs/plans/master-execution-plan.md`
  - `docs/plans/document-coverage-matrix.md`
  - `docs/plans/document-governance-retrospective-2026-04-20.md`
- 约束：
  - 新增治理文档不能只停留在增量附录，必须进入主阅读顺序与主矩阵。
  - 后续治理专项若新增 `Authority` 文档，应同步进入主计划证据集、追踪矩阵、覆盖矩阵和复盘记录。

## 2026-04-20T16:00:00+08:00

- 事件：人类确认继续关闭严格核验中剩余的 7 个未完全闭口缺口。
- 决策：
  - 新增冲突/漂移/缺失矩阵，避免治理缺口只散落在主计划、真值基线和复盘中。
  - 新增阶段前置条件矩阵，显式列出阶段输入文档、ADR、规则、验证和人工确认点。
  - 新增服务接口契约基线，显式列出统一身份上下文字段、错误码归属、服务间 DTO/事件边界和审计字段。
  - 新增 Task 扩展治理字段矩阵，显式列出人工确认点、数据影响和回滚策略。
  - 本轮治理批次必须在 `tasks-done.md`、验证日志、复盘与 git history 中一致闭口。

## 2026-04-20T21:20:00+08:00

- 事件：人类要求把“AI 执行任务时默认始终保持严格模式、不得丢失任何信息、遇到不确定内容必须转由人类决定或补充”固化为长期项目规则。
- 新增规则：`R-165`。
- 约束：
  - AI 执行任务时默认开启严格模式，不得以省略、压缩、默认假设或静默跳过的方式丢失需求、约束、证据、历史记录、任务状态或文档同步项。
  - 任何不确定、冲突、缺失、无法证明或可能造成语义/历史丢失的内容，都必须先记录影响范围，再进入任务日志、执行计划或 `INBOX.md`，等待人类决定或补充。
  - 未经人类确认，不得把不确定内容补写成既成事实，也不得替人类做最终优先级、边界或验收判断。
- 落点：
  - `docs/rules/codex-rules.md`
  - `docs/operations/human-collaboration.md`
  - `docs/README.md`

## 2026-04-20T21:35:00+08:00

- 事件：人类明确要求保留现有正式业务首页，同时把“关注 AI 编码任务完成情况”的页面固化为独立的临时交付子页面，并明确其数据来源、更新责任和投产后隐藏机制。
- 新增规则：`R-166`。
- 约束：
  - 正式业务首页 `/dashboard` 必须保留，不能被 AI 交付进度展示替代。
  - AI 交付进度页仅用于研发/交付阶段观察 AI 任务的新增、修改、执行、阻塞、完成与验证状态，不属于正式产品功能。
  - 页面展示真值必须来自 `tasks.md`、`tasks-done.md`、验证日志、执行计划与 Git/tag 回写记录，不得自建平行状态源。
  - 生产环境默认隐藏该页面入口；项目全部结束并投产后，该页面不再对外展示，但历史权威记录必须继续保留。
- 落点：
  - `docs/architecture/init.md`
  - `docs/plans/master-execution-plan.md`
  - `docs/rules/codex-rules.md`

## 2026-04-21T02:42:41-05:00

- 事件：人类要求把“任务完成后自动调用 `/contract` 和 `/clear`”改写成符合 Harness Engineering 的正式项目口径。
- 新增规则：`R-168`。
- 约束：
  - 项目要求的是任务完成后的“上下文收缩”和“上下文清理”结果，而不是强绑定某个 Codex 客户端斜杠命令。
  - `/contract`、`/clear` 只有在当前运行环境稳定支持时，才能作为可选辅助动作；环境不支持时，必须通过台账回写、验证日志、`INBOX.md` / 执行计划补录和重新建立上下文完成等价动作。
  - 下一任务开始前必须重新按项目阅读顺序建立上下文，不得把上一任务的局部推理直接带入下一任务。
- 落点：
  - `docs/rules/codex-rules.md`
  - `docs/operations/foreman-workflow.md`
  - `docs/operations/git-and-task-closeout.md`
  - `docs/operations/human-collaboration.md`
  - `docs/README.md`

## 2026-04-23T05:30:00-05:00

- 事件：人类新增持久化设计约束，明确 MySQL / TDSQL 禁止外键约束。
- 新增规则：`R-169`。
- 约束：
  - 面向 MySQL 8.0、TDSQL 及兼容实现时，业务表不得新增物理外键约束。
  - 表间关系必须通过引用键、索引、应用层完整性校验和审计链维护。
  - 历史 schema 若存在外键约束，只能通过专项治理任务清理，不能在新增变更中继续扩散。
- 落点：
  - `docs/rules/codex-rules.md`
  - `docs/architecture/persistence.md`

## 2026-05-08T21:45:00-05:00

- 事件：人类要求把后台页面实现规则固化为长期前端规范，本轮只固化规则，不实现具体页面代码。
- 新增规则：`R-177` 至 `R-184`。
- 约束：
  - 前端页面实现基线固定为 Vue 3 + Element Plus + 自研组件，新增页面必须优先复用既有主题、组件、composable 和 contract 检查。
  - 管理后台页面默认采用顶部搜索筛选、主体操作按钮 + 表格、底部分页 / 状态栏结构。
  - 除明确需求、概览和 KPI 区域外，禁止用多层卡片堆叠承载主要功能；优先使用列表、表格、弹窗、抽屉、提示、Tab 和上下 / 左右分区。
  - 搜索表单、表格列、编辑弹窗、字典翻译、分页和进度反馈必须优先数据驱动并抽象为共享组件或 composable。
  - 前端不得自造权限结论；按钮展示、禁用和隐藏只做体验层控制，后端仍是权限、业务校验、数据隔离和审计权威。
  - 页面必须分离 API / 状态 / UI 编排，落实 i18n，并按触发面执行前端三件套、表单治理、SQL UI 或相关静态契约测试。
- 落点：
  - `docs/rules/codex-rules.md`
  - `docs/frontend/design-system.md`
  - `docs/frontend/form-component-governance.md`

## 2026-05-08T21:55:00-05:00

- 事件：人类要求把 `R-177` 至 `R-184` 从文档约束补强为前端页面新增、修改、重构时的自动化门禁。
- 约束：
  - 新增 `scripts/check-frontend-page-governance.mjs`，检查变更管理页的 i18n 使用、明显硬编码文案、表格 / 分页 / 弹窗 / 字典模式和 `<el-card>` 嵌套高风险结构。
  - `foreman validate` 只要检测到 `src/views/**/*.vue`、`src/components/**/*.vue` 或 `src/locales/**` 变更，必须自动追加 `npm run lint`、`npm run build`、`npm run test:form-governance`、`npm run test:sql-ui-contract` 和 `npm run test:frontend-page-governance`。
  - `task-spec-matrix.md` 必须把 `R-177` 至 `R-184` 和上述验证命令作为所有 `VUE-FE` / frontend 页面任务的默认叠加规则，避免只依赖执行者自觉判断。
- 落点：
  - `scripts/check-frontend-page-governance.mjs`
  - `scripts/foreman.py`
  - `package.json`
  - `docs/rules/codex-rules.md`
  - `docs/plans/task-spec-matrix.md`
