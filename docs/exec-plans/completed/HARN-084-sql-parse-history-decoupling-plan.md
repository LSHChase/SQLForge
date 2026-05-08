# HARN-084 SQL Parse History Decoupling Plan

- Task: `HARN-084`
- Task class: `standard`
- Depends on: `HARN-083`
- Execution mode: `single-agent`

## Requirement

Split SQL execution history and SQL parse records into separate backend ownership, API clients, page data sources, and persistence models. `query_history` remains SQL execution history; `sql_parse_history` becomes the SQL optimization service's parse-record store. The only allowed cross-line interaction in this task is a service skeleton that reads slow SQL execution history candidates and writes independent parse records.

## Scope

1. Add `sql_parse_history` persistence owned by `sql-optimization`: schema, migration, entity/domain/repository, MyBatis XML mapper, application service, and focused schema mapping tests.
2. Replace parse-flow writes to governance `query_history` with writes to `sql_parse_history` for structure parse, combined parse, parse batch, and report batch paths.
3. Add parse-history HTTP APIs under `/api/sql-optimization/parse-history` for page, detail, and export, with tenant/request validation aligned to current service patterns.
4. Add the end-of-day slow SQL parse skeleton: `SlowSqlExecutionHistorySource` port, candidate DTOs, idempotent batch key handling, and tests without real cron scheduling.
5. Split frontend API usage so SQL history uses execution-history clients and parse record pages use parse-history clients, with static contract checks preventing parse pages from calling governance query-history.
6. Update architecture/interface/persistence docs to state the new boundary and residual migration choice.

## Out Of Scope

- No online migration of existing `query_history` rows with `SQL_PARSE`.
- No real cron scheduler or production slow-SQL connector.
- No parser algorithm rewrite and no SQL execution semantics change.
- No delivery tag or release write-back.

## Validation Focus

- Parse entry points persist parse evidence in `sql_parse_history` and no longer call governance parse-history trace writes.
- SQL execution history list/detail/export remain execution-history only.
- Parse-history list/detail/export read only `sql_parse_history`.
- End-of-day slow SQL parse service is idempotent for the same batch key and candidate fingerprint.
- Frontend contract scripts prove `SqlHistoryView` and `ParseRecordView` use separate clients and routes.
- `sql/init-schema.sql` and migration DDL match mapper/entity expectations.

## Implementation Summary

- Added `sql_parse_history` schema, migration, domain/repository/mapper/entity, application service, controller, page/detail/export VOs, and schema mapping tests under `sql-optimization`.
- Repointed structure parse, combined parse, parse batch, report batch, and end-of-day slow SQL skeleton writes to `sql_parse_history`.
- Removed the governance internal parse-history write surface, legacy traceability service, and shared parse-history write DTOs so governance `query_history` no longer owns parse records.
- Split frontend clients and pages so SQL history remains on governance query-history while parse records and acceleration analytics use SQL optimization parse-history.
- Updated architecture, interface, data model, product, schema, and frontend contract documents/scripts to record the new boundary.

## Validation Evidence

- `mvn -pl sqlforge-shared test`
- `mvn -pl governance test`
- `mvn -pl sql-optimization test`
- `node scripts/check-history-page-contract.mjs`
- `node scripts/check-history-detail-contract.mjs`
- `node scripts/check-sql-ui-contract.mjs`
- `node scripts/check-dev-frontend.mjs`
- `npm run lint`
- `npm run build`
- `npm run build:portable`
- `node scripts/check-portable-frontend.mjs`
- `python3 scripts/foreman.py validate HARN-084`

## Standard Chain

1. `python3 scripts/foreman.py preflight --task HARN-084 --task-class standard`
2. `python3 scripts/foreman.py instantiate HARN-084`
3. Implement scoped code, tests, and docs.
4. `python3 scripts/foreman.py validate HARN-084`
5. `python3 scripts/task_audit.py --check --phase pre-closeout`
6. `python3 scripts/foreman.py closeout HARN-084`
7. `python3 scripts/task_audit.py --check --phase post-closeout`
