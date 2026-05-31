# USER-CN-DYNAMIC-REPEATED-SUBGRAPH-MV-20260531 原始需求

原始需求：

> 继续实现执行了一半的任务，任务内容：严格模式，不要丢失任何内容，确保你理解以下内容后，再生成方案后实现：为什么现在项目的推荐mv功能的能力，无法支持对docs/test01.sql推荐生成推荐的mv（参考docs/test01_mv.sql）了？是不是静态解析去掉后，动态解析能力还不足？请找出问题，给出方案，然后后优化实现，直到实现可以推荐出类似于test01_mv.sql内容的能力。严格模式，不要搞半成品，坚决禁止静态解析、静态推荐。sql解析、推荐、改写和推荐mv的时候，不能用任何静态常量或变量做解析推荐。

执行边界：

- 必须解释移除静态解析后 `docs/test01.sql` 推荐能力退化的真实原因。
- 必须通过动态 SQL 结构解析、推荐与改写能力恢复类似 `docs/test01_mv.sql` 的 MV 推荐效果。
- 禁止读取期望 SQL fixture 作为运行时推荐输入。
- 禁止使用 test01 专用字段白名单、别名白名单、阈值默认值、固定报表 CTE、固定输出列或固定 SQL 模板生成解析、推荐、改写或 MV 推荐结果。
- 允许通用 SQL parser、AST 节点类型、协议 key、通用阻断码和通用 SQL 渲染规则使用稳定常量；这些常量不得编码某一条 SQL 或某一类业务报表的业务内容。
- SQLForge 继续保持 `PULL_ONLY_NOT_EXECUTED_BY_SQLFORGE` 边界，不执行真实外部 MV DDL、refresh 或生产 runtime binding。
