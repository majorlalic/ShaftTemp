package com.example.demo.ingest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.example.demo.ingest.mq.PartitionTopicParser;
import com.example.demo.ingest.mq.TemperatureReportMessageConsumer;
import com.example.demo.iot.handler.AlarmHandler;
import com.example.demo.iot.handler.DataHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemperatureReportMessageConsumerTest {

    @Mock
    private DataHandler dataHandler;
    @Mock
    private AlarmHandler alarmHandler;

    @Test
    void shouldParseMeasurePayloadAndDelegate() {
        TemperatureReportMessageConsumer consumer =
            new TemperatureReportMessageConsumer(dataHandler, alarmHandler, new ObjectMapper(), new PartitionTopicParser());

        consumer.consume(
            "{\"IedFullPath\":\"shaft/a\",\"dataReference\":\"/TMP/dev_TMP_th01\",\"MaxTemp\":82.5,\"MinTemp\":70.1,\"AvgTemp\":75.2}",
            "/TMP/dev_TMP_th01/Measure"
        );

        verify(dataHandler).update(any(), any());
    }
}
