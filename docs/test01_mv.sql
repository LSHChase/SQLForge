# 推荐sql：
-- YH_QUERYID=1d516d3b74da409a9f2a5ed3873e5c7e
-- YH_RPTID=SZ_0000003772
-- YH_RPTINSTID=百万客户净增报表20260521154649-1556
-- YH_RPTORG=41H006
-- YH_RPTVIEWUSER=user_4a7d873e0c8d404d839494be151bd70a_850001953VIEW
-- YH_RPTVIEWROLELIST=everyone_role
-- YH_RPTVIEWMODE=PUBLISH
-- YH_SQLSENDTIME=2026-05-21 15:51:40
-- YH_RPTWIDGETTYPE=Table
-- YH_RPTWIDGETNAME=图表1
-- YH_REFDATASET=万象发布管理WXFBGL/SZ_0000003772_20260518100302_a09c1d1217954c88bffc19c00738901/个金/百万客户净增报表
-- YH_DATE_CC=FALSE
-- YH_RPTSEARCHMODE=HETU
WITH
  raw_customer_snapshot AS (
    SELECT
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_0 AS org_no_0,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_1 AS org_no_1,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_2 AS org_no_2,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_3 AS org_no_3,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_4 AS org_no_4,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_5 AS org_no_5,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_6 AS org_no_6,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_7 AS org_no_7,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_SNAM_2 AS org_name_2,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_SNAM_3 AS org_name_3,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_SNAM_4 AS org_name_4,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_LVL AS org_level,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_2 AS org_level2_no,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_SNAM_2 AS org_level2_name,
      CASE BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_4
        WHEN '410003' THEN '41H002'
        ELSE BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_3
      END AS branch_org_no,
      CASE BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_4
        WHEN '410003' THEN '深圳市分行营业部'
        ELSE BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_SNAM_3
      END AS branch_org_name,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__CUST_NO AS customer_no,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__DTE AS snapshot_date,
      SUM(
        BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__AUM_MAVER_BAL
      ) AS snapshot_aum
    FROM
      "BI_HQX00_V".BIM_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM
    WHERE
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_LVL = 4
      AND (
        BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_0 = '41H006'
        OR BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_1 = '41H006'
        OR BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_2 = '41H006'
        OR BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_3 = '41H006'
        OR BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_4 = '41H006'
        OR BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_5 = '41H006'
        OR BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_6 = '41H006'
        OR BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_7 = '41H006'
      )
      AND BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__DTE IN ('20260430', '20260519')
    GROUP BY
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_0,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_1,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_2,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_3,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_4,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_5,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_6,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_NO_7,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_SNAM_2,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_SNAM_3,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_SNAM_4,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__ORG_NO__BIO_PB_W_00_BI_STD_ORG__ORG_LVL,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__CUST_NO,
      BIO_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM__DTE
  ),
  report_customer_snapshot AS (
    SELECT
      org_level2_no AS report_org_no,
      org_level2_name AS report_org_name,
      '深圳市分行' AS report_org_label,
      customer_no,
      snapshot_date,
      SUM(snapshot_aum) AS snapshot_aum
    FROM
      raw_customer_snapshot
    GROUP BY
      org_level2_no,
      org_level2_name,
      customer_no,
      snapshot_date
    UNION ALL
    SELECT
      branch_org_no AS report_org_no,
      branch_org_name AS report_org_name,
      branch_org_name AS report_org_label,
      customer_no,
      snapshot_date,
      SUM(snapshot_aum) AS snapshot_aum
    FROM
      raw_customer_snapshot
    GROUP BY
      branch_org_no,
      branch_org_name,
      customer_no,
      snapshot_date
  ),
  customer_pair AS (
    SELECT
      report_org_no,
      report_org_name,
      report_org_label,
      customer_no,
      SUM(
        CASE
          WHEN snapshot_date = '20260430' THEN snapshot_aum
          ELSE 0
        END
      ) AS base_aum,
      SUM(
        CASE
          WHEN snapshot_date = '20260519' THEN snapshot_aum
          ELSE 0
        END
      ) AS current_aum
    FROM
      report_customer_snapshot
    GROUP BY
      report_org_no,
      report_org_name,
      report_org_label,
      customer_no
  ),
  org_customer_pair AS (
    SELECT
      report_org_no,
      customer_no,
      SUM(
        CASE
          WHEN snapshot_date = '20260430' THEN snapshot_aum
          ELSE 0
        END
      ) AS base_aum,
      SUM(
        CASE
          WHEN snapshot_date = '20260519' THEN snapshot_aum
          ELSE 0
        END
      ) AS current_aum
    FROM
      report_customer_snapshot
    GROUP BY
      report_org_no,
      customer_no
  ),
  base_100_anchor AS (
    SELECT
      report_org_no,
      report_org_name,
      report_org_label,
      COUNT(DISTINCT customer_no) AS base_100
    FROM
      customer_pair
    WHERE
      base_aum >= 1000000
    GROUP BY
      report_org_no,
      report_org_name,
      report_org_label
  ),
  metric_by_org AS (
    SELECT
      report_org_no,
      NULLIF(
        COUNT(
          DISTINCT CASE
            WHEN snapshot_date = '20260519'
            AND snapshot_aum >= 1000000 THEN customer_no
          END
        ),
        0
      ) AS current_100,
      NULLIF(
        COUNT(
          DISTINCT CASE
            WHEN snapshot_date = '20260430'
            AND snapshot_aum >= 1000000
            AND snapshot_aum < 6000000 THEN customer_no
          END
        ),
        0
      ) AS base_100_600,
      NULLIF(
        COUNT(
          DISTINCT CASE
            WHEN snapshot_date = '20260519'
            AND snapshot_aum >= 1000000
            AND snapshot_aum < 6000000 THEN customer_no
          END
        ),
        0
      ) AS current_100_600,
      NULLIF(
        COUNT(
          DISTINCT CASE
            WHEN snapshot_date = '20260430'
            AND snapshot_aum >= 6000000 THEN customer_no
          END
        ),
        0
      ) AS base_600,
      NULLIF(
        COUNT(
          DISTINCT CASE
            WHEN snapshot_date = '20260519'
            AND snapshot_aum >= 6000000 THEN customer_no
          END
        ),
        0
      ) AS current_600
    FROM
      report_customer_snapshot
    GROUP BY
      report_org_no
  ),
  growth_by_org AS (
    SELECT
      report_org_no,
      NULLIF(
        COUNT(
          DISTINCT CASE
            WHEN base_aum < 1000000
            AND current_aum >= 1000000 THEN customer_no
          END
        ),
        0
      ) AS new_100,
      NULLIF(
        COUNT(
          DISTINCT CASE
            WHEN base_aum < 1000000
            AND current_aum >= 1000000
            AND current_aum < 6000000 THEN customer_no
          END
        ),
        0
      ) AS new_100_600
    FROM
      org_customer_pair
    GROUP BY
      report_org_no
  )
