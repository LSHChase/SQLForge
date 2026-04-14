# Design

SQLForge is an operator-facing control plane for Trino OLAP performance engineering.

The product should feel:

- analytical rather than decorative
- operational rather than dashboard-noisy
- explicit about risk, bottlenecks, and decisions

Primary UI concepts for future phases:

- SQL fingerprint as the main investigation unit
- workflow-centric screens instead of raw module screens
- side-by-side comparison for baseline, candidate, and optimized plans
- decision-oriented outputs: approve, optimize, block, expand, rollback

Current repository state:

- Vue frontend scaffold now exists in `frontend/`
- Spring Boot backend scaffold now exists in `backend/`
- legacy Node.js prototype remains only as migration reference
