package com.company.sqloptimization.domain.trial.repository;

import com.company.sqloptimization.domain.trial.RewriteTrialItem;
import com.company.sqloptimization.domain.trial.RewriteTrialRun;
import java.util.List;

public interface RewriteTrialRepository {

    RewriteTrialRun saveRun(RewriteTrialRun run);

    RewriteTrialItem saveItem(RewriteTrialItem item);

    RewriteTrialRun findRunById(String runId);

    List<RewriteTrialRun> findRunsByTenantId(String tenantId);

    RewriteTrialRun findLatestRunByBatchId(String tenantId, String batchId);

    List<RewriteTrialItem> findItemsByRunId(String runId);

    List<RewriteTrialItem> findItemsByTenantId(String tenantId);
}
