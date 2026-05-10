import { GOVERNANCE_COMPENSATION_TRACE_PREFIX } from '../../services/runtimeGateApi'

export const hasDisplayValue = value =>
  !(value === null || value === undefined || String(value).trim() === '')

export const displayValue = value => {
  if (!hasDisplayValue(value)) {
    return '-'
  }
  return String(value)
}

export const formatTraceTimestamp = value => {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ')
}

export const isCompensationTrace = traceId =>
  String(traceId || '').startsWith(GOVERNANCE_COMPENSATION_TRACE_PREFIX)

export const resolveTraceRepairSignal = trace => {
  if (isCompensationTrace(trace?.traceId)) {
    return 'COMPENSATION_TRACE'
  }
  if ((trace?.exportRecordCount || 0) > 0 || hasDisplayValue(trace?.reportId)) {
    return 'REPORT_WRITEBACK'
  }
  if (trace?.degraded === true) {
    return 'DEGRADED_RECOVERY'
  }
  const status = String(trace?.latestStatus || '').toUpperCase()
  if ((trace?.nonSuccessEventCount || 0) > 0 || (status && status !== 'SUCCESS' && status !== 'SUCCEEDED')) {
    return 'FAILURE_CHAIN'
  }
  return 'STEADY_STATE'
}

export const isSuccessfulTraceStatus = status =>
  ['SUCCESS', 'SUCCEEDED'].includes(String(status || '').toUpperCase())

export const toKebabCase = value =>
  String(value || '').replace(/[A-Z]/g, match => `-${match.toLowerCase()}`)

export const eventIdentity = event =>
  event?.id ||
  event?.eventId ||
  [event?.serviceCode, event?.operationType || event?.eventType, event?.requestId, event?.traceId]
    .filter(Boolean)
    .join('-')

export const eventOccurredAt = event => event?.createTime || event?.occurredAt || event?.eventTime || ''

