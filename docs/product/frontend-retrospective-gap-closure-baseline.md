# Frontend Retrospective Gap-Closure Baseline

## Summary

本文件记录 SQLForge 前端在完成三轮重构后的 repo-side 复盘结论，目的不是重复产品规格，而是把“当前前端还缺什么、哪些已经补齐、哪些仍受后端或环境边界约束”固化为一份可执行基线，避免后续再次把页面 shell、临时重定向和局部样本误写成规格闭环。

## Confirmed Repo-Side Findings

### 1. 解析二级能力存在“已实现但不直达”的入口缺口

- 产品规格固定要求 `解析工作台`、`批量解析中心`、`解析结果中心`、`加速与改写中心` 四个二级能力。
- 当前仓库在 `AccelerationView` 内已经承载了单条解析、批量解析、统计视角和解析历史，但此前主导航只保留了“解析工作台 + 推荐中心”。
- 结果是：
  - 批量解析和解析结果能力只能通过页面内 tab、query 参数或 legacy redirect 命中。
  - 用户从导航层面无法直接感知完整的二级能力边界。

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

- 侧栏恢复解析与加速模块的四个显式二级入口：
  - `解析工作台`
  - `批量解析中心`
  - `解析结果中心`
  - `加速与改写中心`
- 导航激活逻辑升级为 query-aware，允许同一路径按 `workspace` / `analytics` 正确高亮对应入口。
- 一级模块标签恢复为 `路由治理`，但页面内仍继续使用 evidence / execution evidence 的只读语义，不把它误写成可写治理控制面。
- `AccelerationView` 顶部新增四个二级入口卡片，确保用户在页面内也能理解二级能力分工，而不是只看到一个统称工作台。
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
