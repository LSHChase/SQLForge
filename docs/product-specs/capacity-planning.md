# Capacity Planning

## Problem

Platform teams need an estimate of worker gap and hardware demand before major traffic events.

## Inputs

- baseline QPS
- traffic growth factor
- average service time
- current worker count
- target P99
- query mix
- hot data size

## Output

- projected arrival model
- worker gap
- resource estimate
- structured capacity report

## Success Criteria

- output remains deterministic for the same forecast inputs
- report clearly separates current state, target demand, and additional capacity
