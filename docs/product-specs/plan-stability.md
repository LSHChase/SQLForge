# Plan Stability

## Problem

Production latency can spike when Trino chooses a different execution plan for the same SQL fingerprint.

## Inputs

- SQL text
- optional historical best plan
- current degraded plan
- optional candidate plans
- optional data freshness hints

## Output

- plan diff summary
- root-cause hints
- stability decision
- recommended protection strategy

## Success Criteria

- the same plan pair always yields the same diff summary
- clear distinction between plan regression and mere workload growth
- output maps to an operator action: `protect-best-plan`, `refresh-stats`, `observe`, or `investigate`

## Initial API Shape

- `POST /api/v1/workflows/plan-stability`

## Current Delivery

- Java Spring Boot backend exposes plan diff, root-cause hints, decision, and operator actions.
- The workflow keeps SQL fingerprint assessment in-band so operators can trace plan regression against the same SQL context.
