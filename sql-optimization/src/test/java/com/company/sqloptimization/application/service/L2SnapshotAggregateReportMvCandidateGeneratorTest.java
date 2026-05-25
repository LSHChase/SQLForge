package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class L2DynamicSnapshotAggregateMvCandidateGeneratorTest {

    private static final String BASE_DATE = "20260430";
    private static final String CURRENT_DATE = "20260519";
    private static final long LOW = 1000000L;
    private static final long HIGH = 6000000L;

    @Test
    void shouldPreserveYonghongAnchorRowsOrgLabelsAndMetricSemantics() {
        List<RawSnapshot> rows = Arrays.asList(
            row("41H006", "深圳市分行", "41H002", "深圳市分行营业部", "C001", BASE_DATE, 1500000L),
            row("41H006", "深圳市分行", "41H002", "深圳市分行营业部", "C001", CURRENT_DATE, 2300000L),
            row("41H006", "深圳市分行", "41H003", "深圳市分行南山支行", "C002", BASE_DATE, 7000000L),
            row("41H006", "深圳市分行", "41H003", "深圳市分行南山支行", "C002", CURRENT_DATE, 8000000L),
            row("41H006", "深圳市分行", "41H004", "深圳市分行福田支行", "C003", BASE_DATE, 500000L),
            row("41H006", "深圳市分行", "41H004", "深圳市分行福田支行", "C003", CURRENT_DATE, 1500000L),
            row("41H006", "深圳市分行", "41H005", "深圳市分行罗湖支行", "C004", BASE_DATE, 1600000L)
        );

        Map<String, ReportMetrics> original = originalReportShape(rows);
        Map<String, ReportMetrics> rewritten = rewrittenReportShape(rows);

        assertEquals(original, rewritten);
        assertTrue(rewritten.containsKey("41H006|深圳市分行|深圳市分行"), rewritten.keySet().toString());
        assertTrue(rewritten.containsKey("41H002|深圳市分行营业部|深圳市分行营业部"), rewritten.keySet().toString());
        assertTrue(rewritten.containsKey("41H003|深圳市分行南山支行|深圳市分行南山支行"), rewritten.keySet().toString());
        assertTrue(rewritten.containsKey("41H005|深圳市分行罗湖支行|深圳市分行罗湖支行"), rewritten.keySet().toString());
        assertFalse(rewritten.containsKey("41H004|深圳市分行福田支行|深圳市分行福田支行"),
            "原 SQL 以基期100为锚点，只有新增但无基期100的机构不应出现在最终结果。");
        assertEquals(null, rewritten.get("41H005|深圳市分行罗湖支行|深圳市分行罗湖支行").current100);
        assertEquals(null, rewritten.get("41H005|深圳市分行罗湖支行|深圳市分行罗湖支行").delta100);
    }

    private Map<String, ReportMetrics> originalReportShape(List<RawSnapshot> rows) {
        List<ReportSnapshot> snapshots = aggregateReportSnapshots(rows);
        Map<String, Integer> base100Anchor = countByLabel(snapshots, BASE_DATE, LOW, null);
        Map<String, OrgMetrics> orgMetrics = countOrgMetricsFromSnapshots(snapshots);
        Map<String, GrowthMetrics> growthMetrics = countGrowthMetricsFromOrgPairs(snapshots);
        return assemble(base100Anchor, orgMetrics, growthMetrics);
    }

    private Map<String, ReportMetrics> rewrittenReportShape(List<RawSnapshot> rows) {
        List<ReportSnapshot> snapshots = aggregateReportSnapshots(rows);
        Map<String, CustomerPair> customerPairs = customerPairsByLabel(snapshots);
        Map<String, Integer> base100Anchor = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, CustomerPair> entry : customerPairs.entrySet()) {
            if (entry.getValue().baseAum >= LOW) {
                increment(base100Anchor, labelKey(entry.getValue()));
            }
        }
        Map<String, OrgMetrics> orgMetrics = countOrgMetricsFromSnapshots(snapshots);
        Map<String, GrowthMetrics> growthMetrics = countGrowthMetricsFromOrgPairs(snapshots);
        return assemble(base100Anchor, orgMetrics, growthMetrics);
    }

    private List<ReportSnapshot> aggregateReportSnapshots(List<RawSnapshot> rows) {
        Map<String, ReportSnapshot> snapshots = new LinkedHashMap<String, ReportSnapshot>();
        for (RawSnapshot row : rows) {
            addSnapshot(snapshots, row.level2No, row.level2Name, row.level2Name, row.customerNo, row.date, row.aum);
            addSnapshot(snapshots, row.branchNo, row.branchName, row.branchName, row.customerNo, row.date, row.aum);
        }
        return new ArrayList<ReportSnapshot>(snapshots.values());
    }

    private void addSnapshot(Map<String, ReportSnapshot> snapshots,
                             String orgNo,
                             String orgName,
                             String orgLabel,
                             String customerNo,
                             String date,
                             long aum) {
        String key = orgNo + "|" + orgName + "|" + orgLabel + "|" + customerNo + "|" + date;
        ReportSnapshot snapshot = snapshots.get(key);
        if (snapshot == null) {
            snapshots.put(key, new ReportSnapshot(orgNo, orgName, orgLabel, customerNo, date, aum));
        } else {
            snapshot.aum += aum;
        }
    }

    private Map<String, Integer> countByLabel(List<ReportSnapshot> snapshots,
                                              String date,
                                              long low,
                                              Long high) {
        Map<String, Set<String>> customersByLabel = new LinkedHashMap<String, Set<String>>();
        for (ReportSnapshot snapshot : snapshots) {
            if (!date.equals(snapshot.date) || snapshot.aum < low || high != null && snapshot.aum >= high.longValue()) {
                continue;
            }
            addDistinct(customersByLabel, labelKey(snapshot), snapshot.customerNo);
        }
        return sizes(customersByLabel);
    }

    private Map<String, OrgMetrics> countOrgMetricsFromSnapshots(List<ReportSnapshot> snapshots) {
        Map<String, OrgMetrics> metricsByOrg = new LinkedHashMap<String, OrgMetrics>();
        for (ReportSnapshot snapshot : snapshots) {
            OrgMetrics metrics = orgMetrics(metricsByOrg, snapshot.orgNo);
            if (BASE_DATE.equals(snapshot.date) && snapshot.aum >= LOW && snapshot.aum < HIGH) {
                metrics.base100600.add(snapshot.customerNo);
            }
            if (BASE_DATE.equals(snapshot.date) && snapshot.aum >= HIGH) {
                metrics.base600.add(snapshot.customerNo);
            }
            if (CURRENT_DATE.equals(snapshot.date) && snapshot.aum >= LOW) {
                metrics.current100.add(snapshot.customerNo);
            }
            if (CURRENT_DATE.equals(snapshot.date) && snapshot.aum >= LOW && snapshot.aum < HIGH) {
                metrics.current100600.add(snapshot.customerNo);
            }
            if (CURRENT_DATE.equals(snapshot.date) && snapshot.aum >= HIGH) {
                metrics.current600.add(snapshot.customerNo);
            }
        }
        return metricsByOrg;
    }

    private Map<String, GrowthMetrics> countGrowthMetricsFromOrgPairs(List<ReportSnapshot> snapshots) {
        Map<String, CustomerPair> pairs = new LinkedHashMap<String, CustomerPair>();
        for (ReportSnapshot snapshot : snapshots) {
            String key = snapshot.orgNo + "|" + snapshot.customerNo;
            CustomerPair pair = pairs.get(key);
            if (pair == null) {
                pair = new CustomerPair(snapshot.orgNo, snapshot.orgName, snapshot.orgLabel, snapshot.customerNo);
                pairs.put(key, pair);
            }
            if (BASE_DATE.equals(snapshot.date)) {
                pair.baseAum += snapshot.aum;
            }
            if (CURRENT_DATE.equals(snapshot.date)) {
                pair.currentAum += snapshot.aum;
            }
        }
        Map<String, GrowthMetrics> metricsByOrg = new LinkedHashMap<String, GrowthMetrics>();
        for (CustomerPair pair : pairs.values()) {
            GrowthMetrics metrics = growthMetrics(metricsByOrg, pair.orgNo);
            if (pair.baseAum < LOW && pair.currentAum >= LOW) {
                metrics.new100.add(pair.customerNo);
            }
            if (pair.baseAum < LOW && pair.currentAum >= LOW && pair.currentAum < HIGH) {
                metrics.new100600.add(pair.customerNo);
            }
        }
        return metricsByOrg;
    }

    private Map<String, CustomerPair> customerPairsByLabel(List<ReportSnapshot> snapshots) {
        Map<String, CustomerPair> pairs = new LinkedHashMap<String, CustomerPair>();
        for (ReportSnapshot snapshot : snapshots) {
            String key = labelKey(snapshot) + "|" + snapshot.customerNo;
            CustomerPair pair = pairs.get(key);
            if (pair == null) {
                pair = new CustomerPair(snapshot.orgNo, snapshot.orgName, snapshot.orgLabel, snapshot.customerNo);
                pairs.put(key, pair);
            }
            if (BASE_DATE.equals(snapshot.date)) {
                pair.baseAum += snapshot.aum;
            }
            if (CURRENT_DATE.equals(snapshot.date)) {
                pair.currentAum += snapshot.aum;
            }
        }
        return pairs;
    }

    private Map<String, ReportMetrics> assemble(Map<String, Integer> base100Anchor,
                                                Map<String, OrgMetrics> orgMetrics,
                                                Map<String, GrowthMetrics> growthMetrics) {
        Map<String, ReportMetrics> rows = new LinkedHashMap<String, ReportMetrics>();
        for (Map.Entry<String, Integer> anchor : base100Anchor.entrySet()) {
            String[] parts = anchor.getKey().split("\\|", -1);
            String orgNo = parts[0];
            OrgMetrics org = orgMetrics.get(orgNo);
            GrowthMetrics growth = growthMetrics.get(orgNo);
            Integer current100 = org == null ? null : nullIfZero(org.current100.size());
            Integer base100600 = org == null ? null : nullIfZero(org.base100600.size());
            Integer current100600 = org == null ? null : nullIfZero(org.current100600.size());
            Integer base600 = org == null ? null : nullIfZero(org.base600.size());
            Integer current600 = org == null ? null : nullIfZero(org.current600.size());
            Integer new100 = growth == null ? null : nullIfZero(growth.new100.size());
            Integer new100600 = growth == null ? null : nullIfZero(growth.new100600.size());
            rows.put(anchor.getKey(), new ReportMetrics(
                anchor.getValue(),
                current100,
                new100,
                new100600,
                subtract(current100, anchor.getValue()),
                subtract(current100600, base100600),
                subtract(current600, base600)
            ));
        }
        return rows;
    }

    private OrgMetrics orgMetrics(Map<String, OrgMetrics> values, String orgNo) {
        OrgMetrics metrics = values.get(orgNo);
        if (metrics == null) {
            metrics = new OrgMetrics();
            values.put(orgNo, metrics);
        }
        return metrics;
    }

    private GrowthMetrics growthMetrics(Map<String, GrowthMetrics> values, String orgNo) {
        GrowthMetrics metrics = values.get(orgNo);
        if (metrics == null) {
            metrics = new GrowthMetrics();
            values.put(orgNo, metrics);
        }
        return metrics;
    }

    private void increment(Map<String, Integer> values, String key) {
        Integer current = values.get(key);
        values.put(key, Integer.valueOf(current == null ? 1 : current.intValue() + 1));
    }

    private void addDistinct(Map<String, Set<String>> values, String key, String customerNo) {
        Set<String> customers = values.get(key);
        if (customers == null) {
            customers = new LinkedHashSet<String>();
            values.put(key, customers);
        }
        customers.add(customerNo);
    }

    private Map<String, Integer> sizes(Map<String, Set<String>> values) {
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Set<String>> entry : values.entrySet()) {
            result.put(entry.getKey(), Integer.valueOf(entry.getValue().size()));
        }
        return result;
    }

    private Integer subtract(Integer left, Integer right) {
        return left == null || right == null ? null : Integer.valueOf(left.intValue() - right.intValue());
    }

    private Integer nullIfZero(int value) {
        return value == 0 ? null : Integer.valueOf(value);
    }

    private String labelKey(ReportSnapshot snapshot) {
        return snapshot.orgNo + "|" + snapshot.orgName + "|" + snapshot.orgLabel;
    }

    private String labelKey(CustomerPair pair) {
        return pair.orgNo + "|" + pair.orgName + "|" + pair.orgLabel;
    }

    private static RawSnapshot row(String level2No,
                                   String level2Name,
                                   String branchNo,
                                   String branchName,
                                   String customerNo,
                                   String date,
                                   long aum) {
        return new RawSnapshot(level2No, level2Name, branchNo, branchName, customerNo, date, aum);
    }

    private static final class RawSnapshot {
        private final String level2No;
        private final String level2Name;
        private final String branchNo;
        private final String branchName;
        private final String customerNo;
        private final String date;
        private final long aum;

        private RawSnapshot(String level2No,
                            String level2Name,
                            String branchNo,
                            String branchName,
                            String customerNo,
                            String date,
                            long aum) {
            this.level2No = level2No;
            this.level2Name = level2Name;
            this.branchNo = branchNo;
            this.branchName = branchName;
            this.customerNo = customerNo;
            this.date = date;
            this.aum = aum;
        }
    }

    private static final class ReportSnapshot {
        private final String orgNo;
        private final String orgName;
        private final String orgLabel;
        private final String customerNo;
        private final String date;
        private long aum;

        private ReportSnapshot(String orgNo, String orgName, String orgLabel, String customerNo, String date, long aum) {
            this.orgNo = orgNo;
            this.orgName = orgName;
            this.orgLabel = orgLabel;
            this.customerNo = customerNo;
            this.date = date;
            this.aum = aum;
        }
    }

    private static final class CustomerPair {
        private final String orgNo;
        private final String orgName;
        private final String orgLabel;
        private final String customerNo;
        private long baseAum;
        private long currentAum;

        private CustomerPair(String orgNo, String orgName, String orgLabel, String customerNo) {
            this.orgNo = orgNo;
            this.orgName = orgName;
            this.orgLabel = orgLabel;
            this.customerNo = customerNo;
        }
    }

    private static final class OrgMetrics {
        private final Set<String> base100600 = new LinkedHashSet<String>();
        private final Set<String> base600 = new LinkedHashSet<String>();
        private final Set<String> current100 = new LinkedHashSet<String>();
        private final Set<String> current100600 = new LinkedHashSet<String>();
        private final Set<String> current600 = new LinkedHashSet<String>();
    }

    private static final class GrowthMetrics {
        private final Set<String> new100 = new LinkedHashSet<String>();
        private final Set<String> new100600 = new LinkedHashSet<String>();
    }

    private static final class ReportMetrics {
        private final Integer base100;
        private final Integer current100;
        private final Integer new100;
        private final Integer new100600;
        private final Integer delta100;
        private final Integer delta100600;
        private final Integer delta600;

        private ReportMetrics(Integer base100,
                              Integer current100,
                              Integer new100,
                              Integer new100600,
                              Integer delta100,
                              Integer delta100600,
                              Integer delta600) {
            this.base100 = base100;
            this.current100 = current100;
            this.new100 = new100;
            this.new100600 = new100600;
            this.delta100 = delta100;
            this.delta100600 = delta100600;
            this.delta600 = delta600;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ReportMetrics)) {
                return false;
            }
            ReportMetrics that = (ReportMetrics) o;
            return java.util.Objects.equals(base100, that.base100)
                && java.util.Objects.equals(current100, that.current100)
                && java.util.Objects.equals(new100, that.new100)
                && java.util.Objects.equals(new100600, that.new100600)
                && java.util.Objects.equals(delta100, that.delta100)
                && java.util.Objects.equals(delta100600, that.delta100600)
                && java.util.Objects.equals(delta600, that.delta600);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(base100, current100, new100, new100600, delta100, delta100600, delta600);
        }

        @Override
        public String toString() {
            return "ReportMetrics{"
                + "base100=" + base100
                + ", current100=" + current100
                + ", new100=" + new100
                + ", new100600=" + new100600
                + ", delta100=" + delta100
                + ", delta100600=" + delta100600
                + ", delta600=" + delta600
                + '}';
        }
    }
}
