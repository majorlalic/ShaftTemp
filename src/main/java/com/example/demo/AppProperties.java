package com.example.demo;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shaft")
public class AppProperties {

    private final Alarm alarm = new Alarm();
    private final Inspection inspection = new Inspection();

    public Alarm getAlarm() {
        return alarm;
    }

    public Inspection getInspection() {
        return inspection;
    }

    public static class Alarm {
        private BigDecimal temperatureThreshold = new BigDecimal("70.0");
        private long offlineThresholdSeconds = 30L;
        private int windowSize = 20;

        public BigDecimal getTemperatureThreshold() {
            return temperatureThreshold;
        }

        public void setTemperatureThreshold(BigDecimal temperatureThreshold) {
            this.temperatureThreshold = temperatureThreshold;
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
