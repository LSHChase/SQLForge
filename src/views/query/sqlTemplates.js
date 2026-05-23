export const sqlTemplates = [
  {
    key: 'report',
    label: '--report_code + biz_date',
    content: '--report_code=RPT_SALES_DAILY\n--stage=PROD\n--biz_date=2026-04-27\n--tenant_id=tenant-a\n--datasource=hetu_main\n--engine_hint=HETU\n--priority=high\n'
  },
  {
    key: 'explain',
    label: 'Explain template',
    content: '--report_code=RPT_EXPLAIN_SAMPLE\n--stage=PROD\n--tenant_id=tenant-a\nEXPLAIN SELECT * FROM orders WHERE query_date = :query_date\n'
  },
  {
    key: 'fallback',
    label: 'Recovery template',
    content: '--report_code=RPT_RECOVERY_CHECK\n--stage=PROD\n--tenant_id=tenant-a\n--datasource=hive_lakehouse\nSELECT count(1) FROM orders WHERE query_date = :query_date\n'
  }
]

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
