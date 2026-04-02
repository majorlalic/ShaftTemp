package com.csg.dgri.szsiom.sysmanage.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * ODS_DWEQ_DM_RAW_DATA_D 对应 VO（自动生成）
 */
@Table(name = "ODS_DWEQ_DM_RAW_DATA_D")
public class RawDataVO extends CapBaseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    public RawDataVO() {
    }
    @Id
    private Long id;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "iot_code")
    private String iotCode;

    private String topic;

    @Column(name = "partition_id")
    private Integer partitionId;

    @Column(name = "monitor_id")
    private Long monitorId;

    @Column(name = "shaft_floor_id")
    private Long shaftFloorId;

    @Column(name = "data_reference")
    private String dataReference;

    @Column(name = "ied_full_path")
    private String iedFullPath;

    @Column(name = "collect_time")
    private LocalDateTime collectTime;

    @Column(name = "max_temp")
    private BigDecimal maxTemp;

    @Column(name = "min_temp")
    private BigDecimal minTemp;

    @Column(name = "avg_temp")
    private BigDecimal avgTemp;

    @Column(name = "max_temp_position")
    private BigDecimal maxTempPosition;

    @Column(name = "min_temp_position")
    private BigDecimal minTempPosition;

    @Column(name = "max_temp_channel")
    private Integer maxTempChannel;

    @Column(name = "min_temp_channel")
    private Integer minTempChannel;

    @Column(name = "payload_json")
    private String payloadJson;

    private Integer deleted;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
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

    public Long getMonitorId() {
        return monitorId;
    }

    public void setMonitorId(Long monitorId) {
        this.monitorId = monitorId;
    }

    public Long getShaftFloorId() {
        return shaftFloorId;
    }

    public void setShaftFloorId(Long shaftFloorId) {
        this.shaftFloorId = shaftFloorId;
    }

    public String getDataReference() {
        return dataReference;
    }

    public void setDataReference(String dataReference) {
        this.dataReference = dataReference;
    }

    public String getIedFullPath() {
        return iedFullPath;
    }

    public void setIedFullPath(String iedFullPath) {
        this.iedFullPath = iedFullPath;
    }

    public LocalDateTime getCollectTime() {
        return collectTime;
    }

    public void setCollectTime(LocalDateTime collectTime) {
        this.collectTime = collectTime;
    }

    public BigDecimal getMaxTemp() {
        return maxTemp;
    }

    public void setMaxTemp(BigDecimal maxTemp) {
        this.maxTemp = maxTemp;
    }

    public BigDecimal getMinTemp() {
        return minTemp;
    }

    public void setMinTemp(BigDecimal minTemp) {
        this.minTemp = minTemp;
    }

    public BigDecimal getAvgTemp() {
        return avgTemp;
    }

    public void setAvgTemp(BigDecimal avgTemp) {
        this.avgTemp = avgTemp;
    }

    public BigDecimal getMaxTempPosition() {
        return maxTempPosition;
    }

    public void setMaxTempPosition(BigDecimal maxTempPosition) {
        this.maxTempPosition = maxTempPosition;
    }

    public BigDecimal getMinTempPosition() {
        return minTempPosition;
    }

    public void setMinTempPosition(BigDecimal minTempPosition) {
        this.minTempPosition = minTempPosition;
    }

    public Integer getMaxTempChannel() {
        return maxTempChannel;
    }

    public void setMaxTempChannel(Integer maxTempChannel) {
        this.maxTempChannel = maxTempChannel;
    }

    public Integer getMinTempChannel() {
        return minTempChannel;
    }

    public void setMinTempChannel(Integer minTempChannel) {
        this.minTempChannel = minTempChannel;
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
