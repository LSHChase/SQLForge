# Engine Connections

## Purpose

This workflow lets operators register and validate data-engine connection definitions before deeper capability integration.

## Initial Scope

1. Show supported engine types.
2. Accept a connection definition from the frontend.
3. Perform offline validation on the backend.
4. Store validated connections in memory.
5. Return the registered connection list to the frontend.

## Supported Engine Codes

- `mysql`
- `trino`
- `presto`
- `clickhouse`
- `mrs-hetu`
- `kyligence`

## Current Limits

- validation is offline only
- no real JDBC or vendor SDK connection is attempted yet
- connection storage is in-memory only
- credentials are not persisted beyond process lifetime

## Initial API Shape

- `GET /api/v1/system/engines`
- `GET /api/v1/connections`
- `POST /api/v1/connections/validate`
- `POST /api/v1/connections`
