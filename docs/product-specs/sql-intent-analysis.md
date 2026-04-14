# SQL Intent Analysis

## Problem

Stress-testing preparation needs a pure structural way to classify incoming SQL before any live execution is available.

## Inputs

- one or more SQL statements
- optional statement ids or labels

## Output

- deterministic SQL fingerprint
- statement type and structural profile
- intent tags based on SQL shape only
- pressure-oriented load classification
- structural alerts that operators can review before deeper testing

## Success Criteria

- the same SQL produces the same fingerprint and intent summary
- analysis does not require database connectivity or query execution
- output is readable enough to group daily SQL samples into pressure buckets

## Initial API Shape

- `POST /api/v1/sql/intent-analysis`
- `POST /api/v1/sql/intent-analysis/daily-batch`

## Current Delivery

- Java Spring Boot backend supports batch SQL intent analysis from input SQL text.
- Output includes structural profile, intent tags, pressure-oriented load class, and structural alerts.
- Frontend provides a minimal analysis panel for manual SQL inspection.
- Daily SQL samples can be pasted as raw text and split by semicolon or blank line before structural analysis.
