package com.example.demo.ingest.mq;

import com.example.demo.config.AppProperties;
import java.util.List;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
@ConditionalOnProperty(prefix = "shaft.mq", name = "enabled", havingValue = "true")
public class MqttInboundConfig {

    @Bean
    public MqttPahoClientFactory mqttClientFactory(AppProperties appProperties) {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[] {appProperties.getMq().getBrokerUrl()});
        if (appProperties.getMq().getUsername() != null && !appProperties.getMq().getUsername().trim().isEmpty()) {
            options.setUserName(appProperties.getMq().getUsername());
        }
        if (appProperties.getMq().getPassword() != null) {
            options.setPassword(appProperties.getMq().getPassword().toCharArray());
        }
        options.setAutomaticReconnect(true);
        options.setCleanSession(false);
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer mqttInbound(
        AppProperties appProperties,
        MqttPahoClientFactory mqttClientFactory,
        MessageChannel mqttInputChannel
    ) {
        List<String> topics = appProperties.getMq().getTopics();
        String[] topicArray = topics == null || topics.isEmpty()
            ? new String[] {"/TMP/+/Measure", "/TMP/+/Alarm"}
            : topics.toArray(new String[0]);
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
            appProperties.getMq().getClientId(),
            mqttClientFactory,
            topicArray
        );
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new org.springframework.integration.mqtt.support.DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel);
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler mqttMessageHandler(final TemperatureReportMessageConsumer consumer) {
        return message -> {
            Object payload = message.getPayload();
            String topic = message.getHeaders().get("mqtt_receivedTopic", String.class);
            consumer.consume(String.valueOf(payload), topic);
        };
    }
}
