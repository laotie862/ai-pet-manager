package com.example.demo.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DeviceAutoStartRunner {
    private static final Logger log = LoggerFactory.getLogger(DeviceAutoStartRunner.class);

    private final DeviceProperties properties;
    private final DeviceRepository deviceRepository;
    private final DeviceStreamManager streamManager;

    public DeviceAutoStartRunner(
            DeviceProperties properties,
            DeviceRepository deviceRepository,
            DeviceStreamManager streamManager
    ) {
        this.properties = properties;
        this.deviceRepository = deviceRepository;
        this.streamManager = streamManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void autoStartAssignedDevices() {
        if (!properties.isAutoStartEnabled()) {
            return;
        }
        for (DeviceRecord device : deviceRepository.listAssignedDevices()) {
            try {
                streamManager.start(device);
            } catch (Exception exception) {
                log.warn("Auto start failed for device {}", device.id(), exception);
            }
        }
    }
}