SELECT
  a.report_org_no AS "机构编码__第二层时点机构号",
  a.report_org_name AS "机构编码__第二层机构简称",
  a.report_org_label AS "org",
  a.base_100 AS "基期100",
  m.current_100 AS "当期100",
  g.new_100 AS "新增100",
  g.new_100_600 AS "新增100-600",
  m.current_100 - a.base_100 AS "Sum_增量100",
  CAST(m.current_100 - a.base_100 AS DOUBLE) / NULLIF(CAST(a.base_100 AS DOUBLE), 0) AS "Sum_增速100",
  m.current_100_600 - m.base_100_600 AS "Sum_增量100-600",
  CAST(m.current_100_600 - m.base_100_600 AS DOUBLE) / NULLIF(CAST(m.base_100_600 AS DOUBLE), 0) AS "Sum_增速100-600",
  m.current_600 - m.base_600 AS "Sum_增量600"
FROM
  base_100_anchor a
  LEFT JOIN metric_by_org m ON a.report_org_no = m.report_org_no
  LEFT JOIN growth_by_org g ON a.report_org_no = g.report_org_no
ORDER BY
  a.report_org_label ASC,
  a.report_org_name ASC,
  a.base_100 ASC,
  m.current_100 ASC,
  g.new_100 ASC,
  g.new_100_600 ASC;
