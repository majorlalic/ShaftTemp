package com.csg.dgri.szsiom.sysmanage.model;

import java.time.LocalDateTime;
import java.io.Serializable;
import javax.persistence.Table;

/**
 * ODS_DWEQ_DM_ALARM_RAW_D 对应 VO（自动生成）
 */
@Table(name = "ODS_DWEQ_DM_ALARM_RAW_D")
public class AlarmRawVO extends CapBaseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    public AlarmRawVO() {
    }
    private Long id;
    private String iotCode;
    private String topic;
    private Integer partitionId;
    private Integer alarmStatus;
    private Integer faultStatus;
    private String iedFullPath;
    private String dataReference;
    private LocalDateTime collectTime;
    private String payloadJson;
    private Integer deleted;
    private LocalDateTime createdOn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIotCode() {
        return iotCode;
    }

    public void setIotCode(String iotCode) {
        this.iotCode = iotCode;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Integer getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(Integer partitionId) {
        this.partitionId = partitionId;
    }

    public Integer getAlarmStatus() {
        return alarmStatus;
    }

    public void setAlarmStatus(Integer alarmStatus) {
        this.alarmStatus = alarmStatus;
    }

    public Integer getFaultStatus() {
        return faultStatus;
    }

    public void setFaultStatus(Integer faultStatus) {
        this.faultStatus = faultStatus;
    }

    public String getIedFullPath() {
        return iedFullPath;
    }

    public void setIedFullPath(String iedFullPath) {
        this.iedFullPath = iedFullPath;
    }

    public String getDataReference() {
        return dataReference;
    }

    public void setDataReference(String dataReference) {
        this.dataReference = dataReference;
    }

    public LocalDateTime getCollectTime() {
        return collectTime;
    }

    public void setCollectTime(LocalDateTime collectTime) {
        this.collectTime = collectTime;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
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
}
