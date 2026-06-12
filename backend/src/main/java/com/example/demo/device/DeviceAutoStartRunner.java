package com.example.demo.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DeviceAutoStartRunner implements ApplicationRunner {
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

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isAutoStartEnabled()) {
            return;
        }
        for (DeviceRecord device : deviceRepository.listAssignedDevices()) {
            try {
                streamManager.start(device);
            } catch (Exception exception) {
                log.warn("Failed to auto-start device {}", device.id(), exception);
            }
        }
    }
}
