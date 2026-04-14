# Production Stack Target

## Required Stack

- frontend: Vue + JavaScript + CSS
- backend: Java 8 + Spring Boot
- architecture: front-end and back-end separated

## Data Engine Scope

The backend connector layer must support:

- MySQL
- Trino
- Presto
- ClickHouse
- MRS-Hetu
- Kyligence

## Engineering Rules

- source files are UTF-8
- repository line endings are Unix/LF
- runtime targets include `amd64` and `arm64`
- connector integration should enter through backend service abstractions, not frontend code

## Delivery Rule

The production path is `frontend/` + `backend/`.

Legacy Node.js code under `src/` is reference material only.
