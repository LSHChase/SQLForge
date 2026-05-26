package com.company.sqloptimization.infrastructure.persistence.mapper;

import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchItemRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReportBatchItemMapper {

    void insert(ReportBatchItemRecord record);

    void upsertBatch(@Param("records") List<ReportBatchItemRecord> records);

    void update(ReportBatchItemRecord record);

    ReportBatchItemRecord selectByItemId(@Param("itemId") String itemId);

    List<ReportBatchItemRecord> selectByBatchId(@Param("batchId") String batchId);

    List<ReportBatchItemRecord> selectAll();

    List<ReportBatchItemRecord> selectPageByBatchId(@Param("batchId") String batchId,
                                                    @Param("reportCode") String reportCode,
                                                    @Param("offset") Integer offset,
                                                    @Param("limit") Integer limit);

    Integer countByBatchId(@Param("batchId") String batchId, @Param("reportCode") String reportCode);
}
