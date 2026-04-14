# Deployment Foundation

## Purpose

This spec defines the baseline deployment path for the split frontend/backend stack.

## Requirements

1. Frontend and backend must have independent container build files.
2. The container path must work for both `amd64` and `arm64` through multi-arch base images.
3. A root compose file must be able to start the split architecture.
4. The deployment path must preserve UTF-8 and Unix/LF conventions already enforced in the repository.

## Initial Delivery

- `frontend/Dockerfile`
- `backend/Dockerfile`
- `docker-compose.yml`
