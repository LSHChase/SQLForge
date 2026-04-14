export class GenerateReportUseCase {
  constructor(reportBuilder) {
    this.reportBuilder = reportBuilder;
  }

  execute(input) {
    if (input.type === 'capacity-plan') {
      return this.reportBuilder.buildCapacityReport(input);
    }

    if (input.type === 'plan-stability') {
      return this.reportBuilder.buildPlanStabilityReport(input);
    }

    return this.reportBuilder.buildSqlPerformanceReport(input);
  }
}
