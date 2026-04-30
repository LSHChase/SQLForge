package com.company.sqloptimization.infrastructure.persistence.mapper;

import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchItemRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReportBatchItemMapper {

    void insert(ReportBatchItemRecord record);

    void update(ReportBatchItemRecord record);

    ReportBatchItemRecord selectByItemId(@Param("itemId") String itemId);

    List<ReportBatchItemRecord> selectByBatchId(@Param("batchId") String batchId);

    List<ReportBatchItemRecord> selectAll();
}
