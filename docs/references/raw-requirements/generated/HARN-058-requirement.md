用户在 HARN-057 后继续追问解析历史与报表导入详情缺口：

1. SQL 解析页面的单条 SQL 解析记录为什么在解析历史页面看不到。
2. 批量解析的报表导入为什么也在解析历史页面看不到。
3. 报表导入解析出的 SQL 为什么看不到解析详情。
4. 报表导入详情需要同时支持 SQL 级和报表级解析统计，参考 SQL 解析页面的统计内容。

核对结论：

- 单条 SQL 与报表导入的结构解析已触发历史写入，但写入的 `queryContextJson` 只有扁平 `commentContext`，治理侧投影只读取 `queryContext.commentContext`，导致 `report_code`、`stage`、`biz_date` 等历史列表筛选/展示字段为空。
- 综合解析和批量解析中的 access parse 结果没有稳定合并回同一条 `query_history`，历史详情缺少 `accessParseSummary`。
- 报表导入页面只展示报表项基本信息，未展示解析出的 SQL 文本、parseTaskId、结构解析状态、access 状态、问题场景和逻辑对象命中。
- 解析历史页的报表导入历史只有跳转入口，缺少在历史页内直接查看报表级与 SQL 级详情的抽屉。

执行边界：

- 不改写 SQL parser 解析算法。
- 不弱化 tenant/request/trace 受保护上下文。
- 按同一个 `parseTaskId` / `historyId` 合并结构解析与 access 解析历史，不制造重复历史行。
- 报表导入文件类型继续由文件名和内容自动识别，不恢复手动文件类型选择。
- 输出物必须包含代码、测试和文档。
