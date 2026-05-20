import { readFileSync } from 'node:fs'
import {
  buildAccelerationArtifactDisplay,
  buildRuntimeRewriteSqlSourceNotice
} from '../src/views/common/accelerationArtifactDisplay.mjs'

const targets = [
  {
    path: 'src/views/recommendation-center/RecommendationCenterView.vue',
    tokens: [
      'data-testid="recommendation-page"',
      'data-testid="recommendation-list"',
      'data-testid="recommendation-detail-drawer"',
      'data-testid="recommendation-detail"',
      'data-testid="recommendation-type-filter"',
      'data-testid="recommendation-status-filter"',
      'data-testid="recommendation-source-category-filter"',
      'data-testid="recommendation-source-object-filter"',
      'data-testid="recommendation-benefit-filter"',
      'data-testid="recommendation-risk-filter"',
      'data-testid="recommendation-validation-filter"',
      'data-testid="recommendation-dispatch-filter"',
      'data-testid="recommendation-manual-review-filter"',
      'data-testid="recommendation-pagination"',
      'data-testid="recommendation-focus-summary"',
      'data-testid="recommendation-dispatch-contract"',
      'data-testid="recommendation-dispatch-event"',
      'data-testid="recommendation-trace-refs"',
      'data-testid="recommendation-review-guard"',
      'data-testid="recommendation-sql-diff"',
      'data-testid="recommendation-sql-compare"',
      'data-testid="recommendation-acceleration-artifact"',
      'accelerationArtifact',
      'accelerationArtifactDisplay',
      'accelerationArtifactCards',
      'accelerationArtifactBlockingRows',
      'accelerationArtifactReviewWarningRows',
      'accelerationArtifactGrainRows',
      'accelerationArtifactDimensionRows',
      'accelerationArtifactMeasureRows',
      'accelerationArtifactPredicateGroups',
      'accelerationArtifactCoverageRows',
      'accelerationArtifactJoinGraphRows',
      'accelerationArtifactEvidenceSections',
      'accelerationArtifactSqlBlocks',
      'data-testid="recommendation-acceleration-artifact-structure"',
      'data-testid="recommendation-acceleration-artifact-grain"',
      'data-testid="recommendation-acceleration-artifact-dimensions"',
      'data-testid="recommendation-acceleration-artifact-measures"',
      'data-testid="recommendation-acceleration-artifact-predicates"',
      'data-testid="recommendation-acceleration-artifact-coverage"',
      'data-testid="recommendation-acceleration-artifact-join-graph"',
      'data-testid="recommendation-acceleration-artifact-type-evidence"',
      'data-testid="recommendation-acceleration-artifact-review-warnings"',
      'data-testid="recommendation-runtime-rewrite-sql-source"',
      'ddlSql',
      'refreshSql',
      'validationSql',
      'rollbackSql',
      'rewriteSql',
      'reviewWarnings',
      'blockingReasons',
      'externalizedPredicates',
      'retainedPredicates',
      'securityPredicates',
      'blockedPredicates',
      'coverage',
      'joinGraph',
      'runtimeRewriteSqlSourceNotice',
      'titleZh',
      'triggerZh',
      'actionZh',
      'data-testid="recommendation-ast-summary-diff"',
      'data-testid="recommendation-rule-diff"',
      'data-testid="recommendation-rule-chain"',
      'data-testid="recommendation-rule-chain-summary"',
      'data-testid="recommendation-preconditions"',
      'data-testid="recommendation-preconditions-summary"',
      'data-testid="recommendation-semantic-risks"',
      'data-testid="recommendation-semantic-risks-summary"',
      'data-testid="recommendation-unapplied-rules"',
      'data-testid="recommendation-unapplied-rules-summary"',
      'data-testid="recommendation-rewrite-lifecycle"',
      'data-testid="recommendation-rewrite-lifecycle-state"',
      'data-testid="recommendation-create-rewrite-record"',
      'data-testid="recommendation-empty-create-rewrite-record"',
      'data-testid="recommendation-rewrite-record-select"',
      'data-testid="recommendation-rewrite-lifecycle-status"',
      'data-testid="recommendation-rewrite-lifecycle-actions"',
      'data-testid="recommendation-rewrite-review-note"',
      'data-testid="recommendation-rewrite-action-reason"',
      'data-testid="recommendation-rewrite-approve"',
      'data-testid="recommendation-rewrite-reject"',
      'data-testid="recommendation-rewrite-publish"',
      'data-testid="recommendation-rewrite-pause"',
      'data-testid="recommendation-rewrite-unpublish"',
      'data-testid="recommendation-rewrite-publish-eligibility"',
      'data-testid="recommendation-rewrite-validation-run-table"',
      'data-testid="recommendation-rewrite-refusal-reasons"',
      'data-testid="recommendation-alert-linkage"',
      'data-testid="recommendation-open-alert-center"',
      'coordinationMode',
      'PULL_ONLY',
      'dispatchEvents',
      'benefitLevel',
      'riskLevel',
      'recommendedSqlText',
      'logicalObjectKey',
      'getRecommendationPage',
      'getRecommendationDiff',
      'textDiff',
      'SqlCompareBlock',
      'buildRecommendedSqlDisplay',
      'astSummaryDiff',
      'focusSummaryCards',
      'frontendCompareOriginalSql',
      'frontendCompareRecommendedSql',
      'hasFrontendCompareSql',
      'ruleChain',
      'preconditions',
      'semanticRisks',
      'unappliedRules',
      'summarizeEvidenceItem',
      "activeDetailTab.value = recommendationDiff.value || hasFrontendCompareSql.value ? 'sqlDiff' : 'summary'",
      "activeDetailTab.value = hasFrontendCompareSql.value ? 'sqlDiff' : 'summary'",
      'normalizeDetailTab(route.query.tab || route.query.detailTab)',
      'normalizeSourceCategoryFromRoute',
      'routeSourceObjectId',
      'routeSourceMeta',
      'route.query.sourceCategory',
      'route.query.sourceType',
      'route.query.sourceKind',
      'route.query.historyId',
      'route.query.parseTaskId',
      'manualReviewRequired',
      'isRewriteReviewCandidate',
      'createSqlRewriteRecord',
      'buildRewriteRecordCreatePayload',
      'resolveRuntimeRewriteSql',
      'runtimeRewriteSqlSource',
      'ACCELERATION_ARTIFACT_REWRITE_SQL',
      'createRewriteRecordAndOpenReview',
      'frontend-recommendation-rewrite-record-create',
      'reviewSqlRewriteRecord',
      'getSqlRewriteRecords',
      'getSqlRewriteRecord',
      'getRewriteValidationRuns',
      'getRewritePublishEligibility',
      'publishSqlRewriteRecord',
      'pauseSqlRewriteRecord',
      'unpublishSqlRewriteRecord',
      'reviewStatus',
      'publishStatus',
      'publishEligibilityCards',
      'rewriteValidationRuns',
      'refusalReasons',
      'runtimeBindingId',
      'runtimeRuleVersion',
      'runtimeBindingScope',
      'approvedNotPublished',
      'runtimeActive',
      'APPROVED',
      'REJECTED',
      'PUBLISHED',
      'PAUSED',
      'UNPUBLISHED',
      'alertLinkageCards',
      'openAlertCenter',
      'SQL_REWRITE_RESULT_DIVERGENCE',
      '<el-table',
      '<el-pagination',
      '<el-drawer',
      'recommendationPager.totalCount',
      'buildRecommendationPageQuery',
      'buildSourceFilterQuery',
      'loadSourceObjectOptions',
      'handleRecommendationSortChange',
      'openEvidenceDrawer'
    ]
  },
  {
    path: 'src/views/common/SqlCompareBlock.vue',
    tokens: [
      'buildSqlCompareRows',
      'sql-compare-block__viewport',
      'sql-compare-block__panes',
      'sql-compare-pane--original',
      'sql-compare-pane--recommended',
      'sql-compare-pane__toolbar',
      'data-testid="sql-compare-original-viewport"',
      'data-testid="sql-compare-recommended-viewport"',
      'data-testid="sql-compare-copy-original"',
      'data-testid="sql-compare-format-original"',
      'syncPaneScroll',
      'buildFormattedSqlDisplayText',
      'sql-compare-row--insert',
      'sql-compare-row--delete',
      'sql-compare-row--replace',
      'sql-compare-token-mark--insert',
      'sql-compare-token-mark--delete'
    ]
  },
  {
    path: 'src/views/common/sqlCompare.mjs',
    tokens: [
      'formatSqlText',
      'highlightSql',
      'buildFormattedSqlDisplayText',
      'extractLeadingSqlComments',
      'buildRecommendedSqlDisplay',
      'buildSqlCompareRows'
    ]
  },
  {
    path: 'src/services/runtimeGateApi.js',
    tokens: [
      'getRecommendations',
      'getRecommendationPage',
      'getGovernanceQueryHistoryPage',
      'getSqlParseHistoryPage',
      'listParseBatches',
      'listReportBatches',
      'sourceType',
      'sourceKind',
      'sourceId',
      'historyId',
      'parseTaskId',
      'batchId',
      '/api/sql-optimization/recommendations/page',
      'getRecommendationDiff',
      'getRecommendationTrace',
      'getDispatchContract',
      'getDispatchEvents',
      'createSqlRewriteRecord',
      '/api/sql-optimization/rewrite-records',
      'reviewSqlRewriteRecord',
      'getRewritePublishEligibility',
      'getRewriteValidationRuns',
      'publishSqlRewriteRecord',
      'pauseSqlRewriteRecord',
      'unpublishSqlRewriteRecord',
      '/api/sql-optimization/rewrite-records/${encodeURIComponent(rewriteRecordId)}/review',
      '/api/sql-optimization/rewrite-records/${encodeURIComponent(rewriteRecordId)}/publish-eligibility',
      '/api/sql-optimization/rewrite-records/${encodeURIComponent(rewriteRecordId)}/validation-runs',
      '/api/sql-optimization/rewrite-records/${encodeURIComponent(rewriteRecordId)}/publish',
      '/api/sql-optimization/rewrite-records/${encodeURIComponent(rewriteRecordId)}/pause',
      '/api/sql-optimization/rewrite-records/${encodeURIComponent(rewriteRecordId)}/unpublish'
    ]
  },
  {
    path: 'src/views/common/accelerationArtifactDisplay.mjs',
    tokens: [
      'PARAMETERIZED_AGG_MV',
      'PREJOIN_MV',
      'STAR_AGG_MV',
      'ROLLUP_MV',
      'COMMON_SUBGRAPH_MV',
      'GENERATED',
      'BLOCKED',
      'REVIEW_REQUIRED',
      'reviewWarnings',
      'blockingReasons',
      'externalizedPredicates',
      'retainedPredicates',
      'securityPredicates',
      'blockedPredicates',
      'rewriteSqlReadonly',
      'rewriteSqlReferencesMv',
      'rewriteSqlAvoidsOriginalSources',
      'timeRollupEvidence',
      'starSchemaEvidence',
      'commonSubgraphEvidence',
      'joinKeys',
      'buildAccelerationArtifactDisplay',
      'buildRuntimeRewriteSqlSourceNotice'
    ]
  },
  {
    path: 'src/config/routePaths.mjs',
    tokens: ['recommendationCenter', '/governance/recommendations']
  },
  {
    path: 'src/router/index.js',
    tokens: ['RecommendationCenterView', "name: 'RecommendationCenter'"]
  }
]

