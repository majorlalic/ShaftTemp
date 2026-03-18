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

    @Column(name = "shaft_floor_id")
    private Long shaftFloorId;

    @Column(name = "partition_code")
    private String partitionCode;

    @Column(name = "partition_name")
    private String partitionName;

    @Column(name = "data_reference")
    private String dataReference;

    @Column(name = "device_token")
    private String deviceToken;

    @Column(name = "partition_no")
    private Integer partitionNo;

    @Column(name = "source_format")
    private String sourceFormat;

    @Column(name = "merge_key")
    private String mergeKey;

    private Integer status;

    @Column(name = "first_alarm_time")
    private LocalDateTime firstAlarmTime;

    @Column(name = "last_alarm_time")
    private LocalDateTime lastAlarmTime;

    @Column(name = "merge_count")
    private Integer mergeCount;

    @Column(name = "event_count")
    private Integer eventCount;

    @Column(name = "alarm_level")
    private Integer alarmLevel;

    private String title;
    private String content;

    @Column(name = "confirm_user_id")
    private Long confirmUserId;

    @Column(name = "confirm_time")
    private LocalDateTime confirmTime;

    @Column(name = "handle_remark")
    private String handleRemark;

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

    public Long getShaftFloorId() {
        return shaftFloorId;
    }

    public void setShaftFloorId(Long shaftFloorId) {
        this.shaftFloorId = shaftFloorId;
    }

    public String getPartitionCode() {
        return partitionCode;
    }

    public void setPartitionCode(String partitionCode) {
        this.partitionCode = partitionCode;
    }

    public String getPartitionName() {
        return partitionName;
    }

    public void setPartitionName(String partitionName) {
        this.partitionName = partitionName;
    }

    public String getDataReference() {
        return dataReference;
    }

    public void setDataReference(String dataReference) {
        this.dataReference = dataReference;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public Integer getPartitionNo() {
        return partitionNo;
    }

    public void setPartitionNo(Integer partitionNo) {
        this.partitionNo = partitionNo;
    }

    public String getSourceFormat() {
        return sourceFormat;
    }

    public void setSourceFormat(String sourceFormat) {
        this.sourceFormat = sourceFormat;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getMergeKey() {
        return mergeKey;
    }

    public void setMergeKey(String mergeKey) {
        this.mergeKey = mergeKey;
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

    public Integer getEventCount() {
        return eventCount;
    }

    public void setEventCount(Integer eventCount) {
        this.eventCount = eventCount;
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

    public Long getConfirmUserId() {
        return confirmUserId;
    }

    public void setConfirmUserId(Long confirmUserId) {
        this.confirmUserId = confirmUserId;
    }

    public LocalDateTime getConfirmTime() {
        return confirmTime;
    }

    public void setConfirmTime(LocalDateTime confirmTime) {
        this.confirmTime = confirmTime;
    }

    public String getHandleRemark() {
        return handleRemark;
    }

    public void setHandleRemark(String handleRemark) {
        this.handleRemark = handleRemark;
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
