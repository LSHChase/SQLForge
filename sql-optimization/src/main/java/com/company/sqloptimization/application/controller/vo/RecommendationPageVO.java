package com.company.sqloptimization.application.controller.vo;

import java.util.List;

public class RecommendationPageVO {

    private List<AccelerationRecommendationVO> items;
    private Integer pageNo;
    private Integer pageSize;
    private Integer totalCount;
    private Integer pageCount;
    private Boolean hasMore;

    public RecommendationPageVO(List<AccelerationRecommendationVO> items,
                                Integer pageNo,
                                Integer pageSize,
                                Integer totalCount,
                                Integer pageCount,
                                Boolean hasMore) {
        this.items = items;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
        this.pageCount = pageCount;
        this.hasMore = hasMore;
    }

    public List<AccelerationRecommendationVO> getItems() { return items; }
    public Integer getPageNo() { return pageNo; }
    public Integer getPageSize() { return pageSize; }
    public Integer getTotalCount() { return totalCount; }
    public Integer getPageCount() { return pageCount; }
    public Boolean getHasMore() { return hasMore; }
}