for (const target of targets) {
  const content = readFileSync(new URL(`../${target.path}`, import.meta.url), 'utf8')
  for (const token of target.tokens) {
    if (!content.includes(token)) {
      throw new Error(`Missing token ${JSON.stringify(token)} in ${target.path}`)
    }
  }
  if (target.path === 'src/views/recommendation-center/RecommendationCenterView.vue') {
    const forbiddenTokens = [
      'class="recommendation-row"',
      'class="dispatch-event-row"',
      'class="evidence-group"',
      '<el-card',
      'class="workspace-frame"',
      'class="list-pane"',
      'class="detail-pane"',
      'data-testid="recommendation-text-diff"',
      'filteredRecommendations',
      'pagedRecommendations',
      'comparePaneClass',
      'hunkTagType',
      'getRecommendations'
    ]
    for (const token of forbiddenTokens) {
      if (content.includes(token)) {
        throw new Error(`Recommendation page must not reintroduce stacked card/list token ${JSON.stringify(token)}`)
      }
    }
    assertOrderedTokens(target.path, content, [
      'name="summary"',
      'name="sqlDiff"',
      'name="rulesRisk"',
      'name="sqlEvidence"',
      'name="rewriteLifecycle"',
      'name="dispatchContract"',
      'name="traceability"',
      'name="dispatchEvents"'
    ])
    assertOrderedTokens(target.path, content, [
      'data-testid="recommendation-focus-summary"',
      'data-testid="recommendation-sql-diff"',
      'data-testid="recommendation-rule-chain-summary"',
      'data-testid="recommendation-rewrite-lifecycle"',
      'data-testid="recommendation-rewrite-validation-runs"',
      'data-testid="recommendation-dispatch-contract"'
    ])
  }
}

