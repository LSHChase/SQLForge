# SQL Governance Degradation Matrix

## Summary

本文件定义 SQL 治理产品线在数据库、引擎、元数据、Redis、报表接口、装数协同、邮件通道与外部接入能力不可用时的统一降级语义。原则是：优先保留结构化可用结果，明确 unavailable 原因，不把 mock / 日志模拟写成真实环境能力。

## 1. Global Rules

- 结构解析优先于数据访问解析。
- 结构解析成功时，不允许因为数据访问解析失败而把整体结果直接写成完全失败。
- 轻量解析超时或规则源不可用时，优先旁路并继续执行。
- 推荐协同、邮件通知、报表接口、Redis 与外部数据源不可用时，必须显式记录 unavailable / degraded 状态。
- mock、txt、日志模拟、environment-backed 证据不得混写成默认仓库事实。

## 2. Execution and Parse Matrix

| Scenario | Primary impact | User-visible behavior | Persistence / audit behavior | Recovery stance |
|:---|:---|:---|:---|:---|
| 数据库连接不可用 | 无法执行 access parse 或真实查询 | 结构解析结果仍返回；执行或 access parse 标记 unavailable | 写入结构解析结果、错误摘要、degrade reason | 可重试，保留 partial success |
| 元数据服务不可用 | 无法补齐对象、分区、SLA 结果 | 结构解析结果仍返回；access parse 标记 metadata unavailable | 写入 service status 与 degrade reason | 允许稍后异步补跑 |
| SQL 语法错误 | 结构解析失败 | 返回结构解析失败与问题详情 | 记录 parse issue 和 error code | 不自动重试 |
| 参数绑定不完整 | query_date / 路由 / 深度解析精度下降 | 展示模板 SQL、部分参数、低置信度说明 | 写入 `binding_render_status=PARTIAL/FAILED` | 允许人工补参重跑 |
| 轻量解析超时 | 主执行不应被拖慢 | 继续执行，页面提示 lightweight parse bypassed | 审计记录轻量解析降级 | 不阻断主流程 |
| Redis 规则源不可用 | JDBC Agent / 轻量路由规则不可用 | Agent 或服务端回退默认策略 / bypass；`query-execution` 主 runtime binding 状态不回滚 | 写告警、route fallback evidence 与 `jdbcAgentRedisSync` 失败证据 | 按 `retryable=true` 证据重试同步，恢复后重新命中规则 |

## 3. Batch and Report Parse Matrix

| Scenario | Primary impact | User-visible behavior | Persistence / audit behavior | Recovery stance |
|:---|:---|:---|:---|:---|
| `xlsx/csv/txt/sql` 导入失败 | 批次无法建立或部分记录失败 | 批次页面显示失败记录 | 保留失败条目与原因 | 修复文件后重提 |
| `xls/et` 兼容解析失败 | 兼容格式无法完全解析 | 提示建议使用 `xlsx/csv` | 记录兼容失败原因 | 不阻断主格式主线 |
| 报表接口不可用 | 无法拉取报表 SQL | 批次保留 report list 记录并标记 resolve failed | 写 resolve failure 与告警 | 可重试或切回 txt/mock |
| 报表 txt/mock source 不存在 | mock 路径不可用 | 批次标记失败并提示缺少 mock source | 记录失败原因 | 修复 source 后重跑 |

## 4. Recommendation and Dispatch Matrix

| Scenario | Primary impact | User-visible behavior | Persistence / audit behavior | Recovery stance |
|:---|:---|:---|:---|:---|
| 推荐生成失败 | 无法输出推荐 SQL | 页面显示 recommendation failed | 写失败原因与 issue link | 允许重新生成 |
| 装数模块尚未拉取事件 | 推荐未进入真实装数 | 页面显示 dispatch created / published / pending pull | 保留 dispatch event 状态 | 后续由外部模块拉取 |
| 装数模块拉取失败或 ACK 失败 | 协同未闭环 | 页面显示 dispatch failed | 写治理事件与告警 | 可重试 / 人工干预 |

## 5. Open Access Matrix

| Scenario | Primary impact | User-visible behavior | Persistence / audit behavior | Recovery stance |
|:---|:---|:---|:---|:---|
| JDBC Agent `Observe` 模式失败 | 无法上报或采集 | 不接管执行；仅提示采集失败 | access audit 记录失败 | 不影响业务查询 |
| JDBC Agent `Governed Execute` 调用平台失败 | 无法通过平台执行 | 若策略允许则回退 direct JDBC；否则失败 | 记录 fallback / failure | 依策略执行 |
| JDBC Agent `Local Rewrite + Direct JDBC` 改写失败 | 不能本地改写 | 回退原 SQL 直连或失败 | 记录 rewrite bypass | 不得 silent mutate |
| Java SDK 调用失败 | API 客户端受影响 | 返回结构化失败 | access audit 与 client error 记录 | 允许重试 |

## 6. Alert and Notification Matrix

| Scenario | Primary impact | User-visible behavior | Persistence / audit behavior | Recovery stance |
|:---|:---|:---|:---|:---|
| 邮件通道未接 | 不能真实发邮件 | 页面显示 notify simulated | 告警记录 + 模拟发送日志 | 后续接真通道 |
| 告警去重命中 | 防止重复打扰 | 页面显示 dedupe suppressed | 保留去重键与源事件 | 不重复发送 |
| 批量失败率过高 | 平台内部告警事件 | Dashboard 摘要或任务详情可见 | 落内部告警 / 任务证据事件，不提供告警中心入口 | 继续保留原任务结果 |

## 7. Repo-Closed vs Environment-Backed

- `repo-closed`
  - 文档
  - 主计划
  - 矩阵
  - 结构解析
  - query history / routing / recommendation / dispatch / alert 数据模型
  - mock / txt / 日志模拟
- `environment-backed`
  - 真实报表接口
  - 真实数据库 access parse 证据
  - 真实 Redis 集群长跑
  - 真实邮件服务
  - 真实装数模块联调

## Related Documents

- `docs/product/sql-governance-platform-implementation-spec.md`
- `docs/architecture/sql-governance-interface-extension-baseline.md`
- `docs/plans/master-execution-plan.md`
