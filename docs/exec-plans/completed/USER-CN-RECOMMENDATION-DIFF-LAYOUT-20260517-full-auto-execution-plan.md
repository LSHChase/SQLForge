# USER-CN-RECOMMENDATION-DIFF-LAYOUT-20260517 Full-Auto Execution Plan

## Objective

收口推荐结果页的推荐列表、详情抽屉和 SQL compare 展示，并补齐后端分页/过滤/安全排序接口，使推荐页不再依赖前端本地分页或页面内左列表右详情布局。

## Scope

1. 后端新增 recommendation page 查询链路：filter DTO、page VO、application service、repository、MyBatis XML 查询和 controller endpoint。
2. 保留 legacy `GET /api/sql-optimization/recommendations` 数组接口语义，新增分页接口独立服务推荐页。
3. 前端 API client 增加 `getRecommendationPage`，推荐页改为顶部查询条件、下方表格分页、点击行打开详情抽屉。
4. 抽取公共 `SqlCompareBlock.vue`，由推荐页 SQL diff tab 复用，保留后端 diff/AST evidence 为权威证据入口。
5. 更新推荐页 contract、frontend dev smoke 和 production rewrite closed-loop smoke，确保抽屉、分页接口、compare 组件和改写生命周期仍可用。

## Guardrails

- 不改变数据库 schema。
- 不改变推荐状态机、diff 语义、审批、发布、dispatch、自动应用或执行 SQL 语义。
- 排序字段只允许后端白名单生成固定列名；MyBatis XML 不接受前端原始字段拼接。
- 深链 `recommendationId`、`rewriteRecordId`、`tab` 仍能直接打开对应详情抽屉和页签。

## Validation

- `java -version` 确认 `1.8.0_112`。
- `mvn -pl sql-optimization,sqlforge-shared -am test`。
- `npm run lint`。
- `npm run build`。
- `npm run test:sql-ui-contract`。
- `npm run test:frontend-page-governance`。
- `node scripts/check-recommendation-page-contract.mjs`。
- `npm run smoke:frontend-dev`。
- `npm run smoke:production-rewrite-closed-loop`。
- `git diff --check`。
- `python3 scripts/foreman.py validate USER-CN-RECOMMENDATION-DIFF-LAYOUT-20260517 --include-task-audit` plus task audit pre/post closeout.

## Residual Risk

- Repo-closed browser smoke uses mocked recommendation payloads; real Hetu/MRS evidence remains environment-backed and tracked separately by existing blocked validation work.
