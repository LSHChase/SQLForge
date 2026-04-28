package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.application.controller.vo.BenchmarkReportResponse;
import com.company.benchmarkengine.domain.benchmark.BenchmarkAlertLinkage;
import com.company.benchmarkengine.domain.benchmark.BenchmarkRegressionSummary;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReport;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqlforge.common.governance.GovernanceBenchmarkRegressionAlertLinkage;
import com.company.sqlforge.common.governance.GovernanceBenchmarkRegressionAlertRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkRegressionAlertResponse;
import com.company.sqlforge.common.utils.JsonUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BenchmarkRegressionAlertService {

    private final GovernanceCapabilityClient governanceCapabilityClient;

    public BenchmarkRegressionAlertService(GovernanceCapabilityClient governanceCapabilityClient) {
        this.governanceCapabilityClient = governanceCapabilityClient;
    }

    public BenchmarkReport attachAlertLinkage(BenchmarkTask task,
                                              BenchmarkReport report,
                                              BenchmarkReportResponse reportResponse,
                                              String historyId) {
        if (task == null
            || report == null
            || reportResponse == null
            || task.getTaskType() != BenchmarkTaskType.REGRESSION_GUARD) {
            return report;
        }
        BenchmarkRegressionSummary regressionSummary = report.getRegressionSummary();
        if (regressionSummary == null || !Boolean.TRUE.equals(regressionSummary.getAlertRequired())) {
            return report;
        }
        GovernanceBenchmarkRegressionAlertRequest request = new GovernanceBenchmarkRegressionAlertRequest();
        request.setTenantId(task.getTenantId());
        request.setReportId(report.getReportId());
        request.setTaskId(task.getTaskId());
        request.setHistoryId(historyId);
        request.setSqlFingerprint(report.getSqlFingerprint());
        request.setVerdict(report.getVerdict().name());
        request.setThresholdHitCount(regressionSummary.getThresholdHitCount());
        request.setFailedThresholdCount(regressionSummary.getFailedThresholdCount());
        request.setWarningThresholdCount(regressionSummary.getWarningThresholdCount());
        request.setSummary(regressionSummary.getSummary());
        request.setReportQueryPath(reportResponse.getReportQueryPath());
        request.setRawDataDownloadPath(reportResponse.getRawDataDownloadPath());
        request.setThresholdAssessmentsJson(JsonUtils.toJson(report.getThresholdAssessments()));
        request.setExecutionSummaryJson(JsonUtils.toJson(report.getExecutionSummary()));

        GovernanceBenchmarkRegressionAlertResponse response =
            governanceCapabilityClient.emitBenchmarkRegressionAlert(request);
        return report.withAlertLinkages(toAlertLinkages(response == null ? null : response.getAlertLinkages()));
    }

    private List<BenchmarkAlertLinkage> toAlertLinkages(List<GovernanceBenchmarkRegressionAlertLinkage> linkages) {
        if (linkages == null || linkages.isEmpty()) {
            return Collections.emptyList();
        }
        List<BenchmarkAlertLinkage> items = new ArrayList<BenchmarkAlertLinkage>(linkages.size());
        for (GovernanceBenchmarkRegressionAlertLinkage linkage : linkages) {
            items.add(
                new BenchmarkAlertLinkage(
                    linkage.getAlertId(),
                    linkage.getAlertType(),
                    linkage.getAlertLevel(),
                    linkage.getAlertStatus(),
                    linkage.getNotifyStatus(),
                    linkage.getSummary(),
                    linkage.getDetailPath(),
                    linkage.getLinkageMode(),
                    linkage.getNotificationLogId()
                )
            );
        }
        return Collections.unmodifiableList(items);
    }
}
