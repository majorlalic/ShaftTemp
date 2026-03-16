package com.example.demo.ingest.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class TemperatureReportRequest {

    @NotBlank
    private String iotCode;

    private LocalDateTime collectTime;

    @NotEmpty
    private List<@NotNull BigDecimal> values;

    public String getIotCode() {
        return iotCode;
    }

    public void setIotCode(String iotCode) {
        this.iotCode = iotCode;
    }

    public LocalDateTime getCollectTime() {
        return collectTime;
    }

    public void setCollectTime(LocalDateTime collectTime) {
        this.collectTime = collectTime;
    }

    public List<BigDecimal> getValues() {
        return values;
    }

    public void setValues(List<BigDecimal> values) {
        this.values = values;
    }
}
