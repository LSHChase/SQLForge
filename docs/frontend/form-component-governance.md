# Frontend Form Component Governance

本文件记录 HARN-045 的页面表单组件治理基线。目标是在不改变后端 API contract、字段名、提交格式和权限边界的前提下，把可确认语义的输入框替换为更合适的 Element Plus 组件。

## Principles

- 自由文本继续使用 `el-input`，例如名称、编码、路径模板、JSONPath、trace/task/report id。
- 单点业务日字段使用 `el-date-picker type="date"`，提交格式保持 `YYYY-MM-DD`。
- 起止日期筛选必须优先合并为 `el-date-picker type="daterange"`，提交前拆回既有 `*Start` / `*End` 字段，格式保持 `YYYY-MM-DD`。
- 起止日期时间筛选必须优先合并为 `el-date-picker type="datetimerange"`，提交前拆回既有 `*Start` / `*End` 字段，格式保持 `YYYY-MM-DDTHH:mm:ss`。
- 受控候选字段使用 `el-select`；候选值来自现有接口、当前页面已加载数据或仓库已验证的静态枚举。
- 租户和数据源字段在候选值不可见或加载失败时使用 `filterable allow-create`，保留原有手动值输入能力，避免破坏权限不可见或环境缺数据场景。
- 布尔字段使用 `el-switch`。
- 数值字段使用 `el-input-number`，提交前仍按现有逻辑转换为 `Number(...)`。
- 密钥、密码和 token 类临时输入使用密码输入组件；原始凭证仍不得回显。

## Management Page Boundary

- 既有 HARN-045 / HARN-046 表单组件治理继续有效；新增规则只扩展管理后台页面默认组件边界，不改变已确认字段映射和提交格式。
- 新增 CRUD、配置、历史、任务、审计、数据源、权限、报表接口等管理类页面，默认复用 `SearchForm.vue`、`EditDialog.vue`、`useDict` 字典翻译、统一分页绑定和 Element Plus 语义组件。
- 搜索表单、编辑弹窗和表格列应优先通过字段 / 列定义数组驱动，页面层只保留状态编排、接口调用和事件处理。
- 表单字段候选值必须来自后端接口、当前页面已授权加载数据或仓库已验证静态枚举；不得用前端临时枚举绕过权限可见性、候选值过滤或后端契约。
- 权限可见性只允许作为体验层控制。按钮展示、禁用和隐藏不得替代后端身份鉴别、权限判断、业务校验和审计。
- 提交格式必须继续遵守后端 API contract；日期、时间、区间、数值、布尔、字典 code 和敏感字段不得因组件替换改变 payload 语义。
- 新增或重构页面必须同步 i18n key，并在触发表单治理时运行 `npm run test:form-governance` 或对应静态契约检查。

## HARN-045 Field Mapping

| Page | Field category | Component | Source / format |
|:---|:---|:---|:---|
| `SystemView` | 顶部租户、弹窗租户 | `el-select filterable allow-create` | 当前租户、tenant-config、页面已加载资源中的 `tenantId` |
| `SystemView` | 报表接口数据源、Dispatch 目标数据源 | `el-select filterable allow-create` | `getGovernanceDatasources(tenantId)` 已加载结果 |
| `SystemView` | 连接模式、stage、authMode、sourceType、HTTP method、dispatchType、targetEngine、ackMode、retryStrategy | `el-select` | `src/views/common/formComponentGovernance.js` 的受控枚举，保留当前未知值 |
| `SystemView` | timeout、pullWindowSeconds、maxBatchSize | `el-input-number` | 现有提交字段，提交前保持 `Number(...)` |
| `SystemView` | enabled、readonly、tlsEnabled、verifyPeer、bypassOnUnavailable | `el-switch` | 现有布尔字段 |
| `SystemView` | credentialSecret | password `el-input` | 仅提交临时 secret，不回显原始凭证 |
| `ParseRecordView` | tenantId | `el-select filterable allow-create clearable` | 页面查询条件默认空；空值表示不追加 `tenantId` 查询参数，由受保护请求上下文租户承载隔离边界 |
| `ParseRecordView` | datasourceCode | `el-select filterable allow-create` | `getGovernanceDatasources(tenantId)`，失败时保留手动值 |
| `ParseRecordView` | bizDate | `el-date-picker type="date"` | 单个业务日，现有 API 只有 `bizDate` 单字段 |
| `ParseRecordView` | queryDateStart、queryDateEnd | `el-date-picker type="daterange"` | UI 选择日期区间，提交前拆回 `YYYY-MM-DD` 起止字段 |
| `ParseRecordView` | submittedStart、submittedEnd | `el-date-picker type="datetimerange"` | UI 选择提交时间区间，提交前拆回 `YYYY-MM-DDTHH:mm:ss` 起止字段 |

