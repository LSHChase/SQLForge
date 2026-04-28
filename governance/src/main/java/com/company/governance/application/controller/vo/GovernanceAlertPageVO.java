package com.company.governance.application.controller.vo;

import java.util.List;

public class GovernanceAlertPageVO {

    private final List<GovernanceAlertSummaryVO> items;
    private final Integer pageNo;
    private final Integer pageSize;
    private final Integer total;
    private final Boolean hasNext;

    public GovernanceAlertPageVO(List<GovernanceAlertSummaryVO> items,
                                 Integer pageNo,
                                 Integer pageSize,
                                 Integer total,
                                 Boolean hasNext) {
        this.items = items;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.total = total;
        this.hasNext = hasNext;
    }

    public List<GovernanceAlertSummaryVO> getItems() { return items; }
    public Integer getPageNo() { return pageNo; }
    public Integer getPageSize() { return pageSize; }
    public Integer getTotal() { return total; }
    public Boolean getHasNext() { return hasNext; }
}
