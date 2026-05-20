const hasText = value => value !== null && value !== undefined && String(value).trim() !== ''

const firstText = (...values) => {
  const value = values.find(hasText)
  return hasText(value) ? String(value).trim() : ''
}

const hasBlockingReasons = artifact => {
  const reasons = artifact?.blockingReasons
  if (Array.isArray(reasons)) {
    return reasons.length > 0
  }
  return hasText(reasons)
}

export const isGeneratedRuntimeRewriteArtifact = artifact =>
  artifact &&
  String(artifact.rule || '').toUpperCase() === 'PRECOMPUTE_MV' &&
  String(artifact.artifactStatus || '').toUpperCase() === 'GENERATED' &&
  hasText(artifact.rewriteSql) &&
  !hasBlockingReasons(artifact)

export const resolveRuntimeRewriteSql = ({ artifact, recommendedSqlText, diffRecommendedSql } = {}) => {
  if (isGeneratedRuntimeRewriteArtifact(artifact)) {
    return {
      sqlText: String(artifact.rewriteSql).trim(),
      source: 'ACCELERATION_ARTIFACT_REWRITE_SQL',
      accelerationArtifact: artifact
    }
  }
  return {
    sqlText: firstText(recommendedSqlText, diffRecommendedSql),
    source: 'RECOMMENDATION_RECOMMENDED_SQL',
    accelerationArtifact: null
  }
}

export const buildRuntimeRewriteTraceRefs = selection => {
  if (!selection || !hasText(selection.source)) {
    return {}
  }
  const artifact = selection.accelerationArtifact
  const traceRefs = {
    runtimeRewriteSqlSource: selection.source
  }
  if (artifact) {
    traceRefs.accelerationArtifact = artifact
    traceRefs.mvType = artifact.mvType
    traceRefs.mvName = artifact.mvName
    traceRefs.artifactStatus = artifact.artifactStatus
    traceRefs.ddlSql = artifact.ddlSql
    traceRefs.refreshSql = artifact.refreshSql
    traceRefs.validationSql = artifact.validationSql
    traceRefs.rewriteSql = artifact.rewriteSql
    traceRefs.governanceBoundary = artifact.governanceBoundary
  }
  return traceRefs
}
