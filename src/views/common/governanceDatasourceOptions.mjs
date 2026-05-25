const hasValue = value => !(value === null || value === undefined || String(value).trim() === '')

const normalizeEngineType = value => String(value || 'UNKNOWN').trim().toUpperCase()

export const normalizeGovernanceDatasource = record => {
  const engineType = normalizeEngineType(record?.engineType || record?.datasourceType)
  const datasourceCode = String(record?.datasourceCode || record?.datasourceId || '').trim()
  return {
    id: record?.datasourceId || `${engineType}-${datasourceCode}`,
    label: record?.datasourceName
      ? `${datasourceCode} · ${record.datasourceName}`
      : `${datasourceCode}${record?.stage ? `.${record.stage}` : ''}`,
    datasourceType: engineType,
    engineType,
    datasourceCode,
    stage: record?.stage,
    raw: record
  }
}

export const groupGovernanceDatasources = records => {
  const grouped = new Map()
  ;(Array.isArray(records) ? records : [])
    .map(normalizeGovernanceDatasource)
    .filter(item => hasValue(item.datasourceCode))
    .forEach(item => {
      if (!grouped.has(item.engineType)) {
        grouped.set(item.engineType, [])
      }
      grouped.get(item.engineType).push(item)
    })
  return grouped
}

export const flattenDatasourceGroups = grouped => {
  const items = []
  grouped.forEach(children => {
    items.push(...children)
  })
  return items
}

export const buildDatasourceTypeOptions = grouped => [
  { label: 'AUTO', value: 'AUTO' },
  ...Array.from(grouped.keys()).map(engineType => ({
    label: engineType,
    value: engineType
  }))
]

export const findDatasourceOption = (grouped, datasourceCode, engineType = '') => {
  const normalizedCode = String(datasourceCode || '').trim()
  if (!normalizedCode) {
    return null
  }
  const candidates =
    engineType && engineType !== 'AUTO'
      ? grouped.get(String(engineType).toUpperCase()) || []
      : flattenDatasourceGroups(grouped)
  return candidates.find(item => item.datasourceCode === normalizedCode) || null
}

export const firstDatasourceForEngine = (grouped, engineType = '') => {
  if (engineType && engineType !== 'AUTO') {
    return (grouped.get(String(engineType).toUpperCase()) || [])[0] || null
  }
  return flattenDatasourceGroups(grouped)[0] || null
}
