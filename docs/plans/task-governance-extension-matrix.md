# SQLForge Task Governance Extension Matrix

## Summary

`task-spec-matrix.md` 已补齐 Harness Engineering 核心 10 字段。本文件继续补齐严格治理场景需要的 3 个扩展字段：

- `人工确认点`
- `数据影响`
- `回滚 / 恢复`

说明：

- 本文件不替代 `task-spec-matrix.md`，而是追加扩展治理字段。
- 其中“回滚”在 append-only 文档治理场景下通常表现为“追加纠正记录或兼容修复”，而不是删除历史。

## Phase-A

| Task ID | Human confirmation point | Data impact | Rollback / recovery |
|:---|:---|:---|:---|
| `A-TASK-001` | 无 | 无运行时数据；仅文档目录清单 | 追加更正清单并刷新矩阵 |
| `A-TASK-002` | 若要调整权威口径需人工确认 | 无运行时数据；仅文档角色标注 | 追加更正说明并修正索引 |
| `A-TASK-003` | 若冲突涉及删规则/改边界则进入 `HC` 台账 | 无运行时数据；仅缺口台账 | 追加补充冲突项，不删除历史项 |
| `A-TASK-004` | 若发现前端目标要被删减需人工确认 | 无持久化影响；前端现状盘点 | 追加更正差异说明 |
| `A-TASK-005` | 若后端事实与目标边界冲突需人工确认 | 无持久化影响；后端现状盘点 | 追加更正说明与证据 |
| `A-TASK-006` | 若部署目标语义变化需人工确认 | 无持久化影响；脚本/部署盘点 | 追加更正部署说明 |
| `A-TASK-007` | 若规则语义本身变化需人工确认 | 无运行时数据；规则映射 | 追加映射修正或缺口说明 |
| `A-TASK-008` | 冲突推荐方案需人工确认 | 无运行时数据；冲突台账 | 追加新方案，不覆写旧方案 |
| `A-TASK-009` | 模板字段删减需人工确认 | 无运行时数据；计划模板 | 追加模板扩展说明 |
| `A-TASK-010` | 若 active wave 恢复点或已执行任务归属需要改写历史语义则需人工确认 | 无运行时数据；主计划、矩阵、台账与 INBOX 对齐 | 追加 reconciliation 记录并修正矩阵/索引，不删除历史归档块 |
| `A-TASK-011` | 若剩余任务对齐需要把既有已完成事实重新归类、或修复动作会改变鉴权/租户/审计语义边界，则需人工确认 | 主计划、矩阵、台账、配置、日志与审计语义 | 追加 reconciliation 记录并恢复旧鉴权/审计路径或拆出兼容残差任务 |
| `A-TASK-012` | 若 shared 抽取会改变 header 鉴权、请求上下文或治理内调契约语义，则需人工确认 | 公共上下文、跨服务治理客户端、审计元数据透传链路 | 恢复服务本地实现或增加兼容包装层，并保留 shared 抽取后的回归测试 |

## Phase-B

| Task ID | Human confirmation point | Data impact | Rollback / recovery |
|:---|:---|:---|:---|
| `B-TASK-001` | 若阶段0状态要从事实改回目标语义需人工确认 | 无运行时数据；阶段状态文档 | 追加状态更正记录 |
| `B-TASK-002` | 判定标准若改变历史语义需人工确认 | 无运行时数据；状态口径 | 追加口径修订说明 |
| `B-TASK-003` | 无 | 无运行时数据；验证结果回写 | 追加更正验证条目 |
| `B-TASK-004` | ADR 编号或结论重排需人工确认 | 无运行时数据；ADR 实体/索引 | 追加 ADR 或状态修正 |
| `B-TASK-005` | 若生产部署口径改变需人工确认 | 无运行时数据；部署规范 | 追加修正文档，不删除历史说明 |
| `B-TASK-006` | 若交付元数据与 git 历史冲突需人工确认 | 无运行时数据；交付记录 | 追加 repair record |
| `B-TASK-007` | 冲突定性需要人工确认 | 无运行时数据；冲突台账 | 追加冲突说明 |
| `B-TASK-008` | 若原始归档语义发生实质变更需人工确认 | 无运行时数据；归档规则说明 | 追加澄清说明 |
| `B-TASK-009` | 角色体系删改需人工确认 | 无运行时数据；身份/角色规范 | 追加角色矩阵修订 |
| `B-TASK-010` | 租户/资源隔离边界调整需人工确认 | 无运行时数据；资源与授权规范 | 追加授权顺序修订 |
| `B-TASK-011` | 审计保留、敏感字段处理、失败策略变更需人工确认 | 无运行时数据；合规文档规范 | 追加合规修订说明 |

