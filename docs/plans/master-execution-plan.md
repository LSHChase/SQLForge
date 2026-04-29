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
  - `docs/plans/document-truth-baseline.md`
  - `docs/plans/document-gap-matrix.md`
  - `docs/architecture/init.md`
  - `docs/rules/codex-rules.md`
  - `docs/quality/validation-rules.md`
  - `docs/plans/implementation-readiness.md`
  - `docs/plans/phase-prerequisite-matrix.md`
  - `docs/architecture/messaging-abstraction.md`
  - `docs/architecture/service-capability-map.md`
  - `docs/architecture/service-interface-contract-baseline.md`
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
- 全部 Task 的 Harness 10 字段补全集见：
  - [task-spec-matrix.md](./task-spec-matrix.md)
- 全部 Task 的人工确认点、数据影响与回滚扩展字段见：
  - [task-governance-extension-matrix.md](./task-governance-extension-matrix.md)
- 本轮治理专项复盘见：
  - [document-governance-retrospective-2026-04-20.md](./document-governance-retrospective-2026-04-20.md)
  - [document-governance-repair-retrospective-2026-04-20.md](./document-governance-repair-retrospective-2026-04-20.md)

### 2.2 Repository facts already verified

- `node scripts/lint-repository-knowledge.js` passed
- `node scripts/check-frontend-backend-separation.js` passed
- `mvn -B test` passed
- `npm run build` passed
- `npm run lint` passed

### 2.3 Current implementation facts

- 当前仓库存在根级前端工程，入口为 `package.json`、`vite.config.js`、`src/`
- 当前 Maven 聚合工程包含 `sqlforge-shared/`、`governance/`、`query-execution/`、`sql-optimization/` 与 `benchmark-engine/`
- `sqlforge-shared/` 已有共享错误码、上下文、异常、审计契约、日志与工具基线源码
- `governance/` 已有基础应用、位于 `application` 包域内的 controller/service、统一请求上下文校验能力、MyBatis XML、配置文件和基础测试
- `query-execution/` 已有独立应用入口、分层骨架、边界定义、多环境配置和基础测试
- `sql-optimization/` 已有独立应用入口、异步任务模型、MySQL 任务持久化与 scheduled worker 基线
- `benchmark-engine/` 已有独立应用入口、压测任务/报告模型、MySQL 任务与报告持久化基线
- `docs/adr/` 已按 `ADR-001` 至 `ADR-013` 补齐实体文件
- `docs/deployments/` 已补齐 `huawei-cloud-setup.md`

### 2.4 Decisions confirmed after initial plan publication

- `HC-001` 已确认：最终服务目标固定为 4 个微服务
- `HC-003` 已确认并执行：补齐 `ADR-001` 至 `ADR-013`
- `HC-005` 已确认并执行：补齐华为云部署文档，并与 `R-144` 生产 Kafka 模式对齐
- `HC-006` 已确认并执行：访问控制文档补全为完整规格
- `HC-007` 已确认并执行：`A-TASK-010` 用于回补 post-publication 已执行任务并将主线恢复点显式切回 `Phase-D / D-TASK-015`

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

## 4.1 Current Active Wave

- 当前运行波次：`Phase-D / D-STORY-005`
- 当前活跃目标：`D-TASK-031` 至 `D-TASK-037` 已全部完成 closeout，分别把 `sql-optimization` 真实 parse/rewrite/acceleration suggestion 链、acceleration plan 治理闭环、`query-execution` 生产级 Hetu 路由校准证据、`benchmark-engine` 外部队列/provider-native 语义、真正的 cache governance 闭环、provider-neutral distributed cache backend baseline，以及 cache capacity / eviction / metrics governance baseline 推进到当前仓库真值。默认主路径仍保持 repo-closed，不得把 environment-backed provider、object storage、distributed cache 或真实 Redis 长跑/恢复演练写成仓库默认事实。
- `HARN-042` 已完成 closeout：SQL 治理平台实施规格包与 D/E/F 全量 Story / Task inventory 已写入仓库真值。
- 当前治理任务：当前没有新的已实例化治理收口任务。
- 当前下一条可执行主线任务：
  - 当前没有新的已实例化 repo-side mainline task。
  - 下一条 repo-side Wave 1 候选任务应从 `D-TASK-038` 启动。
  - `D-TASK-036` 与 `D-TASK-037` 均已完成 closeout，不再作为下一条候选任务。
  - 若继续 Phase-D 主线，应先基于最新 repository truth 重新塑形下一条 repo-side follow-up；真实 Redis 集群长跑和恢复演练仍属于 environment-backed follow-up，不应被 foreman 当成默认仓库主线。
