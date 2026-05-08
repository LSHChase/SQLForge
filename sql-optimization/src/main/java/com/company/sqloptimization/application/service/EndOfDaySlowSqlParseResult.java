package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EndOfDaySlowSqlParseResult {

    private String batchKey;
    private int candidateCount;
    private int parsedCount;
    private int persistedCount;
    private int failedCount;
    private final List<String> parseHistoryIds = new ArrayList<String>();

    public String getBatchKey() { return batchKey; }
    public void setBatchKey(String batchKey) { this.batchKey = batchKey; }
    public int getCandidateCount() { return candidateCount; }
    public void setCandidateCount(int candidateCount) { this.candidateCount = candidateCount; }
    public int getParsedCount() { return parsedCount; }
    public void setParsedCount(int parsedCount) { this.parsedCount = parsedCount; }
    public int getPersistedCount() { return persistedCount; }
    public void setPersistedCount(int persistedCount) { this.persistedCount = persistedCount; }
    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
    public List<String> getParseHistoryIds() { return Collections.unmodifiableList(parseHistoryIds); }

    public void record(SqlParseHistoryWriteResult writeResult) {
        parsedCount += 1;
        if (writeResult != null && Boolean.TRUE.equals(writeResult.getPersisted())) {
            persistedCount += 1;
            if (writeResult.getParseHistoryId() != null) {
                parseHistoryIds.add(writeResult.getParseHistoryId());
            }
            return;
        }
        failedCount += 1;
    }

    public void recordFailure() {
        failedCount += 1;
    }
}
