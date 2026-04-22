package com.company.governance.application.controller.vo;

import java.util.List;

public class GovernanceTraceLookupPageVO {

    private List<GovernanceTraceSummaryVO> items;
    private Boolean hasMore;
    private String nextCursor;

    public GovernanceTraceLookupPageVO() {
    }

    public GovernanceTraceLookupPageVO(List<GovernanceTraceSummaryVO> items, Boolean hasMore, String nextCursor) {
        this.items = items;
        this.hasMore = hasMore;
        this.nextCursor = nextCursor;
    }

    public List<GovernanceTraceSummaryVO> getItems() {
        return items;
    }

    public void setItems(List<GovernanceTraceSummaryVO> items) {
        this.items = items;
    }

    public Boolean getHasMore() {
        return hasMore;
    }

    public void setHasMore(Boolean hasMore) {
        this.hasMore = hasMore;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }
}