## Phase-C

| Task ID | Human confirmation point | Data impact | Rollback / recovery |
|:---|:---|:---|:---|
| `C-TASK-001` | 若 common 边界放宽到业务逻辑需人工确认 | 无直接持久化；公共能力清单 | 回退到文档边界并恢复模块归属 |
| `C-TASK-002` | 若包结构偏离强制分层需人工确认 | 代码结构影响，无直接数据变更 | 恢复目录结构或增加兼容层 |
| `C-TASK-003` | 若迁移会破坏现有服务边界需人工确认 | 共享代码迁移，可能影响序列化/错误码 | 恢复旧调用点并保留兼容包装层 |
| `C-TASK-004` | 生产配置职责重分配需人工确认 | 环境配置数据 | 恢复原 profile 配置并补兼容键 |
| `C-TASK-005` | 消息模式默认值变更需人工确认 | 配置、消息表、运行模式 | 切回原消息模式并恢复旧配置项 |
| `C-TASK-006` | 无 | 消息管理接口、消息表记录 | 禁用新增入口并保留已有消息数据 |
| `C-TASK-007` | 若治理服务边界跨到其他服务需人工确认 | 代码分层调整 | 恢复原层次或增加过渡适配层 |
| `C-TASK-008` | 若租户校验策略放宽需人工确认 | 租户访问控制链路 | 恢复严格校验并清理越权配置 |
| `C-TASK-009` | 审计/数据源/调度扩展点若引入破坏式契约需人工确认 | 接口契约、错误码、配置元数据 | 恢复旧契约并保留兼容 DTO |

## Phase-D

| Task ID | Human confirmation point | Data impact | Rollback / recovery |
|:---|:---|:---|:---|
| `D-TASK-001` | 服务边界重切需人工确认 | 模块归属、接口文档 | 恢复边界文档并回退模块声明 |
| `D-TASK-002` | 错误码区间和外部接口破坏式变更需人工确认 | API 契约、调用方适配 | 保留旧版本 DTO/错误码兼容层 |
| `D-TASK-003` | 只读约束放宽或执行语义升级需人工确认 | 查询任务、执行日志、结果对象 | 禁用新路径并切回同步占位闭环 |
| `D-TASK-004` | 回滚/日志策略若影响审计完整性需人工确认 | 运行日志、回滚记录 | 关闭新增日志点或恢复旧处理链 |
| `D-TASK-005` | 优化任务模型若改变审批语义需人工确认 | 优化任务元数据 | 恢复旧任务结构并保留迁移脚本 |
| `D-TASK-006` | 无 | 异步任务记录、状态机数据 | 停用新接口并保留旧状态查询 |
| `D-TASK-007` | 建议输出字段删减需人工确认 | 建议 VO、前后端消费契约 | 恢复旧字段并标记废弃 |
| `D-TASK-008` | 压测模型若触及生产路径需人工确认 | 压测任务、报告元数据 | 禁用新模型并恢复只读占位路径 |
| `D-TASK-009` | 若压测入口可能写生产需人工确认 | 压测任务记录、调度状态 | 立即关闭入口并保留已生成审计证据 |
| `D-TASK-010` | 报告导出格式删改需人工确认 | 报告查询结果、导出元数据 | 恢复旧输出格式或保留双版本查询 |
| `D-TASK-011` | 核心表结构变更需人工确认 | schema、实体、关联键 | 提供兼容 DDL / 数据修复脚本并回退映射 |
| `D-TASK-012` | 审计链路覆盖范围缩减需人工确认 | 审计日志表和事件记录 | 恢复旧审计写入并保留补录脚本 |
| `D-TASK-013` | 加密策略或密钥管理变更需人工确认 | 敏感字段密文、配置 | 恢复旧密钥/算法并执行密文修复 |
| `D-TASK-014` | 若异步服务占位执行或审计兜底语义放宽需人工确认 | 异步任务、治理审计兜底与消息回退状态 | 恢复旧鉴权/隔离/审计兜底路径并保留兼容补录 |
| `D-TASK-015` | 若真实执行适配放宽只读边界或引入破坏式执行契约需人工确认 | 查询执行适配配置、执行结果聚合、审计记录 | 切回最小同步基线并保留兼容执行模式/错误映射 |

