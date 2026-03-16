package com.example.demo.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "raw_data")
public class RawDataEntity {

    @Id
    private Long id;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "iot_code")
    private String iotCode;

    @Column(name = "monitor_id")
    private Long monitorId;

    @Column(name = "collect_time")
    private LocalDateTime collectTime;

    @Column(name = "point_count")
    private Integer pointCount;

    @Column(name = "valid_start_point")
    private Integer validStartPoint;

    @Column(name = "valid_end_point")
    private Integer validEndPoint;

    @Column(name = "values_json", columnDefinition = "json")
    private String valuesJson;

    @Column(name = "max_temp")
    private BigDecimal maxTemp;

    @Column(name = "min_temp")
    private BigDecimal minTemp;

    @Column(name = "avg_temp")
    private BigDecimal avgTemp;

    @Column(name = "abnormal_flag")
    private Integer abnormalFlag;

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

    public Long getMonitorId() {
        return monitorId;
    }

    public void setMonitorId(Long monitorId) {
        this.monitorId = monitorId;
    }

    public LocalDateTime getCollectTime() {
        return collectTime;
    }

    public void setCollectTime(LocalDateTime collectTime) {
        this.collectTime = collectTime;
    }

    public Integer getPointCount() {
        return pointCount;
    }

    public void setPointCount(Integer pointCount) {
        this.pointCount = pointCount;
    }

    public Integer getValidStartPoint() {
        return validStartPoint;
    }

    public void setValidStartPoint(Integer validStartPoint) {
        this.validStartPoint = validStartPoint;
    }

    public Integer getValidEndPoint() {
        return validEndPoint;
    }

    public void setValidEndPoint(Integer validEndPoint) {
        this.validEndPoint = validEndPoint;
    }

    public String getValuesJson() {
        return valuesJson;
    }

    public void setValuesJson(String valuesJson) {
        this.valuesJson = valuesJson;
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

    public Integer getAbnormalFlag() {
        return abnormalFlag;
    }

    public void setAbnormalFlag(Integer abnormalFlag) {
        this.abnormalFlag = abnormalFlag;
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