## Validation

- `npm run test:form-governance` 检查关键页面是否已使用语义组件，并阻止 HARN-045 覆盖字段退回普通输入框。
- `npm run lint` 覆盖 Vue / JS 语法与风格。
- `npm run build` 覆盖 Element Plus 组件、Vite 打包和前端集成。

## Conservative Boundary

HARN-046 修正了 HARN-045 中把 `queryDateStart/queryDateEnd` 和 `submittedStart/submittedEnd` 拆成两个单点日期组件的问题。后续日期治理默认判断顺序为：已有 start/end 字段的筛选项先做区间；只有单字段且业务语义明确为某一天时才保留单点日期。

HARN-045 / HARN-046 未改写无法从代码或文档确认语义的自由文本字段，例如 SQL 文本、报表编码、路径模板、JSONPath、schemaName、traceId、taskId 和 reportId。后续若需要把这些字段升级为搜索选择或关联资源组件，必须先确认候选值权威来源和权限过滤契约。

## HARN-057 Parse History Defaults

解析历史页的可见查询条件默认值必须保持为空，包括 `tenantId`、`sortBy` 和 `sortOrder`。页面仍可在空筛选状态下刷新列表，但前端必须把“查询条件租户”和“受保护请求上下文租户”分离：只有用户显式选择租户时才把 `tenantId` 放入 query param；未选择时由当前 route 上下文或开发默认上下文租户提供请求头，避免空租户导致受保护接口刷新失败。

## HARN-058 Parse History And Report Import Detail

解析历史页不只展示 `query_history` SQL 级历史，还必须展示批量解析和报表导入批次历史。报表导入历史在解析历史页内提供详情抽屉，抽屉中至少展示报表级统计、SQL 级解析状态、`parseTaskId`、结构解析状态、access 连接状态、问题场景、逻辑对象命中和解析出的 SQL 文本。

SQL 解析页在拿到 `historyId` 后提供“查看解析历史”入口，跳转到解析历史页并通过 `historyId` 直接打开详情弹窗。该跳转不得改变 HARN-057 的默认空查询条件规则。

## HARN-063 Batch Parse Result Detail Visibility

批量解析页的当前批次工作台必须让失败记录也能打开解析详情。普通批量解析的失败记录、结果弹窗中的 SQL 记录，以及报表导入的报表分组、失败 SQL 和 SQL 级记录都必须保留可点击详情入口。

详情弹窗沿用现有接口字段，不新增权限或本地持久化模型。普通批量解析详情至少展示记录标识、报表标识、状态、`parseTaskId`、结构解析状态、Access 状态、失败原因、问题场景、逻辑对象和 SQL 文本；报表导入 SQL 详情还必须展示 `sourceFileLine`、`sqlColumnName` 与 `sqlOrdinalInReport`，用于定位导入宽表中的失败列。报表导入结构解析失败时，`failureReason` 可能携带 `line`、`col`、`token` 和 `near` 片段，页面必须完整展示，不得只保留 `STRUCTURE_PARSE_INVALID` 粗粒度状态。

## HARN-066 Report Batch Statistics And Parse Diagnostics

