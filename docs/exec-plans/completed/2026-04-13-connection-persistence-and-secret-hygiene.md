# 2026-04-13 Connection Persistence And Secret Hygiene

## Goal

Deliver a complete feature family that keeps registered engine connections across backend restarts while preventing raw passwords from being written to disk.

## Delivered

1. Replaced in-memory-only storage with file-backed connection metadata persistence.
2. Kept raw passwords out of persisted records and API responses.
3. Added backend tests that verify persistence reload and password absence in stored JSON.
4. Updated frontend messaging to make the storage behavior explicit.
5. Updated product documentation to describe the new storage mode and its limits.

## Checkpoint

- tag: `checkpoint/2026-04-13-connection-persistence-and-secret-hygiene`

## Current Limits

- current storage is file-backed metadata persistence, not a secret vault
- passwords must still be re-entered for future real connectivity workflows
