# 2026-04-13 Engine Profiles And Arm Delivery

## Goal

Close the remaining baseline delivery gaps by making engine connection profiles explicit and by adding container delivery files that support `arm64` and `amd64`.

## Delivered

1. Expanded engine metadata with default ports, transport hints, JDBC scheme hints, and profile notes.
2. Updated the frontend to apply engine-specific defaults while editing connection definitions.
3. Added frontend and backend Dockerfiles for split deployment.
4. Added a root compose file for the split architecture.
5. Added docs and tests for richer engine profiles and ARM-ready delivery.

## Checkpoint

- tag: `checkpoint/2026-04-13-engine-profiles-and-arm-delivery`
