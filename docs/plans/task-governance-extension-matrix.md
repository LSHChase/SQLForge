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
| `HARN-025` | 若要把半自动多 agent 提升为默认自动执行路径、弱化 Main Foreman 唯一收口、允许 worker 修改台账/validation-log/closeout 文档，或用隐式 subagent 取代显式 `codex exec` + worktree 编排，需人工确认 | 文档真值、运行期 prompt 模板、manifest 编排、worktree orchestration 脚本，以及 `.codex/` 下的运行态 multi-agent 会话元数据；不影响业务运行时数据 | 停用 multi-agent 脚本与运行态目录，回退新增 docs/模板/脚本到单 agent `foreman` 路径，并保留 prompt/manifest 作为历史治理记录或拆出兼容改造任务 |
| `HARN-026` | 若要把全自动路径升级为“无任务治理前置、无 Main Foreman 收口、可绕过 `foreman validate/task_audit/closeout` 的黑盒自动执行”，或允许 auto-planner / auto-foreman 直接改写台账真值而不经过仓库审计链，需人工确认 | auto-planner / auto-foreman prompt 模板、requirement-driven exec plan 与 manifest 生成脚本、`.codex/state` 下的 full-auto 运行态产物，以及由 autonomous Main Foreman 落地到 `docs/` / 台账的最终收口记录；不直接改变业务运行时数据 | 停用 full-auto 脚本并回退到 `HARN-025` 半自动模式，保留需求输入、生成的 plan/manifest 和失败日志作为治理证据，必要时拆出更细粒度的 auto-planning/closeout follow-up |

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
| `D-TASK-016` | 若角色矩阵、资源模型或数据源授权矩阵被放宽为 fail-open，或三服务重新分叉授权入口，需人工确认 | governance 授权配置、跨服务授权决策、审计记录与 runtime smoke 证据 | 恢复统一授权入口、默认拒绝语义、被吊销访问阻断，以及权限变更审计补录 |
| `D-TASK-017` | 若真实 Hetu 集成重新退回 `SIMULATED` 冒充成功、放宽只读边界、绕过统一授权入口，或在未确认外部依赖时默认启用高风险生产参数，需人工确认 | query-execution Hetu 连接配置、执行链路、审计记录、runtime/env smoke 证据 | 恢复受控模式顺序、严格 HETU 路由失败语义、统一授权前置检查，并回退到上一版受控配置与文档说明 |
| `D-TASK-018` | 若去外键改动会破坏历史追溯链、弱化租户隔离，或把 Win10/IDEA 测试环境文档重新写回 Linux/Kafka/env-var 优先口径，需人工确认 | 核心 traceability schema、应用层引用完整性校验、部署文档与测试环境配置方式 | 恢复兼容 DDL / 引用校验与旧文档版本，或拆分为独立迁移批次并补数据修复与确认记录 |
| `D-TASK-019` | 若新增遥测暴露敏感信息、引入高基数标签导致生产指标失控，或削弱现有日志/审计语义以换取指标简化，需人工确认 | query-execution/governance 指标、执行模式信号、队列 backlog 可观测数据 | 移除高风险 meter、恢复仅日志审计语义，并回退到上一版稳定 tags 与文档说明 |
| `D-TASK-020` | 若异步服务新增遥测暴露敏感信息、为便于排障引入 task id / tenant id 等高基数标签，或削弱既有日志/审计语义以换取指标简化，需人工确认 | sql-optimization/benchmark-engine 指标、异步任务终态信号、worker/report 延迟可观测数据 | 移除高风险 meter、恢复以日志/审计为主的既有语义，并回退到上一版稳定 tags 与文档说明 |
| `D-TASK-021` | 若真实压测执行链会放宽只读/影子环境隔离、绕过统一授权入口/治理审计、或把占位导出直接冒充为真实快照导出，需人工确认 | benchmark-engine 执行配置、`benchmark_task` / `benchmark_task_report` 数据、报告快照/导出产物元数据、跨服务审计与 runtime smoke 证据 | 关闭新增真实执行/导出路径，恢复到当前持久化 placeholder 基线，并回退到上一版报告查询契约、隔离约束与审计说明 |
| `D-TASK-022` | 若外部 artifact storage 会泄露明文敏感数据、绕过 governance 追溯链/统一授权入口，或把环境级对象存储依赖误写成 repo-closed 默认主路径，需人工确认 | benchmark-engine artifact storage 配置、raw-data 下载快照、governance `config_snapshot/execution_result/query_history/export_record/audit_log` 追溯链、跨服务 runtime smoke 证据 | 关闭新增 artifact externalization / trace orchestration 路径，回退到当前 `benchmark_task_report` 持久化导出基线，并恢复上一版报告查询/下载契约与治理文档说明 |
| `D-TASK-023` | 若查询审计追溯增强会把错误的 trace/export 键写入 `audit_log`、让 cleanup 删除仍应保留的 artifact，或把 repo-local 生命周期语义误升级为环境级对象存储默认路径，需人工确认 | benchmark 报告/下载审计记录、`config_snapshot/execution_result/query_history/export_record/audit_log` 链接键、repo-local artifact 文件与 recovery/cleanup 证据 | 恢复到上一版报告查询/下载审计基线，关闭新增 recovery/cleanup 路径，并回退 artifact lifecycle 文档与验证说明 |
| `D-TASK-024` | 若 tenant-specific retention/backfill policy 会误删仍需保留的 artifact、让 environment-backed adapter 变成 repo-side 默认主路径、或引入未经确认的真实对象存储依赖/凭据写入，需人工确认 | benchmark artifact policy 配置、repo-local / environment-backed storage adapter 接线、artifact evidence 与 recovery/backfill 记录 | 保持 repo-local lifecycle 为默认主路径，关闭 environment-backed adapter 默认启用，回退新增 artifact policy/adapter 语义与文档说明 |
| `D-TASK-025` | 若 workload/backfill orchestration 会绕过 `query-execution` 现有只读/鉴权/审计边界、把 synthetic evidence 冒充成真实环境 live evidence、或把 environment-backed object storage 重新写成 repo-side 默认主路径，需人工确认 | benchmark/query-execution 内部 workload snapshot 与 backfill 证据、跨服务执行/审计记录、environment-backed object storage live evidence 与 runbook/验证留痕 | 保持 repo-local lifecycle 与 synthetic fallback 为默认仓库路径，关闭新增跨服务编排或 live evidence 默认启用，回退内部契约/文档说明并恢复到 `D-TASK-024` 已验证基线 |
| `D-TASK-026` | 若 workload/backfill evidence 的治理沉淀会弱化既有只读/鉴权/审计边界、把 synthetic backfill 冒充成真实 live capture，或把 environment-backed external write/recovery verification 误写成仓库默认主路径，需人工确认 | governance `config_snapshot/execution_result/query_history/export_record` 追溯载荷、benchmark/query-execution workload/backfill 证据、environment-backed object storage external write/readback evidence 与恢复留痕 | 保持 repo-local lifecycle 与 `LOCAL_FILE` 默认主路径，关闭 external write/recovery verification 默认启用，回退新增治理字段/adapter 语义与文档说明，并恢复到 `D-TASK-025` 已验证基线 |
| `D-TASK-027` | 若 compensation-replay orchestration 会绕过 `query-execution` 既有只读/鉴权/审计边界、把 compensated replay 冒充成原始 live capture，或把 provider-backed object-storage live evidence 误写成仓库默认主路径，需人工确认 | benchmark/query-execution compensation-replay 证据、governance 长期追溯载荷、provider-backed object storage write/readback/recovery 留痕 | 保持 repo-local lifecycle 与 `LOCAL_FILE` 默认主路径，关闭 provider-backed live evidence 默认启用，回退新增 compensation/provider 语义与文档说明，并恢复到 `D-TASK-026` 已验证基线 |
| `D-TASK-028` | 若 provider-specific / multi-provider contract 会把 provider 差异误写成统一默认能力、让 cleanup/recovery 误删仍需保留的 artifact、绕过既有只读/鉴权/审计边界，或把 provider-backed object storage 误写成仓库默认主路径，需人工确认 | provider-specific / multi-provider artifact contract 配置、cleanup/recovery/failure-replay 证据、benchmark/query-execution compensation-replay 结构载荷，以及 governance 历史查询/恢复面的 provider 与 recovery 留痕 | 保持 repo-local lifecycle 与 `LOCAL_FILE` 默认主路径，关闭新增 provider/multi-provider 默认启用，回退 cleanup/recovery/provider 语义与治理查询字段说明，并恢复到 `D-TASK-027` 已验证基线 |
| `D-TASK-029` | 若 provider-native live evidence 会引入未经确认的 SDK/凭据写入、把 environment-backed object storage 误写成仓库默认主路径，或让 governance-triggered cleanup/recovery 绕过既有鉴权/审计边界、删除当前仍需保留的 artifact，需人工确认 | provider-native / environment-backed live-evidence 配置与 manifest、artifact cleanup/recovery operation 请求/审计/追溯留痕，以及 governance 历史操作面返回的 operation surface | 保持 repo-local lifecycle 与 `LOCAL_FILE` 默认主路径，关闭新增 provider-native live evidence 与 governance-triggered operation 默认启用，回退 cleanup/recovery operation surface 与 live-evidence 语义说明，并恢复到 `D-TASK-028` 已验证基线 |

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
| `E-TASK-010` | 若供应链/环境策略要求彻底禁用 `@vitejs/plugin-vue` 或任意层级 `@vue/compiler-sfc`，需人工确认是否把问题升级为前端架构迁移而非版本调整 | 根级前端 toolchain 声明、锁文件与依赖约束证据 | 恢复上一个已验证的 toolchain 组合，并把依赖禁用冲突回写到 INBOX/计划而不是伪造“已解决”结论 |
| `E-TASK-011` | 若非 SFC 迁移会改变既有页面 IA、削弱代理请求头/认证透传语义，或删除 portable 产物交付能力，需人工确认 | 根级前端构建链、路由与组件实现、portable 包产物与启动脚本 | 恢复已验证的 SFC 迁移前后边界，保留 portable 交付入口，并回退破坏式路由/代理改动 |
| `E-TASK-012` | 若布局修复需要重新引入 `@vitejs/plugin-vue` / `@vue/compiler-sfc`、放弃现有设计 token，或用回退页面结构替代样式修复，需人工确认 | 前端壳层样式、设计 token、portable 产物中的页面布局表现 | 保持非 SFC 构建链不变，回退高风险样式/结构修改，并恢复到上一个已验证的布局基线 |
| `E-TASK-013` | 若 portable 验证被降级为 health-only 检查、分包方案改变路由/代理/缓存语义，或为压低 chunk 告警而牺牲关键页面可用性，需人工确认 | portable 浏览器 smoke 覆盖、前端 chunk 输出、关键路由与代理语义 | 保留当前 portable 包与关键路由语义，回退高风险分包策略，并恢复到现有可工作的构建输出 |
| `E-TASK-014` | 若恢复 Vue SFC 需要放弃现有 portable 包、浏览器 smoke、显式 Element Plus 注册或分包策略，或改变既有页面 IA / 代理语义，需人工确认 | 根级前端 `.vue` 源文件、Vite SFC 构建链、portable 产物、chunk 输出与路由/代理语义 | 保留 `E-TASK-013` 已验证的 portable 与分包结果，回退高风险 SFC 恢复改动，并恢复到上一个已验证的前端交付基线 |
| `E-TASK-015` | 若新增 dev browser smoke 会替代既有 full-stack runtime smoke、削弱现有 portable / 代理语义验证，或为清理叙事而改写历史任务完成记录，需人工确认 | Vite dev server 浏览器 smoke 覆盖、当前前端治理叙事、路由/代理语义 | 保留 `E-TASK-014` 已验证的 Vue SFC / portable 基线，回退高风险 dev smoke 或文档清理改动，并恢复到上一个已验证的前端交付真值 |
| `E-TASK-016` | 若把 dev browser smoke 从当前 local repo-closed 基线升级为默认 CI/runtime gate、削弱现有 full-stack runtime smoke 主路径，或通过该任务改写 `E-TASK-015` 的历史完成结论，需人工确认 | dev browser smoke 的边界定义、前端验证语义、CI/runtime gating 叙事与文档表述 | 保留 `E-TASK-015` 已验证的 local dev smoke 基线，回退高风险边界/脚本/文档改动，并恢复 full-stack runtime smoke 作为多服务主路径的既有真值 |

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
| `F-TASK-030` | 若 coverage 提升方案会删除既有测试、放宽 85% 门槛，或 Sonar 发布环境接线涉及敏感 secrets 管理策略调整需人工确认 | 覆盖率结果、CI/release 环境变量、Sonar 扫描结果与相关测试资产 | 恢复到当前自动阻断发布路径，保留覆盖率 / Sonar 失败证据，并回退新增测试或 workflow 环境接线 |
| `F-TASK-031` | 若要把 Sonar 或真实 Kafka 再次恢复为仓库默认硬阻断，或改变 repo-closed / environment-backed 双层边界，需人工确认 | phase gate/release gate workflow、脚本默认值、门禁文档口径、INBOX 环境恢复项 | 恢复 fallback 语义、保留环境恢复 runbook 与 INBOX 追踪，必要时再拆独立任务重新升级为强制门禁 |
| `F-TASK-032` | 若要把“环境已 provision”重新视为“默认自动启用 Sonar 强制门禁”，或恢复 release workflow 的环境级默认绑定，需人工确认 | CI/release workflow 触发条件、Sonar enable flag、环境恢复 runbook、INBOX 语义与部署基线 | 恢复当前显式 enable 语义，保留 provisioning 证据与恢复入口；如要再次升级为默认强制，需拆新任务追加治理记录 |
| `F-TASK-033` | 若要把测试环境 minimal smoke 升级为仓库 repo-closed 主路径替代项、重新引入对测试环境内部 DB/容器的强绑定，或要求外部环境 owner 接受新的破坏式认证/访问前提，需人工确认 | 环境无关 smoke 脚本、外部测试环境 CI/CD 接入方式、health/API 断言语义与部署文档真值 | 保留新增入口为 environment-backed 部署后验证层，回退对外部环境的强绑定假设，并继续维持本地 runtime smoke 作为 repo-closed 主路径 |

## Related Documents

- `docs/plans/task-spec-matrix.md`
- `docs/plans/phase-prerequisite-matrix.md`
- `docs/architecture/service-interface-contract-baseline.md`
- `docs/rules/codex-rules.md`
