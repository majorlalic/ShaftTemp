package com.example.demo.ingest.mq;

import com.example.demo.ingest.dto.PartitionAlarmRequest;
import com.example.demo.ingest.dto.PartitionMeasureRequest;
import com.example.demo.ingest.service.ReportIngestService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class TemperatureReportMessageConsumer {

    private final ReportIngestService reportIngestService;
    private final ObjectMapper objectMapper;
    private final PartitionTopicParser partitionTopicParser;

    public TemperatureReportMessageConsumer(
        ReportIngestService reportIngestService,
        ObjectMapper objectMapper,
        PartitionTopicParser partitionTopicParser
    ) {
        this.reportIngestService = reportIngestService;
        this.objectMapper = objectMapper;
        this.partitionTopicParser = partitionTopicParser;
    }

    public void consume(String payload, String topic) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            PartitionTopicParser.MessageType messageType = partitionTopicParser.detect(topic, root);
            if (messageType == PartitionTopicParser.MessageType.MEASURE) {
                PartitionMeasureRequest request = objectMapper.treeToValue(root, PartitionMeasureRequest.class);
                request.setTopic(topic);
                reportIngestService.ingestMeasure(request);
            } else {
                PartitionAlarmRequest request = objectMapper.treeToValue(root, PartitionAlarmRequest.class);
                request.setTopic(topic);
                reportIngestService.ingestAlarm(request);
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to parse MQTT payload", ex);
        }
    }
}
