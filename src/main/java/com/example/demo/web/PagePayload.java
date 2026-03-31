package com.example.demo.web;

import java.util.List;

public class PagePayload<T> {

    private long total;
    private List<T> list;
    private Integer pageNo;
    private Long totalSize;
    private List<T> dataList;

    public PagePayload(long total, List<T> list) {
        this(total, list, 1);
    }

    public PagePayload(long total, List<T> list, Integer pageNo) {
        this.total = total;
        this.list = list;
        this.pageNo = pageNo == null ? Integer.valueOf(1) : pageNo;
        this.totalSize = Long.valueOf(total);
        this.dataList = list;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }

    public List<T> getDataList() {
        return dataList;
    }

    public void setDataList(List<T> dataList) {
        this.dataList = dataList;
    }
}
