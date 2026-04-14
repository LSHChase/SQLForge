export class ExtrapolationModel {
  project({ sampleLatencyMs = 1000, fullRows = 100000000, sampleRows = 2000000, skew = 1, alpha = 0.9, beta = 0.3 }) {
    const ratio = Math.max(1, fullRows / Math.max(sampleRows, 1));
    const fullLatencyMs = sampleLatencyMs * (1 + alpha * Math.log(ratio) + beta * Math.sqrt(Math.max(skew, 1)));
    const errorBand = Math.max(0.08, Math.min(0.35, 0.1 + skew * 0.01));

    return {
      projectedLatencyMs: Number(fullLatencyMs.toFixed(2)),
      confidence95: {
        lowerMs: Number((fullLatencyMs * (1 - errorBand)).toFixed(2)),
        upperMs: Number((fullLatencyMs * (1 + errorBand)).toFixed(2))
      },
      model: {
        alpha,
        beta,
        ratio: Number(ratio.toFixed(2)),
        skew
      }
    };
  }
}
