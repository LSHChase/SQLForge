import { quantile, summarizeDurations } from '../../../shared/utils/statistics.js';

export class ResultSanitizer {
  sanitize(metrics = []) {
    const warmupDiscarded = metrics.slice(0, 3);
    const stable = metrics.slice(3);
    const durations = stable.map((item) => item.latencyMs);
    const q1 = quantile(durations, 0.25);
    const q3 = quantile(durations, 0.75);
    const iqr = q3 - q1;
    const lowerBound = q1 - 1.5 * iqr;
    const upperBound = q3 + 1.5 * iqr;

    const cleaned = [];
    const anomalies = [];

    for (const metric of stable) {
      const isOutlier = metric.latencyMs < lowerBound || metric.latencyMs > upperBound;

      if (metric.gcPauseMs > 100) {
        anomalies.push({
          type: 'gc-pause',
          metric
        });
      }

      if (isOutlier) {
        anomalies.push({
          type: 'iqr-outlier',
          metric
        });
        continue;
      }

      cleaned.push(metric);
    }

    return {
      warmupDiscarded,
      cleaned,
      anomalies,
      summary: summarizeDurations(cleaned.map((item) => item.latencyMs))
    };
  }
}
