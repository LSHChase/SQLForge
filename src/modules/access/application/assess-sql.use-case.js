export class AssessSqlUseCase {
  constructor(queryIntentEngine, tenantPolicy, defaultTargetConcurrency) {
    this.queryIntentEngine = queryIntentEngine;
    this.tenantPolicy = tenantPolicy;
    this.defaultTargetConcurrency = defaultTargetConcurrency;
  }

  execute(input) {
    const tenant = this.tenantPolicy.resolve(input.tenantId);
    const assessment = this.queryIntentEngine.assess(input.sql, {
      slaMs: input.slaMs
    });
    const targetConcurrency = input.targetConcurrency ?? this.defaultTargetConcurrency;
    const riskLevel = assessment.risks.some((risk) => risk.severity === 'critical')
      ? 'critical'
      : assessment.risks.some((risk) => risk.severity === 'high')
        ? 'high'
        : assessment.risks.length > 0
          ? 'medium'
          : 'low';

    return {
      tenant,
      targetConcurrency,
      riskLevel,
      requiresBenchmark: ['critical', 'high'].includes(riskLevel),
      assessment
    };
  }
}
