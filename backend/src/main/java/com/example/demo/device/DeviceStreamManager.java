package com.example.demo.device;

import com.example.demo.common.api.ErrorCode;
import com.example.demo.common.exception.BusinessException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class DeviceStreamManager {
    private static final Logger log = LoggerFactory.getLogger(DeviceStreamManager.class);

    private final DeviceProperties properties;
    private final DeviceRepository deviceRepository;
    private final DeviceCredentialCrypto credentialCrypto;
    private final RtspUrlSupport rtspUrlSupport;
    private final LocalWebcamSupport localWebcamSupport;
    private final DeviceStatusCache statusCache;
    private final Map<Long, ManagedStream> streams = new ConcurrentHashMap<>();
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();
    private final ScheduledExecutorService restartExecutor = Executors.newScheduledThreadPool(2);

    public DeviceStreamManager(
            DeviceProperties properties,
            DeviceRepository deviceRepository,
            DeviceCredentialCrypto credentialCrypto,
            RtspUrlSupport rtspUrlSupport,
            LocalWebcamSupport localWebcamSupport,
            DeviceStatusCache statusCache
    ) {
        this.properties = properties;
        this.deviceRepository = deviceRepository;
        this.credentialCrypto = credentialCrypto;
        this.rtspUrlSupport = rtspUrlSupport;
        this.localWebcamSupport = localWebcamSupport;
        this.statusCache = statusCache;
    }

    public DeviceStreamSnapshot start(DeviceRecord device) {
        return start(device, 0);
    }

    private DeviceStreamSnapshot start(DeviceRecord device, int restartAttempts) {
        ManagedStream existing = streams.get(device.id());
        if (existing != null && existing.isRunning()) {
            return existing.snapshot();
        }

        try {
            ManagedStream stream = new ManagedStream(device, restartAttempts);
            Process process = new ProcessBuilder(buildCommand(device))
                    .redirectErrorStream(false)
                    .start();
            stream.attach(process);
            streams.put(device.id(), stream);
            statusCache.put(device.id(), DeviceStatus.ANALYZING);
            deviceRepository.updateRuntimeState(device.id(), DeviceStatus.ANALYZING, null, true);

            streamExecutor.submit(() -> readFrames(stream));
            streamExecutor.submit(() -> drainErrors(stream));
            streamExecutor.submit(() -> monitor(stream));
            return stream.snapshot();
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Failed to start FFmpeg for device {}", device.id(), exception);
            deviceRepository.updateRuntimeState(device.id(), DeviceStatus.OFFLINE, "FFmpeg start failed", false);
            statusCache.put(device.id(), DeviceStatus.OFFLINE);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "FFmpeg start failed");
        }
    }

    public void stop(Long deviceId) {
        ManagedStream stream = streams.remove(deviceId);
        if (stream == null) {
            return;
        }
        stream.stop();
        deviceRepository.updateRuntimeState(deviceId, DeviceStatus.ONLINE, null, false);
        statusCache.put(deviceId, DeviceStatus.ONLINE);
    }

    public Optional<byte[]> latestFrame(Long deviceId) {
        ManagedStream stream = streams.get(deviceId);
        if (stream == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(stream.latestFrame());
    }

    public DeviceStreamSnapshot snapshot(Long deviceId) {
        ManagedStream stream = streams.get(deviceId);
        if (stream != null) {
            return stream.snapshot();
        }
        DeviceStatus status = statusCache.get(deviceId).orElse(DeviceStatus.OFFLINE);
        return DeviceStreamSnapshot.stopped(deviceId, status, null);
    }

    @PreDestroy
    public void shutdown() {
        streams.keySet().forEach(this::stop);
        streamExecutor.shutdownNow();
        restartExecutor.shutdownNow();
    }

    private List<String> buildCommand(DeviceRecord device) {
        List<String> command = new ArrayList<>();
        command.add(properties.getFfmpegPath());
        command.add("-hide_banner");
        command.add("-loglevel");
        command.add("error");

        if (rtspUrlSupport.isLoopVideoSource(device.rtspUrl())) {
            command.add("-stream_loop");
            command.add("-1");
            command.add("-re");
            command.add("-i");
            command.add(rtspUrlSupport.loopVideoPath(device.rtspUrl()));
        } else if (rtspUrlSupport.isMockSource(device.rtspUrl())) {
            command.add("-f");
            command.add("lavfi");
            command.add("-re");
            command.add("-i");
            command.add("testsrc=size=" + properties.getStreamWidth() + "x360:rate="
                    + Math.max(1, properties.getStreamFrameRate()));
        } else if (rtspUrlSupport.isLoopVideoSource(device.rtspUrl())) {
            String path = rtspUrlSupport.queryParameter(device.rtspUrl(), "path");
            if (path == null || path.isBlank()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Loop video source requires path query parameter");
            }
            command.add("-stream_loop");
            command.add("-1");
            command.add("-re");
            command.add("-i");
            command.add(path);
        } else if (rtspUrlSupport.isLocalWebcamSource(device.rtspUrl())) {
            localWebcamSupport.appendInputArguments(command, device.rtspUrl());
        } else {
            command.add("-rtsp_transport");
            command.add("tcp");
            command.add("-i");
            command.add(rtspUrlSupport.withCredentials(
                    device.rtspUrl(),
                    device.rtspUsername(),
                    credentialCrypto.decrypt(device.rtspPasswordCipher())
            ));
        }

        command.add("-vf");
        command.add("fps=" + Math.max(1, properties.getStreamFrameRate()) + ",scale="
                + Math.max(160, properties.getStreamWidth()) + ":-1");
        command.add("-q:v");
        command.add("6");
        command.add("-f");
        command.add("image2pipe");
        command.add("-vcodec");
        command.add("mjpeg");
        command.add("pipe:1");
        return command;
    }

    private void readFrames(ManagedStream stream) {
        try (InputStream inputStream = stream.process().getInputStream()) {
            ByteArrayOutputStream frame = new ByteArrayOutputStream(256 * 1024);
            boolean inFrame = false;
            int previous = -1;
            int current;
            while ((current = inputStream.read()) != -1 && !stream.intentionalStop()) {
                if (!inFrame) {
                    if (previous == 0xFF && current == 0xD8) {
                        inFrame = true;
                        frame.reset();
                        frame.write(0xFF);
                        frame.write(0xD8);
                    }
                } else {
                    frame.write(current);
                    if (previous == 0xFF && current == 0xD9) {
                        stream.setLatestFrame(frame.toByteArray());
                        touchHeartbeat(stream);
                        inFrame = false;
                        frame.reset();
                    }
                }
                previous = current;
            }
        } catch (Exception exception) {
            if (!stream.intentionalStop()) {
                log.warn("Frame reader stopped for device {}", stream.device().id(), exception);
            }
        }
    }

    private void drainErrors(ManagedStream stream) {
        try (InputStream inputStream = stream.process().getErrorStream()) {
            byte[] buffer = new byte[1024];
            while (inputStream.read(buffer) != -1 && !stream.intentionalStop()) {
                // Draining stderr prevents FFmpeg from blocking when it emits diagnostics.
            }
        } catch (Exception exception) {
            if (!stream.intentionalStop()) {
                log.debug("FFmpeg stderr drain stopped for device {}", stream.device().id(), exception);
            }
        }
    }

    private void monitor(ManagedStream stream) {
        try {
            int exitCode = stream.process().waitFor();
            streams.remove(stream.device().id(), stream);
            if (stream.intentionalStop()) {
                return;
            }

            String error = "FFmpeg exited with code " + exitCode;
            log.warn("{} for device {}", error, stream.device().id());
            deviceRepository.updateRuntimeState(stream.device().id(), DeviceStatus.OFFLINE, error, false);
            statusCache.put(stream.device().id(), DeviceStatus.OFFLINE);

            int attempt = stream.nextRestartAttempt();
            if (attempt <= properties.getMaxRestartAttempts()) {
                restartExecutor.schedule(
                        () -> restart(stream, attempt),
                        properties.getRestartDelaySeconds(),
                        TimeUnit.SECONDS
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void restart(ManagedStream previous, int restartAttempts) {
        deviceRepository.findById(previous.device().id()).ifPresent(device -> start(device, restartAttempts));
    }

    private void touchHeartbeat(ManagedStream stream) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime lastWrite = stream.lastHeartbeatWrite();
        stream.setLastFrameAt(now);
        if (lastWrite == null || Duration.between(lastWrite, now).toSeconds() >= 5) {
            stream.setLastHeartbeatWrite(now);
            deviceRepository.touchHeartbeat(stream.device().id());
            statusCache.put(stream.device().id(), DeviceStatus.ANALYZING);
        }
    }

    private static final class ManagedStream {
        private final DeviceRecord device;
        private volatile Process process;
        private volatile boolean intentionalStop;
        private volatile byte[] latestFrame;
        private volatile OffsetDateTime lastFrameAt;
        private volatile OffsetDateTime lastHeartbeatWrite;
        private volatile int restartAttempts;

        private ManagedStream(DeviceRecord device, int restartAttempts) {
            this.device = device;
            this.restartAttempts = restartAttempts;
        }

        private void attach(Process process) {
            this.process = process;
        }

        private DeviceRecord device() {
            return device;
        }

        private Process process() {
            return process;
        }

        private boolean isRunning() {
            return process != null && process.isAlive();
        }

        private boolean intentionalStop() {
            return intentionalStop;
        }

        private void stop() {
            intentionalStop = true;
            if (process != null) {
                process.destroy();
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            }
        }

        private byte[] latestFrame() {
            return latestFrame;
        }

        private void setLatestFrame(byte[] latestFrame) {
            this.latestFrame = latestFrame;
        }

        private void setLastFrameAt(OffsetDateTime lastFrameAt) {
            this.lastFrameAt = lastFrameAt;
        }

        private OffsetDateTime lastHeartbeatWrite() {
            return lastHeartbeatWrite;
        }

        private void setLastHeartbeatWrite(OffsetDateTime lastHeartbeatWrite) {
            this.lastHeartbeatWrite = lastHeartbeatWrite;
        }

        private int nextRestartAttempt() {
            restartAttempts++;
            return restartAttempts;
        }

        private DeviceStreamSnapshot snapshot() {
            return new DeviceStreamSnapshot(
                    device.id(),
                    isRunning() ? DeviceStatus.ANALYZING : DeviceStatus.OFFLINE,
                    isRunning(),
                    latestFrame != null,
                    lastFrameAt,
                    null
            );
        }
    }
}
