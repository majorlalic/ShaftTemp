package com.example.demo.service;

import com.example.demo.config.AppProperties;
import com.csg.dgri.szsiom.sysmanage.model.DeviceVO;
import com.csg.dgri.szsiom.sysmanage.model.MonitorPartitionBindVO;
import com.csg.dgri.szsiom.sysmanage.model.MonitorVO;
import com.csg.dgri.szsiom.sysmanage.model.ShaftFloorVO;
import com.csg.dgri.szsiom.sysmanage.appservice.DeviceAppService;
import com.csg.dgri.szsiom.sysmanage.appservice.MonitorPartitionBindAppService;
import com.csg.dgri.szsiom.sysmanage.appservice.MonitorAppService;
import com.csg.dgri.szsiom.sysmanage.appservice.ShaftFloorAppService;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DeviceResolverService {

    private final DeviceAppService<?> deviceRepository;
    private final MonitorAppService<?> monitorRepository;
    private final MonitorPartitionBindAppService<?> monitorPartitionBindRepository;
    private final ShaftFloorAppService<?> shaftFloorRepository;
    private final AppProperties appProperties;
    private final ConcurrentHashMap<String, ResolvedTarget> dataReferenceCache = new ConcurrentHashMap<String, ResolvedTarget>();
    private final ConcurrentHashMap<String, ResolvedTarget> partitionCodeCache = new ConcurrentHashMap<String, ResolvedTarget>();

    public DeviceResolverService(
        DeviceAppService<?> deviceRepository,
        MonitorAppService<?> monitorRepository,
        MonitorPartitionBindAppService<?> monitorPartitionBindRepository,
        ShaftFloorAppService<?> shaftFloorRepository,
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

    private MonitorPartitionBindVO resolveBinding(String iotCode, Integer partitionId, String dataReference, String partitionCode) {
        if (iotCode != null && !iotCode.trim().isEmpty() && partitionId != null) {
            Optional<DeviceVO> deviceOptional = deviceRepository.findActiveByIotCode(iotCode);
            if (deviceOptional.isPresent()) {
                Optional<MonitorPartitionBindVO> byPartitionId = monitorPartitionBindRepository
                    .findActiveByDeviceAndPartitionId(deviceOptional.get().getId(), partitionId);
                if (byPartitionId.isPresent()) {
                    return byPartitionId.get();
                }
            }
        }
        Optional<MonitorPartitionBindVO> byReference = Optional.empty();
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

    private ResolvedTarget buildResolvedTarget(MonitorPartitionBindVO binding) {
        DeviceVO device = deviceRepository.findActiveById(binding.getDeviceId())
            .orElseThrow(() -> new IllegalArgumentException("Device not found for binding deviceId=" + binding.getDeviceId()));
        MonitorVO monitor = monitorRepository.findActiveById(binding.getMonitorId())
            .orElseThrow(() -> new IllegalArgumentException("Monitor not found for binding monitorId=" + binding.getMonitorId()));
        ShaftFloorVO shaftFloor = null;
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
        List<MonitorPartitionBindVO> bindings = monitorPartitionBindRepository.findAllActive();
        ConcurrentHashMap<String, ResolvedTarget> nextByReference = new ConcurrentHashMap<String, ResolvedTarget>();
        ConcurrentHashMap<String, ResolvedTarget> nextByPartition = new ConcurrentHashMap<String, ResolvedTarget>();
        for (MonitorPartitionBindVO binding : bindings) {
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
        private final DeviceVO device;
        private final MonitorVO monitor;
        private final ShaftFloorVO shaftFloor;
        private final String partitionCode;
        private final String partitionName;
        private final String dataReference;
        private final Integer partitionId;
        private final String deviceToken;
        private final Integer partitionNo;
        private final String sourceFormat;

        public ResolvedTarget(
            DeviceVO device,
            MonitorVO monitor,
            ShaftFloorVO shaftFloor,
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
            DeviceVO device,
            MonitorVO monitor,
            ShaftFloorVO shaftFloor,
            MonitorPartitionBindVO binding
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

        public static ResolvedTarget forDevice(DeviceVO device, MonitorVO monitor) {
            return new ResolvedTarget(device, monitor, null, null, null, null, null, null, null, "MQ_PARTITION");
        }

        public DeviceVO getDevice() {
            return device;
        }

        public MonitorVO getMonitor() {
            return monitor;
        }

        public ShaftFloorVO getShaftFloor() {
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