- 说明：
  - `A-TASK-011`、`A-TASK-012` 已完成 active wave 对齐、跨服务鉴权/审计高优先缺口收口，以及 shared 认证与治理客户端支撑下沉。
  - `E-TASK-004`、`E-TASK-005`、`E-TASK-006` 已按 repository truth 完成业务页骨架、治理接口接入与设计系统基线；`E-TASK-007`、`E-TASK-008` 已完成前后端分离检查加固与前端受保护请求头清理；`E-TASK-009` 已补齐临时 AI 交付页与生产隐藏语义。
  - `E-TASK-010` 已收敛根级前端 Node/Vite/Vue 版本并核对 Vue SFC 构建约束，把 `@vitejs/plugin-vue` / `@vue/compiler-sfc` 的策略冲突真实回写到 `INBOX-003`，不再把架构迁移误写成单纯版本调整。
  - `E-TASK-011` 已完成当时策略下的后续仓库主线：把根级前端迁移到非 `.vue` 组件方案、移除活动 SFC 构建链，并新增可复制到其他主机运行的 `dist-portable/` 双产物前端包与本地代理启动脚本。
  - `E-TASK-012` 已修复非 SFC 迁移后的布局回归，恢复 Element Plus 壳层布局与设计 token 基线。
  - `HARN-019` 只用于修正 `E-TASK-011`、`E-TASK-012` closeout 后遗留的 active-wave / task-matrix / validation-log 漂移，不改写两条前端任务的历史完成结论；同时把 residual risk 收口为新的 repo-closed follow-up `E-TASK-013`，而不是重新回到 `INBOX-003` 待决策状态。
  - `E-TASK-013` 已完成 portable 包关键路由浏览器 smoke、代理语义校验和 Vite 分包收口；`E-TASK-014` 则承接新的人工决策，把根级前端恢复到允许使用 `@vitejs/plugin-vue` / `@vue/compiler-sfc` 的 Vue SFC 架构，同时保留 `E-TASK-013` 已落地的 portable 与 chunk hardening 成果。
  - `HARN-020` 只用于修正 `E-TASK-014` closeout 后遗留的 active-wave / validation-log 漂移，并把 residual risk 与 next-step 收口为新的 repo-side follow-up `E-TASK-015`；它不改写 `E-TASK-014` 的历史完成结论，也不把 full-stack runtime smoke 或 external-environment follow-up 误写成当前前端主线。
  - `E-TASK-015` 已完成前端 dev browser smoke 与当前真值文档清理；它在完成前是 Phase-E 的前端主线，但 closeout 后不应继续被写成“当前活跃目标”或“下一条可执行主线任务”。
  - `HARN-021` 已完成 `E-TASK-015` closeout 后的治理收口，恢复了“当前没有已实例化 repo-side mainline task”的仓库真值，并吸收了该批次的 append-only validation-log residue。
  - `HARN-022` 只用于在 `HARN-021` 之后承接新的人工决策：把 `E-TASK-015` 的 residual risk 塑形成新的 repo-side follow-up `E-TASK-016`，并明确 dev browser smoke 只保留为 local repo-closed baseline，不进入更广的 CI/runtime gating。
  - `E-TASK-016` 已完成 dev browser smoke local repo-closed baseline 语义固化；其 closeout 后的 active-wave 漂移由 `HARN-023` 收口，不改写 `E-TASK-016` 的历史完成结论，也不把该 smoke 提升为默认 CI/runtime gate。
  - `HARN-023` 只用于修正 `E-TASK-016` closeout 后遗留的 active-wave 漂移，并把下一条 repo-side mainline 显式塑形为 `D-TASK-021`；它不改写前端 smoke 边界，也不改变 environment-backed follow-up 的既有语义。
  - `F-TASK-001`、`F-TASK-002`、`F-TASK-003` 已完成部署文档、compose/脚本说明、环境提醒与恢复指引的真值修正。
  - `F-TASK-015`、`F-TASK-016` 已完成 `sql-optimization` 与 `benchmark-engine` 的持久化 carrier / scheduler 主线；`F-TASK-017` 至 `F-TASK-028` 也已把 browser runtime gate、治理历史链路、真实 Kafka gate 与 Phase-F 退出门禁推进到当前仓库真值。
  - `F-TASK-029` 已把发布链自动触发、coverage 阻断语义与 Sonar-required 失败语义写成当时仓库真值；`F-TASK-030` 已把 `phase1plus` 聚合覆盖率提升到 `86.9763%`，并保留 Sonar / release gate 接线成果。
  - `F-TASK-031` 已在不重写 `F-TASK-030` 历史完成记录的前提下吸收其 residual risk：把 Sonar 与真实 Kafka 从“默认强制门禁”降级为环境增强 fallback，并显式建立 `repo-closed` 主路径与 `environment-backed` 增强项的当前仓库真值。
  - `F-TASK-032` 已完成后续治理修正：收口了 `F-TASK-031` 复盘中发现的隐性自动恢复接线，包括 release workflow 的 `quality-gate` environment 默认绑定，以及主 CI 在仅有 Sonar secrets 时就自动重新强制扫描的问题。
  - `F-TASK-033` 已补齐外部测试环境部署后的最小 smoke 入口：仓库新增环境无关的 `scripts/run-env-smoke.sh`、测试环境 smoke 基线文档，以及与 CI / phase gate / local development / truth baseline 一致的语义说明；该任务按顺延编号处理，因为 `F-TASK-032` 已被既有 Sonar 治理任务占用，不能重写历史结论。
  - `HARN-011` 只用于修正 `F-TASK-032` closeout 后遗留的 active-wave / follow-up 措辞漂移，不改变 `F-TASK-032` 的历史完成结论，也不引入新的仓库主线实现范围。
  - `D-TASK-015` 已把 `query-execution` 推进到 feature-flagged Hetu 模式链基线；`D-TASK-016` 已完成统一授权入口收口；`D-TASK-017` 现已完成仓库侧真实 Hetu 集成与 smoke 分层，剩余 follow-up 转为外部 Hetu/MRS 环境的证据沉淀与运维参数校准。
  - `D-TASK-018` 承接 `R-169` 新增后的直接实现收口：移除历史 schema 外键、补齐应用层引用完整性校验，并把 Hetu/MRS 测试环境部署文档切到 Win10 + IDEA + yml 配置读取与确认清单口径。
  - `D-TASK-019` 已完成 `query-execution` 与 `governance` 的最小业务指标与执行遥测收口；其 closeout 后的 active-wave 漂移由 `HARN-017` 修正，不改写 `D-TASK-019` 的历史完成结论。
  - `D-TASK-020` 已完成 `sql-optimization` 与 `benchmark-engine` 的最小业务级 Micrometer 指标、异步任务终态信号与处理延迟观测收口；其 closeout 后的 active-wave 漂移由 `HARN-018` 修正，不改写 `D-TASK-020` 的历史完成结论。
  - `D-TASK-021` 已完成 `benchmark-engine` repo-closed 隔离执行链路、执行摘要持久化与 `JSON/PDF/HTML` 导出产物落库/回放收口。
  - `D-TASK-022` 已完成 repo-local artifact externalization、raw-data download 与 benchmark/governance trace-export orchestration 的 repo-side 收口；其 residual risk 收窄为 audit-link enrichment、artifact retention/recovery/cleanup 语义与更广 environment-backed 证据，不再把外部对象存储或环境级依赖误写成当前默认主路径。
  - `D-TASK-023` 已完成 `D-TASK-022` residual risk 的 repo-side 收口：它把报告/下载查询审计补齐到 trace/export 级别，并把 repo-local artifact lifecycle 收口为“保留当前 report-set、重写时清理陈旧文件、缺失文件可从持久化报告快照恢复”的仓库默认基线，而不是扩写为环境级对象存储默认方案。
  - `D-TASK-024` 已完成 `D-TASK-023` residual risk 的 repo-side 收口：它把 benchmark artifact 的 tenant-specific retention/backfill policy 接到 governance `tenant_config.retention_days`，并增加显式配置的 environment-backed object-storage adapter/evidence，同时仍把 `LOCAL_FILE` 保持为当前默认主路径，不把外部对象存储误写成已落地仓库事实。
  - `D-TASK-025` 已完成 `D-TASK-024` residual risk 的 repo-side 收口：它把 `benchmark-engine` worker 接到 `query-execution` 内部 workload capture 契约，成功时写入 live workload snapshot、失败时写入显式 synthetic backfill evidence，并为 `ENVIRONMENT_OBJECT_STORAGE` 增加 live-evidence manifest；默认主路径仍保持 repo-local lifecycle 和 synthetic fallback，不把 environment-backed 路径误写成仓库默认事实。
  - `D-TASK-026` 已完成 `D-TASK-025` residual risk 的 repo-side 收口：它把 workloadDigest/workloadSource/backfillApplied/workloadEvidence 显式沉淀进 governance `config_snapshot/execution_result/query_history/export_record` 追溯载荷，并让 `ENVIRONMENT_OBJECT_STORAGE` 在提供 external write dir 时执行真实 external write/readback recovery verification；默认主路径仍保持 `LOCAL_FILE`，不把 environment-backed 路径误写成仓库默认事实。
  - `D-TASK-027` 已完成 `D-TASK-026` residual risk 的 repo-side 收口：它把 mixed live/fallback workload 收口为显式 compensation-replay evidence，并让 `ENVIRONMENT_OBJECT_STORAGE` 在提供 provider endpoint 时执行真实 provider-backed write/readback recovery verification，同时把 provider/external verification 与 compensation evidence 一并沉淀进 benchmark/governance 长期追溯链；closeout 后当前重新回到“无已实例化 repo-side mainline task”的计划真值。
  - `D-TASK-028` 已完成 `D-TASK-027` residual risk 的 repo-side 收口：它把 provider-specific / multi-provider object-storage contract、cleanup/recovery semantics，以及 compensation-replay evidence 的 governance query/recovery surfaces 收口为已验证基线；closeout 后当前重新回到“无已实例化 repo-side mainline task”的计划真值。
  - `D-TASK-029` 已完成 `D-TASK-028` residual risk 的 repo-side 收口：它把 provider-native / environment-backed object-storage live evidence 进一步沉淀到 provider header/request-id 级别，并补齐 governance-triggered artifact cleanup/recovery operation surfaces；closeout 后当前重新回到“无已实例化 repo-side mainline task”的计划真值。
  - `D-TASK-030` 已完成 `D-TASK-029` residual risk 的 repo-side 收口：它把 provider-authenticated object-storage operations 与 governance-side batch retention/recovery orchestration 推进到当前仓库真值；closeout 后当前重新回到“无已实例化 repo-side mainline task”的计划真值。
  - `D-TASK-031` 至 `D-TASK-035` 已按新的业务主线全部完成 closeout：`D-TASK-031` 负责 `sql-optimization` 真实 parse/rewrite/acceleration suggestion 链，`D-TASK-032` 负责 acceleration plan 治理闭环，`D-TASK-033` 负责 query-execution 的生产级 Hetu 集群证据与路由参数校准，`D-TASK-034` 负责 benchmark-engine 的外部队列/文件存储与 provider-native 语义，`D-TASK-035` 负责真正的缓存治理能力；其后续 residual risk 已由 `D-TASK-036` 收口，把 cache governance 从 repo-closed in-memory baseline 推进到 provider-native distributed backend baseline。
  - `D-TASK-036` 已完成 closeout：它为 `query-execution` 补齐 provider-neutral cache backend contract、默认 in-memory backend、显式 Redis RESP provider adapter、backend/provider evidence 与 fail-closed bypass 语义。
  - `D-TASK-037` 已完成 closeout：它在不要求真实 Redis 集群长跑和恢复演练的前提下，收口 per-tenant / per-policy capacity limit、TTL/capacity/manual/schema eviction reason evidence、cache governance metrics、policy verify capacity/backend health summary，以及 benchmark/governance 对 eviction/capacity evidence 的延续透出；closeout 后当前重新回到“无已实例化 repo-side mainline task”的计划真值。
  - `HARN-025` 已把“半自动多 agent 协作基础设施（C 方案）”正式落为独立治理/工具能力：Main Foreman 唯一收口、多 `codex exec` 会话替代隐式 subagent、多 `git worktree` 隔离、manifest 驱动、prompt 模板化，以及最终仍走 `foreman validate` / `task_audit` / `closeout`；它不混入业务主线功能，也不改变当前 repo-side business mainline 为空的事实。
  - `HARN-026` 已完成 closeout：它把多 agent 能力从“Main Foreman 手工写 plan/manifest 再启动”升级到“从需求输入开始，由 codex 自动生成 exec plan、manifest，并驱动 prepare/launch/collect，再由 autonomous Main Foreman 继续 fan-in / validate / closeout”的全自动主路径；它仍保持 Main Foreman 唯一收口，不引入第二套长期真值，也不混入业务主线功能。
  - `HARN-027` 用于在 `HARN-026` 之上补齐“从无 task 开始”的上游治理自动化：先做 requirement normalization、candidate execution plan shaping、candidate task pack 与 governance gate，再 formal materialize 成正式 task，最后 handoff 给现有 `HARN-026` full-auto 执行链；它仍保持 Main Foreman 唯一收口，不绕过 `preflight` / `instantiate` / `validate` / `task_audit` / `closeout`。
  - `HARN-028` 用于继续加固 `HARN-027`：补齐 governed intake/healthcheck 入口、`executed_commands` 与细粒度 suggestion 输出、candidate materialization rollback，以及 closeout 后不得留下 `validation-log` tracked residue 的仓库级修复；它仍保持 Main Foreman 唯一收口，不引入第二套长期真值，也不绕过既有 `foreman` / `task_audit` / `closeout` 链。
  - `HARN-016` 已把外部 Win10 测试环境的 Hetu/MRS 实际联通与留证动作挂起到 `INBOX-002`；它继续是 blocked 的 environment-backed follow-up，不构成当前 repo-side mainline。
  - `INBOX-001` 继续保留为未来恢复 Sonar 强制门禁的环境恢复项；它仍是独立的 environment-backed follow-up，不改变当前仓库真值：当前没有新的已实例化 repo-side mainline，而 `HARN-016` / `INBOX-001` 仍只属于非主线的 environment-backed follow-up。

## 5. Traceability Matrix

| Source | Governs | Current repository target | Execution impact | Verification anchor |
|:---|:---|:---|:---|:---|
| `docs/plans/document-truth-baseline.md` | 当前仓库真值、历史记录、漂移映射 | 全仓 | 决定“已实现事实”和“目标架构”的读取方式 | 文档全量覆盖验证 |
| `docs/plans/document-gap-matrix.md` | 冲突、漂移、缺失与残余实现缺口矩阵 | 全仓 | 决定严格核验时的缺口闭口判断 | 文档全量覆盖验证 |
| `docs/architecture/init.md` | 总体架构、阶段、服务边界、任务拆解模板 | 全仓主基线 | 决定阶段拆解、服务边界、接口优先顺序 | `R-116`, `R-121` |
| `docs/rules/codex-rules.md` | 执行规则、工程约束、验证规则 | 全仓 | 决定可变更边界与验证门禁 | `R-117` 至 `R-154` |
| `docs/quality/validation-rules.md` | 阶段/Task/回归/环境验证 | 全仓 | 决定所有 Task 的验证格式 | `R-119` 至 `R-154` |
| `docs/plans/implementation-readiness.md` | 编码前置消费顺序与执行波次 | 全仓 | 决定任务入场条件、波次推进和冲突处理 | `R-116`, `R-133` |
| `docs/plans/phase-prerequisite-matrix.md` | 各阶段输入文档、ADR、规则、验证和确认点矩阵 | 全仓 | 决定阶段 ready/not ready 判断 | `R-116`, `R-163` |
| `docs/security/compliance.md` | 等保与审计 | 后端、部署、运维 | 决定身份、租户、审计、加密、备份 | `R-111` 至 `R-118` |
| `docs/security/access-control-spec.md` | 完整访问控制规格与阶段实现基线 | `governance` 及后续 4 微服务 | 决定身份、角色、资源、租户、审计和失败处理边界 | `R-111` 至 `R-115` |
| `docs/architecture/messaging-abstraction.md` | 消息抽象与环境切换 | `governance`, SQL, 配置, 脚本 | 决定 `DATABASE/MOCK/KAFKA` 模式 | `R-144` |
| `docs/architecture/service-capability-map.md` | 4 微服务与当前模块的过渡映射 | `governance`, `sqlforge-shared`, 后续新模块 | 决定模块归属、过渡实现和服务边界迁移顺序 | `R-126`, 文档全量覆盖验证 |
| `docs/architecture/service-interface-contract-baseline.md` | 统一身份上下文、错误码归属、DTO/事件和审计契约 | 后续 4 微服务与公共层 | 决定接口级约束和跨服务契约边界 | `R-057`, `R-068`, `R-111` 至 `R-115` |
| `docs/frontend/design-system.md` | 前端视觉与组件基线 | `src/` | 决定 Dashboard 和业务页视觉风格 | `R-023` 至 `R-030` |
| `docs/quality/alibaba-java-guidelines.md` | Java 代码规范治理 | `sqlforge-shared/`, `governance/` | 决定 Java 实现方式与扫描要求 | `R-145` 至 `R-154` |
| `docs/quality/frontend-backend-separation-baseline.md` | 前后端边界 | 根级前端与 Maven 后端 | 决定目录和职责边界 | 分离检查脚本 |
| `docs/plans/phase-0-plan.md` | 历史阶段0任务顺序 | 阶段0基线对照 | 用于真值修正，不直接代表当前全部现状 | `HC-002` |
| `docs/deliveries/init-completion.md` | 阶段0交付记录 | 初始化交付基线 | 用于交付状态与 Tag 回写对齐 | `HC-007` |
| `docs/adr/README.md` | 决策索引 | `docs/adr/` | 决定 ADR 落地缺口与优先级 | `HC-003` |
| `docs/plans/document-coverage-matrix.md` | `docs/` 全量文件覆盖证明 | `docs/` | 证明所有文档与归档资料已被盘点和分类 | 文档全量覆盖验证 |
| `docs/plans/task-spec-matrix.md` | Harness Task 字段补全集 | 全部 Task | 为每个 Task 补齐 10 个必填字段 | Task 规格完整性验证 |
| `docs/plans/task-governance-extension-matrix.md` | Task 的人工确认点、数据影响和回滚扩展字段 | 全部 Task | 为严格治理提供 10 字段之外的补充约束 | `R-164`, Task 规格完整性验证 |
| `docs/plans/document-governance-retrospective-2026-04-20.md` | 本轮治理专项复盘 | `docs/` | 沉淀漂移、差距和后续治理动作 | `R-133`, `R-140` |
| `docs/plans/document-governance-repair-retrospective-2026-04-20.md` | 本轮严格核验缺口修复复盘 | `docs/` | 沉淀 7 项缺口的闭口动作与批次一致性 | `R-133`, `R-140`, `R-162` |

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
| `A-TASK-005` | 盘点后端实现现状 | common、governance、测试、配置 | 无 | `mvn -B test` |
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

