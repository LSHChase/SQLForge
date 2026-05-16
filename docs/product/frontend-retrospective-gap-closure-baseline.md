# Frontend Retrospective Gap-Closure Baseline

## Summary

本文件记录 SQLForge 前端在完成三轮重构后的 repo-side 复盘结论，目的不是重复产品规格，而是把“当前前端还缺什么、哪些已经补齐、哪些仍受后端或环境边界约束”固化为一份可执行基线，避免后续再次把页面 shell、临时重定向和局部样本误写成规格闭环。

## Confirmed Repo-Side Findings

### 1. 解析二级能力由 HARN-049 拆为独立页面

- 产品规格固定要求 `解析工作台`、`批量解析中心`、`解析结果中心`、`加速与改写中心` 四个二级能力。
- HARN-049 后，单条解析工作台继续由 `AccelerationView` 承载，批量解析迁移到 `ParseBatchCenterView`，解析历史查询由 `ParseRecordView` 承载。
- 批量解析和解析历史查询不再依赖 `workspace` query 参数或旧弹窗作为主入口；旧 query 入口只负责跳转到新页面。
- 解析工作台页面内保留统计结果区与显式入口卡片，但页面职责聚焦为单条 SQL 解析、结果阅读和状态刷新。

### 2. 路由治理模块名称与实施规格存在漂移

- 产品规格固定一级模块名为 `路由治理`。
- 当前壳层曾为了强调 read-only 语义，把一级模块标签改成了“路由证据”。
- 页面本身继续保持 evidence-first 没问题，但一级模块名漂移会让导航与实施规格包不一致。

### 3. Dashboard 指标覆盖不完整

- 现有首页已经具备 KPI、主入口、健康风险、活动流和下一步建议。
- 但相对于实施规格，仍缺少一批可由现有 repo-side 证据直接表达的 sample KPI：
  - 成功率 / 失败率样本
  - 缓存命中样本
  - 轻量改写命中样本
  - 加速命中样本
  - 装数协同状态
  - 接入方式样本
  - 告警样本
- 同时，仍有一部分指标没有全局后端聚合，不能被前端伪装成租户级事实：
  - benchmark 全局通过率
  - recommendation 采纳率
  - tenant-wide access audit summary

## Repo-Closed Closures Implemented By U-TASK-004

- 侧栏恢复解析与加速模块的显式二级入口：
  - `解析工作台`
  - `批量解析中心`
  - `解析历史查询`
  - `加速与改写中心`
- 导航激活逻辑保留 legacy query 兼容，但批量解析和解析历史查询已经使用独立路由高亮。
- 一级模块标签恢复为 `路由治理`，但页面内仍继续使用 evidence / execution evidence 的只读语义，不把它误写成可写治理控制面。
- `AccelerationView` 顶部保留二级入口卡片，入口指向独立批量解析页、独立解析历史查询页和推荐中心，避免用户只看到一个统称工作台。
- Dashboard 扩展为 sample-aware KPI：
  - success-rate sample
  - failure-rate sample
  - cache-hit sample
  - rewrite-hit sample
  - acceleration-hit sample
  - deep recommendation sample
  - dispatch coordination
  - alert sample
  - access-channel sample
- 所有新增首页指标都必须明确写成：
  - `sample`
  - `window`
  - `session`
  - 或 `PULL_ONLY`
  之一，禁止把当前仓库没有全局聚合接口的指标写成全租户最终事实。

## HARN-FE-001A Display IA Supersession

U-TASK-004 的结论仍用于说明当时的 repo-closed 修复结果，但 HARN-FE-001 / HARN-FE-001A 已把后续前端展示目标调整为“核心 SQL 工作流优先”。后续导航、首页和页面标题改造以 `docs/plans/frontend-core-workflow-refocus-task-pack.md` 为展示层权威：`SQL 查询分析`、`SQL 历史查询`、`SQL 解析`、`解析历史`、`推荐结果`、`改写记录` 和 `改写历史` 构成核心路径，审计、追踪、告警、运行门禁和恢复演练作为辅助治理证据后置。

该 supersession 只覆盖前端信息架构和视觉权重，不改变已有后端接口、数据模型、推荐/改写状态语义或 `PULL_ONLY` 边界。HARN-FE-002 实施导航时，必须在同一任务内同步导航契约脚本，避免旧 contract 继续强制 `加速治理工作台` 作为 `解析与加速` 正式菜单项。

## Remaining Pending Gaps

以下缺口在当前仓库内不能仅靠前端闭环，必须继续保持显式边界：

- `路由治理`、`开放接入`、`告警中心` 仍缺真实写 API，相关 create/edit 行为继续保留 placeholder 或 simulated 语义。
- Dashboard 仍没有 repo-side 全局 benchmark task list，因此只能保留 benchmark workbench 入口与边界说明，不能声称真实全局通过率。
- recommendation adoption rate 仍缺专用后端聚合接口，首页只能展示当前 recommendation 样本量与 dispatch-ready 状态。
- `GET /api/governance/access-audit` 独立前端控制器尚不存在，开放接入页继续使用 query-history 的 `accessChannel` 过滤面渲染审计样例。

## Validation Expectations

- 导航 contract 必须覆盖 query-aware 的解析子入口与 `路由治理` 模块标签。
- Dashboard contract 必须覆盖新增 sample KPI 与 dispatch-coordination token。
- 若后续再次合并解析入口，必须同时证明：
  - 导航仍保留规格要求的显式二级可达性
  - 页面与文档不会把“隐藏入口”误写成“功能已完整可发现”
