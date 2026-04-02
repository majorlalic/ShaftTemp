package com.example.demo.entity;

import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "ODS_DWEQ_DM_MONITOR_D")
public class MonitorEntity {

    @Id
    private Long id;

    private String name;

    @Column(name = "area_id")
    private Long areaId;

    @Column(name = "area_name")
    private String areaName;

    @Column(name = "elevator_count")
    private Integer elevatorCount;

    @Column(name = "shaft_type")
    private String shaftType;

    @Column(name = "monitor_status")
    private String monitorStatus;

    @Column(name = "owner_company")
    private String ownerCompany;

    @Column(name = "build_date")
    private LocalDate buildDate;

    @Column(name = "device_id")
    private Long deviceId;

    private String remark;
    private Integer deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public Integer getElevatorCount() {
        return elevatorCount;
    }

    public void setElevatorCount(Integer elevatorCount) {
        this.elevatorCount = elevatorCount;
    }

    public String getShaftType() {
        return shaftType;
    }

    public void setShaftType(String shaftType) {
        this.shaftType = shaftType;
    }

    public String getMonitorStatus() {
        return monitorStatus;
    }

    public void setMonitorStatus(String monitorStatus) {
        this.monitorStatus = monitorStatus;
    }

    public String getOwnerCompany() {
        return ownerCompany;
    }

    public void setOwnerCompany(String ownerCompany) {
        this.ownerCompany = ownerCompany;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public LocalDate getBuildDate() {
        return buildDate;
    }

    public void setBuildDate(LocalDate buildDate) {
        this.buildDate = buildDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
