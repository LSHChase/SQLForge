# Backend Large Class Split Strategy 2026-05-28

## Scope

Task `USER-CN-BACKEND-LARGE-CLASS-SPLIT-20260528` splits backend `src/main/java`
classes whose non-blank, non-comment class body exceeds 200 code lines. The line
limit is an acceptance gate, not the design driver.

## Design Rules

- Keep the original public application service, controller-facing DTO contract,
  MyBatis repository interface, database mapping, exception type, status value,
  audit field, and log semantics unchanged unless a separate product task changes
  them.
- Split by one reason to change. A new class must own a coherent role such as
  parsing, AST traversal, candidate selection, SQL rewrite, validation evidence,
  response assembly, persistence mapping, route resolution, or artifact IO.
- Keep high-level services as facades when callers already depend on them. A
  facade may coordinate collaborators, but it should not contain low-level
  parsing, formatting, storage, or DTO construction details.
- Prefer package-private collaborators for internal behavior. Do not widen
  visibility simply to make extraction easier.
- Extract immutable value/result objects only when they carry a stable concept.
  Do not create generic `Utils`, `Helper`, or numbered fragment classes.
- Preserve transaction, concurrency, request-context, tenant-check, and retry
  boundaries in the original application flow unless the extracted role owns that
  boundary explicitly.
- Move tests with behavior, not with lines. Existing tests should continue to pass;
  add focused tests when an extracted collaborator exposes a new seam.

## Functional Groups

### SQL Optimization

- `*ApplicationService`: request orchestration, tenant/context checks, repository
  calls, workflow state transitions.
- Calcite/profile parsing: parser setup, AST profile collection, visitor logic.
- MV candidate generators: candidate discovery, blocking reason policy,
  output/coverage analysis, rewrite SQL generation, evidence assembly.
- Rewrite domain: parser/IR/RA builders, conflict/cost/semantic analyzers.
- Validation SQL: input normalization, validation case generation, dialect output.
- Persistence repositories: record-to-domain mapping, mapper calls, JSON payload
  conversion.

### Query Execution

- Execution facade: request validation, engine selection, success/failure response.
- Route resolution: datasource and Hetu mode selection, route failure diagnosis.
- Runtime rewrite: binding lookup, rewrite decision, fallback policy.
- Cache runtime: key/digest construction, cache backend calls, governance metadata.
- History writer: request/response context snapshot and async persistence.

### Governance

- History service: query filters, trace detail aggregation, projection assembly,
  execution result assembly.
- Audit/alert services: rule evaluation, event emission, persistence protection.
- Datasource/metadata services: tenant validation, driver/catalog access,
  VO/domain mapping.

### Benchmark Engine

- Artifact storage: path resolution, object storage IO, manifest verification.
- Isolated execution: process preparation, execution lifecycle, result collection.
- Task/test-set services: command orchestration, domain model assembly, queueing.
- Report/comparison services: metric grouping, recommendation comparison,
  export/rendering.

### Shared

- SQL text utilities: lexical scanning and token rewriting.
- JDBC agent: bind capture, rewrite decision integration, execution observation.
- Security utilities: crypto setup, key derivation, encode/decode operations.

## Batch Order

1. Add and enforce repeatable class-code-line measurement.
2. Split low-coupling shared/text utility and small application assemblers as
   safety samples.
3. Split Calcite/MV recommendation classes because they are on the active
   recommendation goal path.
4. Split query-execution runtime facade and cache/rewrite collaborators.
5. Split governance and benchmark application services.
6. Split DTO/domain/record classes only after service behavior is stable; use
   nested value groups or dedicated facet objects only when contracts remain
   unchanged.

## Verification Gates

- `python3 scripts/check_java_class_loc.py --limit 200 --fail`
- `git diff --check`
- Focused module tests for each extracted class group.
- Existing API/controller/mapper tests for any class that touches VO, DTO,
  repository, persistence record, or JSON payload mapping.
- Calcite/MV focused tests for SQL rewrite, coverage, blocking reasons, and
  validation SQL whenever MV classes are touched.
