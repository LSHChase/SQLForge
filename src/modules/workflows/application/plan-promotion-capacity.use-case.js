export class PlanPromotionCapacityUseCase {
  constructor(capacityPlanner, resourceEstimator, reportBuilder) {
    this.capacityPlanner = capacityPlanner;
    this.resourceEstimator = resourceEstimator;
    this.reportBuilder = reportBuilder;
  }

  execute(input) {
    const capacityPlan = this.capacityPlanner.plan({
      baselineQps: input.baselineQps,
      growthFactor: input.trafficGrowthFactor,
      avgServiceTimeSec: input.avgServiceTimeSec,
      currentWorkers: input.currentWorkers,
      targetP99Ms: input.targetP99Ms
    });
    const estimatedResources = this.resourceEstimator.estimate({
      queryMix: input.queryMix ?? [],
      targetConcurrency: input.targetConcurrency ?? capacityPlan.workerPlan.requiredWorkers,
      hotDataGb: input.hotDataGb ?? 2000
    });
    const report = this.reportBuilder.buildCapacityReport({
      capacityPlan,
      estimatedResources
    });

    return {
      workflow: 'promotion-capacity-planning',
      capacityPlan,
      estimatedResources,
      report
    };
  }
}
