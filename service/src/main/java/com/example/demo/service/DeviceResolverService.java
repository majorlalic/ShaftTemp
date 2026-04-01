package com.example.demo.service;

import com.example.demo.config.AppProperties;
import com.example.demo.entity.DeviceEntity;
import com.example.demo.entity.MonitorPartitionBindEntity;
import com.example.demo.entity.MonitorEntity;
import com.example.demo.entity.ShaftFloorEntity;
import com.example.demo.dao.DeviceRepository;
import com.example.demo.dao.MonitorPartitionBindRepository;
import com.example.demo.dao.MonitorRepository;
import com.example.demo.dao.ShaftFloorRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DeviceResolverService {

    private final DeviceRepository deviceRepository;
    private final MonitorRepository monitorRepository;
    private final MonitorPartitionBindRepository monitorPartitionBindRepository;
    private final ShaftFloorRepository shaftFloorRepository;
    private final AppProperties appProperties;
    private final ConcurrentHashMap<String, ResolvedTarget> dataReferenceCache = new ConcurrentHashMap<String, ResolvedTarget>();
    private final ConcurrentHashMap<String, ResolvedTarget> partitionCodeCache = new ConcurrentHashMap<String, ResolvedTarget>();

    public DeviceResolverService(
        DeviceRepository deviceRepository,
        MonitorRepository monitorRepository,
        MonitorPartitionBindRepository monitorPartitionBindRepository,
        ShaftFloorRepository shaftFloorRepository,
        AppProperties appProperties
    ) {
        this.deviceRepository = deviceRepository;
        this.monitorRepository = monitorRepository;
        this.monitorPartitionBindRepository = monitorPartitionBindRepository;
        this.shaftFloorRepository = shaftFloorRepository;
        this.appProperties = appProperties;
    }

    public ResolvedTarget resolve(String iotCode, Integer partitionId, String dataReference, String partitionCode) {
        if (dataReference != null && !dataReference.trim().isEmpty()) {
            ResolvedTarget cachedByReference = dataReferenceCache.get(dataReference);
            if (cachedByReference != null) {
                return cachedByReference;
            }
        }
        if (partitionCode != null && !partitionCode.trim().isEmpty()) {
            ResolvedTarget cachedByPartition = partitionCodeCache.get(partitionCode);
            if (cachedByPartition != null) {
                return cachedByPartition;
            }
        }
        ResolvedTarget resolved = buildResolvedTarget(resolveBinding(iotCode, partitionId, dataReference, partitionCode));
        cacheResolvedTarget(resolved);
        return resolved;
    }

    private MonitorPartitionBindEntity resolveBinding(String iotCode, Integer partitionId, String dataReference, String partitionCode) {
        if (iotCode != null && !iotCode.trim().isEmpty() && partitionId != null) {
            Optional<DeviceEntity> deviceOptional = deviceRepository.findActiveByIotCode(iotCode);
            if (deviceOptional.isPresent()) {
                Optional<MonitorPartitionBindEntity> byPartitionId = monitorPartitionBindRepository
                    .findActiveByDeviceAndPartitionId(deviceOptional.get().getId(), partitionId);
                if (byPartitionId.isPresent()) {
                    return byPartitionId.get();
                }
            }
        }
        Optional<MonitorPartitionBindEntity> byReference = Optional.empty();
        if (dataReference != null && !dataReference.trim().isEmpty()) {
            byReference = monitorPartitionBindRepository.findActiveByDataReference(dataReference);
        }
        if (byReference.isPresent()) {
            return byReference.get();
        }
        if (partitionCode != null && !partitionCode.trim().isEmpty()) {
            return monitorPartitionBindRepository.findActiveByPartitionCode(partitionCode)
                .orElseThrow(() -> new IllegalArgumentException("Partition binding not found for partitionCode=" + partitionCode));
        }
        throw new IllegalArgumentException("Partition binding not found");
    }

    private ResolvedTarget buildResolvedTarget(MonitorPartitionBindEntity binding) {
        DeviceEntity device = deviceRepository.findActiveById(binding.getDeviceId())
            .orElseThrow(() -> new IllegalArgumentException("Device not found for binding deviceId=" + binding.getDeviceId()));
        MonitorEntity monitor = monitorRepository.findActiveById(binding.getMonitorId())
            .orElseThrow(() -> new IllegalArgumentException("Monitor not found for binding monitorId=" + binding.getMonitorId()));
        ShaftFloorEntity shaftFloor = null;
        if (binding.getShaftFloorId() != null) {
            shaftFloor = shaftFloorRepository.findActiveById(binding.getShaftFloorId()).orElse(null);
        }
        return ResolvedTarget.fromBinding(device, monitor, shaftFloor, binding);
    }

    private void cacheResolvedTarget(ResolvedTarget resolved) {
        if (resolved.getDataReference() != null && !resolved.getDataReference().trim().isEmpty()) {
            dataReferenceCache.put(resolved.getDataReference(), resolved);
        }
        if (resolved.getPartitionCode() != null && !resolved.getPartitionCode().trim().isEmpty()) {
            partitionCodeCache.put(resolved.getPartitionCode(), resolved);
        }
    }

    @PostConstruct
    public void warmupBindingCache() {
        refreshBindingCache();
    }

    @Scheduled(fixedDelayString = "${shaft.cache.partition-binding-refresh-ms:300000}")
    public void refreshBindingCache() {
        List<MonitorPartitionBindEntity> bindings = monitorPartitionBindRepository.findAllActive();
        ConcurrentHashMap<String, ResolvedTarget> nextByReference = new ConcurrentHashMap<String, ResolvedTarget>();
        ConcurrentHashMap<String, ResolvedTarget> nextByPartition = new ConcurrentHashMap<String, ResolvedTarget>();
        for (MonitorPartitionBindEntity binding : bindings) {
            ResolvedTarget resolved = buildResolvedTarget(binding);
            if (resolved.getDataReference() != null && !resolved.getDataReference().trim().isEmpty()) {
                nextByReference.put(resolved.getDataReference(), resolved);
            }
            if (resolved.getPartitionCode() != null && !resolved.getPartitionCode().trim().isEmpty()) {
                nextByPartition.put(resolved.getPartitionCode(), resolved);
            }
        }
        dataReferenceCache.clear();
        dataReferenceCache.putAll(nextByReference);
        partitionCodeCache.clear();
        partitionCodeCache.putAll(nextByPartition);
    }

    public static class ResolvedTarget {
        private final DeviceEntity device;
        private final MonitorEntity monitor;
        private final ShaftFloorEntity shaftFloor;
        private final String partitionCode;
        private final String partitionName;
        private final String dataReference;
        private final Integer partitionId;
        private final String deviceToken;
        private final Integer partitionNo;
        private final String sourceFormat;

        public ResolvedTarget(
            DeviceEntity device,
            MonitorEntity monitor,
            ShaftFloorEntity shaftFloor,
            String partitionCode,
            String partitionName,
            String dataReference,
            Integer partitionId,
            String deviceToken,
            Integer partitionNo,
            String sourceFormat
        ) {
            this.device = device;
            this.monitor = monitor;
            this.shaftFloor = shaftFloor;
            this.partitionCode = partitionCode;
            this.partitionName = partitionName;
            this.dataReference = dataReference;
            this.partitionId = partitionId;
            this.deviceToken = deviceToken;
            this.partitionNo = partitionNo;
            this.sourceFormat = sourceFormat;
        }

        public static ResolvedTarget fromBinding(
            DeviceEntity device,
            MonitorEntity monitor,
            ShaftFloorEntity shaftFloor,
            MonitorPartitionBindEntity binding
        ) {
            return new ResolvedTarget(
                device,
                monitor,
                shaftFloor,
                binding.getPartitionCode(),
                binding.getPartitionName() == null ? binding.getPartitionCode() : binding.getPartitionName(),
                binding.getDataReference(),
                binding.getPartitionId(),
                binding.getDeviceToken(),
                binding.getPartitionNo(),
                "MQ_PARTITION"
            );
        }

        public static ResolvedTarget forDevice(DeviceEntity device, MonitorEntity monitor) {
            return new ResolvedTarget(device, monitor, null, null, null, null, null, null, null, "MQ_PARTITION");
        }

        public DeviceEntity getDevice() {
            return device;
        }

        public MonitorEntity getMonitor() {
            return monitor;
        }

        public ShaftFloorEntity getShaftFloor() {
            return shaftFloor;
        }

        public Long getShaftFloorId() {
            return shaftFloor == null ? null : shaftFloor.getId();
        }

        public String getPartitionCode() {
            return partitionCode;
        }

        public String getPartitionName() {
            return partitionName;
        }

        public String getDataReference() {
            return dataReference;
        }

        public Integer getPartitionId() {
            return partitionId;
        }

        public String getDeviceToken() {
            return deviceToken;
        }

        public Integer getPartitionNo() {
            return partitionNo;
        }

        public String getSourceFormat() {
            return sourceFormat;
        }
    }
}
