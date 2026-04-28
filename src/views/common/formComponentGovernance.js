export const knownTenantOptions = [
  { label: 'system', value: 'system' },
  { label: 'tenant-a', value: 'tenant-a' },
  { label: 'tenant-b', value: 'tenant-b' }
]

export const stageOptions = [
  { label: 'DEV', value: 'DEV' },
  { label: 'TEST', value: 'TEST' },
  { label: 'PREPROD', value: 'PREPROD' },
  { label: 'PROD', value: 'PROD' }
]

export const connectionModeOptions = [
  { label: 'JDBC', value: 'JDBC' },
  { label: 'REST', value: 'REST' },
  { label: 'CLIENT', value: 'CLIENT' }
]

export const authModeOptions = [
  { label: 'NONE', value: 'NONE' },
  { label: 'PASSWORD', value: 'PASSWORD' },
  { label: 'TOKEN', value: 'TOKEN' },
  { label: 'AK_SK', value: 'AK_SK' }
]

export const credentialModeOptions = [
  { label: 'REF', value: 'REF' },
  { label: 'SECRET', value: 'SECRET' }
]

export const sourceTypeOptions = [
  { label: 'REST', value: 'REST' },
  { label: 'JDBC', value: 'JDBC' }
]

export const httpMethodOptions = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' }
]

export const dispatchTypeOptions = [
  { label: 'PULL', value: 'PULL' },
  { label: 'PUSH', value: 'PUSH' }
]

export const engineOptions = [
  { label: 'HETU', value: 'HETU' },
  { label: 'HIVE', value: 'HIVE' },
  { label: 'SPARK', value: 'SPARK' }
]

export const ackModeOptions = [
  { label: 'MANUAL', value: 'MANUAL' },
  { label: 'AUTO', value: 'AUTO' }
]

export const retryStrategyOptions = [
  { label: 'EXPONENTIAL_BACKOFF', value: 'EXPONENTIAL_BACKOFF' },
  { label: 'FIXED_DELAY', value: 'FIXED_DELAY' },
  { label: 'NONE', value: 'NONE' }
]

const hasValue = value => !(value === null || value === undefined || String(value).trim() === '')

export const uniqueOptions = options => {
  const seen = new Set()
  return options
    .filter(option => hasValue(option?.value))
    .filter(option => {
      const key = String(option.value)
      if (seen.has(key)) {
        return false
      }
      seen.add(key)
      return true
    })
}

export const withCurrentOption = (options, currentValue) => {
  const normalized = uniqueOptions(options)
  if (!hasValue(currentValue) || normalized.some(option => option.value === currentValue)) {
    return normalized
  }
  return [{ label: String(currentValue), value: currentValue }, ...normalized]
}

export const buildTenantOptions = (...sources) => {
  const options = [...knownTenantOptions]
  sources.flat().forEach(source => {
    if (hasValue(source)) {
      if (typeof source === 'string') {
        options.push({ label: source, value: source })
      } else if (hasValue(source.tenantId)) {
        options.push({ label: source.tenantName || source.tenantId, value: source.tenantId })
      }
    }
  })
  return uniqueOptions(options)
}

export const buildDatasourceOptions = datasources =>
  uniqueOptions(
    (Array.isArray(datasources) ? datasources : []).map(item => ({
      label: item.datasourceName
        ? `${item.datasourceCode || item.datasourceId} · ${item.datasourceName}`
        : item.datasourceCode || item.datasourceId,
      value: item.datasourceCode || item.datasourceId
    }))
  )
