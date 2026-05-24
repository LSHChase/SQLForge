import { computed, ref, watch } from 'vue'

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

  const rawSqlText = computed(() => {
    return typeof sqlTextRef === 'function' ? sqlTextRef() : sqlTextRef.value
  })

  // Watch the SQL text to automatically capture parameter variables like :query_date
  watch(
    rawSqlText,
    (newSql) => {
      if (!newSql) return
      
      const matches = String(newSql).match(/:([a-zA-Z_][a-zA-Z0-9_]*)/g)
      if (!matches) return

      const uniqueKeys = [...new Set(matches.map(m => m.slice(1)))]
      const currentKeys = new Set(parameterRows.value.map(row => row.key).filter(Boolean))

      uniqueKeys.forEach(key => {
        if (!currentKeys.has(key)) {
          // If there is an empty parameter placeholder row, overwrite it
          const emptyRow = parameterRows.value.find(row => !row.key)
          if (emptyRow) {
            emptyRow.key = key
          } else {
            parameterRows.value.push({
              id: nextParameterId.value,
              key: key,
              value: ''
            })
            nextParameterId.value += 1
          }
        }
      })
    },
    { immediate: true }
  )

  const boundSqlPreview = computed(() => {
    let preview = rawSqlText.value
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
