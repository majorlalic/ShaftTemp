package com.example.demo.persistence.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "alarm")
public class AlarmEntity {

    @Id
    private Long id;

    @Column(name = "alarm_code")
    private String alarmCode;

    @Column(name = "alarm_type")
    private String alarmType;

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "monitor_id")
    private Long monitorId;

    @Column(name = "device_id")
    private Long deviceId;

    private String status;

    @Column(name = "first_alarm_time")
    private LocalDateTime firstAlarmTime;

    @Column(name = "last_alarm_time")
    private LocalDateTime lastAlarmTime;

    @Column(name = "merge_count")
    private Integer mergeCount;

    @Column(name = "alarm_level")
    private Integer alarmLevel;

    private String title;
    private String content;
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

    public String getAlarmCode() {
        return alarmCode;
    }

    public void setAlarmCode(String alarmCode) {
        this.alarmCode = alarmCode;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getFirstAlarmTime() {
        return firstAlarmTime;
    }

    public void setFirstAlarmTime(LocalDateTime firstAlarmTime) {
        this.firstAlarmTime = firstAlarmTime;
    }

    public LocalDateTime getLastAlarmTime() {
        return lastAlarmTime;
    }

    public void setLastAlarmTime(LocalDateTime lastAlarmTime) {
        this.lastAlarmTime = lastAlarmTime;
    }

    public Integer getMergeCount() {
        return mergeCount;
    }

    public void setMergeCount(Integer mergeCount) {
        this.mergeCount = mergeCount;
    }

    public Integer getAlarmLevel() {
        return alarmLevel;
    }

    public void setAlarmLevel(Integer alarmLevel) {
        this.alarmLevel = alarmLevel;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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
