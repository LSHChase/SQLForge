export class CapacityPlanner {
  plan({
    baselineQps = 100,
    growthFactor = 1,
    avgServiceTimeSec = 2,
    currentWorkers = 10,
    targetP99Ms = 5000
  }) {
    const projectedQps = baselineQps * growthFactor;
    const utilizationTarget = targetP99Ms <= 5000 ? 0.65 : 0.75;
    const workerDemand = Math.ceil((projectedQps * avgServiceTimeSec) / utilizationTarget);
    const workerGap = Math.max(0, workerDemand - currentWorkers);
    const averageWaitingSec = Number(
      Math.max(0, (projectedQps * avgServiceTimeSec) / Math.max(workerDemand, 1) - utilizationTarget).toFixed(2)
    );

    return {
      arrivalModel: {
        baselineQps,
        growthFactor,
        projectedQps
      },
      target: {
        targetP99Ms,
        utilizationTarget
      },
      workerPlan: {
        currentWorkers,
        requiredWorkers: workerDemand,
        additionalWorkers: workerGap
      },
      queueEstimate: {
        averageWaitingSec
      }
    };
  }
}
