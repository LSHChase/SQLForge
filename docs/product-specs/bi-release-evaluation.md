# BI Release Evaluation

## Problem

BI teams need to know whether a new SQL can be released without blowing up Trino latency or cluster cost.

## Inputs

- tenant ID
- SQL text
- target SLA
- target concurrency
- data profile

## Output

- risk level
- whether benchmark is required
- benchmark plan
- benchmark analysis
- final decision: `approved`, `needs-optimization`, or `blocked`

## Success Criteria

- same SQL should produce a stable fingerprint
- decision should be traceable through report fields
- high-risk queries should not bypass benchmarking
