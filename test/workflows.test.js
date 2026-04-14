import test from 'node:test';
import assert from 'node:assert/strict';
import { createContainer } from '../src/bootstrap/create-container.js';
import { getConfig } from '../src/config/index.js';

test('BI release workflow produces benchmark plan and report', () => {
  const container = createContainer(getConfig());
  const result = container.evaluateBiRelease.execute({
    tenantId: 'tenant-a',
    sql: 'select user_id, sum(amount) from lake.orders where ds >= current_date - interval \'7\' day group by 1',
    slaMs: 5000,
    targetConcurrency: 20,
    dataProfile: {
      fullRows: 1800000000,
      sampleRows: 36000000,
      skew: 3.2,
      hotDataGb: 280
    }
  });

  assert.equal(result.workflow, 'bi-release-evaluation');
  assert.ok(result.benchmarkPlan.samplingPlan.sampleRatio >= 0.01);
  assert.ok(result.report.summary.p99Ms > 0);
  assert.ok(result.report.actions.length > 0);
  assert.equal(result.benchmarkAnalysis.baselineComparison.hasBaseline, false);
  assert.ok(['approved', 'needs-optimization', 'blocked'].includes(result.decision));
});

test('BI release workflow compares against previous baseline', () => {
  const container = createContainer(getConfig());
  const input = {
    tenantId: 'tenant-a',
    sql: 'select user_id, sum(amount) from lake.orders where ds >= current_date - interval \'7\' day group by 1',
    slaMs: 5000,
    targetConcurrency: 10,
    dataProfile: {
      fullRows: 1800000000,
      sampleRows: 36000000,
      skew: 3.2,
      hotDataGb: 280
    },
    metrics: [
      { run: 1, latencyMs: 1000, gcPauseMs: 10 },
      { run: 2, latencyMs: 1010, gcPauseMs: 10 },
      { run: 3, latencyMs: 1020, gcPauseMs: 10 },
      { run: 4, latencyMs: 1100, gcPauseMs: 10 },
      { run: 5, latencyMs: 1120, gcPauseMs: 10 },
      { run: 6, latencyMs: 1130, gcPauseMs: 10 }
    ]
  };

  container.evaluateBiRelease.execute(input);

  const regressed = container.evaluateBiRelease.execute({
    ...input,
    metrics: [
      { run: 1, latencyMs: 1000, gcPauseMs: 10 },
      { run: 2, latencyMs: 1010, gcPauseMs: 10 },
      { run: 3, latencyMs: 1020, gcPauseMs: 10 },
      { run: 4, latencyMs: 1800, gcPauseMs: 10 },
      { run: 5, latencyMs: 1900, gcPauseMs: 10 },
      { run: 6, latencyMs: 2100, gcPauseMs: 10 }
    ]
  });

  assert.equal(regressed.benchmarkAnalysis.baselineComparison.hasBaseline, true);
  assert.equal(regressed.benchmarkAnalysis.baselineComparison.status, 'regressed');
  assert.ok(regressed.report.actions.some((item) => item.actionType === 'baseline-guard'));
});

test('capacity workflow computes worker gap and resource report', () => {
  const container = createContainer(getConfig());
  const result = container.planPromotionCapacity.execute({
    trafficGrowthFactor: 3,
    baselineQps: 120,
    avgServiceTimeSec: 2.8,
    currentWorkers: 25,
    targetP99Ms: 5000,
    targetConcurrency: 240,
    hotDataGb: 2800,
    queryMix: [
      { name: 'dashboard', cpuTimeSec: 3.2, arrivalRate: 720, memoryPeakGb: 2.5 },
      { name: 'ad-hoc', cpuTimeSec: 8.4, arrivalRate: 180, memoryPeakGb: 6.0 }
    ]
  });

  assert.equal(result.workflow, 'promotion-capacity-planning');
  assert.ok(result.capacityPlan.workerPlan.requiredWorkers > result.capacityPlan.workerPlan.currentWorkers);
  assert.ok(result.estimatedResources.cpuCores > 0);
  assert.ok(result.report.workerPlan.additionalWorkers >= 0);
});

test('plan stability workflow identifies stale stats and recommends action', () => {
  const container = createContainer(getConfig());
  const result = container.analyzePlanStability.execute({
    tenantId: 'tenant-a',
    sql: 'select o.user_id, sum(o.amount) from lake.orders o join lake.users u on o.user_id = u.id group by 1',
    historicalBestPlan: {
      planHash: 'plan-a',
      joinOrder: ['orders', 'users'],
      distribution: 'broadcast',
      statsAgeHours: 2,
      latencyMs: 2200
    },
    currentPlan: {
      planHash: 'plan-b',
      joinOrder: ['users', 'orders'],
      distribution: 'shuffle',
      statsAgeHours: 48,
      latencyMs: 3600
    },
    candidatePlans: [
      {
        planHash: 'plan-a',
        joinOrder: ['orders', 'users'],
        distribution: 'broadcast',
        statsAgeHours: 2,
        latencyMs: 2200
      }
    ]
  });

  assert.equal(result.workflow, 'plan-stability-analysis');
  assert.equal(result.stabilityAnalysis.decision, 'refresh-stats');
  assert.ok(result.stabilityAnalysis.rootCauses.includes('stale-stats'));
  assert.equal(result.report.reportType, 'plan-stability');
});
