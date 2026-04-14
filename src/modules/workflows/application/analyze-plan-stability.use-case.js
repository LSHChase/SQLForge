export class AnalyzePlanStabilityUseCase {
  constructor(assessSql, planStabilityAnalyzer, generateReport) {
    this.assessSql = assessSql;
    this.planStabilityAnalyzer = planStabilityAnalyzer;
    this.generateReport = generateReport;
  }

  execute(input) {
    const sqlAssessment = this.assessSql.execute(input);
    const stabilityAnalysis = this.planStabilityAnalyzer.analyze({
      fingerprint: sqlAssessment.assessment.fingerprint,
      historicalBestPlan: input.historicalBestPlan,
      currentPlan: input.currentPlan,
      candidatePlans: input.candidatePlans ?? []
    });
    const report = this.generateReport.execute({
      type: 'plan-stability',
      fingerprint: sqlAssessment.assessment.fingerprint,
      assessment: sqlAssessment.assessment,
      stabilityAnalysis
    });

    return {
      workflow: 'plan-stability-analysis',
      sqlAssessment,
      stabilityAnalysis,
      report
    };
  }
}
