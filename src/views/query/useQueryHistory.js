import { ref } from 'vue'

export function useQueryHistory() {
  const executionHistory = ref([])

  const recordExecution = (title, status, mode) => {
    executionHistory.value = [
      {
        id: `${Date.now()}`,
        title: title || '-',
        status: status || 'UNKNOWN',
        mode: mode || '-'
      },
      ...executionHistory.value
    ].slice(0, 6)
  }

  return {
    executionHistory,
    recordExecution
  }
}
