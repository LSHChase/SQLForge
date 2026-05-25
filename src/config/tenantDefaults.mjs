export const DEFAULT_TENANT_ID = 'system'
export const SAMPLE_TENANT_ID = 'tenant-a'
export const SAMPLE_DATASOURCE_CODE = 'hetu_main'

export const normalizeTenantId = value => String(value || '').trim()

export const resolveTenantId = (...candidates) => {
  for (const candidate of candidates) {
    const normalized = normalizeTenantId(candidate)
    if (normalized) {
      return normalized
    }
  }
  return DEFAULT_TENANT_ID
}
