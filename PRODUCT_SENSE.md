# Product Sense

## Primary Users

- BI developers evaluating new SQL before release
- data platform engineers planning Trino capacity
- performance engineers diagnosing plan regressions

## Core Jobs

- decide whether a SQL can go live
- identify what kind of pressure test to run
- explain why a query or workload will fail SLA
- estimate how much infrastructure is actually needed

## Non-Goals For This Phase

- building a full Trino management console
- implementing all Trino/Hudi/K8s integrations up front
- optimizing for custom visualization before analytical correctness

## Product Principle

Every workflow should end in an action:

- approve
- block
- optimize
- expand
- degrade
- monitor