## Phase-E

| Task ID | Human confirmation point | Data impact | Rollback / recovery |
|:---|:---|:---|:---|
| `E-TASK-001` | 主路由改名或删减需人工确认 | 前端路由结构 | 恢复原导航与入口映射 |
| `E-TASK-002` | 若隐藏已承诺信息板块需人工确认 | 前端展示数据，无后端持久化 | 恢复板块与原信息架构 |
| `E-TASK-003` | 若移除规则/合规展示需人工确认 | 前端展示数据，无后端持久化 | 恢复只读展示页面 |
| `E-TASK-004` | 五大业务页若改为合并页需人工确认 | 前端路由结构 | 恢复独立路由和页面壳层 |
| `E-TASK-005` | API 契约破坏式变化需人工确认 | 前端接口调用、缓存态 | 恢复旧 API 适配层或 mock 路径 |
| `E-TASK-006` | 设计系统 token 删除需人工确认 | 前端主题变量 | 恢复原 token 映射 |
| `E-TASK-007` | 分离检查口径放宽需人工确认 | 脚本规则，无业务数据 | 恢复严格检查项 |
| `E-TASK-008` | 若将权威逻辑重新放回前端需人工确认 | 前端状态和判断逻辑 | 恢复后端权威边界并移除越界逻辑 |
| `E-TASK-009` | 若让临时交付页替代 `/dashboard` 或在生产环境默认可见需人工确认 | 前端只读展示状态、环境开关与导航入口 | 恢复 `/delivery-progress` 临时定位、隐藏生产入口并回退到权威台账派生展示 |

## Phase-F

