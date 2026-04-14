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

## Supported Engine Codes

- `mysql`
- `trino`
- `presto`
- `clickhouse`
- `mrs-hetu`
- `kyligence`

## Current Limits

- validation is still offline for credentials and semantics
- probe performs a real TCP connectivity check but does not execute JDBC authentication yet
- credentials are not returned by API responses
- raw passwords are not persisted to disk
- current persistence stores metadata only and is not a secret vault

## Initial API Shape

- `GET /api/v1/system/engines`
- `GET /api/v1/connections`
- `POST /api/v1/connections/validate`
- `POST /api/v1/connections`
- `POST /api/v1/connections/probe`
