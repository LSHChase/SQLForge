import { computed, ref } from 'vue'

export function useQueryParameters(sqlTextRef) {
  const parameterRows = ref([
    { id: 1, key: 'query_date', value: '2026-04-27' },
    { id: 2, key: 'limit', value: '100' },
    { id: 3, key: 'biz_date', value: '2026-04-27' }
  ])

  const nextParameterId = ref(4)

  const addParameter = () => {
    parameterRows.value.push({
      id: nextParameterId.value,
      key: '',
      value: ''
    })
    nextParameterId.value += 1
  }

  const removeParameter = rowId => {
    if (parameterRows.value.length === 1) {
      parameterRows.value[0].key = ''
      parameterRows.value[0].value = ''
      return
    }
    parameterRows.value = parameterRows.value.filter(item => item.id !== rowId)
  }

  const parameterSnapshot = computed(() => {
    const snapshot = {}
    for (const item of parameterRows.value) {
      const key = String(item.key || '').trim()
      if (key) {
        snapshot[key] = item.value
      }
    }
    return snapshot
  })

  const boundSqlPreview = computed(() => {
    let preview = typeof sqlTextRef === 'function' ? sqlTextRef() : sqlTextRef.value
    preview = String(preview || '')
    for (const [key, value] of Object.entries(parameterSnapshot.value)) {
      preview = preview.replaceAll(`:${key}`, `'${value}'`)
    }
    return preview
  })

  return {
    parameterRows,
    nextParameterId,
    addParameter,
    removeParameter,
    parameterSnapshot,
    boundSqlPreview
  }
}
