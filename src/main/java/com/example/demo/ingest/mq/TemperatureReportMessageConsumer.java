package com.example.demo.ingest.mq;

import com.example.demo.ingest.dto.PartitionAlarmRequest;
import com.example.demo.ingest.dto.DeviceArrayRawRequest;
import com.example.demo.ingest.dto.PartitionMeasureRequest;
import com.example.demo.ingest.service.DeviceRawDataIngestService;
import com.example.demo.ingest.service.ReportIngestService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "shaft.mq", name = "enabled", havingValue = "true")
public class TemperatureReportMessageConsumer {

    private final ReportIngestService reportIngestService;
    private final DeviceRawDataIngestService deviceRawDataIngestService;
    private final ObjectMapper objectMapper;
    private final PartitionTopicParser partitionTopicParser;

    public TemperatureReportMessageConsumer(
        ReportIngestService reportIngestService,
        DeviceRawDataIngestService deviceRawDataIngestService,
        ObjectMapper objectMapper,
        PartitionTopicParser partitionTopicParser
    ) {
        this.reportIngestService = reportIngestService;
        this.deviceRawDataIngestService = deviceRawDataIngestService;
        this.objectMapper = objectMapper;
        this.partitionTopicParser = partitionTopicParser;
    }

    @RabbitListener(queues = "${shaft.mq.queue}")
    public void consume(String payload, @Header(value = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String routingKey) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            PartitionTopicParser.MessageType messageType = partitionTopicParser.detect(routingKey, root);
            if (messageType == PartitionTopicParser.MessageType.DEVICE_ARRAY) {
                DeviceArrayRawRequest request = objectMapper.treeToValue(root, DeviceArrayRawRequest.class);
                request.setTopic(routingKey);
                deviceRawDataIngestService.ingest(request);
            } else if (messageType == PartitionTopicParser.MessageType.MEASURE) {
                PartitionMeasureRequest request = objectMapper.treeToValue(root, PartitionMeasureRequest.class);
                request.setTopic(routingKey);
                reportIngestService.ingestMeasure(request);
            } else {
                PartitionAlarmRequest request = objectMapper.treeToValue(root, PartitionAlarmRequest.class);
                request.setTopic(routingKey);
                reportIngestService.ingestAlarm(request);
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to parse MQ payload", ex);
        }
    }
}
