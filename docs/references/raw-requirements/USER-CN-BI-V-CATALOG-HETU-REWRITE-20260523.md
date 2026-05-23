# USER-CN-BI-V-CATALOG-HETU-REWRITE-20260523 Raw Requirement Snapshot

## 原文摘要

在 SQL 进入执行、解析、推荐、改写判断前，先做统一 SQL 级前处理：凡识别到限定名前缀 `BI_XXX_V.`，替换为 `BI_XXX_HETU.`。匹配大小写不敏感并尽量保留原大小写风格；历史、推荐、改写记录中的主 SQL 文本仍保存用户原始 SQL，只在实际分析/执行链路使用替换后的 SQL。

## 关键约束

- 新增共享 Java 8u112 兼容工具，跳过单引号字符串、`--` 行注释和 `/* ... */` 块注释。
- 覆盖未引号、反引号、双引号标识符中的 `BI_<非空XXX>_V` 限定名前缀，且下一个非空白字符为 `.`。
- 共享指纹前处理、查询执行、JDBC Agent、SQL 优化解析、推荐与改写判断使用替换后的功能性 SQL。
- `query_history.sqlText`、解析历史、推荐、改写记录中的主 SQL 文本保留用户原始 SQL；`boundSql` / `actualSql` 反映实际执行 SQL。
- 该规则是 catalog 兼容前处理，不新增 HTTP API、数据库字段、前端页面或配置开关，不把 `rewriteApplied` 置为 `true`。
