package com.example.demo.service;

import com.example.demo.config.AppProperties;
import com.example.demo.service.DeviceResolverService;
import com.csg.dgri.szsiom.sysmanage.model.AlarmRuleVO;
import com.csg.dgri.szsiom.sysmanage.appservice.AlarmRuleAppService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AlarmRuleResolverService {

    private final AlarmRuleAppService<?> alarmRuleRepository;
    private final AppProperties appProperties;
    private final AtomicReference<List<AlarmRuleVO>> cache = new AtomicReference<List<AlarmRuleVO>>(new ArrayList<AlarmRuleVO>());

    public AlarmRuleResolverService(AlarmRuleAppService<?> alarmRuleRepository, AppProperties appProperties) {
        this.alarmRuleRepository = alarmRuleRepository;
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void warmup() {
        refresh();
    }

    @Scheduled(fixedDelayString = "${shaft.cache.partition-binding-refresh-ms:300000}")
    public void refresh() {
        cache.set(alarmRuleRepository.findAllActive().stream()
            .filter(rule -> rule.getEnabled() == null || rule.getEnabled().intValue() == 1)
            .collect(Collectors.toList()));
    }

    public RuleConfig resolveMonitorRule(DeviceResolverService.ResolvedTarget resolved, String alarmType) {
        AlarmRuleVO matched = resolveBest("MONITOR", alarmType);
        if (matched == null) {
            return fallbackMonitorRule(alarmType);
        }
        return toConfig(matched, fallbackThreshold(alarmType));
    }

    public RuleConfig resolveDeviceRule(DeviceResolverService.ResolvedTarget resolved, String alarmType) {
        AlarmRuleVO matched = resolveBest("DEVICE", alarmType);
        if (matched == null) {
            return fallbackDeviceRule(alarmType);
        }
        return toConfig(matched, fallbackThreshold(alarmType));
    }

    private AlarmRuleVO resolveBest(String bizType, String alarmType) {
        return cache.get().stream()
            .filter(rule -> bizType.equals(rule.getBizType()))
            .filter(rule -> alarmType.equals(rule.getAlarmType()))
            .filter(this::isGlobalRule)
            .findFirst()
            .orElse(null);
    }

    private boolean isGlobalRule(AlarmRuleVO rule) {
        String scopeType = rule.getScopeType();
        return scopeType == null || "GLOBAL".equals(scopeType);
    }

    private RuleConfig fallbackMonitorRule(String alarmType) {
        if ("TEMP_THRESHOLD".equals(alarmType)) {
            return new RuleConfig(true, 2, appProperties.getAlarm().getTemperatureThreshold(), null);
        }
        if ("TEMP_DIFFERENCE".equals(alarmType)) {
            return new RuleConfig(true, 2, appProperties.getAlarm().getTemperatureDiffThreshold(), null);
        }
        if ("TEMP_RISE_RATE".equals(alarmType)) {
            return new RuleConfig(true, 2, appProperties.getAlarm().getRiseRateThreshold(), null);
        }
        return new RuleConfig(false, 2, null, null);
    }

    private RuleConfig fallbackDeviceRule(String alarmType) {
        if ("DEVICE_OFFLINE".equals(alarmType)) {
            return new RuleConfig(true, 2, BigDecimal.valueOf(appProperties.getAlarm().getOfflineThresholdSeconds()), null);
        }
        if ("PARTITION_FAULT".equals(alarmType)) {
            return new RuleConfig(true, 3, null, null);
        }
        return new RuleConfig(false, 2, null, null);
    }

    private BigDecimal fallbackThreshold(String alarmType) {
        if ("TEMP_THRESHOLD".equals(alarmType)) {
            return appProperties.getAlarm().getTemperatureThreshold();
        }
        if ("TEMP_DIFFERENCE".equals(alarmType)) {
            return appProperties.getAlarm().getTemperatureDiffThreshold();
        }
        if ("TEMP_RISE_RATE".equals(alarmType)) {
            return appProperties.getAlarm().getRiseRateThreshold();
        }
        if ("DEVICE_OFFLINE".equals(alarmType)) {
            return BigDecimal.valueOf(appProperties.getAlarm().getOfflineThresholdSeconds());
        }
        return null;
    }

    private RuleConfig toConfig(AlarmRuleVO entity, BigDecimal fallbackThreshold) {
        return new RuleConfig(
            entity.getEnabled() == null || entity.getEnabled().intValue() == 1,
            entity.getLevel() == null ? 2 : entity.getLevel().intValue(),
            entity.getThresholdValue() == null ? fallbackThreshold : entity.getThresholdValue(),
            entity.getDurationSeconds()
        );
    }

    public static class RuleConfig {
        private final boolean enabled;
        private final int level;
        private final BigDecimal thresholdValue;
        private final Integer durationSeconds;

        public RuleConfig(boolean enabled, int level, BigDecimal thresholdValue, Integer durationSeconds) {
            this.enabled = enabled;
            this.level = level;
            this.thresholdValue = thresholdValue;
            this.durationSeconds = durationSeconds;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public int getLevel() {
            return level;
        }

        public BigDecimal getThresholdValue() {
            return thresholdValue;
        }

        public Integer getDurationSeconds() {
            return durationSeconds;
        }
    }
}
