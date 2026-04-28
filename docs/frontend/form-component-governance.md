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

## HARN-045 Field Mapping

| Page | Field category | Component | Source / format |
|:---|:---|:---|:---|
| `SystemView` | 顶部租户、弹窗租户 | `el-select filterable allow-create` | 当前租户、tenant-config、页面已加载资源中的 `tenantId` |
| `SystemView` | 报表接口数据源、Dispatch 目标数据源 | `el-select filterable allow-create` | `getGovernanceDatasources(tenantId)` 已加载结果 |
| `SystemView` | 连接模式、stage、authMode、sourceType、HTTP method、dispatchType、targetEngine、ackMode、retryStrategy | `el-select` | `src/views/common/formComponentGovernance.js` 的受控枚举，保留当前未知值 |
| `SystemView` | timeout、pullWindowSeconds、maxBatchSize | `el-input-number` | 现有提交字段，提交前保持 `Number(...)` |
| `SystemView` | enabled、readonly、tlsEnabled、verifyPeer、bypassOnUnavailable | `el-switch` | 现有布尔字段 |
| `SystemView` | credentialSecret | password `el-input` | 仅提交临时 secret，不回显原始凭证 |
| `ParseRecordView` | tenantId | `el-select filterable allow-create` | 默认租户、当前值、查询结果中的 `tenantId` |
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
