package com.example.demo.ingest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.example.demo.ingest.mq.TemperatureReportMessageConsumer;
import com.example.demo.ingest.service.ReportIngestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemperatureReportMessageConsumerTest {

    @Mock
    private ReportIngestService reportIngestService;

    @Test
    void shouldParseMqPayloadAndDelegate() {
        TemperatureReportMessageConsumer consumer =
            new TemperatureReportMessageConsumer(reportIngestService, new ObjectMapper());

        consumer.consume("{\"iotCode\":\"dev-1\",\"values\":[12,13,14]}");

        verify(reportIngestService).ingest(any());
    }
}