报表导入批量解析在用户选择批次后必须展示解析统计，解析历史页的报表导入批次详情也必须展示同一口径的解析统计。统计视图必须拆分为多个标签页，至少覆盖问题场景、重要程度、报表视角、SQL 清单、优先级视角和逻辑对象视角，避免把多维统计堆在单个列表中。

单条 SQL 解析页必须在 SQL、数据源、绑定模式或注释上下文变化后清理旧解析结果和旧错误信息。异步解析返回时只能写回与当前输入快照一致的结果，避免一次失败或慢返回影响后续 SQL 的解析展示。

单条 SQL 和解析历史中的失败问题卡片应展示后端返回的 `failureReason`、`failureLine`、`failureColumn`、`failureToken` 与 `failureSnippet`。当 SQL 内含字符串或标识符外的 `--` 行尾注释时，页面无需额外预处理，直接提交原 SQL，由结构解析统一处理。

报表导入宽表单元格若以同一行 `--` 说明前缀开头，并在该行后续出现 `SELECT` / `WITH` 起点，后端会在导入解析边界提取真实 SQL 文本再解析。页面仍展示返回的 SQL 文本、源列和源行证据，避免把这种历史报表清单格式当作纯注释失败。

## HARN-070 SQL Input Output Display Contract

SQL 输入/输出 UI 统一使用共享组件承载：`SqlEditorField` 用于可编辑 SQL 输入，`SqlCodeBlock` 用于只读 SQL 展示。两者都必须提供复制按钮、SQL 语法着色、等宽字体、明确的最小高度/最大高度、溢出滚动和长 SQL 换行约束。

确认覆盖的 SQL 输入面包括 `SqlQueryView` 查询 SQL、`AccelerationView` 单条解析 SQL / 模板 SQL、`ParseBatchCenterView` 当前批次多 SQL 输入和报表宽表输入、`BenchmarkView` 压测 SQL。纯 SQL 输入启用手动格式化按钮；报表宽表、CSV 或表格原始内容只允许复制和高亮，不允许格式化按钮改写表格结构。

确认覆盖的 SQL 输出面包括 `SqlQueryView` 模板 / SQL 库 / Bound SQL 预览、`RecommendationCenterView` 源 SQL / 推荐 SQL、`ParseBatchCenterView` 批次 SQL 明细 / 失败 SQL / 报表 SQL 详情、`ParseRecordView` 报表 SQL 明细 / 原始 SQL / SQL 三态。只读输出默认通过 `SqlCodeBlock` 在展示边界自动格式化，不回写 API 响应、历史记录、查询结果或后端数据；标记为“原始 SQL / Original SQL”的历史取证面必须关闭自动格式化，展示和复制后端返回的原始文本。

JSON 证据、SQL 指纹、报表编码、统计数字和非 SQL 的原始证据块不属于本任务的 SQL 展示框范围，仍保留原有 `code-block` 或普通文本样式。后续新增 SQL-bearing 页面时，默认复用上述两个共享组件，并用 `npm run test:sql-ui-contract` 扩展静态契约检查。

## HARN-071 Large Batch Parse Display Contract

批量解析和报表导入页面必须按完整统计、有限明细预览的方式展示大批量 SQL。`ParseBatchCenterView` 的 SQL 输入继续使用 `SqlEditorField`：纯多 SQL 输入可手动格式化，CSV / 报表宽表原文不允许格式化按钮改写表格结构。直接多 SQL 输入只预览前 5 条，但提交时必须基于完整输入行构造导入载荷。

批量解析结果、失败记录、报表分组、报表 SQL 明细和报表统计标签页不得对接口返回数组做无界 `v-for` 渲染。页面明细默认预览 25 条，统计列表默认预览 50 条；当后端返回 `omittedItemCount`、`omittedFailureCount` 或 `omittedSqlStatisticCount` 时，页面必须显示剩余省略数量，并说明统计卡片仍基于完整批次。

SQL 明细、失败 SQL 和报表 SQL 详情继续使用 `SqlCodeBlock` 进行只读格式化、高亮、复制、滚动和长 SQL 换行。格式化只发生在展示边界，不回写 API 响应、历史记录、导入载荷或后端持久化内容。
