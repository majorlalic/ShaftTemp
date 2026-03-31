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
            String iotCode = partitionTopicParser.extractIotCode(topic);
            if (root.isArray()) {
                for (JsonNode node : root) {
                    dispatch(topic, iotCode, node);
                }
                return;
            }
            dispatch(topic, iotCode, root);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to parse MQTT payload", ex);
        }
    }

    private void dispatch(String topic, String iotCode, JsonNode payloadNode) throws JsonProcessingException {
        PartitionTopicParser.MessageType messageType = partitionTopicParser.detect(topic, payloadNode);
        if (messageType == PartitionTopicParser.MessageType.MEASURE) {
            PartitionMeasureRequest request = objectMapper.treeToValue(payloadNode, PartitionMeasureRequest.class);
            request.setTopic(topic);
            request.setIotCode(iotCode);
            reportIngestService.ingestMeasure(request);
        } else {
            PartitionAlarmRequest request = objectMapper.treeToValue(payloadNode, PartitionAlarmRequest.class);
            request.setTopic(topic);
            request.setIotCode(iotCode);
            reportIngestService.ingestAlarm(request);
        }
    }
}
