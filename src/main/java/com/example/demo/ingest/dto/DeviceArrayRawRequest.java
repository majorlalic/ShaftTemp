package com.example.demo.ingest.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class DeviceArrayRawRequest {

    private String topic;

    @JsonAlias("IedFullPath")
    private String iedFullPath;

    @JsonAlias("iot_code")
    private String iotCode;

    @NotEmpty
    private List<@NotNull Double> values;

    private Integer validStartPoint;
    private Integer validEndPoint;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
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

    public String getIotCode() {
        return iotCode;
    }

    public void setIotCode(String iotCode) {
        this.iotCode = iotCode;
    }

    public List<Double> getValues() {
        return values;
    }

    public void setValues(List<Double> values) {
        this.values = values;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
