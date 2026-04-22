package com.company.benchmarkengine.infrastructure.persistence.mapper;

import com.company.benchmarkengine.infrastructure.persistence.entity.BenchmarkReportRecord;

public interface BenchmarkReportMapper {

    BenchmarkReportRecord selectByReportId(String reportId);

    BenchmarkReportRecord selectByTaskId(String taskId);

    int insert(BenchmarkReportRecord record);

    int update(BenchmarkReportRecord record);
}
