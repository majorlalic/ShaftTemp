package com.example.demo.ingest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class PartitionMeasureRequest {

    private String topic;
    private String iotCode;

    @JsonProperty("PartitionId")
    private Integer partitionId;

    @NotBlank
    @JsonProperty("IedFullPath")
    private String iedFullPath;

    @NotBlank
    private String dataReference;

    @NotNull
    @JsonProperty("MaxTemp")
    private BigDecimal maxTemp;

    @NotNull
    @JsonProperty("MinTemp")
    private BigDecimal minTemp;

    @NotNull
    @JsonProperty("AvgTemp")
    private BigDecimal avgTemp;

    @JsonProperty("MaxTempPosition")
    private BigDecimal maxTempPosition;

    @JsonProperty("MinTempPosition")
    private BigDecimal minTempPosition;

    @JsonProperty("MaxTempChannel")
    private Integer maxTempChannel;

    @JsonProperty("MinTempChannel")
    private Integer minTempChannel;

    private LocalDateTime timestamp;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getIotCode() {
        return iotCode;
    }

    public void setIotCode(String iotCode) {
        this.iotCode = iotCode;
    }

    public Integer getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(Integer partitionId) {
        this.partitionId = partitionId;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
