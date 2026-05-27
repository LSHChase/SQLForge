# Apache Calcite Migration Report

## Scope

Task: active parser replacement task

This migration replaces the previous SQL parser implementation with Apache Calcite as the authoritative parser for `sql-optimization` structure analysis, rewrite analysis, batch parsing, report parsing, configuration, tests, frontend parser-mode contracts, dependency management, and current architecture documentation.

## Replacement Inventory

| Area | Previous surface | Calcite replacement |
|:---|:---|:---|
| Build dependency | Direct legacy parser dependency in `sql-optimization/pom.xml` | Removed; `org.apache.calcite:calcite-core` remains the parser dependency |
| Transitive dependency | Governance PageHelper starter pulled the legacy parser transitively | Removed PageHelper dependency and MyBatis plugin; governance pagination already uses explicit mapper `LIMIT` clauses |
| Parser mode contract | Request modes exposed both legacy and Calcite values | `SqlParserMode` now accepts `APACHE_CALCITE` and `APACHE_CALCITE_WITH_PLAN` only |
| Default parser config | `sql-optimization.parser.strategy` defaulted to legacy mode | Default is now `APACHE_CALCITE` |
| Structure parser | Legacy AST adapter collected tables, predicates, projections, functions, joins, CTEs, subqueries, and rewrite signals | `ApacheCalciteParserAdapter` uses `SqlParser`, `SqlNode`, `SqlSelect`, `SqlWith`, `SqlJoin`, `SqlBasicCall`, and `SqlNodeList` |
| Rewrite rules | AST mutation backed count-literal, duplicate predicate, duplicate group/order key rules | Calcite `SqlNode` rewrite path applies equivalent rules and records the same rule ids |
| Advanced structure profile | Legacy-specific table/projection/predicate/join/profile recording | Calcite collector now records advanced profile as `AVAILABLE` with tables, projections, predicates, joins, aggregations, group/order keys, limits, CTEs, subqueries, and function signals |
| QBDAG | Calcite was already the preferred QBDAG parser but could fall back to legacy profile language in docs | QBDAG docs and code now describe Calcite-only parser evidence |
| Parser stack fusion | Dual-stack report language | Calcite single-stack report language; metadata tags and rewrite constraints are derived from Calcite profile/text evidence |
| Production gate metadata | Legacy metadata adapter configuration | `calcite-metadata` adapter and `SQL_OPTIMIZATION_REWRITE_PRODUCTION_CALCITE_METADATA_ENABLED` |
| Frontend | Parser-mode options included legacy modes | Parse batch UI now exposes only Apache Calcite modes |
| SQL schema / migrations | Parser mode enums and examples included legacy values | Parser mode defaults and documented column contracts use Apache Calcite values |
| Documentation | Current docs described dual parser ownership | Current docs describe Calcite-only ownership; historical references are neutralized as legacy parser notes |

## API Mapping

| Legacy parser concept | Apache Calcite API |
|:---|:---|
| `Statement` / parsed select statement | `SqlNode`, usually `SqlSelect`, `SqlWith`, or `SqlOrderBy` |
| Select body | `SqlSelect` and set-operation `SqlCall` nodes |
| Projection item | `SqlNode` in `SqlSelect.getSelectList()` |
| Expression | `SqlNode`, `SqlBasicCall`, `SqlIdentifier`, `SqlLiteral` |
| Function expression | `SqlBasicCall` with `getOperator().getName()` |
| Binary predicate | `SqlBasicCall` with `SqlKind` comparison / logical kinds |
| Table reference | `SqlIdentifier` or `AS` `SqlBasicCall` under `SqlSelect.getFrom()` |
| Join | `SqlJoin` |
| CTE item | `SqlWithItem` |
| Group/order list | `SqlNodeList` |
| Limit/offset | `SqlSelect.getFetch()`, `SqlSelect.getOffset()`, `SqlOrderBy.fetch`, `SqlOrderBy.offset` |
| AST rewrite | Mutable Calcite `SqlNode` traversal plus `SqlStdOperatorTable` node construction |
| Relational planning extension point | Calcite `RelNode`, `RelToSqlConverter`, `HepPlanner`, `VolcanoPlanner` adapters gated by existing production capability checks |

## Gap List

| Gap | Impact | Current handling | Recommended path |
|:---|:---|:---|:---|
| Exact original comment/format AST preservation | Calcite SQL AST is not a formatting-preserving source map. | The pipeline uses normalized SQL plus text evidence for BI tags and does not expose mutable parser AST objects across layers. | Add an explicit source-map sidecar only if a future feature needs character-level AST editing. |
| Full engine-specific SQL generation from real `RelNode` | Existing recommendation SQL remains a static template boundary, not a proven engine executable. | Production capability analyzer can exercise Calcite `RelNode` and `RelToSqlConverter` when enabled, while default recommendations stay gated. | Build schema/type catalog and Hetu dialect validation before enabling production SQL generation. |
| External proof / runtime validation | Static equivalence and cost reports do not prove result-set equality or real performance. | Reports remain marked as static, `autoApplyAllowed=false`, and production gates require validation evidence. | Add sample result diff, Hetu EXPLAIN/statistics, and optional SMT proof integration in separate governed tasks. |

## Verification Evidence

- `java -version`: OpenJDK `1.8.0_112`, matching the required JDK 8u112 runtime.
- `mvn clean test`: passed across parent, shared, query-execution, sql-optimization, benchmark-engine, and governance.
- Maven dependency tree check for the previous parser group: passed with no matching dependency entries.
- `npm run lint`: passed.
- `npm run build`: passed.
- `npm run build:portable`: passed.
- `node scripts/check-parse-workbench-contract.mjs`: passed.
- `node scripts/check-batch-import-contract.mjs`: passed.
- `node scripts/lint-repository-knowledge.js`: passed.
- `node scripts/check-frontend-backend-separation.js`: passed.
- Exact lowercase parser-token grep over Java/XML/Markdown/YAML/JSON/HTML/JS/Gradle files returned no matches.
- Broad old-parser token scan outside `target/` and `node_modules/` returns only the immutable task ledger id in `tasks.md`.
