import { computed, reactive, ref } from 'vue'
import {
  formatRuntimeError,
  getGovernanceDatasources,
  getGovernanceQueryHistoryPage
} from '../../services/runtimeGateApi'
import { resolveProtectedTenantId } from '../../config/tenantDefaults.mjs'
import {
  buildDatasourceOptions,
  buildTenantOptions,
  withCurrentOption
} from '../common/formComponentGovernance'

const DEFAULT_HISTORY_CONTEXT_TENANT_ID = resolveProtectedTenantId()
const DEFAULT_PAGE_SIZE = 10

const DEFAULT_SEARCH_FORM = Object.freeze({
  tenantId: '',
  reportCode: '',
  datasourceCode: '',
  stage: '',
  bizDate: '',
  queryDateStart: '',
  queryDateEnd: '',
  status: '',
  logicalObjectType: '',
  accessChannel: '',
  engine: '',
  submittedBy: '',
  submittedStart: '',
  submittedEnd: '',
  cacheHit: '',
  rewriteApplied: '',
  hasRewriteRecord: '',
  rewriteValidationStatus: '',
  rewriteSourceType: '',
  recommendationId: '',
  accelerationApplied: '',
  parameterizedSql: '',
  sortBy: '',
  sortOrder: ''
})

const normalizeQueryValue = value => (Array.isArray(value) ? String(value[0] || '').trim() : String(value || '').trim())

const parseBooleanFilter = value => {
  if (value === 'true') {
    return true
  }
  if (value === 'false') {
    return false
  }
  return undefined
}

const normalizePositiveInteger = (value, fallback) => {
  const numberValue = Number(value)
  if (Number.isFinite(numberValue) && numberValue > 0) {
    return Math.floor(numberValue)
  }
  return fallback
}

const normalizePageCount = (payloadPageCount, total, pageSize) => {
  const pageCount = Number(payloadPageCount)
  if (Number.isFinite(pageCount) && pageCount >= 0) {
    return Math.floor(pageCount)
  }
  if (!total || !pageSize) {
    return 0
  }
  return Math.ceil(total / pageSize)
}

const createSearchForm = () => ({ ...DEFAULT_SEARCH_FORM })

