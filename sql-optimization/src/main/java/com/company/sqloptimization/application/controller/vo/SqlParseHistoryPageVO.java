package com.company.sqloptimization.application.controller.vo;

import java.util.List;
import java.util.Map;

public class SqlParseHistoryPageVO {

    private List<SqlParseHistorySummaryVO> items;
    private Integer pageNo;
    private Integer pageSize;
    private Integer totalCount;
    private Integer pageCount;
    private Boolean hasMore;
    private Map<String, Object> classificationSummary;

    public SqlParseHistoryPageVO(List<SqlParseHistorySummaryVO> items,
                                 Integer pageNo,
                                 Integer pageSize,
                                 Integer totalCount,
                                 Integer pageCount,
                                 Boolean hasMore,
                                 Map<String, Object> classificationSummary) {
        this.items = items;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
        this.pageCount = pageCount;
        this.hasMore = hasMore;
        this.classificationSummary = classificationSummary;
    }

    public List<SqlParseHistorySummaryVO> getItems() { return items; }
    public Integer getPageNo() { return pageNo; }
    public Integer getPageSize() { return pageSize; }
    public Integer getTotalCount() { return totalCount; }
    public Integer getPageCount() { return pageCount; }
    public Boolean getHasMore() { return hasMore; }
    public Map<String, Object> getClassificationSummary() { return classificationSummary; }
}
