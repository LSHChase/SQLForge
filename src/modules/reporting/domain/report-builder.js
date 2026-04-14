export class ReportBuilder {
  buildSqlPerformanceReport({ assessment, benchmarkPlan, benchmarkAnalysis, tenant }) {
    const p99 = benchmarkAnalysis.sanitized.summary.p99Ms;
    const decision = p99 <= 5000 ? 'approve' : 'optimize';

    return {
      reportType: 'sql-performance',
      tenant: tenant.id,
      fingerprint: assessment.fingerprint,
      summary: {
        decision,
        riskLevel: benchmarkPlan.riskLevel,
        tables: assessment.tables,
        p99Ms: p99
      },
      validations: assessment.validations,
      assessment,
      benchmarkPlan: {
        targetConcurrency: benchmarkPlan.targetConcurrency,
        samplingPlan: benchmarkPlan.samplingPlan,
        matrix: benchmarkPlan.matrix,
        projectedFullScale: benchmarkPlan.projectedFullScale
      },
      benchmarkAnalysis: {
        ...benchmarkAnalysis,
        baselineComparison: benchmarkAnalysis.baselineComparison,
        recommendations: benchmarkAnalysis.recommendations
      },
      actions: benchmarkAnalysis.recommendations
    };
  }

  buildCapacityReport({ capacityPlan, estimatedResources }) {
    return {
      reportType: 'capacity-plan',
      arrivalModel: capacityPlan.arrivalModel,
      workerPlan: capacityPlan.workerPlan,
      queueEstimate: capacityPlan.queueEstimate,
      estimatedResources
    };
  }

  buildPlanStabilityReport({ fingerprint, stabilityAnalysis, assessment }) {
    return {
      reportType: 'plan-stability',
      fingerprint,
      summary: {
        decision: stabilityAnalysis.decision,
        message: stabilityAnalysis.message
      },
      assessment,
      stabilityAnalysis
    };
  }
}