| Task ID | Human confirmation point | Data impact | Rollback / recovery |
|:---|:---|:---|:---|
| `F-TASK-001` | 生产部署拓扑调整需人工确认 | 部署文档和配置模板 | 追加修正文档并恢复旧拓扑说明 |
| `F-TASK-002` | compose 语义破坏式变化需人工确认 | 编排配置、脚本入口 | 恢复旧 compose 语义并补兼容脚本 |
| `F-TASK-003` | RPO/RTO 或环境提醒口径改变需人工确认 | 运维文档、恢复指引 | 追加更正提醒 |
| `F-TASK-004` | 无 | CI 清单和流水线映射 | 恢复原 CI 清单说明 |
| `F-TASK-005` | 门禁阻断规则放宽需人工确认 | CI 阻断逻辑 | 恢复原门禁规则 |
| `F-TASK-006` | 扫描阈值与工具变更需人工确认 | CI 质量结果、构建流程 | 回退扫描接入并保留报告 |
| `F-TASK-007` | 监控/日志采样策略显著削弱需人工确认 | 日志、指标、告警配置 | 恢复原监控规则 |
| `F-TASK-008` | 备份恢复目标或演练频率变更需人工确认 | 备份元数据、演练记录 | 恢复旧模板并补录演练 |
| `F-TASK-009` | 交付闭环若省略 commit/tag 回写需人工确认 | 交付记录、git 元数据、验证日志 | 补写交付记录和标签/提交元数据 |
| `F-TASK-010` | 若默认 CI 放宽 runtime smoke 阻断需人工确认 | CI runtime smoke、compose/health 证据 | 恢复默认 smoke 门禁并保留脚本入口 |
| `F-TASK-011` | 若多服务启动探针被降级回单服务检查需人工确认 | CI/runtime 多服务探针 | 恢复统一 runtime smoke 编排 |
| `F-TASK-012` | 若 query-execution 到 governance 的业务门禁被移出默认流水线需人工确认 | 跨服务业务 smoke、审计补偿证据 | 恢复 query-governance business smoke |
| `F-TASK-013` | 若 optimization/benchmark 跨服务门禁被放宽需人工确认 | 优化/压测业务 smoke 与补偿证据 | 恢复 optimization/benchmark smoke |
| `F-TASK-014` | 若浏览器真实业务 smoke 被降级为静态/占位检查需人工确认 | 前端真实业务 smoke、浏览器脚本 | 恢复 browser runtime gate |
| `F-TASK-015` | 若优化服务重新退回 in-memory carrier 需人工确认 | `optimization_task` 数据与调度状态 | 恢复 MySQL carrier 或保留迁移兼容层 |
| `F-TASK-016` | 若压测服务重新退回 placeholder carrier 需人工确认 | `benchmark_task` / `benchmark_task_report` 数据与调度状态 | 恢复持久化 carrier 或保留兼容查询路径 |
| `F-TASK-017` | 若前端失败恢复与补偿证据从默认门禁移除需人工确认 | 浏览器恢复路径与队列证据展示 | 恢复 browser runtime smoke 覆盖 |
| `F-TASK-018` | 若治理管理页删除已交付修复动作需人工确认 | 前端治理运维展示与调用路径 | 恢复 `/system` 治理页和修复动作入口 |
| `F-TASK-019` | 若补偿判定重新退化为单信号易抖动策略需人工确认 | 前端补偿指标与验证日志 | 恢复双信号判定并补录验证证据 |
| `F-TASK-020` | 若治理历史页再次退回 placeholder 页面需人工确认 | 历史诊断展示与运行时 smoke | 恢复 live history page 与 smoke |
| `F-TASK-021` | 若修复追溯页删减取证/补偿证据需人工确认 | repair evidence 展示与 drill-through | 恢复取证链路与兼容入口 |
| `F-TASK-022` | 若长期历史查询再次退回 recent-scan 语义需人工确认 | governance history 索引、分页与旧数据窗口 | 恢复 indexed lookup 并补兼容脚本 |
| `F-TASK-023` | 若审计取证分页与跨页 drill-through 被删减需人工确认 | forensic lookup、分页与前端路径 | 恢复跨页诊断链 |
| `F-TASK-024` | 若 remediation decision page 删除真实修复动作需人工确认 | 治理故障处置页面与修复调用 | 恢复 decision/remediation chain |
| `F-TASK-025` | 若 archival window / deep pagination 能力被削弱需人工确认 | governance history lookup 索引和查询窗口 | 恢复 long-window/deep-pagination 基线 |
| `F-TASK-026` | 若真实 Kafka gate 被降回文档占位或安全参数校验被弱化需人工确认 | Kafka runtime gate、配置与恢复证据 | 恢复 dedicated Kafka gate 与安全校验 |
| `F-TASK-027` | 若 Phase-F 退出门禁再次放宽为非阻断需人工确认 | phase gate、coverage、Sonar、R-118 证据 | 恢复阻断语义并补录缺失证据 |
| `F-TASK-028` | 若主线业务与治理运维路径重新混用需人工确认 | 前端 route namespace 与导航结构 | 恢复主业务/governance namespace 分离 |
| `F-TASK-029` | 若 release automation 会改变 delivery tag / write-back 语义、放宽 Sonar 必需约束或把 phase gate 自动触发绑定到错误发布事件需人工确认 | workflow、release metadata、coverage / Sonar 门禁结果与相关测试稳定性 | 恢复手工 phase gate 入口、保留自动化元数据证据，并回退到上一个可追溯发布路径 |

## Related Documents

- `docs/plans/task-spec-matrix.md`
- `docs/plans/phase-prerequisite-matrix.md`
- `docs/architecture/service-interface-contract-baseline.md`
- `docs/rules/codex-rules.md`
