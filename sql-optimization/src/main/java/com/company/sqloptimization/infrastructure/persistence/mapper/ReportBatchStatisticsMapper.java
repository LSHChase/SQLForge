package com.company.sqloptimization.infrastructure.persistence.mapper;

import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchStatisticsRecords.ImportanceRecord;
import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchStatisticsRecords.IssueSceneRecord;
import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchStatisticsRecords.LogicalObjectRecord;
import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchStatisticsRecords.PriorityRecord;
import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchStatisticsRecords.ReportRecord;
import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchStatisticsRecords.SeverityRecord;
import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchStatisticsRecords.SqlIssueSceneRecord;
import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchStatisticsRecords.SqlLogicalObjectRecord;
import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchStatisticsRecords.SqlRecord;
import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchStatisticsRecords.SummaryRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReportBatchStatisticsMapper {

    void deleteSummaryByBatchId(@Param("batchId") String batchId);

    void deleteIssueScenesByBatchId(@Param("batchId") String batchId);

    void deleteSeveritiesByBatchId(@Param("batchId") String batchId);

    void deleteReportsByBatchId(@Param("batchId") String batchId);

    void deleteSqlStatsByBatchId(@Param("batchId") String batchId);

    void deleteSqlIssueScenesByBatchId(@Param("batchId") String batchId);

    void deleteSqlLogicalObjectsByBatchId(@Param("batchId") String batchId);

    void deletePrioritiesByBatchId(@Param("batchId") String batchId);

    void deleteImportancesByBatchId(@Param("batchId") String batchId);

    void deleteLogicalObjectsByBatchId(@Param("batchId") String batchId);

    void insertSummary(SummaryRecord record);

    void insertIssueScenes(@Param("records") List<IssueSceneRecord> records);

    void insertSeverities(@Param("records") List<SeverityRecord> records);

    void insertReports(@Param("records") List<ReportRecord> records);

    void insertSqlStats(@Param("records") List<SqlRecord> records);

    void insertSqlIssueScenes(@Param("records") List<SqlIssueSceneRecord> records);

    void insertSqlLogicalObjects(@Param("records") List<SqlLogicalObjectRecord> records);

    void insertPriorities(@Param("records") List<PriorityRecord> records);

    void insertImportances(@Param("records") List<ImportanceRecord> records);

    void insertLogicalObjects(@Param("records") List<LogicalObjectRecord> records);

    SummaryRecord selectSummaryByBatchId(@Param("batchId") String batchId);

    List<SummaryRecord> selectSummariesByBatchIds(@Param("batchIds") List<String> batchIds);

    List<IssueSceneRecord> selectIssueScenesByBatchId(@Param("batchId") String batchId);

    List<SeverityRecord> selectSeveritiesByBatchId(@Param("batchId") String batchId);

    List<ReportRecord> selectReportsByBatchId(@Param("batchId") String batchId);

    List<SqlRecord> selectSqlStatsByBatchId(@Param("batchId") String batchId);

    List<SqlIssueSceneRecord> selectSqlIssueScenesByBatchId(@Param("batchId") String batchId);

    List<SqlLogicalObjectRecord> selectSqlLogicalObjectsByBatchId(@Param("batchId") String batchId);

    List<PriorityRecord> selectPrioritiesByBatchId(@Param("batchId") String batchId);

    List<ImportanceRecord> selectImportancesByBatchId(@Param("batchId") String batchId);

    List<LogicalObjectRecord> selectLogicalObjectsByBatchId(@Param("batchId") String batchId);
}
