package com.example.demo;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shaft")
public class AppProperties {

    private final Mq mq = new Mq();
    private final Alarm alarm = new Alarm();
    private final Inspection inspection = new Inspection();

    public Mq getMq() {
        return mq;
    }

    public Alarm getAlarm() {
        return alarm;
    }

    public Inspection getInspection() {
        return inspection;
    }

    public static class Mq {
        private boolean enabled;
        private String queue = "shaft.temp.report";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getQueue() {
            return queue;
        }

        public void setQueue(String queue) {
            this.queue = queue;
        }
    }

    public static class Alarm {
        private BigDecimal temperatureThreshold = new BigDecimal("70.0");
        private BigDecimal temperatureDiffThreshold = new BigDecimal("20.0");
        private BigDecimal riseRateThreshold = new BigDecimal("10.0");
        private BigDecimal fiberBreakThreshold = BigDecimal.ZERO;
        private long offlineThresholdSeconds = 30L;
        private int windowSize = 20;

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
}
