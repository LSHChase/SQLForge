# SQLForge Architecture Map

SQLForge now treats the split Vue + Spring Boot stack as the primary production architecture.

## Primary Structure

### Frontend

- `frontend/`: Vue application built with JavaScript and CSS
- `frontend/src/`: UI composition, API calls, and page-level state

### Backend

- `backend/`: Java 8 Spring Boot application
- `backend/src/main/java/`: HTTP API, service layer, connector contracts
- `backend/src/main/resources/`: runtime configuration

### Repository Knowledge

- `docs/`: architecture, specs, plans, and conventions
- `src/`: legacy Node.js prototype kept only as reference material
- `test/`: legacy prototype tests

## Target Runtime Topology

- frontend is deployed as a standalone static web app
- backend is deployed as a standalone Spring Boot service
- frontend communicates with backend through HTTP APIs
- backend owns data-engine connection management

## Connector Direction

The backend connector layer is expected to support:

- MySQL
- Trino
- Presto
- ClickHouse
- MRS-Hetu
- Kyligence

## Engineering Constraints

- front-end and back-end must remain independently buildable
- source files use UTF-8 and LF line endings
- deployment path must support both `amd64` and `arm64`
- durable design decisions belong in `docs/`