##### Story `A-STORY-004` 主计划与运行台账回联

- 目标：把主计划、任务矩阵、完成台账、验证日志与 INBOX 的真实完成度重新对齐，消除 active wave 与已执行任务脱节。
- 验证：主计划 current active wave、任务矩阵与 `tasks-done.md` 的任务编号和阶段顺序一致。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `A-TASK-010` | 主计划与运行台账对齐 | 回补 post-publication 已执行任务、修正 active wave、清理已解决 INBOX | `A-STORY-003` | `task_audit`、knowledge lint、主计划/矩阵/台账交叉检查通过 |
| `A-TASK-011` | 主计划剩余任务对齐并修复跨服务鉴权审计缺口 | 对齐 active wave、剩余任务与事实完成度，并收口跨服务鉴权、租户归一化与审计元数据高优先缺口 | `A-TASK-010` | `task_audit`、knowledge lint、主计划/矩阵/台账交叉检查、跨服务测试与配置断言通过 |
| `A-TASK-012` | 抽取 shared 认证与治理客户端支撑 | 把 query-execution、sql-optimization、benchmark-engine 重复的认证请求元数据与治理内部客户端支撑下沉到 `sqlforge-shared`，消除跨服务漂移并补 `R-126` 验证 | `A-TASK-011` | `task_audit`、knowledge lint、共享层编译回归、跨服务治理客户端与请求上下文测试通过 |

##### Story `A-STORY-005` 半自动多 agent 协作治理基础设施

- 目标：把 Main Foreman 唯一收口、多 `codex exec` 会话、多 `git worktree`、manifest 驱动和 prompt 模板化正式沉淀为仓库治理/工具能力，不混入业务主线功能。
- 验证：文档、模板、脚本、manifest 模板、SQLForge demo runbook、docs 索引和 closeout 审计链全部对齐，且不绕过现有 `foreman` / `task_audit` 流程。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `HARN-025` | 落地半自动多 agent 协作基础设施（C方案） | 以 Main Foreman 唯一收口、多 `codex exec` worker/explorer/validator 会话、多 worktree 隔离、manifest 编排、prompt 模板化和 worker 禁改台账/closeout 文档为前提，新增 multi-agent playbook、agent prompt 模板、prepare/launch/collect 脚本、manifest 模板与 SQLForge demo runbook，并保持最终验证/审计/closeout 仍走 `foreman.py` 标准动作 | `HARN-024` | `python3 scripts/foreman.py validate HARN-025`、`python3 scripts/task_audit.py --check --phase pre-closeout`、docs 索引/coverage 对齐、multi-agent 脚本 help 与 dry-run 自检通过 |

##### Story `A-STORY-006` 全自动多 agent 协作编排

- 目标：在不改变 `HARN-025` 既有半自动真值的前提下，补齐“需求输入 -> plan/manifest 自动生成 -> prepare/launch/collect -> autonomous Main Foreman 收口”的全自动主路径。
- 验证：auto-planner/auto-foreman prompt 模板、autoplan/full-auto 脚本、playbook 的 full-auto 章节和索引都对齐，且自动生成仍只通过 Main Foreman 落入 `docs/`、台账和 closeout 链。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `HARN-026` | 把多 agent 基础设施升级为从需求到收口的全自动主路径 | 在保留 `HARN-025` 半自动能力、Main Foreman 唯一收口、多 `codex exec` 会话、多 worktree 隔离和既有 `foreman` / `task_audit` / `closeout` 链不变的前提下，新增 requirement-driven auto-planner / auto-foreman prompt 模板与 autoplan/full-auto orchestration 脚本，让既有 multi-agent 流程可以从 codex 读取需求并自动生成 exec plan、manifest、launch/collect 和最终 autonomous 收口 | `HARN-025` | `python3 scripts/foreman.py validate HARN-026`、`bash scripts/multi_agent_autoplan.sh --help`、`bash scripts/multi_agent_full_auto.sh --help`、autoplan dry-run、自生成 manifest 的 prepare/launch dry-run、docs 索引/coverage 对齐 |

##### Story `A-STORY-007` 从无 task 开始的治理自动化

- 目标：把 SQLForge 的自动化起点从“已有 formal task 的 full-auto 执行”前移到“只有规划/需求/计划、还没有正式 task”的场景，同时不绕过现有审计链。
- 验证：requirement-normalizer/plan-shaper/task-shaper/task-governance-reviewer prompt 模板、requirements-to-task playbook、candidate task pack 模板，以及 `requirements_to_plan/task_materialize/governed_full_cycle` 脚本全部对齐；candidate task 只有在 gate 通过后才会写入 plan/matrix/ledger，并继续 handoff 给既有 `HARN-026` full-auto。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `HARN-027` | 落地从无 task 开始的 governed full-cycle 自动化 | 在不改变 `HARN-026` downstream full-auto 真值、不引入第二套长期真值的前提下，新增 requirement normalization、candidate task pack、governance gate 与 materialization 脚本/模板/手册，让 Codex 可以从“只有需求”开始先塑形 formal task，再继续交给现有 full-auto 执行链 | `HARN-026` | `python3 scripts/foreman.py validate HARN-027`、`bash scripts/requirements_to_plan.sh --help`、`bash scripts/task_materialize.sh --help`、`bash scripts/governed_full_cycle.sh --help`、requirements-to-plan dry-run、real candidate pack generation、task_materialize dry-run、governed_full_cycle dry-run、docs 索引/coverage 对齐 |
| `HARN-028` | 加固 governed full-cycle V2 intake / healthcheck / closeout 完整性 | 在不改变 `HARN-027` no-task 起步真值、不引入第二套长期真值的前提下，补齐 governed intake/healthcheck 入口、machine-readable run summary 的 `executed_commands` 与细粒度 suggestion 字段、candidate materialization rollback，以及 closeout 后不得留下 tracked `validation-log` residue 的仓库级修复 | `HARN-027` | `python3 scripts/foreman.py validate HARN-028`、`bash scripts/governed_intake.sh --help`、`python3 scripts/governed_healthcheck.py --check`、`bash scripts/task_materialize.sh --help`、`bash -n scripts/governed_intake.sh`、`bash -n scripts/task_materialize.sh`、`python3 -m py_compile scripts/governed_v2_support.py scripts/governed_healthcheck.py`、docs 索引/手册对齐 |
| `HARN-036` | 修复 governed intake / full-auto 入口治理与统一 execution preview 合同 | 在不改变 `HARN-028` governed intake/healthcheck 真值、`HARN-026` / `HARN-035` downstream full-auto 与只读 MCP 边界、不引入第二套长期真值的前提下，修复 existing-task intake 路径必须先生成 requirements artifact 并透传 输出物/限制，给 `multi_agent_full_auto.sh` 增加 `compile-governance` / `validate_codex_runtime` 硬门禁，定义统一 execution preview 合同，并把 `codex_template_adapter.py` / `UserPromptSubmit` / repo skill-plugin 路径收敛为 chat-native router 与 execution mode router，保持 Main Foreman 唯一 write-back / validate / closeout 入口且默认优先 single-agent。 | `HARN-028`,`HARN-035` | python3 scripts/foreman.py validate HARN-036、bash scripts/governed_intake.sh --help、bash scripts/multi_agent_full_auto.sh --help、bash -n scripts/governed_intake.sh、bash -n scripts/multi_agent_full_auto.sh、python3 -m py_compile scripts/codex_template_adapter.py .codex/hooks/user_prompt_submit.py scripts/validate_codex_runtime.py、python3 scripts/validate_codex_runtime.py、python3 scripts/task_audit.py --check --phase pre-closeout |
| `HARN-038` | 修复 Governed Closeout Post-Closeout Healthcheck 与 Runtime Recovery 语义 | 在 `A-STORY-007` 下新增一个治理/工具 follow-up 任务，修复 governed closeout / post-closeout / healthcheck 的真实治理缺口：让 `python3 scripts/governed_healthcheck.py --check` 在 closeout post-check 语境下不再对当前刚归档任务的 own post-closeout actual evidence 产生自引用误判；补齐 commit 已成功但 post-check 失败时的严格且可恢复 runtime cleanup 语义；同时保持 implementation 期间 tracked dirty worktree 的 healthcheck 阻断不变，并对齐相关运行时/治理文档与自动化验证。 | `HARN-033` | python3 scripts/foreman.py validate HARN-038、python3 -m py_compile scripts/foreman.py scripts/governed_healthcheck.py scripts/governed_v2_support.py、python3 scripts/governed_healthcheck.py --check、python3 scripts/foreman.py compile-governance --check、node scripts/lint-repository-knowledge.js、python3 scripts/task_audit.py --check --phase pre-closeout、python3 scripts/task_audit.py --check --phase post-closeout |
| `HARN-041` | Runtime Reservation Pause/Cleanup and Demand Re-entry Governance | 在不改变 SQLForge 现有 no-task shaping / governed intake 真值、不引入第二套长期真值的前提下，补齐 candidate reservation 的 paused/archived/abandoned 审计语义、healthcheck/dashboard cleanup 判定、HARN-040 暂停入口与 requirements-to-task re-entry playbook，解除旧 dry-run reservation 对 governed confirm-run 的长期阻塞，同时保留历史 shaping 证据而不把其提升为正式任务实现。 | `HARN-028`,`HARN-036`,`HARN-038` | python3 scripts/foreman.py validate HARN-041、python3 -m py_compile scripts/governed_v2_support.py scripts/governed_healthcheck.py scripts/governed_runtime_dashboard.py、python3 scripts/governed_healthcheck.py --check、python3 scripts/governed_runtime_dashboard.py --json、python3 scripts/task_audit.py --check --phase pre-closeout、node scripts/lint-repository-knowledge.js |

##### Story `A-STORY-008` Codex MCP 治理接入

