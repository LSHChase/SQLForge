# 2026-04-14 Tenant Profile Provider

## Goal

Remove duplicated hard-coded tenant resolution logic from backend services by introducing a shared tenant profile provider boundary.

## Delivered

1. Added a typed tenant profile model for workflow and assessment services.
2. Added a `TenantProfileProvider` contract and a `StaticTenantProfileProvider` implementation.
3. Refactored `SqlAssessmentService` and `BiReleaseWorkflowService` to use the shared provider boundary.
4. Preserved direct test construction paths with convenience constructors so focused unit tests stay simple.
5. Updated README, product specs, quality notes, tech debt tracking, and checkpoints to reflect the new tenancy boundary.

## Checkpoint

- tag: `checkpoint/2026-04-14-tenant-profile-provider`
