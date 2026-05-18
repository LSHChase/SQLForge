package com.company.sqloptimization.infrastructure.repository;

import com.company.sqloptimization.domain.trial.RewriteTrialItem;
import com.company.sqloptimization.domain.trial.RewriteTrialRun;
import com.company.sqloptimization.domain.trial.repository.RewriteTrialRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class InMemoryRewriteTrialRepository implements RewriteTrialRepository {

    private final Map<String, RewriteTrialRun> runs = new ConcurrentHashMap<String, RewriteTrialRun>();
    private final Map<String, RewriteTrialItem> items = new ConcurrentHashMap<String, RewriteTrialItem>();

    @Override
    public RewriteTrialRun saveRun(RewriteTrialRun run) {
        runs.put(run.getRunId(), run);
        return run;
    }

    @Override
    public RewriteTrialItem saveItem(RewriteTrialItem item) {
        items.put(item.getTrialItemId(), item);
        return item;
    }

    @Override
    public RewriteTrialRun findRunById(String runId) {
        return runs.get(runId);
    }

    @Override
    public List<RewriteTrialRun> findRunsByTenantId(String tenantId) {
        List<RewriteTrialRun> result = new ArrayList<RewriteTrialRun>();
        for (RewriteTrialRun run : runs.values()) {
            if (tenantId.equals(run.getTenantId())) {
                result.add(run);
            }
        }
        result.sort(Comparator.comparing(RewriteTrialRun::getCreatedAt).reversed());
        return result;
    }

    @Override
    public RewriteTrialRun findLatestRunByBatchId(String tenantId, String batchId) {
        RewriteTrialRun latest = null;
        for (RewriteTrialRun run : runs.values()) {
            if (!tenantId.equals(run.getTenantId()) || batchId == null || !batchId.equals(run.getBatchId())) {
                continue;
            }
            if (latest == null || run.getCreatedAt().isAfter(latest.getCreatedAt())) {
                latest = run;
            }
        }
        return latest;
    }

    @Override
    public List<RewriteTrialItem> findItemsByRunId(String runId) {
        List<RewriteTrialItem> result = new ArrayList<RewriteTrialItem>();
        for (RewriteTrialItem item : items.values()) {
            if (runId.equals(item.getRunId())) {
                result.add(item);
            }
        }
        result.sort(Comparator.comparing(RewriteTrialItem::getCreatedAt).thenComparing(RewriteTrialItem::getTrialItemId));
        return result;
    }

    @Override
    public List<RewriteTrialItem> findItemsByTenantId(String tenantId) {
        List<RewriteTrialItem> result = new ArrayList<RewriteTrialItem>();
        for (RewriteTrialItem item : items.values()) {
            RewriteTrialRun run = runs.get(item.getRunId());
            if (run != null && tenantId.equals(run.getTenantId())) {
                result.add(item);
            }
        }
        result.sort(Comparator.comparing(RewriteTrialItem::getCreatedAt).reversed());
        return result;
    }
}
