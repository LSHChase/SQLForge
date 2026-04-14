# Engine Connections

## Purpose

This workflow lets operators register and validate data-engine connection definitions before deeper capability integration.

## Initial Scope

1. Show supported engine types.
2. Accept a connection definition from the frontend.
3. Perform offline validation on the backend.
4. Persist validated connection metadata on the backend.
5. Return the registered connection list to the frontend.
6. Probe TCP connectivity and generate engine-specific JDBC URL diagnostics.
7. Expose engine profiles with default ports and protocol hints.
8. Execute request-scoped SQL preview when a JDBC driver is available.
9. Allow saved connection deletion without persisting passwords.
10. Keep activity history for saved connection operations.
11. Expose connection-module overview metrics for operators.

## Supported Engine Codes

- `mysql`
- `trino`
- `presto`
- `clickhouse`
- `mrs-hetu`
- `kyligence`

## Current Limits

- validation is still offline for credentials and semantics
- probe performs a real TCP connectivity check and reports whether the expected JDBC driver exists on the classpath
- when a driver is available, probe attempts a real `DriverManager` connection
- credentials are not returned by API responses
- raw passwords are not persisted to disk
- current persistence stores metadata only and is not a secret vault

## Initial API Shape

- `GET /api/v1/system/engines`
- `GET /api/v1/system/driver-audit`
- `GET /api/v1/system/connection-overview`
- `GET /api/v1/connections`
- `GET /api/v1/connections/{id}/activity`
- `POST /api/v1/connections/validate`
- `POST /api/v1/connections`
- `POST /api/v1/connections/probe`
- `POST /api/v1/connections/query-preview`
- `DELETE /api/v1/connections/{id}`
