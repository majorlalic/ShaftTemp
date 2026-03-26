package com.example.demo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shaft")
public class AppProperties {

    private final Mq mq = new Mq();
    private final Alarm alarm = new Alarm();
    private final Inspection inspection = new Inspection();
    private final Cache cache = new Cache();
    private final Stat stat = new Stat();
    private final RawData rawData = new RawData();

    public Mq getMq() {
        return mq;
    }

    public Alarm getAlarm() {
        return alarm;
    }

    public Inspection getInspection() {
        return inspection;
    }

    public Cache getCache() {
        return cache;
    }

    public Stat getStat() {
        return stat;
    }

    public RawData getRawData() {
        return rawData;
    }

    public static class Mq {
        private boolean enabled;
        private String brokerUrl = "tcp://localhost:1883";
        private String clientId = "shaft-temp-consumer";
        private String username;
        private String password;
        private List<String> topics = new ArrayList<String>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBrokerUrl() {
            return brokerUrl;
        }

        public void setBrokerUrl(String brokerUrl) {
            this.brokerUrl = brokerUrl;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public List<String> getTopics() {
            return topics;
        }

        public void setTopics(List<String> topics) {
            this.topics = topics;
        }
    }

    public static class Alarm {
        private BigDecimal temperatureThreshold = new BigDecimal("70.0");
        private BigDecimal temperatureDiffThreshold = new BigDecimal("20.0");
        private BigDecimal riseRateThreshold = new BigDecimal("10.0");
        private BigDecimal fiberBreakThreshold = BigDecimal.ZERO;
        private long offlineThresholdSeconds = 30L;
        private int windowSize = 20;
        private long eventThrottleSeconds = 300L;

        public BigDecimal getTemperatureThreshold() {
            return temperatureThreshold;
        }

        public void setTemperatureThreshold(BigDecimal temperatureThreshold) {
            this.temperatureThreshold = temperatureThreshold;
        }

        public BigDecimal getTemperatureDiffThreshold() {
            return temperatureDiffThreshold;
        }

        public void setTemperatureDiffThreshold(BigDecimal temperatureDiffThreshold) {
            this.temperatureDiffThreshold = temperatureDiffThreshold;
        }

        public BigDecimal getRiseRateThreshold() {
            return riseRateThreshold;
        }

        public void setRiseRateThreshold(BigDecimal riseRateThreshold) {
            this.riseRateThreshold = riseRateThreshold;
        }

        public BigDecimal getFiberBreakThreshold() {
            return fiberBreakThreshold;
        }

        public void setFiberBreakThreshold(BigDecimal fiberBreakThreshold) {
            this.fiberBreakThreshold = fiberBreakThreshold;
        }

        public long getOfflineThresholdSeconds() {
            return offlineThresholdSeconds;
        }

        public void setOfflineThresholdSeconds(long offlineThresholdSeconds) {
            this.offlineThresholdSeconds = offlineThresholdSeconds;
        }

        public int getWindowSize() {
            return windowSize;
        }

        public void setWindowSize(int windowSize) {
            this.windowSize = windowSize;
        }

        public long getEventThrottleSeconds() {
            return eventThrottleSeconds;
        }

        public void setEventThrottleSeconds(long eventThrottleSeconds) {
            this.eventThrottleSeconds = eventThrottleSeconds;
        }
    }

    public static class Inspection {
        private boolean enabled = true;
        private long fixedDelayMs = 30000L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getFixedDelayMs() {
            return fixedDelayMs;
        }

        public void setFixedDelayMs(long fixedDelayMs) {
            this.fixedDelayMs = fixedDelayMs;
        }
    }

    public static class Cache {
        private long partitionBindingRefreshMs = 300000L;

        public long getPartitionBindingRefreshMs() {
            return partitionBindingRefreshMs;
        }

        public void setPartitionBindingRefreshMs(long partitionBindingRefreshMs) {
            this.partitionBindingRefreshMs = partitionBindingRefreshMs;
        }
    }

    public static class Stat {
        private long flushDelayMs = 15000L;

        public long getFlushDelayMs() {
            return flushDelayMs;
        }

        public void setFlushDelayMs(long flushDelayMs) {
            this.flushDelayMs = flushDelayMs;
        }
    }

    public static class RawData {
        private int retentionDays = 365;
        private boolean cleanupEnabled = false;
        private long cleanupFixedDelayMs = 86400000L;

        public int getRetentionDays() {
            return retentionDays;
        }

        public void setRetentionDays(int retentionDays) {
            this.retentionDays = retentionDays;
        }

        public boolean isCleanupEnabled() {
            return cleanupEnabled;
        }

        public void setCleanupEnabled(boolean cleanupEnabled) {
            this.cleanupEnabled = cleanupEnabled;
        }

        public long getCleanupFixedDelayMs() {
            return cleanupFixedDelayMs;
        }

        public void setCleanupFixedDelayMs(long cleanupFixedDelayMs) {
            this.cleanupFixedDelayMs = cleanupFixedDelayMs;
        }
    }
}
