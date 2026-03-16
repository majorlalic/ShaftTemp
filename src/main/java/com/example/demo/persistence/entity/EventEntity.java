package com.example.demo.persistence.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "event")
public class EventEntity {

    @Id
    private Long id;

    @Column(name = "alarm_id")
    private Long alarmId;

    @Column(name = "alarm_type")
    private String alarmType;

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "monitor_id")
    private Long monitorId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "event_time")
    private LocalDateTime eventTime;

    @Column(name = "event_no")
    private Integer eventNo;

    @Column(name = "event_level")
    private Integer eventLevel;

    @Column(name = "point_list_json", columnDefinition = "json")
    private String pointListJson;

    @Column(name = "detail_json", columnDefinition = "json")
    private String detailJson;

    @Column(name = "merged_flag")
    private Integer mergedFlag;

    private Integer deleted;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAlarmId() {
        return alarmId;
    }

    public void setAlarmId(Long alarmId) {
        this.alarmId = alarmId;
    }

    public String getAlarmType() {
        return alarmType;
    }

    public void setAlarmType(String alarmType) {
        this.alarmType = alarmType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getMonitorId() {
        return monitorId;
    }

    public void setMonitorId(Long monitorId) {
        this.monitorId = monitorId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public Integer getEventNo() {
        return eventNo;
    }

    public void setEventNo(Integer eventNo) {
        this.eventNo = eventNo;
    }

    public Integer getEventLevel() {
        return eventLevel;
    }

    public void setEventLevel(Integer eventLevel) {
        this.eventLevel = eventLevel;
    }

    public String getPointListJson() {
        return pointListJson;
    }

    public void setPointListJson(String pointListJson) {
        this.pointListJson = pointListJson;
    }

    public String getDetailJson() {
        return detailJson;
    }

    public void setDetailJson(String detailJson) {
        this.detailJson = detailJson;
    }

    public Integer getMergedFlag() {
        return mergedFlag;
    }

    public void setMergedFlag(Integer mergedFlag) {
        this.mergedFlag = mergedFlag;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }

    public LocalDateTime getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(LocalDateTime updatedOn) {
        this.updatedOn = updatedOn;
    }
}
