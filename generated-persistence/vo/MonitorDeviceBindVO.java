package com.csg.dgri.szsiom.sysmanage.model;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * ODS_DWEQ_DM_MONITOR_DEVICE_BIND_D 对应 VO（自动生成）
 */
@Table(name = "ODS_DWEQ_DM_MONITOR_DEVICE_BIND_D")
public class MonitorDeviceBindVO extends CapBaseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    public MonitorDeviceBindVO() {
    }
    @Id
    private Long id;

    @Column(name = "monitor_id")
    private Long monitorId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "bind_status")
    private Integer bindStatus;

    @Column(name = "bind_time")
    private LocalDateTime bindTime;

    @Column(name = "unbind_time")
    private LocalDateTime unbindTime;

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

    public Integer getBindStatus() {
        return bindStatus;
    }

    public void setBindStatus(Integer bindStatus) {
        this.bindStatus = bindStatus;
    }

    public LocalDateTime getBindTime() {
        return bindTime;
    }

    public void setBindTime(LocalDateTime bindTime) {
        this.bindTime = bindTime;
    }

    public LocalDateTime getUnbindTime() {
        return unbindTime;
    }

    public void setUnbindTime(LocalDateTime unbindTime) {
        this.unbindTime = unbindTime;
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
