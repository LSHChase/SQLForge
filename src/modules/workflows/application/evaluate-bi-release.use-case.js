function buildSyntheticMetrics(targetConcurrency, riskLevel) {
  const baseLatency = riskLevel === 'high' ? 3200 : riskLevel === 'medium' ? 2200 : 1400;
  const pressurePenalty = Math.max(0, targetConcurrency - 10) * 80;

  return Array.from({ length: 10 }, (_, index) => ({
    run: index + 1,
    latencyMs: baseLatency + pressurePenalty + index * 120,
    gcPauseMs: index === 4 ? 120 : 20
  }));
}

export class EvaluateBiReleaseUseCase {
  constructor(assessSql, createBenchmarkPlan, analyzeBenchmark, generateReport, baselineRepository) {
    this.assessSql = assessSql;
    this.createBenchmarkPlan = createBenchmarkPlan;
    this.analyzeBenchmark = analyzeBenchmark;
    this.generateReport = generateReport;
    this.baselineRepository = baselineRepository;
  }

  execute(input) {
    const sqlAssessment = this.assessSql.execute(input);
    const previousBaseline = this.baselineRepository.get(sqlAssessment.assessment.fingerprint);
    const benchmarkPlan = this.createBenchmarkPlan.execute(input);
    const benchmarkAnalysis = this.analyzeBenchmark.execute({
      assessment: sqlAssessment.assessment,
      previousBaseline,
      metrics: input.metrics ?? buildSyntheticMetrics(sqlAssessment.targetConcurrency, sqlAssessment.riskLevel),
      resourceBreakdown: input.resourceBreakdown ?? {
        scanRatio: sqlAssessment.assessment.labels.resourceType === 'io-bound' ? 0.65 : 0.35,
        shuffleRatio: sqlAssessment.assessment.labels.joinType === 'single-table' ? 0.1 : 0.28,
        computeRatio: sqlAssessment.assessment.labels.computeDensity === 'light' ? 0.2 : 0.38,
        networkRatio: 0.08,
        waitingRatio: sqlAssessment.targetConcurrency > sqlAssessment.tenant.maxConcurrency ? 0.25 : 0.12
      },
      queryMix: input.queryMix ?? [
        {
          name: 'bi-release-query',
          cpuTimeSec: sqlAssessment.assessment.resourceEstimate.score / 15,
          arrivalRate: Math.max(60, sqlAssessment.targetConcurrency * 18),
          memoryPeakGb: sqlAssessment.assessment.labels.joinType === 'single-table' ? 2 : 6
        }
      ],
      targetConcurrency: sqlAssessment.targetConcurrency,
      hotDataGb: input.dataProfile?.hotDataGb ?? 200
    });
    const report = this.generateReport.execute({
      assessment: sqlAssessment.assessment,
      benchmarkPlan,
      benchmarkAnalysis,
      tenant: sqlAssessment.tenant
    });

    const decision =
      sqlAssessment.riskLevel === 'critical'
        ? 'blocked'
        : benchmarkAnalysis.sanitized.summary.p99Ms <= (input.slaMs ?? 5000)
          ? 'approved'
          : 'needs-optimization';

    this.baselineRepository.save({
      fingerprint: sqlAssessment.assessment.fingerprint,
      tenantId: sqlAssessment.tenant.id,
      decision,
      summary: benchmarkAnalysis.sanitized.summary
    });

    return {
      workflow: 'bi-release-evaluation',
      decision,
      sqlAssessment,
      benchmarkPlan,
      benchmarkAnalysis,
      previousBaseline,
      report
    };
  }
}
