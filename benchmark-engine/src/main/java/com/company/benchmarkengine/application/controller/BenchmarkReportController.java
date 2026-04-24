package com.company.benchmarkengine.application.controller;

import com.company.benchmarkengine.application.controller.vo.BenchmarkReportResponse;
import com.company.benchmarkengine.application.service.BenchmarkRenderedReport;
import com.company.benchmarkengine.application.service.BenchmarkReportApplicationService;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/benchmark-engine")
public class BenchmarkReportController {

    private final BenchmarkReportApplicationService benchmarkReportApplicationService;

    public BenchmarkReportController(BenchmarkReportApplicationService benchmarkReportApplicationService) {
        this.benchmarkReportApplicationService = benchmarkReportApplicationService;
    }

    @GetMapping("/reports/{reportId}")
    public ResponseEntity<?> getReport(@PathVariable("reportId") String reportId,
                                       @RequestParam(value = "format", defaultValue = "JSON") String format) {
        BenchmarkReportFormat reportFormat = benchmarkReportApplicationService.parseFormat(format);
        if (reportFormat == BenchmarkReportFormat.JSON) {
            BenchmarkReportResponse response = benchmarkReportApplicationService.getJsonReport(reportId);
            return ResponseEntity.ok(response);
        }
        BenchmarkRenderedReport renderedReport = benchmarkReportApplicationService.renderReport(reportId, reportFormat);
        return ResponseEntity.ok()
            .contentType(renderedReport.getMediaType())
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + renderedReport.getFileName() + "\"")
            .body(renderedReport.getContent());
    }

    @GetMapping("/reports/{reportId}/raw-data")
    public ResponseEntity<byte[]> getRawDataReport(@PathVariable("reportId") String reportId) {
        BenchmarkRenderedReport renderedReport = benchmarkReportApplicationService.downloadRawDataReport(reportId);
        return ResponseEntity.ok()
            .contentType(renderedReport.getMediaType())
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + renderedReport.getFileName() + "\"")
            .body(renderedReport.getContent());
    }
}
