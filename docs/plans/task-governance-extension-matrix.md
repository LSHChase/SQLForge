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
| `HARN-027` | 若要允许 candidate task 在未同步 master-execution-plan/task-spec/task-governance 矩阵前直接进入编码、自动越过 INBOX/人工确认点，或把“无 task 起步”的自动化扩展为可绕过 `preflight` / `instantiate` / `validate` / `task_audit` / `closeout` 的黑盒执行，需人工确认 | requirement-normalizer/plan-shaper/task-shaper/task-governance-reviewer prompt 模板、candidate task pack 模板、task-shaping 运行态、requirements-to-task/materialization/full-cycle 脚本，以及由 formal materialization 写入的 plan/matrix/ledger/exec-plan/raw-requirement 记录；不直接改变业务运行时数据 | 停用 governed full-cycle 脚本并回退到“人工写 plan + 人工建 task + `HARN-026` downstream full-auto”路径，保留 candidate task pack 和 review 证据作为治理记录，必要时拆出更细粒度的 task-shaping follow-up |
| `HARN-028` | 若要把 governed intake/healthcheck 升级为跳过确认直接改写台账真值、让 healthcheck 自动回滚或重写 closeout 记录，或允许 closeout 修复继续回到提交后追加 tracked `validation-log` 的模式，需人工确认 | governed intake/healthcheck 入口、run summary/建议字段、candidate materialization rollback、reservation 状态，以及 closeout 与 validation-log 的仓库级治理语义；不直接改变业务运行时数据 | 停用 intake/healthcheck 入口并回退到 `HARN-027` 的 requirements-to-plan / task-materialize 手工组合路径，保留 run summary / healthcheck 证据与回滚记录，必要时拆出更细粒度的 runtime hardening follow-up |
| `HARN-034` | 若要把本任务从只读 MCP 扩展为可写 MCP、把真实 connector 凭据或 server 配置落仓、或允许 MCP 绕过 `foreman` / `task_audit` / `closeout` 成为并行治理入口，需人工确认。 | 文档真值、规则账本、验证规则、治理编译产物、运行时校验脚本与 Codex 本地使用手册；不直接修改业务运行时数据。 | 按追加式治理回退 MCP 基线：移除本任务新增的 MCP 文档入口、规则、验证规则和自动化校验分支，恢复 `compile-governance` / `validate_codex_runtime` 的既有行为，并通过标准 validation 与 task-audit 证明仓库回到改造前治理基线。 |
| `HARN-035` | 若要把 `mcp_profile` 从 explorer / validator 的只读证据面扩展到 worker、允许任何角色通过 MCP 执行可写操作，或削弱 Main Foreman 唯一 write-back / validate / closeout 边界，需人工确认。 | multi-agent playbook、prompt 模板、manifest 契约、编排脚本与 `.codex/` 运行态元数据；不直接修改业务运行时数据。 | 回退 `mcp_profile` 合同改造：移除 multi-agent 文档、模板和脚本中的 MCP profile 字段与处理分支，恢复无 MCP profile 的现有 multi-agent 基线，并通过标准 validation 与 task-audit 证明收口链未被削弱。 |
| `HARN-036` | 若要让 chat-native router 在未显式确认前直接触发 `--confirm-run`、把 simple task 默认强制路由到 multi-agent、弱化 execution preview 固定字段合同，或削弱 Main Foreman 唯一 write-back / validate / closeout 边界，需人工确认。 | governed intake / full-auto 入口脚本、template adapter / hook / router 运行态、execution preview 合同、run_id/confirmation 元数据、验证规则与相关文档索引；不直接修改业务运行时数据。 | 回退 requirements artifact gate、execution preview/router 与 full-auto 硬门禁改造：恢复 `HARN-028` / `HARN-035` 之前的 intake/full-auto 行为，移除新增 preview/router 合同与强制校验分支，并通过标准 validation 与 task-audit 证明 Main Foreman 唯一收口、只读 MCP 边界和审计链未被削弱。 |
| `HARN-037` | 若要把 MCP doctor/healthcheck 扩展为远端自动运维、可写控制面、repo 落 secret/live inventory，或允许 multi-agent 中除 explorer/validator 外的角色消费 manifest-level `mcp_profile`，需人工确认。 | MCP 治理文档、onboarding/doctor/healthcheck 脚本或校验分支、compile/validate/runtime 入口、只读 evidence 写回说明，以及相关 runtime 元数据；不直接修改业务运行时数据，不得把 secret、token、endpoint 或 live server inventory 写入 repo-tracked 文件。 | 回退只读 MCP onboarding / doctor 改造：移除新增的 category onboarding、doctor/healthcheck、evidence 写回说明与定位文案，恢复 `HARN-034` / `HARN-035` 既有只读 MCP 基线，并通过标准 validation 与 task-audit 证明仓库仍保持 Main Foreman 唯一收口、只读 MCP 边界和无 secret/live inventory 落仓语义。 |
| `HARN-038` | 若要把 runtime repair 扩展为自动抹除失败 evidence、放宽 implementation-time dirty-worktree healthcheck 阻断、或绕过 Main Foreman / task_audit 的既有收口链，需人工确认。 | governed closeout / post-closeout runtime state、healthcheck/evidence 判定、执行计划与运行手册文档、以及验证日志与 closeout actual evidence 的治理语义；不修改业务运行时数据，不引入 repo 外第二真值。 | 回退 closeout/healthcheck/runtime repair 语义修复：恢复此前的 governed closeout / post-closeout 判定与 runtime cleanup 行为，保留失败 evidence 与 validation-log 审计链，通过标准 validation 与 task-audit 证明仓库仍保持 Main Foreman 唯一收口和 implementation-time dirty-worktree 阻断边界。 |
| `HARN-041` | 若要把 paused/archived/abandoned 语义扩展为自动重写台账真值、静默删除 runtime evidence、或允许未确认 candidate 继续 confirm-run，则必须先人工确认；本任务仅允许在现有 governed intake/runtime 边界内补齐可审计状态与恢复入口。 | 仅修改 governed runtime reservation/intake/task-shaping 状态语义、文档与运行时清理逻辑；不直接修改业务运行时数据，不把候选证据写成仓库长期真值。 | 若新增 reservation 生命周期语义导致 confirm-run、healthcheck 或 cleanup 行为异常，回退相关脚本与文档改动，并将受影响 reservation 状态恢复到先前的 released/candidate_ready/materialized 语义；历史 shaping evidence 保留在 .codex/state 下，不删除现有证据文件。 |
| `HARN-042` | 若要借本任务改变“不新增微服务”的既定边界、把 mock/日志模拟/抽象阶段误写为真实外部联通事实、跳过主计划/矩阵同步直接批量 materialize 业务任务、或削弱 Main Foreman 唯一 write-back / validate / closeout 入口，需人工确认。 | SQL 治理产品实施规格包、主计划与两张任务矩阵、后续 Story/Task inventory 与由此衍生的 shaped execution evidence；不直接修改业务运行时数据，不接真实外部服务。 | 回退本任务时，仅回退新增规格包、计划与矩阵增量，恢复到 `HARN-041` 后的治理基线；若后续业务任务已 materialize，则以追加 reconciliation 任务修正，不删除既有 task evidence。 |
| `HARN-043` | 若要借本任务改变 `HARN-042` 已归档历史、扩大“不新增微服务”边界、把接口/枚举/只读执行限制以外的实现内容偷渡进来，或跳过 Wave 1 任务正常 instantiate 流程，需人工确认。 | 修复 `HARN-042` follow-up 真值缺口的规格/计划/矩阵文本、补充的数据源/系统管理 Story/Task inventory，以及 Wave 1 启动前的治理收口；不直接修改业务运行时数据。 | 回退时仅回退 `HARN-043` 新增的规格/计划/矩阵修补与 inventory 增量，恢复到 `HARN-042` closeout 后状态；若 Wave 1 已实例化，则通过追加治理修正保留既有 task evidence。 |

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
| `D-TASK-030` | 若 provider-authenticated operations 需要引入未经确认的 provider-specific SDK/签名机制、把 environment-backed object storage 误写成仓库默认主路径，或让 batch retention/recovery 绕过既有鉴权/审计边界、删除当前仍需保留的 artifact，需人工确认 | provider-authenticated / environment-backed object-storage operation 请求与 live-evidence、artifact batch retention/recovery 执行摘要、失败分片、恢复来源与 governance 追溯留痕；不得落仓明文凭据，并持续保持 tenant 级隔离。 | 保持 repo-local lifecycle 与 `LOCAL_FILE` 默认主路径，关闭新增 provider-auth/batch orchestration 默认启用；对部分失败批次保留审计与恢复留痕，回退新增 retention/recovery/provider-auth 语义与文档说明，并恢复到 `D-TASK-029` 已验证基线。 |
| `D-TASK-031` | 若真实 parse/rewrite/acceleration suggestion 链会绕过统一授权入口、把高风险 rewrite 直接自动应用、对不支持方言假装解析成功，或把 placeholder 工件继续冒充真实结果，需人工确认 | `optimization_task` 任务数据、parse/rewrite/acceleration artifact、失败阶段与风险说明、schema/migration 与 runtime smoke 证据 | 保持 MySQL carrier 与 async 契约不变，关闭高风险 rewrite 规则或自动应用分支，回退新增 parser/rewriter/acceleration pipeline 与持久化字段说明，并恢复到 `D-TASK-030` 之后的已验证基线 |
| `D-TASK-032` | 若 acceleration plan 治理闭环会放宽审批/确认边界、绕过统一授权入口与治理审计、允许 fail-open 应用或省略回滚/验证证据，需人工确认 | acceleration plan / apply / verify / rollback 状态、跨服务治理记录、授权与审计证据、相关 schema 与契约载荷 | 保持 suggestion-only 默认边界，关闭 plan apply 默认启用，回退新增 acceleration governance 字段、状态机与文档说明，并恢复到 `D-TASK-031` 已验证基线 |
| `D-TASK-033` | 若 Hetu 集群证据与路由参数校准会放宽只读/影子环境边界、把外部测试环境结果误写成 repo-closed 默认事实、或降低当前结构化失败语义，需人工确认 | Hetu/MRS route calibration 参数、mode priority、env smoke/test-env evidence、执行与审计记录 | 保持 repo-closed Hetu 主路径与当前失败语义不变，关闭高风险校准默认启用，回退新增 calibration/live-evidence 文档与配置说明，并恢复到 `D-TASK-032` 已验证基线 |
| `D-TASK-034` | 若 benchmark-engine 的外部队列/文件存储/provider-native 语义会让 environment-backed path 误写成仓库默认主路径、引入未经确认的 provider SDK/凭据写入、或绕过既有鉴权/审计边界，需人工确认 | external queue/storage/provider-native 配置与运行摘要、artifact cleanup/recovery/write/readback 证据、跨服务追溯与审计留痕 | 保持 repo-local lifecycle 与 `LOCAL_FILE` 默认主路径，关闭外部队列/provider-native 默认启用，回退新增 queue/storage/provider 语义与文档说明，并恢复到 `D-TASK-033` 已验证基线 |
| `D-TASK-035` | 若缓存治理能力会放宽数据新鲜度/一致性边界、让缓存旁路/回填绕过授权或审计、把元数据占位误写成真实 cache governance，需人工确认 | cache policy、命中/失效/旁路/回填/风险标记数据、跨服务治理与审计证据、相关 schema 与运行文档 | 保持当前无强治理缓存默认边界，关闭高风险 cache policy 默认启用，回退新增 cache governance 字段、策略与文档说明，并恢复到 `D-TASK-034` 已验证基线 |
| `D-TASK-036` | 若 distributed cache backend 会放宽数据新鲜度/一致性边界、绕过统一授权入口或治理审计、把 provider/Redis 依赖写成仓库默认主路径、引入明文凭据或 fail-open 命中语义，需人工确认 | cache backend 配置、provider-native 读写/校验证据、cache policy apply/verify/invalidate 证据、命中/旁路/回填/失效数据、跨服务审计记录 | 保持 repo-closed in-memory cache governance baseline 为默认主路径，关闭 environment-backed distributed provider 默认启用，回退新增 backend contract/provider evidence/降级语义与文档说明，并恢复到 `D-TASK-035` 已验证基线 |
| `D-TASK-037` | 若 cache capacity / eviction / metrics governance 会放宽缓存新鲜度边界、让过期或被驱逐 entry 继续命中、引入高基数指标标签、绕过统一授权入口或治理审计、或把真实 Redis 长跑环境写成仓库默认事实，需人工确认 | cache policy capacity/ttl 配置、tenant/policy capacity counters、eviction reason evidence、cache governance metrics、policy verify runtime summary、benchmark/governance cache surface | 保持 D-TASK-036 repo-closed 默认主路径和 fail-closed 语义，关闭高风险 capacity/ttl 配置或 metrics 标签，回退新增 eviction/capacity/metrics 语义与文档说明，并恢复到 `D-TASK-036` 已验证基线 |
| `D-TASK-038` | 若历史追溯字段扩展会改变既有审计语义、删除已存证的 SQL/route/cache/trace 信息，或把未确认字段写成强制事实，需人工确认 | `query_history`、`execution_result`、导出/取证查询字段与索引 | 保留既有追溯链并以追加字段方式扩展；必要时通过视图/兼容 DTO 回退查询面 |
| `D-TASK-039` | 若查询执行摘要契约会改变既有受保护请求、失败语义或让 comment/binding/logical-object 信息在未校验时对外暴露，需人工确认 | 查询执行响应、审计摘要、前后端契约 | 保留原执行与错误响应路径，新增字段可降级为空，不删除旧字段 |
| `D-TASK-040` | 若历史查询面会引入越权钻取、跨租户可见性扩大或破坏已存在分页/审计约束，需人工确认 | governance 历史查询、详情、关联 drill-through 与索引 | 回退新增筛选/详情能力，恢复原历史查询面并保留新索引/字段供后续受控启用 |
| `D-TASK-041` | 若导出/取证视图会放宽敏感字段输出、破坏脱敏语义或把 PDF/SQL 导出写成默认生产事实，需人工确认 | 导出记录、取证视图、导出载荷与审计链 | 恢复原导出白名单与脱敏策略，禁用高风险格式并保留导出审计记录 |
| `D-TASK-042` | 若结构解析契约会把未实现的语义分析写成既成事实、删减问题分类维度或改变严重度/优先级口径，需人工确认 | 解析响应、问题分类、统计口径与文档基线 | 恢复上一版问题分类与评分字段，保留新增字段为可选扩展 |
| `D-TASK-043` | 若结构解析入口引入数据库依赖、阻断查询主路径或把低置信度结果伪装成高置信度，需人工确认 | structure parse 任务、结构化问题、query-date 与逻辑对象命中证据 | 关闭高成本分析支路，保留基础语法/结构解析与低置信度标识 |
| `D-TASK-044` | 若数据访问解析会阻断结构解析返回、把外部服务不可用误写成整体成功，或引入未确认的默认重试策略，需人工确认 | access parse 任务、服务状态、可达性/计划/分区/SLA 证据 | 恢复结构解析先返回、access parse 独立失败的既定语义，停用自动补跑 |
| `D-TASK-045` | 若综合结论会隐藏 partial success、抹平结构与 access parse 的状态差异，或删除 failure evidence，需人工确认 | parse task 总状态、历史详情与统计聚合 | 恢复双轨状态分开展示，保留 partial success 证据与失败原因 |
| `D-TASK-046` | 若统一逻辑对象模型会混淆 `BUSINESS_VIEW` 与 `DB_VIEW` 语义、扩大对象默认可见范围，需人工确认 | 逻辑对象目录、引用键、跨服务 DTO | 通过兼容视图恢复双模对象边界，并保留已落库对象数据 |
| `D-TASK-047` | 若业务逻辑视图目录与映射会引入未确认业务口径、删除既有物理映射或放宽租户隔离，需人工确认 | logic view 目录、映射表与相关治理查询 | 恢复原目录/映射快照，关闭高风险对象或映射规则 |
| `D-TASK-048` | 若 DB View 识别会误把复杂对象链写成确定事实、放宽跨源依赖边界，需人工确认 | DB view 依赖解析与展示数据 | 保留已识别依赖为 evidence，回退高风险展开逻辑为摘要模式 |
| `D-TASK-049` | 若逻辑对象统一展示会破坏现有 query/history/parse 契约兼容性，需人工确认 | 跨服务 DTO/VO 与前端消费面 | 保留旧 DTO/VO 兼容层，并回退统一对象字段为可选扩展 |
| `D-TASK-050` | 若批量解析批次模型与模板契约会把兼容格式、mock source 或未校验列写成正式运行时默认，需人工确认 | batch/task metadata、模板列、导入状态与批次统计 | 保留稳定格式优先与 mock 边界，回退高风险模板/状态语义 |
| `D-TASK-051` | 若稳定格式导入解析会在失败时丢失原始记录、绕过审计或把 access parse 强制为同步阻断，需人工确认 | 批量导入记录、parse task 批次编排与失败记录 | 恢复结构解析优先与失败留痕，不删除原始批次记录 |
| `D-TASK-052` | 若 `xls/et` 兼容支持会拖累主线、把兼容失败误写为平台故障，需人工确认 | 兼容格式解析逻辑与失败提示 | 回退兼容扩展到稳定格式基线，并保留失败原因说明 |
| `D-TASK-053` | 若报表清单 mock 入口会被写成真实接口联通事实、或改变 `report_code` 唯一键语义，需人工确认 | report batch 记录、mock source 解析与报表- SQL 映射 | 恢复 txt/mock 语义与 `report_code` 主键边界 |
| `D-TASK-054` | 若报表接口抽象会直接绑定真实外部接口、落 secret/live inventory 或破坏 mock 可回退路径，需人工确认 | 接口配置、client 抽象、报表 SQL 解析来源 | 回退到 mock 路径并移除高风险外部绑定 |
| `D-TASK-055` | 若统计口径与优先级评分改变已确认的 severity/priority/important/urgent 语义，需人工确认 | 评分规则、统计口径与相关查询面 | 保留旧评分/口径并追加新规则，不覆盖历史结果 |
| `D-TASK-056` | 若按 SQL / 场景统计会引入高成本查询、错误聚合或隐藏问题样本，需人工确认 | 统计聚合、样本明细、索引与缓存面 | 回退高成本聚合，恢复基础统计和样本可追溯性 |
| `D-TASK-057` | 若按报表统计会放大不可靠 mock 数据、或把失败解析也计入成功占比，需人工确认 | 报表聚合、占比计算与报表问题清单 | 恢复成功/失败分层与 mock 标识，纠正聚合口径 |
| `D-TASK-058` | 若重要/紧急矩阵会隐藏判定依据或用于替代原始 issue 结果，需人工确认 | priority matrix、important/urgent 视图与排序逻辑 | 恢复 issue 原始结果优先，矩阵仅作为派生视图 |
| `D-TASK-059` | 若推荐对象扩展会把“建议”写成“已执行结果”、或削弱收益/风险边界，需人工确认 | recommendation 对象、类型、收益/风险与状态字段 | 恢复 recommendation 只读建议语义，保留新增字段为未执行状态 |
| `D-TASK-060` | 若治理事件状态机会绕过外部拉取模式、自动推送真实装数、或删除失败/待拉取状态，需人工确认 | dispatch event、状态机、审计与回执链 | 恢复 pull-based 协同边界，保留全部事件状态证据 |
| `D-TASK-061` | 若推荐关联追溯会跨租户串链、暴露不应展示的 route/parse/history 关系，需人工确认 | recommendation trace keys、治理查询面与关联视图 | 回退跨链关联字段，恢复受保护的最小追溯面 |
| `D-TASK-062` | 若“只管理不装数”契约被扩展成直接执行装数、主动推送生产消息或默认联通外部模块，需人工确认 | recommendation 协同契约、dispatch 行为、文档真值 | 恢复 pull-only 与非执行边界，保留协同事件审计 |
| `D-TASK-063` | 若接入来源模型会让未受管入口绕过审计或混淆真实访问来源，需人工确认 | access channel、access audit 与相关 headers/metadata | 恢复显式来源分类与统一审计，关闭不明来源入口 |
| `D-TASK-064` | 若 HTTP API 接入会绕过统一鉴权/审计、扩大对外暴露面或删改既有契约，需人工确认 | 外部 API、认证上下文、审计记录与错误响应 | 回退对外入口到受保护最小基线，并保留现有内部契约 |
| `D-TASK-065` | 若 JDBC Agent `Observe` 会接管执行、写入敏感信息或在规则源失败时影响业务查询，需人工确认 | Agent JAR、采集上报、access audit 与 Redis 依赖 | 恢复 observe-only 语义，禁用高风险上报或敏感字段透出 |
| `D-TASK-066` | 若 JDBC Agent `Governed Execute` 会在平台不可用时无回退策略、或默认强制所有 SQL 走平台，需人工确认 | Agent 执行模式、fallback 策略、平台调用链 | 恢复租户/数据源级可切换边界和 fallback 语义 |
| `D-TASK-067` | 若 JDBC Agent `Local Rewrite + Direct JDBC` 会静默改写 SQL、绕过审计或改变查询语义，需人工确认 | 本地改写规则、direct JDBC 路径与上报链 | 回退为原 SQL 或 observe-only，保留改写失败记录 |
| `D-TASK-068` | 若 Java SDK 会把未稳定契约写成强依赖、绕过统一 request/trace 语义或暴露敏感配置，需人工确认 | SDK client、配置、请求重试与接入文档 | 回退 SDK 到最小 typed client 基线，并保留 HTTP API 主路径 |
| `D-TASK-069` | 若数据源连接配置与健康检查会落明文凭据、放宽租户隔离或把环境级 endpoint/secret 写入仓库真值，需人工确认 | datasource 配置、测试连接、健康状态与失败原因查询面 | 回退到只读 datasource 查询基线，移除高风险配置字段与敏感信息暴露 |
| `D-TASK-070` | 若 metadata snapshot / freshness / SLA / upstream-downstream 状态会把无证据数据写成确定事实，需人工确认 | metadata snapshot、freshness/SLA/queryability/upstream/downstream 追溯面 | 恢复未知/未采集默认语义，保留证据来源与回退字段 |
| `D-TASK-071` | 若数据资产接口会扩大跨租户可见范围、暴露未授权对象详情或破坏现有查询性能边界，需人工确认 | datasource/schema/table/logical-view/db-view 查询面与详情接口 | 回退高风险详情字段与筛选面，恢复基础受保护查询 |
| `D-TASK-072` | 若系统管理配置接口会把 mock/config abstraction 误写成真实外部联通、或允许未经审批的配置生效，需人工确认 | 报表接口配置、Redis 规则源、装数协同策略与治理查询面 | 回退到查询/模拟基线，保留抽象配置但禁用高风险生效路径 |
| `D-TASK-073` | 若 Trino parser 依赖引入导致许可证、包冲突或大规模迁移，或查询意图理解会破坏旧结构解析响应、执行 SQL、访问生产数据、把启发式资源估算写成真实执行计划结论，需人工确认。 | 预期不变更持久化数据模型；影响结构解析 API 响应字段、前端展示契约、解析规则与文档说明。若实现需要新增数据库字段或改变 access parse 权限/元数据行为，必须升级为人类确认点后再推进。 | 回退新增 parser adapter、查询意图字段、风险/资源估算规则和前端展示块，恢复 D-TASK-045 / E-TASK-020 既有结构解析响应与解析工作台展示基线；保留文档更正记录与测试证据。 |
| `D-TASK-074` | 若修复需要历史 SQL 指纹回填、跨服务缓存迁移、数据库字段变更或把 fingerprint 解释为严格 SQL 语义等价证明，必须暂停并由人工确认。 | 不变更持久化模型；新请求生成的 sqlFingerprint 会对注释和字面量变化更稳定。历史已落库指纹不在本任务中回填或迁移。 | 回退 SqlFingerprintUtils 前处理增强、相关测试和文档补充，恢复 D-TASK-073 后的指纹行为；不触碰历史数据。 |
| `D-TASK-075` | 若实现需要执行用户 SQL、访问真实数据库/元数据、断言真实索引存在性、引入持久化字段或改变结构解析旧字段语义，必须暂停并由人工确认。 | 不变更持久化模型；影响结构解析 API 响应中的新增/更丰富风险标签、issues、featureSummary 计数和启发式资源估算。 | 回退递归 AST 特征提取、复杂 SQL 测试、风险码映射和文档补充，恢复 D-TASK-074 后的结构解析行为；不触碰历史数据。 |

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
| `E-TASK-017` | 若查询工作台增强会在前端复刻后端权威逻辑、引入越权字段展示或改变既有执行入口语义，需人工确认 | 查询页布局、状态编排与前端消费字段 | 保留后端权威，回退高风险前端判断逻辑，仅保留展示/编排层 |
| `E-TASK-018` | 若历史列表筛选会暴露未授权字段、跨租户可见数据或破坏分页性能边界，需人工确认 | 历史列表、筛选状态与前端缓存态 | 回退敏感筛选/列，恢复基础列表视图与分页 |
| `E-TASK-019` | 若取证详情会暴露未脱敏参数、内部错误栈或隐藏部分失败证据，需人工确认 | 历史详情页、SQL 三态和关联取证展示 | 恢复脱敏与失败证据显示边界，关闭高风险详情块 |
| `E-TASK-020` | 若解析工作台把 access parse 不可用伪装成结构解析成功、或在前端合并双轨语义导致用户误解，需人工确认 | 解析工作台页面状态、双卡展示与提示文案 | 恢复结构/访问解析分开展示与 unavailable 提示 |
| `E-TASK-021` | 若批量解析页会把兼容格式失败误写成产品故障、或把 mock 报表清单写成真实接口联通，需人工确认 | 批次页、导入模板、报表清单 UI 语义 | 保留稳定格式优先与 mock 标识，回退高风险文案/行为 |
| `E-TASK-022` | 若解析统计页会改变 severity/priority 口径、隐藏 important/urgent 判定依据，需人工确认 | 统计图表、矩阵与 drill-through 页 | 恢复既定统计口径与标签，保留新增展示为附加视图 |
| `E-TASK-023` | 若数据资产页会混淆业务逻辑视图与 DB View、暴露未授权对象详情，需人工确认 | 资产目录、对象详情、导航结构 | 恢复对象类型区分与权限控制，关闭高风险详情区域 |
| `E-TASK-024` | 若逻辑视图映射页会把数据到位状态、SLA 或热度写成确定事实而无证据来源，需人工确认 | 逻辑对象映射、freshness/SLA/heat 展示 | 恢复字段证据标识与默认未知状态 |
| `E-TASK-025` | 若路由治理页会暴露内部策略细节、误导用户把 environment-backed 证据写成默认事实，需人工确认 | 路由规则、决策详情与说明文案 | 回退高风险字段，恢复基于仓库真值的路由展示 |
| `E-TASK-026` | 若推荐中心会把“推荐”误写成“已执行装数”、或隐藏 dispatch 失败状态，需人工确认 | 推荐中心、dispatch 状态与说明文案 | 恢复 recommendation / dispatch 分离展示，保留失败/待拉取状态 |
| `E-TASK-027` | 若压测中心页会把模板/测试集能力写成已默认启用的真实运行时基线、或混淆回归与对比模式，需人工确认 | benchmark 页面、模板/TestSet UI 与报告对比面 | 恢复模板/测试集/报告分区，保留模式差异与未实现能力标识 |
| `E-TASK-028` | 若开放接入页会把 JDBC Agent 全模式、SDK 或真实接口联通写成既有事实，需人工确认 | 开放接入页、接入策略和文案 | 恢复到契约/规划态展示，明确当前落地阶段 |
| `E-TASK-029` | 若 Dashboard KPI 与待办会聚合不存在的数据、放大 environment-backed 指标权重或引入未审计来源，需人工确认 | Dashboard 聚合指标、卡片与待办清单 | 恢复基于治理查询面的 KPI，移除无证据来源聚合 |
| `E-TASK-030` | 若告警中心页会把模拟邮件写成真实通知成功、或隐藏 dedupe / ACK 语义，需人工确认 | 告警列表、详情、ACK 和通知状态展示 | 恢复 simulated 状态文案与完整事件状态链 |
| `E-TASK-031` | 若系统管理数据源/报表接口页会暴露敏感连接信息、误导用户认为真实外部接口已默认联通，需人工确认 | 系统管理中的 datasource、health-check、report-interface 展示面 | 恢复脱敏与 mock/config 标识，关闭高风险编辑入口 |
| `E-TASK-032` | 若 Redis 规则源、装数协同与系统参数页会把 environment-backed 配置写成默认已启用事实，需人工确认 | rule-source、dispatch policy、system-param/permission 展示面 | 恢复到查询/模拟状态展示，保留 simulated 或未联通提示 |
| `U-TASK-004` | 若实现会移除既有承诺路由、把样本化 KPI 写成全租户事实，或把无写 API 的治理页改成伪可写能力，需人工确认。 | 前端导航、解析工作区入口、Dashboard 指标表达、规格补充文档与验证脚本；不改写后端业务数据或外部系统状态。 | 回退到当前导航与 Dashboard 表达，保留 read-only、sampled、simulated 等边界文案，不新增对外部环境的强依赖。 |
| `HARN-045` | 用户已在 2026-04-28 对 HARN-045 执行模板、任务边界、输出物要求以及 Main Foreman 创建正式任务并进入实现流程作出明确确认。实现过程中若字段语义、候选值来源、权限边界或提交格式无法从权威材料确认，必须暂停请用户再次确认。 | 预期不变更持久化数据模型和后端接口契约；风险集中在前端表单提交格式、默认值、回显、校验和候选值过滤。若实现需要改变 API contract、数据格式或权限行为，必须升级为人类确认点后再推进。 | 普通 standard 任务按单任务单 commit 管理。若组件替换造成行为回归，优先通过该任务 commit 回退或在同一治理链路内做最小修复；文档和测试变更需与代码回退保持一致。不得使用 git reset --hard、git add .、git add -A、git commit -a 等破坏审计链的命令。 |
| `HARN-049` | Main Foreman 已在 HARN-049 preflight 后确认 materialization 边界：任务绑定 E-STORY-008 / Phase-E；实现复用现有 ROUTE_PATHS、ParseBatchCenterView、ParseRecordView、query_history、parse_batch、parse_batch_item 等当前仓库真值；新增或调整持久化仅限解析历史闭环所需字段、接口和兼容性补充；若发现必须改变权限、保留、脱敏策略或核心 parser 算法，则暂停并另行确认。 | 本任务不引入新的敏感数据类别和独立保留策略；解析历史闭环优先复用当前仓库已有 query_history 查询面、parse_batch / parse_batch_item 批量解析证据表和既有 tenant_id 隔离、分页、审计追溯语义。若实现需要补充字段或接口，只允许做兼容性新增，并继续遵守 R-031 至 R-038 的 MySQL 主持久化、后端历史查询、不可依赖浏览器临时状态、历史默认保留与敏感信息不得明文落库规则。解析 SQL 文本按现有历史/批量证据模型处理，不在前端新增本地持久化副本。 | 保留核心 parser 算法不变；若页面拆分或历史能力异常，可回退 HARN-049 单任务 commit，并通过治理台账记录回滚；若涉及数据库迁移，应提供可逆迁移或兼容性降级方案，确保旧解析流程仍可运行且不阻断单条解析。 |

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
| `F-TASK-034` | 若告警模型会弱化当前审计与去重边界、删除关键事件等级或改变责任人语义，需人工确认 | alert event/policy 数据模型与治理查询面 | 恢复既有告警分类与审计语义，保留新增字段为附加扩展 |
| `F-TASK-035` | 若关键事件判定会引入过度噪声、漏报关键故障或把 environment-backed 故障写成 repo 默认事实，需人工确认 | 告警判定规则、阈值与事件生成逻辑 | 回退高风险规则，恢复基础关键事件集 |
| `F-TASK-036` | 若模拟邮件日志会被误写成真实通知、或 dedupe 策略导致关键事件被静默丢弃，需人工确认 | 通知日志、dedupe 状态与告警审计链 | 恢复 simulated-only 语义与原始事件保留 |
| `F-TASK-037` | 若告警查询与 ACK 接口会破坏只读/确认边界、引入跨租户可见性扩大，需人工确认 | governance alert list/detail/ack 接口与数据可见范围 | 回退 ACK/查询粒度，恢复最小可见范围 |
| `F-TASK-038` | 若 benchmark template/test-set 契约会削弱只读、影子环境或阈值边界，需人工确认 | 模板/TestSet 数据模型、阈值与来源语义 | 恢复原 benchmark safety 语义，停用高风险模板字段 |
| `F-TASK-039` | 若批量测试集导入会把未校验 SQL、报表或参数直接提升为可信数据，需人工确认 | test set 导入记录、批次与成员清单 | 恢复严格校验和失败记录，保留导入 evidence |
| `F-TASK-040` | 若 parse-to-benchmark 联动会把解析问题自动视为可直接压测对象、绕过安全边界，需人工确认 | 解析结果到 test set 的联动对象与过滤规则 | 回退自动生成范围，保留人工筛选入口 |
| `F-TASK-041` | 若 recommendation-to-benchmark 会把推荐 SQL 自动执行为压测任务、绕过审批与安全边界，需人工确认 | 推荐对象与对比压测联动链路 | 恢复 recommendation 与 benchmark 的显式确认边界 |
| `F-TASK-042` | 若回归守护统计与告警会把实验性 benchmark 结果提升为默认生产风险判定，需人工确认 | regression summary、threshold hit 与 alert linkage | 恢复为显式模板/测试集范围内的回归守护，不扩大默认告警面 |

## Related Documents

- `docs/plans/task-spec-matrix.md`
- `docs/plans/phase-prerequisite-matrix.md`
- `docs/architecture/service-interface-contract-baseline.md`
- `docs/rules/codex-rules.md`
