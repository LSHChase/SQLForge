package com.company.governance.application.controller.vo;

import java.util.List;
import java.util.Map;

public class GovernanceQueryHistoryPageVO {

    private List<GovernanceQueryHistorySummaryVO> items;
    private Integer pageNo;
    private Integer pageSize;
    private Boolean hasMore;
    private Map<String, Object> classificationSummary;

    public GovernanceQueryHistoryPageVO() {
    }

    public GovernanceQueryHistoryPageVO(List<GovernanceQueryHistorySummaryVO> items,
                                        Integer pageNo,
                                        Integer pageSize,
                                        Boolean hasMore,
                                        Map<String, Object> classificationSummary) {
        this.items = items;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.hasMore = hasMore;
        this.classificationSummary = classificationSummary;
    }

    public List<GovernanceQueryHistorySummaryVO> getItems() {
        return items;
    }

    public void setItems(List<GovernanceQueryHistorySummaryVO> items) {
        this.items = items;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Boolean getHasMore() {
        return hasMore;
    }

    public void setHasMore(Boolean hasMore) {
        this.hasMore = hasMore;
    }

    public Map<String, Object> getClassificationSummary() {
        return classificationSummary;
    }

    public void setClassificationSummary(Map<String, Object> classificationSummary) {
        this.classificationSummary = classificationSummary;
    }
}
