package com.example.demo.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D")
public class MonitorPartitionBindEntity {

    @Id
    private Long id;

    @Column(name = "monitor_id")
    private Long monitorId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "shaft_floor_id")
    private Long shaftFloorId;

    @Column(name = "partition_id")
    private Integer partitionId;

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

    @Column(name = "bind_status")
    private Integer bindStatus;

    private Integer deleted;

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

    public Long getShaftFloorId() {
        return shaftFloorId;
    }

    public void setShaftFloorId(Long shaftFloorId) {
        this.shaftFloorId = shaftFloorId;
    }

    public Integer getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(Integer partitionId) {
        this.partitionId = partitionId;
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

    public Integer getBindStatus() {
        return bindStatus;
    }

    public void setBindStatus(Integer bindStatus) {
        this.bindStatus = bindStatus;
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
