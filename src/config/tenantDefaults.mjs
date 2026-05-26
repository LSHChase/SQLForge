import {
  sampleProfileEnabled,
  sampleProfileFlagState,
  runtimeMode
} from './runtimeFlags.js'

export const DEFAULT_TENANT_ID = 'system'

const SAMPLE_PROFILE_DEFAULTS = Object.freeze({
  tenantId: 'tenant-a',
  secondaryTenantId: 'tenant-b',
  datasourceCode: 'hetu_main'
})

export const SAMPLE_TENANT_ID = SAMPLE_PROFILE_DEFAULTS.tenantId
export const SAMPLE_DATASOURCE_CODE = SAMPLE_PROFILE_DEFAULTS.datasourceCode

export const sampleProfileAvailability = Object.freeze({
  enabled: sampleProfileEnabled,
  mode: runtimeMode,
  flagState: sampleProfileFlagState
})

export const normalizeTenantId = value => String(value || '').trim()
export const normalizeDatasourceCode = value => String(value || '').trim()

const firstValue = (...candidates) => {
  for (const candidate of candidates) {
    const normalized = String(candidate || '').trim()
    if (normalized) {
      return normalized
    }
  }
  return ''
}

export const resolveTenantId = (...candidates) => {
  const normalized = firstValue(...candidates)
  if (normalized) {
    return normalized
  }
  return DEFAULT_TENANT_ID
}

export const resolveRuntimeTenantId = (...candidates) => {
  const normalized = firstValue(...candidates)
  if (normalized) {
    return normalized
  }
  return sampleProfileEnabled ? SAMPLE_PROFILE_DEFAULTS.tenantId : DEFAULT_TENANT_ID
}

export const resolveProtectedTenantId = (...candidates) => resolveTenantId(...candidates)

export const resolveRuntimeDatasourceCode = (...candidates) => {
  const normalized = firstValue(...candidates)
  if (normalized) {
    return normalized
  }
  return sampleProfileEnabled ? SAMPLE_PROFILE_DEFAULTS.datasourceCode : ''
}

export const buildSampleTenantOptions = () =>
  sampleProfileEnabled
    ? [
        {
          label: SAMPLE_PROFILE_DEFAULTS.tenantId,
          value: SAMPLE_PROFILE_DEFAULTS.tenantId
        },
        {
          label: SAMPLE_PROFILE_DEFAULTS.secondaryTenantId,
          value: SAMPLE_PROFILE_DEFAULTS.secondaryTenantId
        }
      ]
    : []

export const buildSqlCommentHeader = ({
  tenantId,
  datasourceCode,
  reportCode = 'RPT_SALES_DAILY',
  stage = 'PROD',
  bizDate = '',
  engineHint = '',
  priority = ''
} = {}) => {
  const lines = []
  if (reportCode) {
    lines.push(`--report_code=${reportCode}`)
  }
  if (stage) {
    lines.push(`--stage=${stage}`)
  }
  if (bizDate) {
    lines.push(`--biz_date=${bizDate}`)
  }
  lines.push(`--tenant_id=${resolveTenantId(tenantId)}`)
  const normalizedDatasourceCode = normalizeDatasourceCode(datasourceCode)
  if (normalizedDatasourceCode) {
    lines.push(`--datasource=${normalizedDatasourceCode}`)
  }
  if (engineHint) {
    lines.push(`--engine_hint=${engineHint}`)
  }
  if (priority) {
    lines.push(`--priority=${priority}`)
  }
  return `${lines.join('\n')}\n`
}

export const buildDefaultQuerySql = ({
  tenantId,
  datasourceCode,
  sqlText = 'SELECT * FROM orders WHERE query_date = :query_date LIMIT :limit'
} = {}) => `${buildSqlCommentHeader({
  tenantId,
  datasourceCode,
  reportCode: 'RPT_SALES_DAILY',
  stage: 'PROD',
  bizDate: '2026-04-27'
})}${sqlText}`

export const buildReportImportRows = ({ datasourceCode } = {}) => {
  const normalizedDatasourceCode = normalizeDatasourceCode(datasourceCode)
  return [
    `RPT_A|Revenue Report|${normalizedDatasourceCode}|PROD|high`,
    `RPT_B|Ops Report|${normalizedDatasourceCode}|PROD|medium`
  ].join('\n')
}
