package com.example.iot;

import com.example.demo.config.AppProperties;
import com.example.demo.config.HttpClientConfig;
import com.example.demo.ingest.mq.MqttInboundConfig;
import com.example.demo.ingest.mq.PartitionTopicParser;
import com.example.demo.ingest.mq.TemperatureReportMessageConsumer;
import com.example.demo.iot.handler.AlarmHandler;
import com.example.demo.iot.handler.DataHandler;
import com.example.demo.iot.service.IotRawPersistService;
import com.example.demo.dao.AlarmRawRepository;
import com.example.demo.dao.RawDataRepository;
import com.example.demo.service.DeviceResolverService;
import com.example.demo.service.IdGenerator;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@MapperScan(basePackageClasses = {RawDataRepository.class, AlarmRawRepository.class})
@Import({
    HttpClientConfig.class,
    PartitionTopicParser.class,
    TemperatureReportMessageConsumer.class,
    MqttInboundConfig.class,
    IotRawPersistService.class,
    DataHandler.class,
    AlarmHandler.class,
    DeviceResolverService.class,
    IdGenerator.class
})
public class IotApplication {

    public static void main(String[] args) {
        SpringApplication.run(IotApplication.class, args);
    }
}
