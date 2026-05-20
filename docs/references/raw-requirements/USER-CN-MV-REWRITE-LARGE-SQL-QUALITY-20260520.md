# USER-CN-MV-REWRITE-LARGE-SQL-QUALITY-20260520

原始需求：

> sql推荐改写中建MV（物理视图）部分有些不太准确，请深入分析并优化，并找50类不同的sql（sql长度要有大有小，sql长度及支持能力与sql解析支持类型差不多）进行测试修复；另外sql改写系列好像能支持的sql长度和sql行数都很小，请搞特别大一些。

执行边界：

- 优化 SQL 推荐改写中 L2 建 MV / 物化视图产物的准确性。
- 补齐 50 类不同 SQL 样例回归，样例长度覆盖短 SQL、普通 SQL、较长 SQL 和多行 SQL。
- 扩大 SQL 改写相关后端 diff / 页面输入对大 SQL 文本和多行 SQL 的承载能力。
- 继续保持 SQLForge `PULL_ONLY_NOT_EXECUTED_BY_SQLFORGE` 边界，不执行真实外部 MV DDL、refresh 或生产 runtime binding。
