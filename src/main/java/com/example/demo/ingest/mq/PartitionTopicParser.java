package com.example.demo.ingest.mq;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class PartitionTopicParser {

    public MessageType detect(String topic, JsonNode payload) {
        if (topic != null) {
            if (topic.endsWith("/Measure")) {
                return MessageType.MEASURE;
            }
            if (topic.endsWith("/Alarm")) {
                return MessageType.ALARM;
            }
        }
        if (payload.has("MaxTemp") || payload.has("AvgTemp") || payload.has("MinTemp")) {
            return MessageType.MEASURE;
        }
        if (payload.has("AlarmStatus") || payload.has("FaultStatus")) {
            return MessageType.ALARM;
        }
        throw new IllegalArgumentException("Unsupported MQ payload type");
    }

    public enum MessageType {
        MEASURE,
        ALARM
    }
}
