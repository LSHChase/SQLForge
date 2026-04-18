# Access Control Spec

## Phase 0 Placeholder

- `governance-service` currently performs tenant-scoped access checks through `TenantAccessLogic`.
- Phase 0 only validates that the current tenant identifier is present and leaves strict datasource authorization behind a documented TODO.

## Phase 1 Target

- Implement role-based access control with datasource permission matrix.
- Add data-scope constraints so each tenant can only access bound datasources and governed resources.
- Replace placeholder token/user identity handling with centralized identity and authorization services.
