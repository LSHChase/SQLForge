export class CreateBenchmarkPlanUseCase {
  constructor(assessSql, samplingEngine, pressureMatrix, extrapolationModel) {
    this.assessSql = assessSql;
    this.samplingEngine = samplingEngine;
    this.pressureMatrix = pressureMatrix;
    this.extrapolationModel = extrapolationModel;
  }

  execute(input) {
    const sqlAssessment = this.assessSql.execute(input);
    const samplingPlan = this.samplingEngine.buildPlan({
      tables: sqlAssessment.assessment.tables,
      dataProfile: input.dataProfile
    });
    const matrix = this.pressureMatrix.build({
      assessment: sqlAssessment.assessment,
      targetConcurrency: sqlAssessment.targetConcurrency,
      dataProfile: input.dataProfile,
      tenant: sqlAssessment.tenant
    });
    const projectedFullScale = this.extrapolationModel.project({
      sampleLatencyMs: Number(input.sampleLatencyMs ?? 1200),
      fullRows: Number(input.dataProfile?.fullRows ?? 100000000),
      sampleRows: Number(input.dataProfile?.sampleRows ?? 2000000),
      skew: Number(input.dataProfile?.skew ?? 1)
    });

    return {
      ...sqlAssessment,
      samplingPlan,
      matrix,
      projectedFullScale
    };
  }
}
