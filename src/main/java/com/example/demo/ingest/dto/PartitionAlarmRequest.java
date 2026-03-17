package com.example.demo.ingest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class PartitionAlarmRequest {

    private String topic;

    @NotBlank
    @JsonProperty("IedFullPath")
    private String iedFullPath;

    @NotBlank
    private String dataReference;

    @NotNull
    @JsonProperty("AlarmStatus")
    private Boolean alarmStatus;

    @NotNull
    @JsonProperty("FaultStatus")
    private Boolean faultStatus;

    private LocalDateTime timestamp;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
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

    public Boolean getAlarmStatus() {
        return alarmStatus;
    }

    public void setAlarmStatus(Boolean alarmStatus) {
        this.alarmStatus = alarmStatus;
    }

    public Boolean getFaultStatus() {
        return faultStatus;
    }

    public void setFaultStatus(Boolean faultStatus) {
        this.faultStatus = faultStatus;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
