export function average(values) {
  if (values.length === 0) {
    return 0;
  }

  return values.reduce((sum, value) => sum + value, 0) / values.length;
}

export function percentile(values, p) {
  if (values.length === 0) {
    return 0;
  }

  const sorted = [...values].sort((left, right) => left - right);
  const index = Math.min(sorted.length - 1, Math.max(0, Math.ceil((p / 100) * sorted.length) - 1));
  return sorted[index];
}

export function median(values) {
  return percentile(values, 50);
}

export function quantile(values, q) {
  if (values.length === 0) {
    return 0;
  }

  const sorted = [...values].sort((left, right) => left - right);
  const position = (sorted.length - 1) * q;
  const base = Math.floor(position);
  const rest = position - base;
  const current = sorted[base];
  const next = sorted[base + 1] ?? current;

  return current + rest * (next - current);
}

export function summarizeDurations(values) {
  return {
    count: values.length,
    avgMs: Number(average(values).toFixed(2)),
    p50Ms: Number(percentile(values, 50).toFixed(2)),
    p95Ms: Number(percentile(values, 95).toFixed(2)),
    p99Ms: Number(percentile(values, 99).toFixed(2))
  };
}
