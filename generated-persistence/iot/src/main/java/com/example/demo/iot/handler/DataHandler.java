package com.example.demo.iot.handler;

import com.example.demo.config.AppProperties;
import com.example.demo.vo.PartitionMeasureRequest;
import com.example.demo.ingest.mq.PartitionTopicParser;
import com.example.demo.iot.service.IotRawPersistService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class DataHandler {

    private static final Logger log = LoggerFactory.getLogger(DataHandler.class);

    private final ObjectMapper objectMapper;
    private final PartitionTopicParser partitionTopicParser;
    private final IotRawPersistService iotRawPersistService;
    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public DataHandler(
        ObjectMapper objectMapper,
        PartitionTopicParser partitionTopicParser,
        IotRawPersistService iotRawPersistService,
        RestTemplate restTemplate,
        AppProperties appProperties
    ) {
        this.objectMapper = objectMapper;
        this.partitionTopicParser = partitionTopicParser;
        this.iotRawPersistService = iotRawPersistService;
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
    }

    public void update(String jsonData, String topic) {
        List<PartitionMeasureRequest> requests = parseMeasureBatch(jsonData, topic);
        for (PartitionMeasureRequest request : requests) {
            try {
                iotRawPersistService.persistMeasure(request);
            } catch (Exception ex) {
                log.error("iot persist ODS_DWEQ_DM_RAW_DATA_D failed, topic={}, dataReference={}, partitionId={}",
                    topic, request.getDataReference(), request.getPartitionId(), ex);
            }
        }
        callServiceBatch(requests);
    }

    private List<PartitionMeasureRequest> parseMeasureBatch(String jsonData, String topic) {
        try {
            JsonNode root = objectMapper.readTree(jsonData);
            String iotCode = partitionTopicParser.extractIotCode(topic);
            List<PartitionMeasureRequest> requests = new ArrayList<PartitionMeasureRequest>();
            if (root.isArray()) {
                for (JsonNode node : root) {
                    PartitionMeasureRequest request = objectMapper.treeToValue(node, PartitionMeasureRequest.class);
                    request.setTopic(topic);
                    request.setIotCode(iotCode);
                    requests.add(request);
                }
                return requests;
            }
            PartitionMeasureRequest request = objectMapper.treeToValue(root, PartitionMeasureRequest.class);
            request.setTopic(topic);
            request.setIotCode(iotCode);
            requests.add(request);
            return requests;
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to parse measure payload", ex);
        }
    }

    private void callServiceBatch(List<PartitionMeasureRequest> requests) {
        String url = appProperties.getIot().getServiceBaseUrl() + "/api/service/iot/data/batch";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<PartitionMeasureRequest>> requestEntity = new HttpEntity<List<PartitionMeasureRequest>>(requests, headers);
        restTemplate.postForEntity(url, requestEntity, String.class);
    }
}
