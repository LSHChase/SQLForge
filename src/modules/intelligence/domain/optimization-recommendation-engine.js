function recommendation(priority, title, rationale, expectedImpact, actionType) {
  return {
    priority,
    title,
    rationale,
    expectedImpact,
    actionType
  };
}

export class OptimizationRecommendationEngine {
  build({ assessment, benchmarkAnalysis, baselineComparison }) {
    const recommendations = [];
    const safeAssessment = assessment ?? {
      labels: {
        scanPattern: 'range'
      },
      validations: {
        predicatePushdown: { status: 'pass' },
        bucketJoin: { status: 'na' },
        sortNecessity: { status: 'pass' },
        repeatedJoin: { status: 'pass' },
        expressionReuse: { status: 'pass' },
        projectionPushdown: { status: 'pass' },
        resultSetControl: { status: 'pass' }
      }
    };
    const validations = safeAssessment.validations;

    if (safeAssessment.labels.scanPattern === 'full-table' || validations.predicatePushdown.status === 'warn') {
      recommendations.push(
        recommendation(
          'high',
          '优化分区与谓词下推',
          '当前 SQL 存在全表扫描或谓词下推不稳风险，应优先检查分区键与过滤表达式写法',
          '降低 Scan 占比并显著缩短 P95/P99',
          'sql-or-table-design'
        )
      );
    }

    if (benchmarkAnalysis.bottleneck.type === 'distribution-bottleneck' || validations.bucketJoin.status === 'opportunity') {
      recommendations.push(
        recommendation(
          'high',
          '验证分桶或 Broadcast 策略',
          'Join 具备优化空间，应比较 Broadcast、Shuffle、Bucket Join 的真实收益',
          '减少 Shuffle 和网络放大',
          'execution-plan'
        )
      );
    }

    if (validations.sortNecessity.status === 'warn') {
      recommendations.push(
        recommendation(
          'medium',
          '移除非必要排序',
          'ORDER BY 未配合 LIMIT，可能触发全局排序与额外内存开销',
          '降低 CPU 与内存峰值',
          'sql-rewrite'
        )
      );
    }

    if (validations.repeatedJoin.status === 'warn') {
      recommendations.push(
        recommendation(
          'medium',
          '物化重复关联',
          '检测到同表多次关联，可考虑使用 CTE、临时表或预聚合来减少重复扫描',
          '降低 Join 链复杂度与计划脆弱性',
          'sql-rewrite'
        )
      );
    }

    if (validations.expressionReuse.status === 'warn') {
      recommendations.push(
        recommendation(
          'medium',
          '复用重复表达式',
          '存在重复函数表达式，应考虑复用别名、CTE 或预计算',
          '降低 CPU 计算浪费',
          'sql-rewrite'
        )
      );
    }

    if (validations.projectionPushdown.status === 'warn') {
      recommendations.push(
        recommendation(
          'medium',
          '收缩投影列',
          'SELECT * 会放大扫描列数与结果传输量，应仅保留必要列',
          '提升投影下推效果，降低扫描和网络开销',
          'sql-rewrite'
        )
      );
    }

    if (validations.resultSetControl.status === 'warn') {
      recommendations.push(
        recommendation(
          'medium',
          '控制结果集规模',
          '结果集缺少边界，建议分页、预聚合或下推聚合',
          '降低客户端和网络瓶颈',
          'query-pattern'
        )
      );
    }

    if (baselineComparison?.status === 'regressed') {
      recommendations.push(
        recommendation(
          'high',
          '排查基线漂移与计划变化',
          '当前结果相对历史基线有明显劣化，需要验证统计信息、数据分布和执行计划是否发生变化',
          '尽快恢复已知稳定表现',
          'baseline-guard'
        )
      );
    }

    if (recommendations.length === 0) {
      recommendations.push(
        recommendation(
          'low',
          '继续观察并扩展样本',
          '当前没有明显单一缺陷，可继续扩大样本或并发梯度验证稳态表现',
          '提高结论置信度',
          'observe'
        )
      );
    }

    const order = { high: 0, medium: 1, low: 2 };
    recommendations.sort((left, right) => order[left.priority] - order[right.priority]);

    return recommendations;
  }
}
