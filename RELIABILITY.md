# Reliability

## Current Expectations

- `npm start` boots cleanly
- `npm test` is the minimum merge gate
- HTTP handlers should fail with explicit JSON errors
- workflows should remain deterministic for identical inputs

## Future Reliability Targets

- reproducible benchmark plans for the same SQL fingerprint
- bounded startup time
- traceable workflow decisions
- shadow testing behind explicit feature flags
- stable contracts around external connector failures

## Reliability Rule

If a runtime assumption matters to operators, encode it in:

- tests
- logs
- config
- docs

Do not leave critical behavior implicit in prompts only.
