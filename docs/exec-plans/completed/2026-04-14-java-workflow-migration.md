# 2026-04-14 Java Workflow Migration

## Goal

Port core SQLForge workflow APIs from the legacy Node prototype into the Spring Boot backend.

## Delivered

1. Added Spring Boot BI release evaluation workflow endpoint and service coverage.
2. Added Spring Boot capacity planning workflow endpoint with deterministic worker-gap and resource-estimate output.
3. Added Spring Boot plan stability workflow endpoint with plan diff, root-cause hints, and operator actions.
4. Added shared SQL assessment support so Java workflow outputs stay fingerprint-stable and operator-readable.
5. Updated README, product specs, and tests to reflect the Java workflow mainline.

## Checkpoint

- tag: `checkpoint/2026-04-14-java-workflow-migration`
