import { inject } from 'vue'

export const PARSE_RECORD_CONTEXT_KEY = Symbol('ParseRecordContext')

export function useParseRecordContext() {
  const context = inject(PARSE_RECORD_CONTEXT_KEY)
  if (!context) {
    throw new Error('ParseRecord context is not available')
  }
  return context
}
