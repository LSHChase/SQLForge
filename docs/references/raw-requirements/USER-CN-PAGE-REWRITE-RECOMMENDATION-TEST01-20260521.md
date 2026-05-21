# USER-CN-PAGE-REWRITE-RECOMMENDATION-TEST01-20260521 Raw Requirement

用户在上一阶段算法链路一致性实现后，要求开始适配页面，页面尽量轻量改造，并从页面输入 `docs/test01.sql` 内容进行测试，截图分析是否成功推荐出正确 SQL。

## Scope

- 轻量适配 `SQL 改写验证` 页面，不重做页面结构。
- 页面应展示后端改写试算返回的最终推荐报告、算法链路状态和候选 SQL 关键形态。
- 页面输入必须使用 `docs/test01.sql` 内容，而不是只跑后端单元测试。
- 截图验证需说明是否成功推荐正确 SQL。

## Expected Evidence

- 改写任务状态达到 `SUCCEEDED`。
- 页面展示 `CONFORMS_WITH_STATIC_SURROGATES` / `RECOMMENDATION_GENERATED` 等核心状态。
- 候选 SQL 命中净增报表关键形态：
  - `raw_customer_snapshot`
  - `report_customer_snapshot`
  - `base_100_anchor`
  - `metric_by_org`
  - `growth_by_org`
  - `UNION ALL`
  - 不使用 `GROUPING SETS`
- 截图保存并完成视觉分析。

## Boundary

- 本阶段不执行真实 SQL，不读取生产数据。
- 本阶段不创建生产 runtime rewrite binding。
- 页面核验只展示静态试算、推荐报告和候选 SQL 形态，不把前端判断写成结果集等价结论。
