## Candidate Execution Plan: HARN-057

- Task: `HARN-057`
- Title: 修复解析历史默认查询与刷新
- Scope: 修复解析历史页默认查询条件非空和刷新失败问题，分离页面筛选条件与请求上下文租户，补齐批量历史刷新、测试与文档。

## Execution Steps

1. 通过 `python3 scripts/foreman.py preflight` 和 `python3 scripts/foreman.py instantiate HARN-057` 建立任务上下文。
2. 核对 `ParseRecordView`、`runtimeGateApi` 与后端 query-history controller 的默认筛选、租户上下文和刷新行为。
3. 将解析历史页可见查询条件默认值统一为空，包括租户筛选和排序筛选。
4. 分离页面筛选租户与受保护接口请求上下文租户，确保空筛选条件下刷新仍携带有效开发请求上下文。
5. 保持批量解析历史和报表导入历史随刷新加载，并避免空租户导致列表接口失败。
6. 更新前端表单治理文档，记录解析历史默认空筛选与上下文租户分离规则。
7. 执行 `node scripts/check-history-page-contract.mjs`、`npm run build`、`python3 scripts/foreman.py validate HARN-057`、`python3 scripts/task_audit.py --check --phase pre-closeout`。
8. 通过 `python3 scripts/foreman.py closeout HARN-057` 收口并运行 post-closeout audit。

## Constraints

- 不新增浏览器本地历史事实源。
- 不弱化后端 tenant 隔离；空筛选仅表示不在 query param 中限定租户，受保护请求仍携带当前开发上下文租户。
- 不把排序默认值作为页面默认筛选条件；后端保持既有默认排序兜底。
- 不改写 HARN-056 已完成的批量解析持久化、统计和文件识别主体语义。

## Validation

- `npm run build`
- `node scripts/check-history-page-contract.mjs`
- `python3 scripts/foreman.py validate HARN-057`
- `python3 scripts/task_audit.py --check --phase pre-closeout`
