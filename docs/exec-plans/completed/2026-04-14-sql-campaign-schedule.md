# 2026-04-14 SQL Campaign Schedule

## Goal

Turn SQL execution manifests into timed campaign schedules that operators can follow stage by stage.

## Delivered

1. Added a Java backend campaign-schedule API derived from SQL execution manifests.
2. Added stage windows with warmup, sample, cooldown, and end-minute markers.
3. Added promotion gates, fallback actions, campaign summary, and handoff notes.
4. Added a frontend batch view for campaign schedule review.
5. Updated docs so the stress-preparation flow now reaches a staged execution schedule.

## Checkpoint

- tag: `checkpoint/2026-04-14-sql-campaign-schedule`
