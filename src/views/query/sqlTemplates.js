import { buildSqlCommentHeader } from '../../config/tenantDefaults.mjs'

const templateContext = context => ({
  tenantId: context?.tenantId,
  datasourceCode: context?.datasourceCode
})

export const buildSqlTemplates = (context = {}) => [
  {
    key: 'report',
    label: '--report_code + biz_date',
    content: buildSqlCommentHeader({
      ...templateContext(context),
      reportCode: 'RPT_SALES_DAILY',
      stage: 'PROD',
      bizDate: '2026-04-27',
      engineHint: 'HETU',
      priority: 'high'
    })
  },
  {
    key: 'explain',
    label: 'Explain template',
    content: `${buildSqlCommentHeader({
      ...templateContext(context),
      reportCode: 'RPT_EXPLAIN_SAMPLE',
      stage: 'PROD'
    })}EXPLAIN SELECT * FROM orders WHERE query_date = :query_date\n`
  },
  {
    key: 'fallback',
    label: 'Recovery template',
    content: `${buildSqlCommentHeader({
      ...templateContext(context),
      reportCode: 'RPT_RECOVERY_CHECK',
      stage: 'PROD'
    })}SELECT count(1) FROM orders WHERE query_date = :query_date\n`
  }
]

export const sqlTemplates = buildSqlTemplates()

export const sqlLibrary = [
  {
    key: 'recent-1',
    type: 'recent',
    title: 'Recent · sales daily',
    summary: 'Revenue rollup with query_date binding',
    sqlText: "--report_code=RPT_SALES_DAILY\nSELECT order_id, revenue FROM orders WHERE query_date = :query_date LIMIT :limit"
  },
  {
    key: 'recent-2',
    type: 'recent',
    title: 'Recent · logic view',
    summary: 'Business view sample',
    sqlText: '--report_code=RPT_CUSTOMER_360\nSELECT * FROM customer_360 WHERE biz_date = :biz_date LIMIT 50'
  },
  {
    key: 'favorite-1',
    type: 'favorite',
    title: 'Favorite · benchmark candidate',
    summary: 'Candidate SQL for benchmark and explain',
    sqlText: '--report_code=RPT_BENCHMARK_SAMPLE\nSELECT region, sum(revenue) FROM orders GROUP BY region'
  }
]
