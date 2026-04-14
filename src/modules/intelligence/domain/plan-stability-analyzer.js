function arraysEqual(left = [], right = []) {
  if (left.length !== right.length) {
    return false;
  }

  return left.every((value, index) => value === right[index]);
}

export class PlanStabilityAnalyzer {
  analyze({ fingerprint, historicalBestPlan, currentPlan, candidatePlans = [] }) {
    if (!currentPlan?.planHash) {
      throw new Error('currentPlan.planHash is required');
    }

    const bestPlan = historicalBestPlan ?? currentPlan;
    const latencyRegressionRatio =
      Number(bestPlan.latencyMs ?? 0) > 0
        ? (Number(currentPlan.latencyMs ?? 0) - Number(bestPlan.latencyMs ?? 0)) / Number(bestPlan.latencyMs)
        : 0;
    const diff = {
      fingerprint,
      bestPlanHash: bestPlan.planHash,
      currentPlanHash: currentPlan.planHash,
      planHashChanged: bestPlan.planHash !== currentPlan.planHash,
      joinOrderChanged: !arraysEqual(bestPlan.joinOrder ?? [], currentPlan.joinOrder ?? []),
      distributionChanged: (bestPlan.distribution ?? 'unknown') !== (currentPlan.distribution ?? 'unknown'),
      statsAgeRegressionHours: Number(currentPlan.statsAgeHours ?? 0) - Number(bestPlan.statsAgeHours ?? 0),
      latencyRegressionRatio: Number(latencyRegressionRatio.toFixed(4))
    };

    const rootCauses = [];

    if (diff.statsAgeRegressionHours >= 24) {
      rootCauses.push('stale-stats');
    }

    if (diff.joinOrderChanged || diff.distributionChanged) {
      rootCauses.push('plan-drift');
    }

    if (candidatePlans.some((plan) => Number(plan.latencyMs ?? Infinity) < Number(currentPlan.latencyMs ?? Infinity))) {
      rootCauses.push('better-candidate-exists');
    }

    let decision = 'observe';
    let message = '未检测到需要立即干预的计划稳定性风险';

    if (diff.latencyRegressionRatio > 0.2 && rootCauses.includes('stale-stats')) {
      decision = 'refresh-stats';
      message = '当前计划明显劣化且统计信息更旧，应优先刷新统计信息并重新验证';
    } else if (diff.latencyRegressionRatio > 0.2 && rootCauses.includes('plan-drift')) {
      decision = 'protect-best-plan';
      message = '当前计划相对历史最优计划明显退化，建议优先保护已知更优计划';
    } else if (diff.latencyRegressionRatio > 0.1) {
      decision = 'investigate';
      message = '存在中度延迟劣化，需要进一步对比数据分布和候选计划';
    }

    return {
      diff,
      rootCauses,
      decision,
      message,
      recommendedActions:
        decision === 'refresh-stats'
          ? ['触发 ANALYZE', '建立关键表统计信息新鲜度 SLA']
          : decision === 'protect-best-plan'
            ? ['锁定较优 Hint 或白名单策略', '监控计划 Hash 变化']
            : decision === 'investigate'
              ? ['回放候选计划', '检查数据分布与 CBO 参数']
              : ['继续监控计划 Hash 和 P99']
    };
  }
}
