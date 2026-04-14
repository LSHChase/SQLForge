function buildConcurrencyLadder(targetConcurrency) {
  const seeds = [1, 5, 10, 20, 50, 100];
  const ladder = seeds.filter((value) => value <= targetConcurrency);

  if (!ladder.includes(targetConcurrency)) {
    ladder.push(targetConcurrency);
  }

  return [...new Set(ladder)].sort((left, right) => left - right);
}

export class PressureMatrix {
  build({ assessment, targetConcurrency, dataProfile = {}, tenant }) {
    const ladder = buildConcurrencyLadder(targetConcurrency);
    const hasJoin = assessment.labels.joinType !== 'single-table';
    const skew = Number(dataProfile.skew ?? 1);
    const scanScale = assessment.labels.scanPattern === 'full-table' ? 'full-check-required' : 'sample-first';

    return {
      dataScaleMatrix: [
        {
          name: 'sample',
          purpose: '快速反馈与上线前评估'
        },
        {
          name: 'medium',
          purpose: '验证分区裁剪与统计信息稳定性'
        },
        {
          name: 'full',
          purpose: scanScale === 'full-check-required' ? '高风险 SQL 需审慎验证全量行为' : '关键 SQL 的月度全量验证'
        }
      ],
      concurrencyMatrix: ladder.map((concurrency) => ({
        concurrency,
        expectedMode: concurrency >= tenant.maxConcurrency ? 'queue-risk' : 'steady',
        queueRisk: Number((concurrency / Math.max(tenant.maxConcurrency, 1)).toFixed(2))
      })),
      distributionMatrix: [
        {
          name: 'uniform-baseline',
          purpose: '理想基线'
        },
        {
          name: 'production-like',
          purpose: '贴近生产分布'
        },
        {
          name: skew >= 10 ? 'extreme-skew' : 'skew-aware',
          purpose: '验证倾斜场景'
        }
      ],
      executionPlanMatrix: hasJoin
        ? [
            'broadcast',
            'shuffle',
            'repartition',
            'stats-aging-scan',
            'cbo-parameter-scan'
          ]
        : ['default-plan', 'predicate-pushdown-check'],
      resourceConstraintMatrix: [
        'memory-gradient',
        'cpu-gradient',
        'io-latency-injection'
      ],
      queryStructureMatrix: [
        'predicate-pushdown-validation',
        'bucket-join-validation',
        'expression-reuse-validation',
        'repeated-join-validation',
        'sort-elimination-validation',
        'projection-pushdown-validation'
      ],
      advancedFlags: {
        tenantIsolation: true,
        shadowTestingReady: true
      },
      turningPointRule: {
        method: 'latency-curvature-or-tps-plateau',
        hint: '当延迟增长曲率显著上升或 TPS 进入平台期时，判定吞吐拐点'
      }
    };
  }
}
