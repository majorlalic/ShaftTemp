package com.example.demo.ingest.service;

import com.example.demo.persistence.entity.DeviceEntity;
import com.example.demo.persistence.entity.MonitorPartitionBindEntity;
import com.example.demo.persistence.entity.MonitorEntity;
import com.example.demo.persistence.entity.ShaftFloorEntity;
import com.example.demo.persistence.repository.DeviceRepository;
import com.example.demo.persistence.repository.MonitorPartitionBindRepository;
import com.example.demo.persistence.repository.MonitorRepository;
import com.example.demo.persistence.repository.ShaftFloorRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DeviceResolverService {

    private final DeviceRepository deviceRepository;
    private final MonitorRepository monitorRepository;
    private final MonitorPartitionBindRepository monitorPartitionBindRepository;
    private final ShaftFloorRepository shaftFloorRepository;

    public DeviceResolverService(
        DeviceRepository deviceRepository,
        MonitorRepository monitorRepository,
        MonitorPartitionBindRepository monitorPartitionBindRepository,
        ShaftFloorRepository shaftFloorRepository
    ) {
        this.deviceRepository = deviceRepository;
        this.monitorRepository = monitorRepository;
        this.monitorPartitionBindRepository = monitorPartitionBindRepository;
        this.shaftFloorRepository = shaftFloorRepository;
    }

    public ResolvedTarget resolve(String dataReference, String partitionCode) {
        MonitorPartitionBindEntity binding = resolveBinding(dataReference, partitionCode);
        DeviceEntity device = deviceRepository.findActiveById(binding.getDeviceId())
            .orElseThrow(() -> new IllegalArgumentException("Device not found for binding deviceId=" + binding.getDeviceId()));
        MonitorEntity monitor = monitorRepository.findById(binding.getMonitorId())
            .orElseThrow(() -> new IllegalArgumentException("Monitor not found for binding monitorId=" + binding.getMonitorId()));
        ShaftFloorEntity shaftFloor = null;
        if (binding.getShaftFloorId() != null) {
            shaftFloor = shaftFloorRepository.findActiveById(binding.getShaftFloorId()).orElse(null);
        }
        return ResolvedTarget.fromBinding(device, monitor, shaftFloor, binding);
    }

    private MonitorPartitionBindEntity resolveBinding(String dataReference, String partitionCode) {
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

    public static class ResolvedTarget {
        private final DeviceEntity device;
        private final MonitorEntity monitor;
        private final ShaftFloorEntity shaftFloor;
        private final String partitionCode;
        private final String partitionName;
        private final String dataReference;
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
                binding.getDeviceToken(),
                binding.getPartitionNo(),
                "MQ_PARTITION"
            );
        }

        public static ResolvedTarget forDevice(DeviceEntity device, MonitorEntity monitor) {
            return new ResolvedTarget(device, monitor, null, null, null, null, null, null, "MQ_PARTITION");
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