assertAmv013DisplayContract()

function assertOrderedTokens(path, content, orderedTokens) {
  let previousIndex = -1
  for (const token of orderedTokens) {
    const index = content.indexOf(token)
    if (index === -1) {
      throw new Error(`Missing ordered token ${JSON.stringify(token)} in ${path}`)
    }
    if (index <= previousIndex) {
      throw new Error(`推荐结果页 token 顺序错误：${path} 中 ${JSON.stringify(token)} 未位于前一个 token 之后`)
    }
    previousIndex = index
  }
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

function syntheticArtifact(mvType, overrides = {}) {
  return {
    rule: 'PRECOMPUTE_MV',
    mvType,
    artifactStatus: 'GENERATED',
    mvName: `mv_${mvType.toLowerCase()}`,
    targetEngine: 'HETU',
    targetDatasource: 'hetu_main',
    dialect: 'HETU',
    grain: ['customer_id'],
    dimensions: ['customer_id'],
    measures: [
      {
        name: 'sum_amount',
        sourceExpression: 'SUM(amount)',
        rewriteExpression: 'SUM(sum_amount)',
        mergeable: true
      }
    ],
    externalizedPredicates: [{ expression: 'tenant_id = ?' }],
    retainedPredicates: [{ expression: "status = 'PAID'" }],
    securityPredicates: [{ expression: "access_domain = 'BI'" }],
    blockedPredicates: [],
    coverage: {
      coversProjection: true,
      coversFilters: true,
      coversGrouping: true,
      coversMeasures: true,
      coversSecurity: true,
      rewriteSqlReadonly: true,
      rewriteSqlReferencesMv: true,
      rewriteSqlAvoidsOriginalSources: true
    },
    joinGraph: [{ joinType: 'INNER', leftTable: 'orders', rightTable: 'customers', condition: 'orders.customer_id = customers.customer_id' }],
    joinKeys: [{ leftColumn: 'orders.customer_id', rightColumn: 'customers.customer_id' }],
    timeRollupEvidence: mvType === 'ROLLUP_MV' ? { queryTargetGrain: 'MONTH', mvFinestGrain: 'DAY' } : undefined,
    starSchemaEvidence: mvType === 'STAR_AGG_MV' ? { factInference: 'MEASURE_SOURCE_AND_JOIN_TOPOLOGY' } : undefined,
    commonSubgraphEvidence: mvType === 'COMMON_SUBGRAPH_MV' ? { sourceKind: 'CTE', referenceCount: 2 } : undefined,
    ddlSql: 'CREATE MATERIALIZED VIEW mv AS SELECT customer_id, SUM(amount) AS sum_amount FROM orders GROUP BY customer_id',
    refreshSql: 'REFRESH MATERIALIZED VIEW mv',
    validationSql: syntheticValidationSql(mvType),
    rollbackSql: 'DROP MATERIALIZED VIEW mv',
    rewriteSql: 'SELECT customer_id, SUM(sum_amount) AS total_amount FROM mv GROUP BY customer_id',
    runtimeRewriteBinding: 'NOT_CREATED',
    governanceBoundary: 'PULL_ONLY_NOT_EXECUTED_BY_SQLFORGE',
    ...overrides
  }
}

function syntheticValidationSql(mvType) {
  const checks = [
    "SELECT 'ROW_COUNT_CHECK' AS check_name, 'final_result' AS check_target",
    "SELECT 'MEASURE_DIFF' AS check_name, 'sum_amount' AS check_target",
    "SELECT 'GROUP_MEASURE_DIFF' AS check_name, 'sum_amount' AS check_target",
    "SELECT 'GROUP_KEY_DIFF' AS check_name, 'customer_id' AS check_target"
  ]
  if (mvType === 'PREJOIN_MV' || mvType === 'STAR_AGG_MV') {
    checks.push("SELECT 'JOIN_ROW_COUNT_CHECK' AS check_name, 'post_join_result' AS check_target")
  }
  if (mvType === 'COMMON_SUBGRAPH_MV') {
    checks.push("SELECT 'COMMON_SUBGRAPH_OUTPUT_CHECK' AS check_name, 'common_subgraph_output' AS check_target")
    checks.push("SELECT 'UPPER_REWRITE_RESULT_CHECK' AS check_name, 'upper_query_result' AS check_target")
  }
  return [
    'WITH original_result AS (',
    'SELECT customer_id, SUM(amount) AS sum_amount FROM orders GROUP BY customer_id',
    '),',
    'rewrite_result AS (',
    'SELECT customer_id, SUM(sum_amount) AS sum_amount FROM mv GROUP BY customer_id',
    '),',
    'original_group AS (SELECT customer_id, SUM(sum_amount) AS sum_amount FROM original_result GROUP BY customer_id),',
    'rewrite_group AS (SELECT customer_id, SUM(sum_amount) AS sum_amount FROM rewrite_result GROUP BY customer_id)',
    checks.join('\nUNION ALL\n')
  ].join('\n')
}

function assertAmv013DisplayContract() {
  for (const mvType of ['PARAMETERIZED_AGG_MV', 'PREJOIN_MV', 'STAR_AGG_MV', 'ROLLUP_MV', 'COMMON_SUBGRAPH_MV']) {
    const display = buildAccelerationArtifactDisplay(syntheticArtifact(mvType))
    assert(display.overviewRows.some(row => row.value.includes('MV')), `AMV-013 display missing Chinese MV explanation for ${mvType}.`)
    assert(display.measureRows[0]?.sourceExpression === 'SUM(amount)', `AMV-013 display missing sourceExpression for ${mvType}.`)
    assert(display.measureRows[0]?.rewriteExpression === 'SUM(sum_amount)', `AMV-013 display missing rewriteExpression for ${mvType}.`)
    assert(display.predicateGroups.length === 4, `AMV-013 display must expose four predicate groups for ${mvType}.`)
    assert(display.coverageRows.some(row => row.key === 'rewriteSqlReferencesMv' && row.value === 'true'), `AMV-013 display missing rewrite MV reference proof for ${mvType}.`)
    const validationBlock = display.sqlBlocks.find(block => block.key === 'validationSql')
    assert(validationBlock?.value.includes('ROW_COUNT_CHECK'), `AMV-015 validation SQL missing ROW_COUNT_CHECK for ${mvType}.`)
    assert(validationBlock?.value.includes('MEASURE_DIFF'), `AMV-015 validation SQL missing MEASURE_DIFF for ${mvType}.`)
    assert(validationBlock?.value.includes('GROUP_MEASURE_DIFF'), `AMV-015 validation SQL missing GROUP_MEASURE_DIFF for ${mvType}.`)
    assert(validationBlock?.value.includes('GROUP_KEY_DIFF'), `AMV-015 validation SQL missing GROUP_KEY_DIFF for ${mvType}.`)
    if (mvType === 'PREJOIN_MV' || mvType === 'STAR_AGG_MV') {
      assert(validationBlock?.value.includes('JOIN_ROW_COUNT_CHECK'), `AMV-015 validation SQL missing join check for ${mvType}.`)
    }
    if (mvType === 'COMMON_SUBGRAPH_MV') {
      assert(validationBlock?.value.includes('COMMON_SUBGRAPH_OUTPUT_CHECK'), 'AMV-015 validation SQL missing common subgraph output check.')
      assert(validationBlock?.value.includes('UPPER_REWRITE_RESULT_CHECK'), 'AMV-015 validation SQL missing upper rewrite result check.')
    }
  }

  const blocked = buildAccelerationArtifactDisplay(
    syntheticArtifact('ROLLUP_MV', {
      artifactStatus: 'BLOCKED',
      blockingReasons: [{ code: 'TIME_ROLLUP_EXPRESSION_NOT_NORMALIZABLE', description: 'blocked' }],
      ddlSql: '',
      rewriteSql: ''
    })
  )
  assert(blocked.blockingRows[0]?.code === 'TIME_ROLLUP_EXPRESSION_NOT_NORMALIZABLE', 'AMV-013 display must expose BLOCKED reasons.')

  const reviewRequired = buildAccelerationArtifactDisplay(
    syntheticArtifact('PREJOIN_MV', {
      artifactStatus: 'REVIEW_REQUIRED',
      reviewWarnings: [{ code: 'ROW_AMPLIFICATION_METADATA_MISSING', description: 'review' }]
    })
  )
  assert(reviewRequired.reviewWarningRows[0]?.code === 'ROW_AMPLIFICATION_METADATA_MISSING', 'AMV-013 display must expose REVIEW_REQUIRED warnings.')

  const generatedWithWarnings = buildAccelerationArtifactDisplay(
    syntheticArtifact('PREJOIN_MV', {
      artifactStatus: 'GENERATED',
      reviewWarnings: [{ code: 'ROW_AMPLIFICATION_METADATA_MISSING', description: 'review' }]
    })
  )
  assert(generatedWithWarnings.reviewWarningRows.length === 1, 'AMV-013 display must expose GENERATED artifacts with reviewWarnings.')

  const legacy = buildAccelerationArtifactDisplay({ mvType: 'PARAMETERIZED_AGG_MV', artifactStatus: 'GENERATED' })
  assert(legacy.grainRows.length === 0 && legacy.measureRows.length === 0, 'AMV-013 display must tolerate legacy snapshots with missing arrays.')

  const notice = buildRuntimeRewriteSqlSourceNotice({
    source: 'ACCELERATION_ARTIFACT_REWRITE_SQL',
    accelerationArtifact: syntheticArtifact('PARAMETERIZED_AGG_MV')
  })
  assert(notice.includes('accelerationArtifact.rewriteSql') && notice.includes('runtime binding ACTIVE'), 'AMV-013 notice must explain rewriteSql source and runtime boundary.')
}

console.log('recommendation page contract ok')