- 目标：在不引入第二套长期真值、不绕过现有 `foreman` / `task_audit` / `closeout` 审计链、且第一批仅允许只读 MCP 的前提下，把 Codex MCP 能力增量接入 SQLForge 的治理体系，并为后续 multi-agent 受控使用预留扩展点。
- 验证：`docs/security/connectors.md`、MCP 规则与验证规则、`compile-governance` / `validate_codex_runtime` 的 MCP 扩展、Codex MCP 使用手册，以及 multi-agent `mcp_profile` 契约全部按 Main Foreman 唯一收口原则落入正式文档、脚本与验证链，不把外部 MCP 结果误写成仓库长期真值。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `HARN-034` | 落地最小可用 MCP 治理底座与只读接入边界 | 在不引入第二套长期真值、不绕过 `foreman` / `task_audit` / `closeout`、且第一批仅允许只读 MCP 的前提下，补齐 `docs/security/connectors.md`、MCP 规则与验证规则、`compile-governance` / `validate_codex_runtime` 的 MCP 扩展、Codex MCP 使用手册与本地入口说明；不包含 multi-agent `mcp_profile` 扩展，不包含任何可写 MCP、SSH/K8s/数据库执行型 MCP，也不把外部 MCP 结果写成仓库默认事实。 | `HARN-028`,`HARN-033` | python3 scripts/foreman.py validate HARN-034、python3 scripts/validate_codex_runtime.py、python3 scripts/foreman.py compile-governance --check、node scripts/lint-repository-knowledge.js、python3 scripts/task_audit.py --check --phase pre-closeout |
| `HARN-035` | 扩展 multi-agent 受控 mcp_profile 只读证据接入 | 在 `HARN-034` 已建立 MCP 治理底座的前提下，扩展 multi-agent playbook、agent prompt 模板、manifest 模板和 `prepare` / `launch` / `collect` / `autoplan` / `full-auto` 脚本的 `mcp_profile` 契约，让 explorer / validator 可以读取外部只读证据，同时保持 Main Foreman 仍是唯一 write-back / validate / closeout 入口。不得把可写 MCP 引入默认主路径，不得允许 worker 通过 MCP 修改台账、validation log、closeout 文档或业务数据。 | `HARN-034` | python3 scripts/foreman.py validate HARN-035、python3 scripts/foreman.py compile-governance --check、bash scripts/multi_agent_prepare.sh --help、bash scripts/multi_agent_launch.sh --help、bash scripts/multi_agent_collect.sh --help、bash scripts/multi_agent_autoplan.sh --help、bash scripts/multi_agent_full_auto.sh --help、python3 scripts/validate_codex_runtime.py、node scripts/lint-repository-knowledge.js |
| `HARN-037` | 补齐只读 MCP onboarding / doctor 与治理定位手册 | 在 `HARN-034` / `HARN-035` 已建立只读 MCP 治理基线与 multi-agent `mcp_profile` 契约的前提下，补齐每个允许 category 的本地 onboarding 指南、MCP doctor/healthcheck、evidence 写回位置说明，以及 `docs/security/connectors.md`、`docs/operations/codex-mcp-playbook.md`、`docs/operations/multi-agent-playbook.md`、`docs/README.md`、`docs/operations/README.md` 和 compile/validate/runtime 入口的治理定位对齐；保持产品定位为“受治理的只读证据增强”，不得扩展为远端自动运维、可写控制面或第二套长期真值。 | `HARN-035` | python3 scripts/foreman.py validate HARN-037、python3 scripts/validate_codex_runtime.py、python3 scripts/foreman.py compile-governance --check、python3 scripts/governed_healthcheck.py --check、node scripts/lint-repository-knowledge.js、python3 scripts/task_audit.py --check --phase pre-closeout、python3 scripts/task_audit.py --check --phase post-closeout |

##### Story `A-STORY-009` SQL 治理实施规格与任务塑形

- 目标：把 SQL 治理中后台 + 开放接入平台的完整需求一次性落成事实规格包，并同步塑形成 D/E/F 主线 Story / Task inventory，作为后续 materialize / instantiate 的唯一权威输入。
- 验证：规格包、主计划、task-spec 矩阵与 task-governance 扩展矩阵同步完成，且不丢失已确认需求。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `HARN-042` | 落地 SQL 治理实施规格包与完整任务清单 | 在不新增微服务、不绕过 `foreman` / `task_audit` / `closeout`、不引入第二套长期真值的前提下，新增 SQL 治理平台实施规格、接口扩展基线、数据模型扩展与降级矩阵，并把后续 `D-STORY-006` 至 `D-STORY-012`、`E-STORY-007` 至 `E-STORY-012`、`F-STORY-010` 与 `F-STORY-011` 一次性写入主计划与两张任务矩阵；本任务不直接实现业务代码、不接真实外部服务，只负责把已确认需求固化为可执行工程输入。 | `HARN-041` | `python3 scripts/foreman.py validate HARN-042`、`python3 scripts/foreman.py compile-governance --check`、`node scripts/lint-repository-knowledge.js`、`python3 scripts/task_audit.py --check --phase pre-closeout` |
| `HARN-043` | 修复 SQL 治理规格包 follow-up 真值缺口并启动 Wave 1 | 在不新增微服务、不改写 `HARN-042` 历史完成语义、不引入第二套长期真值的前提下，修复 `HARN-042` closeout 后主计划 active-wave 漂移、补齐接口/枚举/只读执行边界与数据模型漏项，并为数据源/数据资产/系统管理补充缺失的 D/E Story inventory；随后以当前仓库真值启动 Wave 1 的首个 repo-side mainline task。 | `HARN-042` | `python3 scripts/foreman.py validate HARN-043`、`python3 scripts/foreman.py compile-governance --check`、`node scripts/lint-repository-knowledge.js`、`python3 scripts/task_audit.py --check --phase pre-closeout` |

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
  - `sqlforge-shared` 具备公共源码
  - `governance` 满足当前治理底座职责
  - 配置、消息、日志、异常、审计、上下文遵循统一约束
- 依赖：`Phase-B`
- 当前进展：
  - `C-STORY-001` 已完成首轮落地，`sqlforge-shared` 已被 `governance` 消费
  - `C-STORY-002` 已完成 `DATABASE` / `MOCK` 可运行基线、管理接口和 `KAFKA` 客户端接入，真实集群运行验证仍待补齐
  - `C-STORY-003` 已完成治理扩展契约骨架和严格请求上下文基线，完整治理能力仍待后续阶段增强

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

- 目标：把 `governance` 明确为公共管理服务的当前实现基线。
- 验证：接口契约、tenant 校验、错误码、MyBatis XML、日志满足现有规则。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `C-TASK-007` | 对齐现有 `application` 包域下的 `controller/service` 与 `domain/infrastructure` 分层 | 不扁平化 | `Phase-B` | `R-120` 检查通过 |
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

##### Story `D-STORY-005` 运行时执行链与跨服务补完

