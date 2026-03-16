package com.example.demo.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "temp_stat_minute")
public class TempStatMinuteEntity {

    @Id
    private Long id;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "monitor_id")
    private Long monitorId;

    @Column(name = "stat_time")
    private LocalDateTime statTime;

    @Column(name = "max_temp")
    private BigDecimal maxTemp;

    @Column(name = "min_temp")
    private BigDecimal minTemp;

    @Column(name = "avg_temp")
    private BigDecimal avgTemp;

    @Column(name = "alarm_point_count")
    private Integer alarmPointCount;

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

    public Long getMonitorId() {
        return monitorId;
    }

    public void setMonitorId(Long monitorId) {
        this.monitorId = monitorId;
    }

    public LocalDateTime getStatTime() {
        return statTime;
    }

    public void setStatTime(LocalDateTime statTime) {
        this.statTime = statTime;
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

    public Integer getAlarmPointCount() {
        return alarmPointCount;
    }

    public void setAlarmPointCount(Integer alarmPointCount) {
        this.alarmPointCount = alarmPointCount;
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
