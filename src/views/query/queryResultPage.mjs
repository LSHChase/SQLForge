const DEFAULT_PAGE_NO = 1
export const DEFAULT_QUERY_RESULT_PAGE_SIZE = 10

const LIST_KEYS = ['items', 'records', 'rows', 'list', 'content', 'resultRows']
const PAGE_NO_KEYS = ['pageNo', 'pageNumber', 'currentPage', 'current', 'pageNum']
const PAGE_SIZE_KEYS = ['pageSize', 'size', 'limit']
const TOTAL_KEYS = ['totalCount', 'total', 'totalRows', 'rowCount', 'count']
const PAGE_COUNT_KEYS = ['pageCount', 'totalPages', 'pages']

const isRecord = value =>
  value !== null &&
  typeof value === 'object' &&
  !Array.isArray(value)

const toPositiveInteger = (value, fallback) => {
  const numberValue = Number(value)
  if (Number.isFinite(numberValue) && numberValue > 0) {
    return Math.floor(numberValue)
  }
  return fallback
}

const toNonNegativeInteger = (value, fallback) => {
  const numberValue = Number(value)
  if (Number.isFinite(numberValue) && numberValue >= 0) {
    return Math.floor(numberValue)
  }
  return fallback
}

const firstNumberValue = (source, keys, fallback) => {
  if (!isRecord(source)) {
    return fallback
  }
  for (const key of keys) {
    if (source[key] !== undefined && source[key] !== null && source[key] !== '') {
      return source[key]
    }
  }
  return fallback
}

const hasAnyKey = (source, keys) =>
  isRecord(source) && keys.some(key => Object.prototype.hasOwnProperty.call(source, key))

const findListValue = source => {
  if (!isRecord(source)) {
    return null
  }
  for (const key of LIST_KEYS) {
    if (Array.isArray(source[key])) {
      return {
        items: source[key],
        source
      }
    }
  }
  return null
}

const looksLikePageEnvelope = source =>
  isRecord(source) &&
  findListValue(source) !== null &&
  (
    hasAnyKey(source, PAGE_NO_KEYS) ||
    hasAnyKey(source, PAGE_SIZE_KEYS) ||
    hasAnyKey(source, TOTAL_KEYS) ||
    hasAnyKey(source, PAGE_COUNT_KEYS)
  )

const normalizeRows = rows => {
  if (!Array.isArray(rows)) {
    return []
  }
  return rows.map((row, index) => {
    if (isRecord(row)) {
      return row
    }
    return {
      rowIndex: index + 1,
      value: row
    }
  })
}

export const stripLeadingSqlComments = sqlText => {
  let remaining = String(sqlText || '').trimStart()
  let consumed = true
  while (consumed && remaining) {
    consumed = false
    remaining = remaining.trimStart()
    if (remaining.startsWith('--')) {
      const nextLineIndex = remaining.indexOf('\n')
      remaining = nextLineIndex === -1 ? '' : remaining.slice(nextLineIndex + 1)
      consumed = true
    } else if (remaining.startsWith('/*')) {
      const endIndex = remaining.indexOf('*/', 2)
      remaining = endIndex === -1 ? '' : remaining.slice(endIndex + 2)
      consumed = true
    }
  }
  return remaining.trimStart()
}

export const isExplainSql = sqlText =>
  /^EXPLAIN\b/i.test(stripLeadingSqlComments(sqlText))

export const isExplainQueryResult = (response, executedSql = '') => {
  const sqlType = String(response?.lightweightParseSummary?.sqlType || '').trim().toUpperCase()
  const actualSql = response?.metadata?.actualSql || executedSql
  return sqlType === 'EXPLAIN' || isExplainSql(actualSql)
}

export const resolveQueryResultKind = (response, executedSql = '') =>
  isExplainQueryResult(response, executedSql) ? 'EXPLAIN_PLAN' : 'DATA_ROWS'

const planCellText = value => {
  if (value === null || value === undefined) {
    return ''
  }
  if (isRecord(value) || Array.isArray(value)) {
    return JSON.stringify(value)
  }
  return String(value)
}

export const resolveExplainPlanText = resultPage => {
  const items = Array.isArray(resultPage?.items) ? resultPage.items : []
  if (!items.length) {
    return ''
  }
  const lines = []
  for (const row of items) {
    if (!isRecord(row)) {
      return ''
    }
    const keys = Object.keys(row)
    if (keys.length !== 1) {
      return ''
    }
    lines.push(planCellText(row[keys[0]]))
  }
  return lines.join('\n').trim()
}

const extractRows = payload => {
  if (Array.isArray(payload)) {
    if (payload.length === 1 && looksLikePageEnvelope(payload[0])) {
      const nested = findListValue(payload[0])
      return {
        items: normalizeRows(nested.items),
        metaSource: nested.source,
        remotePaged: true
      }
    }
    return {
      items: normalizeRows(payload),
      metaSource: null,
      remotePaged: false
    }
  }

  const listValue = findListValue(payload)
  if (listValue) {
    return {
      items: normalizeRows(listValue.items),
      metaSource: listValue.source,
      remotePaged: looksLikePageEnvelope(listValue.source)
    }
  }

  return {
    items: [],
    metaSource: null,
    remotePaged: false
  }
}

export const normalizeQueryResultPage = (response, fallbackPageSize = DEFAULT_QUERY_RESULT_PAGE_SIZE) => {
  const payload = isRecord(response) && response.rows !== undefined ? response.rows : response
  const extracted = extractRows(payload)
  const metaSource = extracted.metaSource || (isRecord(response) ? response : null)
  const fallbackTotal = extracted.items.length
  const pageSize = toPositiveInteger(
    firstNumberValue(metaSource, PAGE_SIZE_KEYS, fallbackPageSize),
    fallbackPageSize
  )
  const totalCount = toNonNegativeInteger(
    firstNumberValue(metaSource, TOTAL_KEYS, fallbackTotal),
    fallbackTotal
  )
  const pageNo = toPositiveInteger(
    firstNumberValue(metaSource, PAGE_NO_KEYS, DEFAULT_PAGE_NO),
    DEFAULT_PAGE_NO
  )
  const pageCount = toNonNegativeInteger(
    firstNumberValue(metaSource, PAGE_COUNT_KEYS, Math.ceil(totalCount / pageSize)),
    totalCount ? Math.ceil(totalCount / pageSize) : 0
  )

  return {
    items: extracted.items,
    pageNo,
    pageSize,
    totalCount,
    pageCount,
    remotePaged: extracted.remotePaged
  }
}

export const resolveVisibleQueryRows = (resultPage, pager = {}) => {
  const items = Array.isArray(resultPage?.items) ? resultPage.items : []
  if (resultPage?.remotePaged) {
    return items
  }
  const pageNo = toPositiveInteger(pager.pageNo, DEFAULT_PAGE_NO)
  const pageSize = toPositiveInteger(pager.pageSize, resultPage?.pageSize || DEFAULT_QUERY_RESULT_PAGE_SIZE)
  const start = (pageNo - 1) * pageSize
  return items.slice(start, start + pageSize)
}