- 目标：把已落地的跨服务运行时兜底补回主计划，并继续推进 `query-execution` 从最小同步闭环走向真实执行适配与结果聚合基线。
- 验证：跨服务治理调用、执行适配、结果聚合与失败恢复路径有真实测试和文档证据。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `D-TASK-014` | 收口异步服务鉴权、占位执行与审计兜底 | `sql-optimization`、`benchmark-engine`、`governance` 的跨服务运行时兜底 | `D-TASK-013` | 异步服务与治理服务模块测试、task audit 通过 |
| `D-TASK-015` | 补完 `query-execution` 真实执行适配与结果聚合基线 | Hetu 多模式适配、执行模式选择、结果聚合与审计证据收口 | `D-TASK-014` | 模块测试、跨模式适配测试、runtime smoke 与契约文档同步 |
| `D-TASK-016` | 收口治理授权矩阵并下沉统一授权入口 | 在 `governance` 落地可配置角色矩阵、资源模型、数据源授权矩阵，把占位式 datasource check 升级为真实授权决策，并让 `query-execution`、`sql-optimization`、`benchmark-engine` 全部复用同一授权入口；同时补齐授权成功/拒绝/跨租户/吊销后访问测试、runtime smoke，以及授权成功/失败/权限变更审计链 | `D-TASK-015` | governance/三服务模块测试、授权回归测试、runtime smoke、task audit 与契约文档同步 |
| `D-TASK-017` | 落实 `query-execution` 真实 Hetu 集成与 smoke 分层 | 让 `query-execution` 的 HETU 主路径不再以 `SIMULATED` 冒充成功，补齐 JDBC 驱动接线、Hetu client 协议接入、REST/CLIENT 模式真实联通、运行参数/错误语义收口，并在保留统一授权入口前提下补齐本地 runtime smoke 与外部 Hetu/MRS environment-backed smoke 入口/文档 | `D-TASK-016` | query-execution 模块测试、跨模式适配测试、runtime smoke、Hetu env smoke 入口与契约文档同步 |
| `D-TASK-018` | 去除核心追溯链历史外键并补齐应用层完整性校验 | 移除 `config/result/history/export/audit` 在 MySQL / TDSQL 上的历史外键约束，改为索引 + 应用层完整性校验；同步补齐 drop-foreign-key migration、schema 映射测试，以及 Win10 + IDEA + yml 配置读取口径的 Hetu/MRS 测试环境部署文档与确认清单 | `D-TASK-017` | governance 模块测试、schema/migration 映射测试、task audit、知识检查与部署文档同步 |
| `D-TASK-019` | 补齐 `query-execution` 执行遥测与业务指标基线 | 为 `query-execution` 与 `governance` 增加最小业务级 Micrometer 指标，覆盖查询执行结果、模式命中、timeout/degraded/fallback、治理审计兜底与数据库消息队列 backlog 等 repo-closed 可观测信号；同步更新 observability / truth 文档与验证基线 | `D-TASK-018` | query-execution/governance 模块测试、prometheus 指标断言、runtime smoke、task audit 与文档同步 |
| `D-TASK-020` | 补齐异步服务执行遥测与业务指标基线 | 为 `sql-optimization` 与 `benchmark-engine` 增加最小业务级 Micrometer 指标，覆盖任务提交、终态成功/失败、worker 或报告处理延迟等 repo-closed 可观测信号；同步更新 observability / truth 文档与验证基线，并保持低基数标签约束 | `D-TASK-019` | sql-optimization/benchmark-engine 模块测试、prometheus 指标断言、runtime smoke、task audit 与文档同步 |
| `D-TASK-021` | 推进 `benchmark-engine` 真实隔离执行与导出链路 | 在保留 `benchmark_task` / `benchmark_task_report` MySQL carrier、scheduled worker、统一授权入口和治理审计语义的前提下，把压测任务从 placeholder 执行推进到真实隔离执行链路，补齐可复现的报告快照/导出产物生成与更深层跨服务协同验证，并持续满足只读、影子环境优先与租户隔离边界 | `D-TASK-020` | benchmark-engine 模块测试、导出/报告契约测试、runtime smoke、task audit 与文档同步 |
| `D-TASK-022` | 推进 `benchmark-engine` 外部 artifact storage、raw-data download 与治理追溯编排 | 在保留 repo-closed 隔离执行、统一授权入口、治理审计与只读/影子环境边界的前提下，把 benchmark 报告导出与 raw-data snapshot 提升到外置 artifact storage 基线，并通过 governance 内部受保护编排把 config/result/history/export 追溯链接到 `benchmark-engine` 报告与下载路径 | `D-TASK-021` | benchmark-engine/governance 模块测试、导出/下载契约测试、runtime smoke、task audit、schema/mapping 校验、文档同步 |
| `D-TASK-023` | 收口 `benchmark-engine` 报告查询审计追溯增强与 artifact 生命周期基线 | 在保留 repo-closed artifact storage、统一授权入口、治理审计与只读/影子环境边界的前提下，为 benchmark 报告/下载查询补齐 `configSnapshotId/resultId/historyId/exportId` 审计链接，并建立 repo-local artifact retention/recovery/cleanup 语义与验证基线 | `D-TASK-022` | benchmark-engine/governance 模块测试、报告/下载审计契约测试、runtime smoke、task audit、schema/mapping 校验、文档同步 |
| `D-TASK-024` | 收口 `benchmark-engine` artifact tenant-specific retention/backfill policy 与 environment-backed storage adapter/evidence | 在保持 repo-local artifact lifecycle 仍是默认主路径的前提下，为 benchmark artifact 增加 tenant-specific retention/backfill policy 语义，并补齐 environment-backed object-storage adapter/evidence 的明确边界、接线与验证基线 | `D-TASK-023` | benchmark-engine/governance 模块测试、artifact policy/adapter 契约测试、runtime smoke、task audit、schema/mapping 校验、文档同步 |
| `D-TASK-025` | 收口 `benchmark-engine` / `query-execution` workload/backfill orchestration 与真实环境 object storage live evidence | 在保持 repo-local artifact lifecycle 仍是默认主路径、统一授权入口、治理审计与只读/影子环境边界不变的前提下，为 `benchmark-engine` 补齐面向 `query-execution` 的 workload/backfill 内部编排契约，并把真实环境 object storage live evidence 沉淀为显式 environment-backed 证据而非仓库默认主路径 | `D-TASK-024` | sqlforge-shared/query-execution/benchmark-engine 模块测试、跨服务 workload/backfill 契约测试、runtime smoke、task audit、knowledge lint、文档同步 |
| `D-TASK-026` | 把 workload/backfill evidence 沉淀进 governance 长期追溯链，并推进真实 external write/recovery verification | 在保持 repo-local artifact lifecycle 与 `LOCAL_FILE` 默认主路径不变、统一授权入口、治理审计及只读/影子环境边界不变的前提下，把 benchmark/query-execution 的 workload/backfill 证据提升为 governance 长期追溯链中的显式结构化字段，并把 environment-backed object-storage 从 repo-side live-evidence manifest 推进到真实 external write/readback recovery verification | `D-TASK-025` | sqlforge-shared/governance/query-execution/benchmark-engine 模块测试、跨服务 workload/backfill 与 trace persistence 契约测试、artifact adapter verification 测试、runtime smoke、task audit、knowledge lint、文档同步 |
| `D-TASK-027` | 收口更深层 workload compensation-replay orchestration 与 provider-backed object-storage live evidence | 在保持 repo-local artifact lifecycle 与 `LOCAL_FILE` 默认主路径不变、统一授权入口、治理审计及只读/影子环境边界不变的前提下，为 benchmark-engine/query-execution 补齐更深层的 workload compensation-replay orchestration，并把 environment-backed object-storage 从 writable-dir verification 推进到 provider-backed live evidence/readback recovery verification | `D-TASK-026` | sqlforge-shared/query-execution/benchmark-engine/governance 模块测试、跨服务 compensation-replay 契约测试、provider-backed object-storage adapter/live-evidence 测试、runtime smoke、task audit、knowledge lint、文档同步 |
| `D-TASK-028` | 收口 provider-specific / multi-provider object-storage contract 与 cleanup/recovery 语义，并提升 compensation-replay evidence 的治理查询/恢复面 | 在保持 repo-local artifact lifecycle 与 `LOCAL_FILE` 默认主路径不变、统一授权入口、治理审计及只读/影子环境边界不变的前提下，为 benchmark-engine 收口 provider-specific / multi-provider object-storage contract、cleanup/recovery / failure-replay 语义，并把 compensation-replay 与 artifact recovery/provider evidence 提升为 governance 历史查询与恢复操作面的显式结构字段 | `D-TASK-027` | sqlforge-shared/query-execution/benchmark-engine/governance 模块测试、provider/multi-provider artifact contract 测试、governance query/detail 契约测试、runtime smoke、task audit、knowledge lint、文档同步 |
| `D-TASK-029` | 推进 provider-native / environment-backed object-storage live evidence 与 governance-triggered artifact cleanup/recovery operation surfaces | 在保持 repo-local artifact lifecycle 与 `LOCAL_FILE` 默认主路径不变、统一授权入口、治理审计及只读/影子环境边界不变的前提下，为 benchmark-engine/environment-backed object storage 推进更接近 provider-native 的 live evidence 沉淀，并为 governance 补齐可审计的 artifact cleanup/recovery operation surface 与受控触发链路 | `D-TASK-028` | sqlforge-shared/benchmark-engine/governance 模块测试、provider-native live-evidence 契约测试、governance-triggered cleanup/recovery operation 测试、runtime smoke、task audit、knowledge lint、文档同步 |
| `D-TASK-030` | 推进 provider-authenticated object-storage operations 与 governance-side batch retention/recovery orchestration | 在保持 repo-local artifact lifecycle 与 `LOCAL_FILE` 默认主路径不变、统一授权入口、治理审计及只读/影子环境边界不变的前提下，为 benchmark-engine/environment-backed object storage 补齐 provider-authenticated object-storage operations，并为 governance 收口可审计的 batch retention / batch recovery orchestration、部分失败回滚及长期追溯证据；输出代码、测试与文档，不引入不受治理的 provider SDK 或明文凭据落仓，不把 environment-backed path 写成仓库默认事实。 | `D-TASK-029` | sqlforge-shared/benchmark-engine/governance 模块测试、provider-authenticated object-storage contract 与 auth failure 测试、governance-side batch retention/recovery orchestration 测试、runtime smoke、task audit、knowledge lint、文档同步 |
| `D-TASK-031` | 推进 `sql-optimization` 真实 parse/rewrite/acceleration suggestion 链 | 在保留 MySQL `optimization_task` carrier、scheduled worker、统一授权入口与异步任务契约不变的前提下，把 `sql-optimization` 从 placeholder suggestion 推进到真实 SQL parser / AST analysis / rewrite rule / acceleration suggestion pipeline，输出可执行的 rewrite candidate、结构化 parse artifact、加速建议工件与失败阶段证据，不提前引入跨服务自动应用或审批旁路 | `D-TASK-030` | sql-optimization 模块测试、parse/rewrite/acceleration pipeline 测试、persistence/schema/mapping 校验、runtime smoke、task audit、文档同步 |
| `D-TASK-032` | 收口 acceleration plan 治理闭环 | 在保持统一授权入口、治理审计、tenant 隔离与 `sql-optimization` suggestion 链不变的前提下，补齐 acceleration plan 的提交、审批/确认、应用、验证、回滚与长期追溯闭环，使 acceleration 不再只是建议元数据而成为受治理的正式对象 | `D-TASK-031` | sqlforge-shared/sql-optimization/query-execution/governance 模块测试、跨服务 acceleration plan 契约测试、runtime smoke、task audit、knowledge lint、文档同步 |
| `D-TASK-033` | 收口 `query-execution` 生产级 Hetu 集群证据与路由参数校准 | 在保留 repo-closed Hetu 多模式执行链、统一授权入口、只读/影子环境边界与结构化失败语义不变的前提下，补齐生产级 Hetu/MRS 集群证据、路由参数校准、模式优先级与失败分层证据沉淀，不把外部测试环境依赖误写成仓库默认主路径 | `D-TASK-032` | query-execution 模块测试、route calibration 契约测试、runtime smoke、Hetu env smoke/test-env evidence、task audit、文档同步 |
| `D-TASK-034` | 收口 `benchmark-engine` 外部队列/文件存储与 provider-native 语义 | 在保持 repo-local artifact lifecycle 与 `LOCAL_FILE` 默认主路径、统一授权入口、治理审计及只读/影子环境边界不变的前提下，为 `benchmark-engine` 补齐外部队列 carrier、文件存储编排与 provider-native 语义边界，把 provider-backed write/readback/cleanup/recovery 证据推进到更接近真实运行形态的基线 | `D-TASK-033` | sqlforge-shared/benchmark-engine/governance 模块测试、external queue/storage/provider-native 契约测试、runtime smoke、task audit、knowledge lint、文档同步 |
| `D-TASK-035` | 收口真正的缓存治理能力 | 在保持查询执行主路径、统一授权入口、治理审计、缓存一致性与只读边界不变的前提下，建立可审计的 cache governance 模型、命中/失效/旁路/回填/风险标记语义，以及与 query-execution/sql-optimization/benchmark 的最小联动闭环，不把缓存元数据占位误写成已治理完成 | `D-TASK-034` | sqlforge-shared/query-execution/sql-optimization/governance 模块测试、cache governance 契约测试、runtime smoke、task audit、knowledge lint、文档同步 |
| `D-TASK-036` | 推进 provider-native distributed cache governance backend | 在保持 repo-closed in-memory cache governance baseline、统一授权入口、治理审计、缓存一致性与只读边界不变的前提下，为 `query-execution` 补齐 provider-neutral distributed cache backend contract、environment-backed carrier 语义、provider-native evidence、失败降级与可审计读写校验；默认仍不启用外部 provider，不把 Redis/provider cache 写成仓库默认事实 | `D-TASK-035` | sqlforge-shared/query-execution/governance 模块测试、distributed cache backend contract 测试、cache policy apply/verify/invalidate backend 证据测试、runtime smoke、task audit、knowledge lint、文档同步 |
| `D-TASK-037` | 收口 cache capacity / eviction / metrics governance baseline | 在保持 D-TASK-036 provider-neutral cache backend、默认 repo-closed 主路径、统一授权入口、治理审计、缓存一致性与只读边界不变的前提下，为 cache governance 补齐 per-tenant / per-policy capacity limit、TTL 与 capacity/manual/schema eviction reason evidence、cache hit/miss/bypass/backfill/invalidate/backend-unavailable metrics、policy verify capacity/backend health summary，并让 benchmark/governance 继续透出 eviction/capacity evidence；真实 Redis 集群长跑和恢复演练仍是 environment-backed follow-up | `D-TASK-036` | sqlforge-shared/query-execution/benchmark-engine/governance 模块测试、cache capacity/eviction 契约测试、cache governance metrics 断言、policy verify summary 测试、task audit、knowledge lint、文档同步 |

##### Story `D-STORY-006` 查询历史与执行取证闭环

- 目标：补齐 SQL 查询执行主链的历史、取证、绑定 SQL、注释上下文、命中对象和导出基线，为解析、推荐、压测与审计提供统一追溯入口。
- 验证：执行详情、历史列表、详情查询、导出与追溯键可测。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `D-TASK-038` | 扩展 query-history / execution-result 追溯字段 | 为注释上下文、绑定 SQL、query-date、逻辑对象命中、接入来源与路由摘要补齐持久化与查询字段 | `D-TASK-037` | schema/mapping 测试、history trace persistence 测试 |
| `D-TASK-039` | 扩展 SQL 查询执行摘要契约 | 查询执行返回 comment context、binding summary、logical object hits、route/cache summary | `D-TASK-038` | query-execution controller/service 契约测试 |
| `D-TASK-040` | 落地 SQL 历史列表与详情查询面 | 历史过滤、分类、详情 drill-through、执行链路与关联记录查询 | `D-TASK-039` | governance history list/detail 测试 |
| `D-TASK-041` | 补齐 SQL 历史导出与取证视图 | `CSV/EXCEL/JSON/SQL/PDF` 导出基线与取证页字段收口 | `D-TASK-040` | export 测试、审计联动测试 |

##### Story `D-STORY-007` 结构解析与数据访问解析双轨闭环

