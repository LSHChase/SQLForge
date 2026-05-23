import fs from 'node:fs'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

const root = process.cwd()
const viewPath = path.join(root, 'src/views/query/SqlQueryView.vue')
const source = fs.readFileSync(viewPath, 'utf8')
const helperPath = path.join(root, 'src/views/query/queryResultPage.mjs')
const {
  normalizeQueryResultPage,
  resolveVisibleQueryRows
} = await import(pathToFileURL(helperPath).href)

const requiredTokens = [
  'class="query-workbench__grid"',
  'class="query-rail surface-card"',
  'class="editor-rail surface-card"',
  'class="result-rail surface-card"',
  'data-testid="query-flow-page"',
  'data-testid="query-flow-submit"',
  'data-testid="query-flow-submit-recovery"',
  'governance summary',
  'result tabs',
  'el-tree',
  'el-tabs',
  'Bound SQL preview',
  'normalizeQueryResultPage',
  'data-testid="query-result-table"',
  'data-testid="query-result-pagination"',
  'class="table-footer"',
  'class="pagination-cluster"'
]

const missing = requiredTokens.filter(token => !source.includes(token))

const directRowsPage = normalizeQueryResultPage({
  rows: [
    { orderId: 'order-1', amount: 10 },
    { orderId: 'order-2', amount: 20 },
    { orderId: 'order-3', amount: 30 }
  ]
}, 2)
if (directRowsPage.totalCount !== 3) {
  missing.push('direct rows normalization totalCount')
}
if (resolveVisibleQueryRows(directRowsPage, { pageNo: 2, pageSize: 2 })[0]?.orderId !== 'order-3') {
  missing.push('direct rows client-side pagination')
}

const pagedRowsPage = normalizeQueryResultPage({
  rows: {
    records: [
      { orderId: 'order-11', amount: 110 },
      { orderId: 'order-12', amount: 120 }
    ],
    pageNo: 2,
    pageSize: 2,
    totalCount: 5,
    pageCount: 3
  }
})
if (
  pagedRowsPage.items.length !== 2 ||
  pagedRowsPage.pageNo !== 2 ||
  pagedRowsPage.totalCount !== 5 ||
  !pagedRowsPage.remotePaged
) {
  missing.push('paged records normalization')
}

const nestedPagedRowsPage = normalizeQueryResultPage({
  rows: [
    {
      records: [
        { orderId: 'order-21' },
        { orderId: 'order-22' }
      ],
      current: 1,
      size: 2,
      total: 2,
      pages: 1
    }
  ]
})
if (nestedPagedRowsPage.items.length !== 2 || nestedPagedRowsPage.items[1]?.orderId !== 'order-22') {
  missing.push('nested paged rows normalization')
}

if (missing.length > 0) {
  console.error('Query workbench contract check failed.')
  for (const token of missing) {
    console.error(`- missing token: ${token}`)
  }
  process.exit(1)
}

console.log('Query workbench contract check passed.')
