package com.example.demo.persistence.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "device")
public class DeviceEntity {

    @Id
    private Long id;

    @Column(name = "iot_code")
    private String iotCode;

    private String name;

    @Column(name = "online_status")
    private Integer onlineStatus;

    @Column(name = "last_report_time")
    private LocalDateTime lastReportTime;

    @Column(name = "last_offline_time")
    private LocalDateTime lastOfflineTime;

    private Integer deleted;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(Integer onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public LocalDateTime getLastReportTime() {
        return lastReportTime;
    }

    public void setLastReportTime(LocalDateTime lastReportTime) {
        this.lastReportTime = lastReportTime;
    }

    public LocalDateTime getLastOfflineTime() {
        return lastOfflineTime;
    }

    public void setLastOfflineTime(LocalDateTime lastOfflineTime) {
        this.lastOfflineTime = lastOfflineTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(LocalDateTime updatedOn) {
        this.updatedOn = updatedOn;
    }
}