export const useSqlHistoryList = ({ routeTenantId, historyType }) => {
  const searchForm = reactive(createSearchForm())
  const pageInfo = reactive({
    currentPage: 1,
    pageSize: DEFAULT_PAGE_SIZE,
    total: 0,
    pageCount: 0
  })
  const loadingList = ref(false)
  const tablePage = ref(null)
  const listErrorMessage = ref('')
  const listStatus = ref('idle')
  const lastQueryAt = ref('')
  const datasourceOptions = ref([])
  const datasourceOptionsLoadFailed = ref(false)

  const routeTenantValue = computed(() => normalizeQueryValue(routeTenantId.value))
  const requestTenantId = computed(
    () => normalizeQueryValue(searchForm.tenantId)
      || routeTenantValue.value
      || DEFAULT_HISTORY_CONTEXT_TENANT_ID
  )
  const tableRows = computed(() => tablePage.value?.items || [])
  const classificationSummary = computed(() => tablePage.value?.classificationSummary || {})
  const tenantOptions = computed(() => buildTenantOptions(searchForm.tenantId, tableRows.value))
  const currentTenantOptions = computed(() => withCurrentOption(tenantOptions.value, searchForm.tenantId))
  const currentDatasourceOptions = computed(() =>
    withCurrentOption(datasourceOptions.value, searchForm.datasourceCode)
  )
  const pageWindow = computed(() => {
    if (!pageInfo.total || !pageInfo.pageCount) {
      return { current: 0, total: 0 }
    }
    return { current: pageInfo.currentPage, total: pageInfo.pageCount }
  })

  const applyPageInfo = payload => {
    const nextPageSize = normalizePositiveInteger(payload?.pageSize, pageInfo.pageSize || DEFAULT_PAGE_SIZE)
    const total = Math.max(0, Number(payload?.totalCount ?? payload?.total ?? 0) || 0)
    const pageCount = normalizePageCount(payload?.pageCount, total, nextPageSize)
    const fallbackPageNo = total > 0 ? pageInfo.currentPage || 1 : 1
    const nextPageNo = normalizePositiveInteger(payload?.pageNo, fallbackPageNo)

    pageInfo.pageSize = nextPageSize
    pageInfo.total = total
    pageInfo.pageCount = pageCount
    pageInfo.currentPage = pageCount > 0 ? Math.min(nextPageNo, pageCount) : 1
  }

  const loadDatasourceOptions = async () => {
    datasourceOptionsLoadFailed.value = false
    try {
      const nextDatasources = await getGovernanceDatasources(requestTenantId.value, {
        requestPrefix: 'frontend-sql-history-datasource-options'
      })
      datasourceOptions.value = buildDatasourceOptions(nextDatasources)
    } catch {
      datasourceOptions.value = withCurrentOption([], searchForm.datasourceCode)
      datasourceOptionsLoadFailed.value = true
    }
  }

  const loadPage = async () => {
    loadingList.value = true
    listStatus.value = 'loading'
    listErrorMessage.value = ''
    try {
      tablePage.value = await getGovernanceQueryHistoryPage(
        {
          tenantId: normalizeQueryValue(searchForm.tenantId),
          requestTenantId: requestTenantId.value,
          historyType,
          reportCode: searchForm.reportCode,
          datasourceCode: searchForm.datasourceCode,
          stage: searchForm.stage,
          bizDate: searchForm.bizDate,
          queryDateStart: searchForm.queryDateStart,
          queryDateEnd: searchForm.queryDateEnd,
          status: searchForm.status,
          logicalObjectType: searchForm.logicalObjectType,
          accessChannel: searchForm.accessChannel,
          engine: searchForm.engine,
          submittedBy: searchForm.submittedBy,
          submittedStart: searchForm.submittedStart,
          submittedEnd: searchForm.submittedEnd,
          cacheHit: parseBooleanFilter(searchForm.cacheHit),
          rewriteApplied: parseBooleanFilter(searchForm.rewriteApplied),
          hasRewriteRecord: parseBooleanFilter(searchForm.hasRewriteRecord),
          rewriteValidationStatus: searchForm.rewriteValidationStatus,
          rewriteSourceType: searchForm.rewriteSourceType,
          recommendationId: searchForm.recommendationId,
          accelerationApplied: parseBooleanFilter(searchForm.accelerationApplied),
          parameterizedSql: parseBooleanFilter(searchForm.parameterizedSql),
          sortBy: searchForm.sortBy,
          sortOrder: searchForm.sortOrder,
          pageNo: pageInfo.currentPage,
          pageSize: pageInfo.pageSize
        },
        {
          requestPrefix: 'frontend-sql-history-page'
        }
      )
      applyPageInfo(tablePage.value)
      listStatus.value = 'success'
      lastQueryAt.value = new Date().toISOString()
    } catch (error) {
      tablePage.value = null
      pageInfo.total = 0
      pageInfo.pageCount = 0
      listStatus.value = 'error'
      lastQueryAt.value = new Date().toISOString()
      listErrorMessage.value = formatRuntimeError(error)
    } finally {
      loadingList.value = false
    }
  }

  const initializeList = async () => {
    await Promise.all([loadDatasourceOptions(), loadPage()])
  }

  const search = async () => {
    pageInfo.currentPage = 1
    await Promise.all([loadDatasourceOptions(), loadPage()])
  }

  const clearFilters = async () => {
    Object.assign(searchForm, createSearchForm())
    await search()
  }

  const handlePageChange = async currentPage => {
    pageInfo.currentPage = currentPage
    await loadPage()
  }

  const handlePageSizeChange = async pageSize => {
    pageInfo.pageSize = pageSize
    pageInfo.currentPage = 1
    await loadPage()
  }

  const syncRouteTenant = () => {
    searchForm.tenantId = routeTenantValue.value
  }

  return {
    searchForm,
    pageInfo,
    loadingList,
    tableRows,
    classificationSummary,
    listErrorMessage,
    listStatus,
    lastQueryAt,
    requestTenantId,
    pageWindow,
    currentTenantOptions,
    currentDatasourceOptions,
    datasourceOptionsLoadFailed,
    initializeList,
    loadPage,
    search,
    clearFilters,
    handlePageChange,
    handlePageSizeChange,
    syncRouteTenant,
    normalizeQueryValue
  }
}