- 目标：在 `sql-optimization` 中建立单条 SQL 的结构解析、数据访问解析、异步补跑与 partial success 语义。
- 验证：结构解析、access parse、异步补跑、服务不可用降级与综合状态机可测。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `D-TASK-042` | 固化结构解析契约与问题分类模型 | `StructureParseResult`、问题域、场景、severity、priority 评分基线 | `D-TASK-041` | parser/domain 契约测试 |
| `D-TASK-043` | 落地单条结构解析入口 | 不连库的 SQL 结构解析、query-date 提取、逻辑对象命中与基础风险识别 | `D-TASK-042` | parse structure controller/service 测试 |
| `D-TASK-044` | 落地数据访问解析入口与异步补跑语义 | 结构解析成功后自动异步补跑 access parse，并保留 unavailable / skipped / failed 语义 | `D-TASK-043` | async parse flow 测试、降级测试 |
| `D-TASK-045` | 补齐解析综合结论与 partial-success 追溯 | 统一展示结构解析成功 / access parse 失败的综合结论与追溯字段 | `D-TASK-044` | state machine 与 history/detail 测试 |
| `D-TASK-073` | 升级结构解析查询意图理解与双 parser 抽象 | 在保持结构解析不依赖数据库、旧响应兼容和 access parse 独立失败语义不变的前提下，为 sql-optimization 结构解析新增查询意图理解、多维特征、结构化风险、启发式资源估算、SQL 指纹与双 parser adapter 抽象，并让解析工作台展示新增字段。 Tech: `JAVA-BE`,`VUE-FE`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`frontend/router/views/styles`,`docs`. | `D-TASK-045`,`D-TASK-051`,`E-TASK-020` | sql-optimization 模块测试、structure parse contract/controller 测试、parse workbench contract 测试、npm run build、npm run lint、task audit、knowledge lint |
| `D-TASK-074` | 补齐结构解析 SQL 指纹前处理契约 | 复盘 D-TASK-073 后补齐 SQL 指纹前处理缺口：共享 fingerprint 工具对注释、字面量、大小写、空白和末尾分号做确定性标准化，使结构解析查询意图输出可按同一查询形态稳定聚合；不执行 SQL、不访问生产数据、不改变旧结构解析字段。 Tech: `JAVA-BE`,`DOCS`. Layer: `shared/utils`,`application(controller/service)`,`docs`. | `D-TASK-073` | sqlforge-shared utility tests、structure parse controller regression、sql-optimization module tests、task audit、knowledge lint |
| `D-TASK-075` | 补强复杂反模式 SQL 结构解析验证 | 使用用户提供的复杂反模式 SQL 固化预期结构解析结果，并补强结构解析递归 AST 特征提取与风险识别：SELECT 标量子查询、多层/相关子查询、EXISTS/NOT EXISTS/IN、OR 谓词、函数包裹谓词、前导通配符 LIKE、ORDER BY RAND()、重复表扫描等。 Tech: `JAVA-BE`,`DOCS`. Layer: `application(controller/service)/domain`,`shared contract docs`. | `D-TASK-073`,`D-TASK-074` | SqlOptimizationPipelineService complex SQL profile test、StructureParseController complex anti-pattern contract test、sql-optimization module tests、task audit、knowledge lint |

##### Story `D-STORY-008` 逻辑视图与 DB View 治理

- 目标：同时支持 `BUSINESS_VIEW` 与 `DB_VIEW`，并让查询、历史、解析、路由共用统一逻辑对象语义。
- 验证：对象目录、映射、识别、展示与关联查询可测。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `D-TASK-046` | 建立逻辑对象统一模型 | `BUSINESS_VIEW/DB_VIEW/TABLE` 统一对象契约与跨服务引用字段 | `D-TASK-045` | contract 与 DTO 测试 |
| `D-TASK-047` | 落地业务逻辑视图目录与映射 | governance 目录、映射、物理表关系和基础查询面 | `D-TASK-046` | repository/controller 测试 |
| `D-TASK-048` | 落地 DB View 识别与依赖展示 | 解析链识别数据库 View，沉淀依赖和命中明细 | `D-TASK-047` | parser/integration 测试 |
| `D-TASK-049` | 统一逻辑对象在查询/历史/解析中的展示契约 | 跨服务 DTO/VO 对齐，统一 detail/list/export surfaces | `D-TASK-048` | cross-service contract 测试 |

##### Story `D-STORY-009` 批量解析与报表清单解析

- 目标：提供批量 SQL 解析、表格导入、报表清单解析与 mock/真实接口抽象。
- 验证：稳定文件格式导入、兼容格式降级、报表清单 mock 与接口抽象可测。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `D-TASK-050` | 建立批量解析批次模型与模板契约 | `ParseBatch`、模板列、导入模式、批次状态基线 | `D-TASK-049` | batch contract 测试 |
| `D-TASK-051` | 落地 SQL/表格批量导入解析 | 稳定支持 `xlsx/csv/txt/sql` 的结构解析与 access parse 编排 | `D-TASK-050` | import parsing 测试 |
| `D-TASK-052` | 扩展 `xls/et` 兼容导入与失败语义 | 二级兼容格式支持与推荐使用稳定格式的失败提示 | `D-TASK-051` | compatibility 测试 |
| `D-TASK-053` | 落地报表清单解析文件模拟入口 | 以 `report_code` 为主键，从 txt/mock source 获取 SQL 再解析 | `D-TASK-052` | mock resolve 测试 |
| `D-TASK-054` | 接入报表接口配置与真实拉取抽象 | governance 配置接口，sql-optimization 通过统一抽象调用 | `D-TASK-053` | config/client abstraction 测试 |

##### Story `D-STORY-010` 解析统计与优先级分层

- 目标：将解析结果收口为按 SQL、问题场景、报表与优先级可消费的统计面。
- 验证：统计聚合、占比、重要/紧急清单与优先级矩阵可测。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `D-TASK-055` | 固化解析统计口径与优先级评分 | scene/domain/severity/priority/important/urgent 评分规则 | `D-TASK-054` | scoring rule 测试 |
| `D-TASK-056` | 落地按 SQL 与问题场景统计 | parse overview、scene aggregation、single-SQL issue 汇总 | `D-TASK-055` | statistics API 测试 |
| `D-TASK-057` | 落地按报表统计与占比分析 | report dimension aggregation、报表问题数量与占比计算 | `D-TASK-056` | report aggregation 测试 |
| `D-TASK-058` | 落地重要/紧急清单与优先级矩阵 | priority matrix、important/urgent view 和 drill-through | `D-TASK-057` | matrix/list 测试 |

##### Story `D-STORY-011` 推荐 SQL、加速建议与装数协同事件

- 目标：将推荐改写、加速建议与装数协同收口为治理对象和可追溯事件，而不承担真实装数。
- 验证：推荐对象、事件状态机、追溯链与“只治理不装数”边界可测。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `D-TASK-059` | 扩展推荐对象类型与收益/风险模型 | `REWRITE/ACCELERATION/CREATE_TABLE/PREWARM/MAINTENANCE` 建模 | `D-TASK-058` | recommendation domain 测试 |
| `D-TASK-060` | 落地推荐治理事件创建与状态机 | `DispatchEvent` 创建、published/pulled/acked/failed 状态语义 | `D-TASK-059` | event state 测试 |
| `D-TASK-061` | 打通推荐与历史/解析/路由的关联追溯 | recommendation 对应 history、parse、route、alert 关联键 | `D-TASK-060` | traceability 测试 |
| `D-TASK-062` | 固化“只管理不装数”的协同契约 | 外部拉取事件、非主动装数、回执与审计边界 | `D-TASK-061` | dispatch contract 测试 |

##### Story `D-STORY-012` 开放接入与 JDBC Agent

- 目标：把 API、JDBC Agent、Java SDK 与 direct client mode 统一纳入治理主链。
- 验证：接入来源、审计、Agent 三模式与 Java SDK 契约可测。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `D-TASK-063` | 固化接入来源模型与统一审计契约 | `PAGE/API/JDBC_AGENT/SDK/CLIENT` 与 access audit 模型 | `D-TASK-062` | access contract 测试 |
| `D-TASK-064` | 落地 HTTP API 接入基线 | query/parse/history/recommendation 等 API 入口对外基线 | `D-TASK-063` | API integration 测试 |
| `D-TASK-065` | 落地 JDBC Agent 首版 `Observe` | JAR 采集、注释解析、上报与不接管执行 | `D-TASK-064` | JDBC agent sample/integration 测试 |
| `D-TASK-066` | 扩展 JDBC Agent `Governed Execute` | 通过平台执行 SQL、保留 fallback 语义 | `D-TASK-065` | governed-execute 测试 |
| `D-TASK-067` | 扩展 JDBC Agent `Local Rewrite + Direct JDBC` | 本地轻量改写 / 路由后直连目标 JDBC | `D-TASK-066` | local rewrite/direct JDBC 测试 |
| `D-TASK-068` | 落地 Java SDK 首版 | 鉴权、trace/requestId、typed clients、重试基线 | `D-TASK-067` | SDK 测试 |

##### Story `D-STORY-013` 数据源与数据资产治理增强

- 目标：补齐数据源管理、元数据快照、数据到位/SLA/上下游状态与系统管理后端接口，使数据资产页和系统管理页具备完整后端支撑。
- 验证：数据源配置、健康检查、metadata snapshot、freshness/SLA、报表接口/Redis 规则源/装数协同配置查询面可测。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `D-TASK-069` | 固化数据源连接配置与健康检查契约 | JDBC/API/Client/Gateway 连接信息、凭证方式、测试连接、健康状态与失败原因字段基线 | `D-TASK-049` | datasource contract 与 health-check 测试 |
| `D-TASK-070` | 建立 `MetadataSnapshot` 与数据到位/SLA/上下游状态模型 | metadata snapshot、freshness、SLA、upstream/downstream/queryability 语义和追溯字段 | `D-TASK-069` | metadata model 与 snapshot query 测试 |
| `D-TASK-071` | 落地数据资产与数据源治理查询/详情接口 | datasource/schema/table/logical-view/db-view 列表、详情、metadata snapshot 查询面 | `D-TASK-070` | data-asset API 与 detail query 测试 |
| `D-TASK-072` | 落地系统管理配置接口基线 | 报表接口配置、Redis 规则源、装数协同策略和相关治理查询面 | `D-TASK-071` | config/query API 测试 |

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
- 补充口径：
  - 正式产品首页 `/dashboard` 保留为业务首页，不被 AI 交付进度能力替代。
  - AI 交付进度页属于研发/交付阶段的临时子页面，建议独立路由 `/delivery-progress`。
  - 进度页展示真值必须来自 `tasks.md`、`tasks-done.md`、验证日志、执行计划与 Git 回写记录，不得另造平行状态源。
  - 生产环境默认隐藏该临时页面入口，避免其进入正式投产体验。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `E-TASK-001` | 建立驾驶舱路由与导航骨架 | dashboard only | `Phase-C` | `npm run build`、路由可达 |
| `E-TASK-002` | 落地项目全景/架构设计/进度管理板块 | 文字与状态展示 | `E-TASK-001` | 页面结构与 `10.1` 一致 |
| `E-TASK-003` | 落地合规中心与规则库板块 | 只读展示 | `E-TASK-002` | 合规/规则入口存在且可见 |

##### Story `E-STORY-002` 业务页面拆分

- 目标：保留 `SQL 查询`、`解析记录`、`压测报告`、`加速配置`、`系统管理` 独立页面。
- 验证：不堆叠长页面，能力可见，前后端边界清晰。
- 当前进展：仓库已经存在独立的业务页与治理扩展路由；后续任务以 repository-truth reconciliation 和能力收口为主，而不是重新造壳层。

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
| `U-TASK-004` | 前端复盘补漏并恢复规格直达能力 | 基于 SQL 治理产品规格与当前仓库前端实现，完成一次可审计的 repo-side gap closure：复盘页面设计、导航与信息架构缺口；恢复解析与加速模块对批量解析/解析结果/历史视角的显式直达能力；扩展 Dashboard 的规格覆盖与样本化 KPI 表达；补齐与上述实现对应的规格包说明、验证脚本与治理记录。Tech: VUE-FE,DOCS. Layer: frontend/router/views/styles/scripts/docs. | `U-TASK-003`,`U-TASK-002`,`U-TASK-001` | python3 scripts/foreman.py validate U-TASK-004 |
| `HARN-045` | HARN-045 页面组件语义治理执行模板 | 用户已确认 HARN-045 进入正式 materialization。围绕 E-STORY-003 / Phase-E 对现有页面表单字段进行组件语义治理：先在正式任务流程内验证页面字段现状、字段来源、API schema、校验与测试，再将日期时间、枚举、关联资源、布尔、数值、搜索选择等字段改为更合适的 UI 组件。 | `N/A` | 覆盖日期时间组件选择、默认值、回显和提交格式、覆盖租户、数据源等受控候选字段的选择、候选值加载和提交、覆盖布尔、数值、枚举、关联资源等字段的关键交互与边界输入、覆盖表单加载、编辑、校验、提交、回显的主流程、覆盖候选值加载失败或权限不可见时的保守行为，若仓库现有测试体系支持 |

