package com.csg.dgri.szsiom.sysmanage.model;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * ODS_DWEQ_DM_DEVICE_ONLINE_LOG_D 对应 VO（自动生成）
 */
@Table(name = "ODS_DWEQ_DM_DEVICE_ONLINE_LOG_D")
public class DeviceOnlineLogVO extends CapBaseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    public DeviceOnlineLogVO() {
    }
    @Id
    private Long id;

    @Column(name = "device_id")
    private Long deviceId;

    private Integer status;

    @Column(name = "change_time")
    private LocalDateTime changeTime;

    private String reason;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getChangeTime() {
        return changeTime;
    }

    public void setChangeTime(LocalDateTime changeTime) {
        this.changeTime = changeTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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
