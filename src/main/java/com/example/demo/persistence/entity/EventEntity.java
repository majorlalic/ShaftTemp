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

    @Column(name = "event_type")
    private Integer eventType;

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

    private String content;

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

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public Integer getEventType() {
        return eventType;
    }

    public void setEventType(Integer eventType) {
        this.eventType = eventType;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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
