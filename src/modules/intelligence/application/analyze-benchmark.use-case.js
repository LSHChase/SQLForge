export class AnalyzeBenchmarkUseCase {
  constructor(resultSanitizer, bottleneckDetector, resourceEstimator, optimizationRecommendationEngine, baselineComparator) {
    this.resultSanitizer = resultSanitizer;
    this.bottleneckDetector = bottleneckDetector;
    this.resourceEstimator = resourceEstimator;
    this.optimizationRecommendationEngine = optimizationRecommendationEngine;
    this.baselineComparator = baselineComparator;
  }

  execute(input) {
    const sanitized = this.resultSanitizer.sanitize(input.metrics ?? []);
    const bottleneck = this.bottleneckDetector.detect(input.resourceBreakdown ?? {});
    const estimatedResources = this.resourceEstimator.estimate({
      queryMix: input.queryMix ?? [],
      targetConcurrency: input.targetConcurrency ?? 20,
      hotDataGb: input.hotDataGb ?? 200
    });
    const baselineComparison = this.baselineComparator.compare(input.previousBaseline, sanitized.summary);
    const recommendations = this.optimizationRecommendationEngine.build({
      assessment: input.assessment,
      benchmarkAnalysis: {
        sanitized,
        bottleneck,
        estimatedResources
      },
      baselineComparison
    });

    return {
      sanitized,
      bottleneck,
      estimatedResources,
      baselineComparison,
      recommendations
    };
  }
}
