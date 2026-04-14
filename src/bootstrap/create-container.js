import { InMemoryBaselineRepository } from '../infrastructure/repositories/in-memory-baseline-repository.js';
import { AssessSqlUseCase } from '../modules/access/application/assess-sql.use-case.js';
import { QueryIntentEngine } from '../modules/access/domain/query-intent-engine.js';
import { SamplingEngine } from '../modules/data-construction/domain/sampling-engine.js';
import { ExtrapolationModel } from '../modules/data-construction/domain/extrapolation-model.js';
import { CreateBenchmarkPlanUseCase } from '../modules/execution/application/create-benchmark-plan.use-case.js';
import { PressureMatrix } from '../modules/execution/domain/pressure-matrix.js';
import { AnalyzeBenchmarkUseCase } from '../modules/intelligence/application/analyze-benchmark.use-case.js';
import { BottleneckDetector } from '../modules/intelligence/domain/bottleneck-detector.js';
import { BaselineComparator } from '../modules/intelligence/domain/baseline-comparator.js';
import { CapacityPlanner } from '../modules/intelligence/domain/capacity-planner.js';
import { OptimizationRecommendationEngine } from '../modules/intelligence/domain/optimization-recommendation-engine.js';
import { PlanStabilityAnalyzer } from '../modules/intelligence/domain/plan-stability-analyzer.js';
import { ResourceEstimator } from '../modules/intelligence/domain/resource-estimator.js';
import { ResultSanitizer } from '../modules/intelligence/domain/result-sanitizer.js';
import { GenerateReportUseCase } from '../modules/reporting/application/generate-report.use-case.js';
import { ReportBuilder } from '../modules/reporting/domain/report-builder.js';
import { TenantPolicy } from '../modules/tenancy/domain/tenant-policy.js';
import { AnalyzePlanStabilityUseCase } from '../modules/workflows/application/analyze-plan-stability.use-case.js';
import { EvaluateBiReleaseUseCase } from '../modules/workflows/application/evaluate-bi-release.use-case.js';
import { PlanPromotionCapacityUseCase } from '../modules/workflows/application/plan-promotion-capacity.use-case.js';

export function createContainer(config) {
  const tenantPolicy = new TenantPolicy(config.defaultTenantProfile);
  const queryIntentEngine = new QueryIntentEngine(config.defaultSlaMs);
  const assessSql = new AssessSqlUseCase(queryIntentEngine, tenantPolicy, config.defaultTargetConcurrency);
  const samplingEngine = new SamplingEngine();
  const extrapolationModel = new ExtrapolationModel();
  const pressureMatrix = new PressureMatrix();
  const createBenchmarkPlan = new CreateBenchmarkPlanUseCase(
    assessSql,
    samplingEngine,
    pressureMatrix,
    extrapolationModel
  );
  const resultSanitizer = new ResultSanitizer();
  const bottleneckDetector = new BottleneckDetector();
  const capacityPlanner = new CapacityPlanner();
  const resourceEstimator = new ResourceEstimator();
  const optimizationRecommendationEngine = new OptimizationRecommendationEngine();
  const baselineComparator = new BaselineComparator();
  const analyzeBenchmark = new AnalyzeBenchmarkUseCase(
    resultSanitizer,
    bottleneckDetector,
    resourceEstimator,
    optimizationRecommendationEngine,
    baselineComparator
  );
  const reportBuilder = new ReportBuilder();
  const generateReport = new GenerateReportUseCase(reportBuilder);
  const baselineRepository = new InMemoryBaselineRepository();
  const evaluateBiRelease = new EvaluateBiReleaseUseCase(
    assessSql,
    createBenchmarkPlan,
    analyzeBenchmark,
    generateReport,
    baselineRepository
  );
  const planStabilityAnalyzer = new PlanStabilityAnalyzer();
  const analyzePlanStability = new AnalyzePlanStabilityUseCase(
    assessSql,
    planStabilityAnalyzer,
    generateReport
  );
  const planPromotionCapacity = new PlanPromotionCapacityUseCase(
    capacityPlanner,
    resourceEstimator,
    reportBuilder
  );

  return {
    assessSql,
    createBenchmarkPlan,
    analyzeBenchmark,
    generateReport,
    evaluateBiRelease,
    analyzePlanStability,
    planPromotionCapacity
  };
}
