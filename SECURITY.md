# Security

## Current Boundaries

- the scaffold does not execute arbitrary SQL
- there is no outbound Trino/Hudi/K8s integration yet
- all persistence is in-memory only

## Security Expectations For Future Integrations

- read-only access for metadata collection by default
- explicit separation between control plane credentials and benchmark execution credentials
- sensitive SQL text and lineage metadata must be handled as protected data
- shadow testing must remain read-only and auditable

## Required Guardrails

- validate request boundaries at transport edges
- keep tenant identity explicit in workflow inputs
- document every new external connector and its access scope in-repo
