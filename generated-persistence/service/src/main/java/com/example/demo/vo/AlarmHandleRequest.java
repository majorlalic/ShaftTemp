package com.example.demo.vo;

import java.util.List;

public class AlarmHandleRequest {

    private List<Long> alarmIds;
    private Integer status;
    private String remark;

    public List<Long> getAlarmIds() {
        return alarmIds;
    }

    public void setAlarmIds(List<Long> alarmIds) {
        this.alarmIds = alarmIds;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
