package com.example.demo.vo;

public class AlarmActionRequest {

    private Long handler;
    private String remark;
    private Integer status;

    public Long getHandler() {
        return handler;
    }

    public void setHandler(Long handler) {
        this.handler = handler;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
