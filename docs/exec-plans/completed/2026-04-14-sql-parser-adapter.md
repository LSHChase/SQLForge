# 2026-04-14 SQL Parser Adapter

## Goal

Introduce a typed SQL structure contract and parser adapter boundary so the current heuristic parser can be replaced later without rewriting the SQL stress-preparation workflow chain.

## Delivered

1. Added typed SQL structure models for parsed query shape and join conditions.
2. Added a `SqlParserAdapter` boundary with a `HeuristicSqlParserAdapter` implementation.
3. Refactored `SqlIntentAnalysisService` to consume the typed AST contract instead of owning raw regex parsing details directly.
4. Updated downstream tests to use the new parser abstraction while preserving current behavior.
5. Updated README, product specs, quality notes, tech debt tracking, and checkpoints to reflect the new parser swap-in path.

## Checkpoint

- tag: `checkpoint/2026-04-14-sql-parser-adapter`
