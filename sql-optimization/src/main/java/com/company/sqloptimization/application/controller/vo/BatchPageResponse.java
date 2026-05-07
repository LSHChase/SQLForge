package com.company.sqloptimization.application.controller.vo;

import java.util.List;

public class BatchPageResponse<T> {

    private List<T> items;
    private Integer pageNo;
    private Integer pageSize;
    private Integer totalCount;
    private Integer pageCount;
    private Boolean hasMore;

    public BatchPageResponse() {
    }

    public BatchPageResponse(List<T> items,
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

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
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

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public Boolean getHasMore() {
        return hasMore;
    }

    public void setHasMore(Boolean hasMore) {
        this.hasMore = hasMore;
    }
}
