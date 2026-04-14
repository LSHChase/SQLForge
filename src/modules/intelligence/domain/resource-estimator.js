export class ResourceEstimator {
  estimate({
    queryMix = [],
    targetConcurrency = 20,
    hotDataGb = 200
  }) {
    const cpuCoreDemand = queryMix.reduce((sum, query) => {
      return sum + (Number(query.cpuTimeSec ?? 0) * Number(query.arrivalRate ?? 0)) / (3600 * 0.7);
    }, 0);
    const executionMemoryGb = queryMix.reduce((sum, query) => {
      return sum + Number(query.memoryPeakGb ?? 0);
    }, 0);
    const memoryGb = executionMemoryGb * Math.max(1, targetConcurrency * 0.15) * 1.5 + hotDataGb * 0.1 + 32;
    const diskTb = hotDataGb * 0.002 + memoryGb / 512 + 0.1;
    const networkGbps = Math.max(10, Number(((hotDataGb * 8 * 1.5) / 1024).toFixed(2)));

    return {
      cpuCores: Math.ceil(cpuCoreDemand),
      memoryGb: Math.ceil(memoryGb),
      diskTb: Number(diskTb.toFixed(2)),
      networkGbps
    };
  }
}
