# 2026-04-14 SQL Intent Analysis Foundation

## Goal

Add a Java-backend SQL intent analysis module for stress-testing preparation that performs pure structural analysis on input SQL without executing queries.

## Delivered

1. Added a Spring Boot SQL intent analysis API that accepts batch SQL input.
2. Added deterministic structural profiling with fingerprint, statement shape, and join/predicate summaries.
3. Added structure-only intent tags and pressure-oriented load classification for stress-testing preparation.
4. Added a minimal frontend panel for manual SQL structure inspection.
5. Updated tests, specs, and README to reflect the Java mainline capability.

## Checkpoint

- tag: `checkpoint/2026-04-14-sql-intent-analysis-foundation`
