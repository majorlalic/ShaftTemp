package com.example.demo.ingest.mq;

import com.example.demo.iot.handler.AlarmHandler;
import com.example.demo.iot.handler.DataHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class TemperatureReportMessageConsumer {

    private final DataHandler dataHandler;
    private final AlarmHandler alarmHandler;
    private final ObjectMapper objectMapper;
    private final PartitionTopicParser partitionTopicParser;

    public TemperatureReportMessageConsumer(
        DataHandler dataHandler,
        AlarmHandler alarmHandler,
        ObjectMapper objectMapper,
        PartitionTopicParser partitionTopicParser
    ) {
        this.dataHandler = dataHandler;
        this.alarmHandler = alarmHandler;
        this.objectMapper = objectMapper;
        this.partitionTopicParser = partitionTopicParser;
    }

    public void consume(String payload, String topic) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode sample = root.isArray() && root.size() > 0 ? root.get(0) : root;
            PartitionTopicParser.MessageType messageType = partitionTopicParser.detect(topic, sample);
            if (messageType == PartitionTopicParser.MessageType.MEASURE) {
                dataHandler.update(payload, topic);
            } else {
                alarmHandler.update(payload, topic);
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to parse MQTT payload", ex);
        }
    }
}