##### Story `E-STORY-004` 临时 AI 交付进度页

- 目标：为架构师和交付负责人交付独立的 `/delivery-progress` 临时页面，用于只读展示 AI 编码任务推进状态，并与正式业务首页 `/dashboard` 严格分离。
- 验证：页面可读取权威台账派生状态、生产环境默认隐藏入口、业务首页不受影响。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `E-TASK-009` | 建立临时 AI 交付进度页路由与展示骨架 | `/delivery-progress`、只读展示、非生产隐藏 | `E-TASK-001`,`E-TASK-002`,`Phase-C` | `npm run build`、非生产路由可达、生产默认隐藏、展示源仅来自权威台账 |

##### Story `E-STORY-005` 前端构建链迁移与便携产物治理

- 目标：巩固根级前端已恢复的 Vue SFC toolchain、便携前端包与浏览器 smoke 基线，并把当前前端治理叙事从历史 non-SFC 迁移背景收口到现行 SFC 真值。
- 验证：toolchain 检查、`npm run lint`、`npm run build`、`npm run build:portable`、portable package 检查，以及轻量 dev / portable browser smoke 证据一致。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `E-TASK-010` | 收敛前端 Node/Vite/Vue 版本并核对 Vue SFC 构建约束 | 固定 Node/Vite/Vue 版本，核对 `@vitejs/plugin-vue` / `@vue/compiler-sfc` 约束是否可在当前仓库下移除，并把无法达成的架构约束真实写回治理链 | N/A | `npm run build`、toolchain/依赖核对、知识检查 |
| `E-TASK-011` | 去除 Vue SFC 构建链并增加双产物便携前端包 | 把根级前端迁移到非 `.vue` 方案，移除活动 SFC 构建链，补齐标准构建与 `dist-portable/` 可复制运行产物 | `E-TASK-010` | `npm run lint`、`npm run build`、`npm run build:portable`、toolchain/portable 检查 |
| `E-TASK-012` | 修复前端非 SFC 迁移后的布局回归 | 修复迁移后 Element Plus 壳层、设计 token 和代表性页面布局回归，不回退到 SFC 构建链 | `E-TASK-011` | `npm run lint`、`npm run build`、`npm run build:portable`、浏览器布局核验 |
| `E-TASK-013` | 补齐 portable 前端浏览器 smoke 并收口构建分包告警 | 把 portable 包纳入关键路由浏览器验证，并收口当前 Vite 大 chunk 告警，不重新引入 `@vitejs/plugin-vue` / `@vue/compiler-sfc` | `E-TASK-012` | `npm run lint`、`npm run build`、`npm run build:portable`、portable browser smoke、分包告警收口 |
| `E-TASK-014` | 恢复 Vue SFC 构建链并保留前端便携产物 | 在允许 `@vitejs/plugin-vue` / `@vue/compiler-sfc` 的前提下恢复根级前端 `.vue` 源文件与 SFC 构建链，同时保留现有 portable 包、浏览器 smoke 和分包优化结果 | `E-TASK-013` | `npm run lint`、`npm run build`、`npm run build:portable`、toolchain/portable/browser smoke 检查 |
| `E-TASK-015` | 补齐前端 dev browser smoke 并清理 SFC 恢复后的当前叙事 | 为 Vite dev server 补齐轻量浏览器 smoke，并清理仍把 non-SFC 迁移表述成当前真值的计划/操作文档；保留现有 Vue SFC、portable 产物与 full-stack runtime smoke 语义 | `E-TASK-014` | `npm run lint`、`npm run build`、`npm run build:portable`、toolchain/portable/dev browser smoke 检查 |
| `E-TASK-016` | 固化 dev browser smoke 的 local repo-closed 基线语义 | 明确 Vite dev browser smoke 只作为本地 repo-closed 开发验证基线存在，不把它升级为更广的 CI/runtime gating，并同步后续计划/操作文档对 full-stack runtime smoke 主路径的表述 | `E-TASK-015` | `npm run lint`、`npm run build`、`npm run build:portable`、`node scripts/check-dev-frontend.mjs`、knowledge/task audit 检查 |

##### Story `E-STORY-007` SQL 查询与历史前端增强

- 目标：让前端完整承载 SQL 查询、结果、历史列表、历史详情与取证 drill-through。
- 验证：查询工作台、结果页签、历史列表与详情可消费后端契约。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `E-TASK-017` | 扩展 SQL 查询工作台三栏布局与执行摘要 | 数据源树、SQL 编辑器、参数输入、右侧治理摘要与结果页签 | `E-TASK-016`,`D-TASK-039` | `npm run lint`、`npm run build`、frontend contract 测试 |
| `E-TASK-018` | 落地 SQL 历史列表筛选与分类面 | 历史过滤、分类、排序、分页与列表列渲染 | `E-TASK-017`,`D-TASK-040` | `npm run lint`、`npm run build`、history page contract 测试 |
| `E-TASK-019` | 落地 SQL 历史详情与取证视图 | SQL 三态、注释上下文、结构/访问解析、route/recommendation/alert/benchmark 关联取证视图 | `E-TASK-018`,`D-TASK-041` | `npm run lint`、`npm run build`、detail page contract 测试 |

##### Story `E-STORY-008` 解析工作台与批量解析中心

- 目标：交付单条解析、双轨结果展示、批量导入、报表清单解析与解析结果中心前端。
- 验证：结构解析 / access parse 卡片、批次页、统计页可消费后端契约。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `E-TASK-020` | 落地解析工作台双卡结果布局 | 单条 SQL 解析输入、结构解析卡、access parse 卡和综合结论 | `E-TASK-019`,`D-TASK-045` | `npm run lint`、`npm run build`、parse workbench contract 测试 |
| `E-TASK-021` | 落地批量解析中心与报表清单导入页 | 模板下载、上传、批次列表、批次详情与失败记录展示 | `E-TASK-020`,`D-TASK-054` | `npm run lint`、`npm run build`、batch import contract 测试 |
| `E-TASK-022` | 落地解析结果中心与优先级矩阵 | 解析统计、问题分布、priority matrix、important/urgent 清单 | `E-TASK-021`,`D-TASK-058` | `npm run lint`、`npm run build`、statistics page contract 测试 |
| `HARN-049` | HARN-049 解析工作台、批量解析与解析历史能力正式实现 | 在 E-STORY-008 / Phase-E 下，正式实现解析工作台、批量解析、解析历史查询三个独立页面；改造单条解析结果展示结构、中文化/help 提示、状态颜色提示；新增解析完成记录持久化落库、查询与详情查看；补充相关测试与文档。不得重写核心 SQL/parser 算法，不得修改无关治理或 runtime 流程。 | `N/A` | 页面拆分与独立路由测试：解析工作台、批量解析、解析历史查询互不串状态、单条解析结果上下布局与重复标题移除测试、总结/结论合并为统一解析结果结构的展示测试、urgent=true、priority=P1 及其他状态颜色提示测试、中文展示与代码/字段/缩写 help 入口测试、解析完成后历史记录持久化测试、解析历史查询与详情查看关键路径测试、治理验证：foreman validate、pre-closeout audit、post-closeout audit |

##### Story `E-STORY-009` 数据资产与逻辑视图

- 目标：交付数据源、Schema、表、业务逻辑视图和 DB View 的前端浏览与详情页。
- 验证：列表、详情、映射关系与使用热度展示可消费治理契约。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `E-TASK-023` | 落地数据资产目录与对象详情页 | datasource/schema/table/logical-view/db-view 列表与详情页 | `E-TASK-022`,`D-TASK-049` | `npm run lint`、`npm run build`、asset page contract 测试 |
| `E-TASK-024` | 落地逻辑视图映射、数据到位与热度视图 | 逻辑对象映射、freshness/SLA/usage heat 与相关 SQL 展示 | `E-TASK-023`,`D-TASK-049` | `npm run lint`、`npm run build`、logical object contract 测试 |

##### Story `E-STORY-010` 路由治理与推荐中心

- 目标：交付路由规则、历史决策、推荐 SQL 与治理事件状态的前端消费面。
- 验证：路由规则、历史决策、推荐详情与 dispatch 状态可视化可用。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `E-TASK-025` | 落地路由治理页与历史决策详情 | 当前规则、决策样例、历史记录、注释协议说明与路由详情页 | `E-TASK-024`,`D-TASK-061` | `npm run lint`、`npm run build`、routing page contract 测试 |
| `E-TASK-026` | 落地推荐与加速中心页 | 推荐分类、详情、收益/风险、dispatch 状态与关联追溯页 | `E-TASK-025`,`D-TASK-062` | `npm run lint`、`npm run build`、recommendation page contract 测试 |

##### Story `E-STORY-011` 压测中心与开放接入页

- 目标：交付压测任务、模板、测试集、报告，以及开放接入说明和策略展示页。
- 验证：压测中心与开放接入页可消费后端契约。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `E-TASK-027` | 落地压测任务、模板、测试集与报告页 | 任务列表、模板详情、测试集、报告对比和回归结果页 | `E-TASK-026`,`F-TASK-042` | `npm run lint`、`npm run build`、benchmark page contract 测试 |
| `E-TASK-028` | 落地开放接入页与 JDBC Agent / SDK 展示 | API、JDBC Agent、SDK、接入策略和接入审计展示页 | `E-TASK-027`,`D-TASK-068` | `npm run lint`、`npm run build`、access page contract 测试 |

##### Story `E-STORY-012` Dashboard 与告警中心

- 目标：交付多角色总览驾驶舱与告警中心前端。
- 验证：总览 KPI、待办与告警中心可消费治理契约。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `E-TASK-029` | 落地 Dashboard KPI、分布与待办区块 | 核心 KPI、问题分布、接入分布与待处理清单卡片 | `E-TASK-028`,`F-TASK-037` | `npm run lint`、`npm run build`、dashboard contract 测试 |
| `E-TASK-030` | 落地告警中心与通知状态视图 | 告警列表、详情、ACK、notify simulated 状态展示 | `E-TASK-029`,`F-TASK-037` | `npm run lint`、`npm run build`、alert page contract 测试 |

##### Story `E-STORY-013` 系统管理与数据源治理页

- 目标：补齐系统管理模块中的数据源管理、报表接口、Redis 规则源、装数协同配置与系统参数消费页。
- 验证：系统管理页面能消费 `D-STORY-013` 后端契约并通过前端构建/契约校验。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `E-TASK-031` | 落地系统管理中的数据源与报表接口页 | datasource 管理、测试连接、报表接口配置与健康状态页 | `E-TASK-024`,`D-TASK-072` | `npm run lint`、`npm run build`、system-management datasource contract 测试 |
| `E-TASK-032` | 落地 Redis 规则源、装数协同与系统参数页 | Redis rule source、dispatch policy、系统参数与权限审计展示 | `E-TASK-031`,`D-TASK-072` | `npm run lint`、`npm run build`、system-management config contract 测试 |

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

##### Story `F-STORY-004` 运行时门禁扩展与治理链路硬化

