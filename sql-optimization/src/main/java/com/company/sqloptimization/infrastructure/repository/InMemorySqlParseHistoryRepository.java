package com.company.sqloptimization.infrastructure.repository;

import com.company.sqloptimization.domain.parsehistory.SqlParseHistory;
import com.company.sqloptimization.domain.parsehistory.SqlParseHistoryFilter;
import com.company.sqloptimization.domain.parsehistory.repository.SqlParseHistoryRepository;
import com.company.sqlforge.common.utils.DateUtils;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.util.StringUtils;

public class InMemorySqlParseHistoryRepository implements SqlParseHistoryRepository {

    private final Map<String, SqlParseHistory> records = new ConcurrentHashMap<String, SqlParseHistory>();

    @Override
    public SqlParseHistory save(SqlParseHistory history) {
        records.put(history.getParseHistoryId(), history);
        return history;
    }

    @Override
    public SqlParseHistory findByParseHistoryId(String parseHistoryId) {
        return records.get(parseHistoryId);
    }

    @Override
    public SqlParseHistory findByBatchKeyAndSqlFingerprint(String batchKey, String sqlFingerprint) {
        if (!StringUtils.hasText(batchKey) || !StringUtils.hasText(sqlFingerprint)) {
            return null;
        }
        for (SqlParseHistory history : records.values()) {
            if (batchKey.equals(history.getBatchKey()) && sqlFingerprint.equals(history.getSqlFingerprint())) {
                return history;
            }
        }
        return null;
    }

    @Override
    public List<SqlParseHistory> findPage(SqlParseHistoryFilter filter) {
        List<SqlParseHistory> matches = matching(filter);
        int offset = Math.max(0, filter == null ? 0 : filter.getOffset());
        int limit = Math.max(0, filter == null ? matches.size() : filter.getLimit());
        int end = Math.min(matches.size(), offset + limit);
        if (offset >= matches.size()) {
            return new ArrayList<SqlParseHistory>();
        }
        return new ArrayList<SqlParseHistory>(matches.subList(offset, end));
    }

    @Override
    public int count(SqlParseHistoryFilter filter) {
        return matching(filter).size();
    }

    private List<SqlParseHistory> matching(SqlParseHistoryFilter filter) {
        List<SqlParseHistory> result = new ArrayList<SqlParseHistory>();
        for (SqlParseHistory history : records.values()) {
            if (matches(history, filter)) {
                result.add(history);
            }
        }
        result.sort(new Comparator<SqlParseHistory>() {
            @Override
            public int compare(SqlParseHistory left, SqlParseHistory right) {
                Instant leftTime = left.getSubmittedAt();
                Instant rightTime = right.getSubmittedAt();
                if (leftTime == null && rightTime == null) {
                    return right.getParseHistoryId().compareTo(left.getParseHistoryId());
                }
                if (leftTime == null) {
                    return 1;
                }
                if (rightTime == null) {
                    return -1;
                }
                int compared = rightTime.compareTo(leftTime);
                return compared == 0 ? right.getParseHistoryId().compareTo(left.getParseHistoryId()) : compared;
            }
        });
        return result;
    }

    private boolean matches(SqlParseHistory history, SqlParseHistoryFilter filter) {
        if (filter == null) {
            return true;
        }
        if (!equalsIfPresent(filter.getTenantId(), history.getTenantId())) {
            return false;
        }
        if (!equalsIfPresent(filter.getSourceType(), history.getSourceType())) {
            return false;
        }
        if (!equalsIfPresent(filter.getReportCode(), history.getReportCode())) {
            return false;
        }
        if (!equalsIfPresent(filter.getDatasourceCode(), history.getDatasourceCode())) {
            return false;
        }
        if (!equalsIfPresent(filter.getStageCode(), history.getStageCode())) {
            return false;
        }
        if (!equalsIfPresent(filter.getBizDate(), history.getBizDate())) {
            return false;
        }
        if (!equalsIfPresent(filter.getStatus(), history.getResultStatus())) {
            return false;
        }
        if (!equalsIfPresent(filter.getAccessChannel(), history.getAccessChannel())) {
            return false;
        }
        if (!equalsIfPresent(filter.getEngine(), history.getTargetEngine())) {
            return false;
        }
        if (!equalsIfPresent(filter.getSubmittedBy(), history.getSubmittedBy())) {
            return false;
        }
        if (!equalsIfPresent(filter.getTraceId(), history.getTraceId())) {
            return false;
        }
        if (!equalsIfPresent(filter.getParseTaskId(), history.getParseTaskId())) {
            return false;
        }
        if (StringUtils.hasText(filter.getLogicalObjectType())
            && (history.getLogicalObjectHitsJson() == null
                || !history.getLogicalObjectHitsJson().contains(filter.getLogicalObjectType()))) {
            return false;
        }
        if (StringUtils.hasText(filter.getQueryDateStart())
            && (history.getQueryDateStart() == null || history.getQueryDateStart().compareTo(filter.getQueryDateStart()) < 0)) {
            return false;
        }
        if (StringUtils.hasText(filter.getQueryDateEnd())
            && (history.getQueryDateEnd() == null || history.getQueryDateEnd().compareTo(filter.getQueryDateEnd()) > 0)) {
            return false;
        }
        LocalDateTime submitted = history.getSubmittedAt() == null
            ? null
            : DateUtils.toBeijingDateTime(history.getSubmittedAt());
        if (filter.getSubmittedStart() != null && (submitted == null || submitted.isBefore(filter.getSubmittedStart()))) {
            return false;
        }
        return filter.getSubmittedEnd() == null || (submitted != null && !submitted.isAfter(filter.getSubmittedEnd()));
    }

    private boolean equalsIfPresent(String expected, String actual) {
        return !StringUtils.hasText(expected) || expected.equals(actual);
    }
}
