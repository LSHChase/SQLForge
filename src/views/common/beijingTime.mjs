export const BEIJING_TIME_ZONE = 'Asia/Shanghai'
export const BEIJING_TIME_ZONE_LABEL = '北京时间'

const DEFAULT_EMPTY_TEXT = '-'
const EXPLICIT_ZONE_PATTERN = /(Z|[+-]\d{2}:?\d{2})$/i
const NAIVE_DATE_TIME_PATTERN =
  /^(\d{4})[-/](\d{2})[-/](\d{2})[ T](\d{2}):(\d{2})(?::(\d{2})(?:\.\d{1,9})?)?$/

const formatterCache = new Map()

const formatterFor = precision => {
  const normalizedPrecision = precision === 'minute' ? 'minute' : 'second'
  if (!formatterCache.has(normalizedPrecision)) {
    formatterCache.set(
      normalizedPrecision,
      new Intl.DateTimeFormat('zh-CN', {
        timeZone: BEIJING_TIME_ZONE,
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        ...(normalizedPrecision === 'second' ? { second: '2-digit' } : {}),
        hour12: false,
        hourCycle: 'h23'
      })
    )
  }
  return formatterCache.get(normalizedPrecision)
}

const partsMap = (date, precision) =>
  Object.fromEntries(
    formatterFor(precision)
      .formatToParts(date)
      .filter(part => part.type !== 'literal')
      .map(part => [part.type, part.value])
  )

const formatDate = (date, precision) => {
  const parts = partsMap(date, precision)
  const second = precision === 'minute' ? '' : `:${parts.second || '00'}`
  return `${parts.year}-${parts.month}-${parts.day} ${parts.hour}:${parts.minute}${second}`
}

const normalizeNaiveDateTime = (value, precision) => {
  const match = String(value || '').trim().match(NAIVE_DATE_TIME_PATTERN)
  if (!match) {
    return ''
  }
  const [, year, month, day, hour, minute, second = '00'] = match
  return precision === 'minute'
    ? `${year}-${month}-${day} ${hour}:${minute}`
    : `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

const parseNaiveBeijingEpoch = value => {
  const match = String(value || '').trim().match(NAIVE_DATE_TIME_PATTERN)
  if (!match) {
    return 0
  }
  const [, year, month, day, hour, minute, second = '00'] = match
  return Date.UTC(
    Number(year),
    Number(month) - 1,
    Number(day),
    Number(hour) - 8,
    Number(minute),
    Number(second)
  )
}

const parseDate = value => {
  if (value instanceof Date) {
    return value
  }
  const numeric = Number(value)
  if (!Number.isNaN(numeric) && numeric > 0) {
    return new Date(numeric)
  }
  const parsed = new Date(String(value))
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

export const formatBeijingTimestamp = (value, options = {}) => {
  if (value === null || value === undefined || value === '') {
    return options.emptyText || DEFAULT_EMPTY_TEXT
  }
  const precision = options.precision === 'minute' ? 'minute' : 'second'
  const rawValue = String(value).trim()
  if (rawValue && !EXPLICIT_ZONE_PATTERN.test(rawValue)) {
    const normalized = normalizeNaiveDateTime(rawValue, precision)
    if (normalized) {
      return normalized
    }
  }
  const date = parseDate(value)
  if (!date) {
    return rawValue.replace('T', ' ').slice(0, precision === 'minute' ? 16 : 19)
  }
  return formatDate(date, precision)
}

export const formatBeijingMinute = value =>
  formatBeijingTimestamp(value, { precision: 'minute' })

export const nowBeijingTimestamp = () => formatBeijingTimestamp(Date.now())

export const toBeijingEpochMilli = value => {
  if (value === null || value === undefined || value === '') {
    return 0
  }
  const numeric = Number(value)
  if (!Number.isNaN(numeric) && numeric > 0) {
    return numeric
  }
  const rawValue = String(value).trim()
  if (rawValue && !EXPLICIT_ZONE_PATTERN.test(rawValue)) {
    const naiveEpoch = parseNaiveBeijingEpoch(rawValue)
    if (naiveEpoch > 0) {
      return naiveEpoch
    }
  }
  const date = parseDate(value)
  return date ? date.getTime() : 0
}