- 目标：把运行时 smoke、浏览器门禁、治理历史链路、Kafka 运行验证与 Phase-F 退出门禁扩展补回主计划。
- 验证：默认 CI、Phase Gate、浏览器 runtime smoke、Kafka gate 与治理历史页面的验证证据链一致。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `F-TASK-010` | 接入运行时 smoke 到默认 CI | compose/health/message queue smoke 入 CI | `F-TASK-005`,`F-TASK-006` | runtime smoke 与 CI 证据链通过 |
| `F-TASK-011` | 扩展多服务运行时 smoke 门禁 | `query-execution`、`sql-optimization`、`benchmark-engine`、frontend 启动探针 | `F-TASK-010` | 多服务 startup/runtime smoke 通过 |
| `F-TASK-012` | 扩展跨服务业务级 runtime smoke 门禁 | `query-execution -> governance` 成功/失败/审计补偿 | `F-TASK-011` | query-governance business smoke 通过 |
| `F-TASK-013` | 扩展优化与压测业务级 runtime smoke 门禁 | `sql-optimization` / `benchmark-engine` 到治理服务的业务门禁 | `F-TASK-012` | optimization/benchmark business smoke 通过 |
| `F-TASK-014` | 扩展前端真实业务 runtime smoke 门禁 | 浏览器驱动 `sql-query`/`acceleration`/`benchmark` 真实路径 | `F-TASK-013` | frontend runtime smoke 通过 |
| `F-TASK-015` | 推进 `sql-optimization` 真实持久化与调度链路 | MySQL 任务表、scheduled worker、治理 smoke | `F-TASK-013` | 优化服务模块测试与手工 smoke 通过 |
| `F-TASK-016` | 推进 `benchmark-engine` 真实持久化与调度链路 | MySQL 任务/报告表、scheduled worker、治理 smoke | `F-TASK-015` | 压测服务模块测试与手工 smoke 通过 |
| `F-TASK-017` | 扩展前端失败恢复与审计补偿 runtime gate | 浏览器侧失败恢复、补偿与 backlog 证据 | `F-TASK-016` | browser runtime smoke 通过 |
| `F-TASK-018` | 扩展 `system` 治理管理页 browser runtime gate | tenant-config、message stats、retry failed messages | `F-TASK-017` | browser runtime smoke 与治理修复动作通过 |
| `F-TASK-019` | 加固前端补偿信号稳定性 | pending/total 双信号、收口残余验证日志 | `F-TASK-018` | lint/build/browser smoke 通过 |
| `F-TASK-020` | 扩展治理历史页 browser runtime gate | `/parse-record` 真实历史诊断页 | `F-TASK-019` | governance history runtime smoke 通过 |
| `F-TASK-021` | 扩展治理历史修复追溯页 browser runtime gate | `/repair-evidence` 取证与补偿证据 | `F-TASK-020` | repair evidence runtime smoke 通过 |
| `F-TASK-022` | 升级治理长期历史反查与分页追溯 | indexed history lookup、分页与旧数据追溯 | `F-TASK-021` | governance history test + frontend runtime smoke 通过 |
| `F-TASK-023` | 扩展历史诊断与审计取证分页链路 | `/audit-forensics` 与 parse record drill-through | `F-TASK-022` | governance history test + frontend runtime smoke 通过 |
| `F-TASK-024` | 新增审计故障处置与修复决策页 | `/audit-troubleshooting` remediation decision page | `F-TASK-023` | remediation runtime chain 通过 |
| `F-TASK-025` | 补齐治理归档历史窗口与深分页链路 | archival-window query、深分页和 drill-through | `F-TASK-024` | governance history API + runtime smoke 通过 |
| `F-TASK-026` | 接入真实 Kafka 运行验证与环境安全参数门禁 | bootstrap/security 校验、成功/恢复 smoke | `F-TASK-025` | real Kafka runtime gate 通过 |
| `F-TASK-027` | 收口 Phase-F 退出门禁缺口 | DB script、coverage、Sonar、R-118 证据 | `F-TASK-026` | phase gate / DB / compliance baseline 通过 |
| `F-TASK-028` | 拆分主线业务与治理运维页面路径 | `/governance/history/*` 与 `/governance/ops/*` route namespace | `F-TASK-024` | lint/build/browser routing 校验通过 |

##### Story `F-STORY-005` 发布门禁自动化与稳定性收口

- 目标：把 `F-TASK-027` closeout 后剩余的 coverage、Sonar、release automation 与环境级门禁 follow-up 从人工注意事项收口为正式计划任务，并把仓库门禁修正为 `repo-closed` 主路径 + `environment-backed` 增强项的双层模型。
- 验证：phase gate / release gate 默认路径、coverage 门禁入口、Sonar / 真实 Kafka fallback 入口与 release metadata 证据链一致。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `F-TASK-029` | 收口 release automation 与门禁稳定性 | 稳定 coverage 入口、明确 Sonar 强制约束、把 phase gate 绑定到 release metadata 自动触发链 | `F-TASK-027`,`F-TASK-028` | release gate workflow、coverage phase gate、Sonar-required path 通过 |
| `F-TASK-030` | 提升覆盖率并补齐 Sonar 发布环境 | phase1plus 覆盖率提升到 85%+、补齐 Sonar secrets / 发布环境接线、验证自动 release gate 可稳定放行 | `F-TASK-029` | coverage phase1plus 达标、Sonar-required path 可运行、release gate 通过 |
| `F-TASK-031` | 将 Sonar 与环境级门禁降级为 fallback，并建立双层门禁语义 | 把仓库主线固定为 repo-closed 门禁，把 Sonar / real Kafka / 环境级发布验证重述为 environment-backed fallback，修正 workflow 默认值、R-117 和相关文档真值 | `F-TASK-030` | phase gate/release gate 默认不再强制 Sonar 或真实 Kafka，且 repo-closed 与 environment-backed 语义在脚本、workflow、文档、台账一致 |
| `F-TASK-032` | 去除 Sonar fallback 的隐性自动恢复接线，并分离 provisioning / enable 语义 | 修正 release workflow 的默认 environment 绑定与主 CI 的 Sonar 自动触发条件，明确“环境已 provision”不等于“治理已启用强制 Sonar”，同步 runbook、INBOX 与部署基线 | `F-TASK-031` | release/CI workflow 默认不因已有 Sonar 环境自动升级为阻断；文档、INBOX、workflow 对 provisioning 与 enable 语义一致 |
| `F-TASK-033` | 补齐测试环境最小 smoke 门禁 | 提供环境无关的最小 smoke 入口给外部测试环境 CI/CD 调用，覆盖四个后端 health、前端可达性、query/sql-optimization/benchmark 到 governance 的最小业务链路，以及受保护请求头有效性验证；保持本地 runtime smoke 不变 | `F-TASK-032` | 环境无关 smoke 脚本、帮助/参数校验、最小本地验证、repo-closed 与 test-environment smoke 文档语义一致 |

##### Story `F-STORY-010` 告警中心与模拟邮件

- 目标：补齐 SQL 治理产品线的关键事件判定、告警去重、ACK 与模拟邮件日志。
- 验证：告警类型、等级、查询、ACK 与模拟通知链可测。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `F-TASK-034` | 固化告警事件类型与等级模型 | 定义 alert type、level、dedup key、notify status 和规则基线 | `F-TASK-033`,`D-TASK-062` | domain/model 测试 |
| `F-TASK-035` | 落地关键事件告警判定 | mass failure、service unavailable、report resolve failure、Redis unavailable、dispatch failure 等告警判定 | `F-TASK-034` | alert rule 测试 |
| `F-TASK-036` | 落地告警去重与模拟邮件日志 | dedupe、notify simulated、日志模板与审计留痕 | `F-TASK-035` | notification/dedup 测试 |
| `F-TASK-037` | 落地告警查询与 ACK 接口 | alert list/detail/ack API 与治理查询面 | `F-TASK-036` | governance alert API 测试 |

##### Story `F-STORY-011` 压测模板、测试集与解析联动

- 目标：让 benchmark-engine 支持模板、测试集、批量导入以及从解析/推荐结果衍生压测对象。
- 验证：模板、测试集、联动生成与回归守护可测。

Tasks:

| Task ID | Task | Scope | Dependencies | Verification |
|:---|:---|:---|:---|:---|
| `F-TASK-038` | 固化压测模板与测试集契约 | 模板类型、阈值、测试集来源与标签模型 | `F-TASK-037`,`D-TASK-068` | contract/domain 测试 |
| `F-TASK-039` | 落地批量测试集导入 | 从文件导入 test set 与 case 字段映射 | `F-TASK-038` | import 测试 |
| `F-TASK-040` | 打通解析结果到测试集一键生成 | parse issue / report / SQL 结果生成 benchmark test set | `F-TASK-039`,`D-TASK-058` | parse-to-benchmark 测试 |
| `F-TASK-041` | 打通推荐 SQL 到对比压测 | recommendation -> comparison benchmark 契约与编排 | `F-TASK-040`,`D-TASK-062` | recommendation-to-benchmark 测试 |
| `F-TASK-042` | 落地回归守护统计与告警 | regression summary、threshold hit 与 alert linkage | `F-TASK-041`,`F-TASK-037` | regression/alert linkage 测试 |

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
- 当前默认把 `governance` 视为公共管理服务的现阶段实现基线，而不是全部目标能力已完成。
- 当前默认所有新增计划和后续任务都遵循“task 内高内聚、task 间低耦合、验证先行”。
- 当前默认每个阶段结束前必须回填：阶段状态、验证结果、剩余风险、待确认项状态、交付记录。

## 10. 2026-04-20 Documentation Governance Addendum

本附录用于把本轮文档治理输出接入主计划，不改变前文历史语义和阶段拆分。

### 10.1 New planning authorities

- 当前仓库真值分层：`docs/plans/document-truth-baseline.md`
- 严格核验缺口矩阵：`docs/plans/document-gap-matrix.md`
- 编码前置消费顺序与波次执行：`docs/plans/implementation-readiness.md`
- 阶段输入文档、ADR、规则和确认点矩阵：`docs/plans/phase-prerequisite-matrix.md`
- 4 微服务到当前仓库的能力映射：`docs/architecture/service-capability-map.md`
- 服务间统一接口契约：`docs/architecture/service-interface-contract-baseline.md`
- Task 扩展治理字段：`docs/plans/task-governance-extension-matrix.md`
- 阶段/批次复盘模板：`docs/plans/retrospective-template.md`

### 10.2 Immediate execution effect

- `A-STORY-001` 和 `A-STORY-002` 的输出已被进一步固化到 `document-truth-baseline.md`
- `A-STORY-003` 中“规则、文档、实现、验证之间的映射”新增了当前消费顺序与漂移映射说明
- 严格核验与阶段切换时，必须同时检查 `document-gap-matrix.md` 与 `phase-prerequisite-matrix.md`
- `C-STORY-001` 与 `C-STORY-003` 后续实现时，必须先遵守 `service-capability-map.md` 对 `sqlforge-shared` 与 `governance` 的边界约束
- `Phase-C` 之后的跨服务实现，必须先遵守 `service-interface-contract-baseline.md` 中的错误码归属和 DTO/事件边界
- 所有后续编码波次默认先遵守 `implementation-readiness.md` 的消费顺序和执行分波
- 若核心 10 字段无法承载严格治理要求，则同步维护 `task-governance-extension-matrix.md`

### 10.3 Current truth reminder

- 当前通过验证的事实仍是：
  - `governance` 是当前最小治理基线
  - `sqlforge-shared` 仍为共享层占位模块
  - 查询执行服务、SQL 优化服务、压测引擎服务尚未形成独立代码模块
- 因此 Phase-C 之后的所有实现都必须显式区分：
  - “当前在哪个模块落地”
  - “最终属于哪一个微服务边界”

### 10.4 Retro requirement

- 任何跨服务、跨前后端或跨规则治理批次结束后，都应补一份基于 `retrospective-template.md` 的复盘记录。

### 10.5 Strict audit closeout

- 2026-04-20 严格核验中识别出的 7 项未完全闭口缺口，已通过新增 gap matrix、phase prerequisite matrix、service interface contract baseline、task governance extension matrix，以及 `DOC-GOV-001` / `DOC-GOV-002` 任务归档和 repair 复盘完成闭口。
