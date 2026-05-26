package com.company.sqloptimization.infrastructure.persistence.mapper;

import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReportBatchMapper {

    void insert(ReportBatchRecord record);

    void update(ReportBatchRecord record);

    ReportBatchRecord selectByBatchId(@Param("batchId") String batchId);

    List<ReportBatchRecord> selectAll();

    List<ReportBatchRecord> selectPageByTenantId(@Param("tenantId") String tenantId,
                                                 @Param("offset") Integer offset,
                                                 @Param("limit") Integer limit);

    Integer countByTenantId(@Param("tenantId") String tenantId);
}
