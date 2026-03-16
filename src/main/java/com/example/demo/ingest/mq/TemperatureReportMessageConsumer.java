package com.example.demo.ingest.mq;

import com.example.demo.ingest.dto.TemperatureReportRequest;
import com.example.demo.ingest.service.ReportIngestService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "shaft.mq", name = "enabled", havingValue = "true")
public class TemperatureReportMessageConsumer {

    private final ReportIngestService reportIngestService;
    private final ObjectMapper objectMapper;

    public TemperatureReportMessageConsumer(ReportIngestService reportIngestService, ObjectMapper objectMapper) {
        this.reportIngestService = reportIngestService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "${shaft.mq.queue}")
    public void consume(String payload) {
        try {
            TemperatureReportRequest request = objectMapper.readValue(payload, TemperatureReportRequest.class);
            reportIngestService.ingest(request);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to parse MQ payload", ex);
        }
    }
}
