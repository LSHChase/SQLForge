export class SamplingEngine {
  buildPlan({ tables, dataProfile = {} }) {
    const skew = Number(dataProfile.skew ?? 1);
    const fullRows = Number(dataProfile.fullRows ?? 100000000);
    const sampleRows = Number(dataProfile.sampleRows ?? Math.max(1000000, Math.floor(fullRows * 0.02)));
    const baseRatio = Math.max(0.01, Math.min(0.05, sampleRows / Math.max(fullRows, 1)));
    const skewStrategy =
      skew < 2 ? 'simple-random' : skew < 10 ? 'stratified-hotspot-oversampling' : 'hot-key-full-cover';

    return {
      sampleRatio: Number(baseRatio.toFixed(4)),
      skew,
      strategies: [
        'retain-all-partition-keys',
        skewStrategy,
        'preserve-foreign-key-integrity',
        'preserve-commit-timeline-order'
      ],
      tablePlans: tables.map((table) => ({
        table,
        sampleRows: Math.max(100000, Math.floor(sampleRows / Math.max(tables.length, 1))),
        mode: skewStrategy
      }))
    };
  }
}
