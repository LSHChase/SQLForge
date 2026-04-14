# 2026-04-13 Connection Probe Foundation

## Goal

Deliver the next complete feature family for engine connections by adding real connectivity probing and engine-specific JDBC URL generation.

## Delivered

1. Added engine-specific JDBC URL generation on the backend.
2. Added a real TCP connectivity probe API for connection definitions.
3. Exposed probe diagnostics to the frontend.
4. Added backend tests for URL generation and reachable/unreachable socket cases.
5. Updated product docs and README for the probe workflow.

## Checkpoint

- tag: `checkpoint/2026-04-13-connection-probe-foundation`

## Current Limits

- the probe verifies TCP reachability only
- it does not perform JDBC authentication or engine-specific handshake yet
