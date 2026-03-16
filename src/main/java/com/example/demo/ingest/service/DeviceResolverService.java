package com.example.demo.ingest.service;

import com.example.demo.persistence.entity.DeviceEntity;
import com.example.demo.persistence.entity.MonitorEntity;
import com.example.demo.persistence.repository.DeviceRepository;
import com.example.demo.persistence.repository.MonitorRepository;
import org.springframework.stereotype.Service;

@Service
public class DeviceResolverService {

    private final DeviceRepository deviceRepository;
    private final MonitorRepository monitorRepository;

    public DeviceResolverService(DeviceRepository deviceRepository, MonitorRepository monitorRepository) {
        this.deviceRepository = deviceRepository;
        this.monitorRepository = monitorRepository;
    }

    public ResolvedDevice resolve(String iotCode) {
        DeviceEntity device = deviceRepository.findActiveByIotCode(iotCode)
            .orElseThrow(() -> new IllegalArgumentException("Device not found for iotCode=" + iotCode));
        MonitorEntity monitor = monitorRepository.findActiveByDeviceId(device.getId())
            .orElseThrow(() -> new IllegalArgumentException("Monitor not found for deviceId=" + device.getId()));
        return new ResolvedDevice(device, monitor);
    }

    public static class ResolvedDevice {
        private final DeviceEntity device;
        private final MonitorEntity monitor;

        public ResolvedDevice(DeviceEntity device, MonitorEntity monitor) {
            this.device = device;
            this.monitor = monitor;
        }

        public DeviceEntity getDevice() {
            return device;
        }

        public MonitorEntity getMonitor() {
            return monitor;
        }
    }
}
