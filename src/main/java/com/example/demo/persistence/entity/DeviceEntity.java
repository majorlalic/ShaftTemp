package com.example.demo.persistence.entity;

import java.time.LocalDate;
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

    @Column(name = "device_type")
    private String deviceType;

    private String model;

    private String manufacturer;

    @Column(name = "factory_date")
    private LocalDate factoryDate;

    @Column(name = "run_date")
    private LocalDate runDate;

    @Column(name = "asset_status")
    private String assetStatus;

    @Column(name = "area_id")
    private Long areaId;

    @Column(name = "org_id")
    private Long orgId;

    @Column(name = "online_status")
    private Integer onlineStatus;

    @Column(name = "last_report_time")
    private LocalDateTime lastReportTime;

    @Column(name = "last_offline_time")
    private LocalDateTime lastOfflineTime;

    private Integer deleted;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    private String remark;

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

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public LocalDate getFactoryDate() {
        return factoryDate;
    }

    public void setFactoryDate(LocalDate factoryDate) {
        this.factoryDate = factoryDate;
    }

    public LocalDate getRunDate() {
        return runDate;
    }

    public void setRunDate(LocalDate runDate) {
        this.runDate = runDate;
    }

    public String getAssetStatus() {
        return assetStatus;
    }

    public void setAssetStatus(String assetStatus) {
        this.assetStatus = assetStatus;
    }

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
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

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
