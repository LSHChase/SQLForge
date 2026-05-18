package com.company.sqloptimization.infrastructure.persistence.mapper;

import com.company.sqloptimization.infrastructure.persistence.entity.RewriteTrialItemRecord;
import com.company.sqloptimization.infrastructure.persistence.entity.RewriteTrialRunRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RewriteTrialMapper {

    RewriteTrialRunRecord selectRunById(@Param("runId") String runId);

    List<RewriteTrialRunRecord> selectRunsByTenantId(@Param("tenantId") String tenantId);

    RewriteTrialRunRecord selectLatestRunByBatchId(@Param("tenantId") String tenantId,
                                                   @Param("batchId") String batchId);

    int insertRun(RewriteTrialRunRecord record);

    int updateRun(RewriteTrialRunRecord record);

    RewriteTrialItemRecord selectItemById(@Param("trialItemId") String trialItemId);

    List<RewriteTrialItemRecord> selectItemsByRunId(@Param("runId") String runId);

    List<RewriteTrialItemRecord> selectItemsByTenantId(@Param("tenantId") String tenantId);

    int insertItem(RewriteTrialItemRecord record);

    int updateItem(RewriteTrialItemRecord record);
}
