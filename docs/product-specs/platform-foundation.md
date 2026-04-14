# Platform Foundation

## Scope

This spec defines the mandatory delivery baseline for the SQLForge platform foundation.

## Functional Requirements

1. The frontend must be implemented with Vue, JavaScript, and CSS.
2. The backend must be implemented with Java 8 and Spring Boot.
3. The system must use a front-end/back-end separation model.
4. The backend must expose an API that allows the frontend to query supported engine metadata.
5. The backend connector capability must cover MySQL, Trino, Presto, ClickHouse, MRS-Hetu, and Kyligence.

## Non-Functional Requirements

1. Repository text files use UTF-8.
2. Repository line endings use Unix/LF.
3. Build and deployment paths must remain compatible with `amd64` and `arm64`.
4. The frontend and backend must be independently deployable.

## Initial Acceptance

- `frontend/` contains a runnable Vue scaffold.
- `backend/` contains a runnable Spring Boot scaffold.
- backend exposes health and supported-engine endpoints.
