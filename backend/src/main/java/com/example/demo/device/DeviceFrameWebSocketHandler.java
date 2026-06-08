package com.example.demo.device;

import com.example.demo.common.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DeviceFrameWebSocketHandler extends TextWebSocketHandler {
    private static final Pattern DEVICE_STREAM_PATH = Pattern.compile("/ws/devices/(\\d+)/stream");

    private final DeviceRepository deviceRepository;
    private final DeviceStreamManager streamManager;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService senderExecutor = Executors.newScheduledThreadPool(4);
    private final Map<String, ScheduledFuture<?>> sendTasks = new ConcurrentHashMap<>();

    public DeviceFrameWebSocketHandler(DeviceRepository deviceRepository, DeviceStreamManager streamManager) {
        this.deviceRepository = deviceRepository;
        this.streamManager = streamManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        CurrentUser currentUser = (CurrentUser) session.getAttributes()
                .get(DeviceStreamHandshakeInterceptor.CURRENT_USER_ATTRIBUTE);
        Long deviceId = parseDeviceId(session.getUri());
        Optional<DeviceRecord> device = deviceRepository.findByIdAndUser(deviceId, currentUser.id());
        if (device.isEmpty()) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Device not found"));
            return;
        }

        streamManager.start(device.get());
        ScheduledFuture<?> future = senderExecutor.scheduleAtFixedRate(
                () -> sendFrame(session, deviceId),
                0,
                500,
                TimeUnit.MILLISECONDS
        );
        sendTasks.put(session.getId(), future);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        ScheduledFuture<?> future = sendTasks.remove(session.getId());
        if (future != null) {
            future.cancel(true);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
        session.close(CloseStatus.SERVER_ERROR);
    }

    @PreDestroy
    public void shutdown() {
        sendTasks.values().forEach(task -> task.cancel(true));
        senderExecutor.shutdownNow();
    }

    private void sendFrame(WebSocketSession session, Long deviceId) {
        if (!session.isOpen()) {
            return;
        }
        try {
            DeviceStreamSnapshot snapshot = streamManager.snapshot(deviceId);
            Optional<byte[]> frame = streamManager.latestFrame(deviceId);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "frame");
            payload.put("deviceId", deviceId);
            payload.put("status", snapshot.status().name());
            payload.put("running", snapshot.running());
            payload.put("frameReady", frame.isPresent());
            payload.put("lastFrameAt", snapshot.lastFrameAt() == null ? null : snapshot.lastFrameAt().toString());
            payload.put("serverTime", OffsetDateTime.now().toString());
            frame.ifPresent(bytes -> payload.put("imageBase64", Base64.getEncoder().encodeToString(bytes)));
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
                }
            }
        } catch (Exception exception) {
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (Exception ignored) {
                // Session is already closing; nothing else to do.
            }
        }
    }

    private Long parseDeviceId(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("Missing WebSocket URI");
        }
        Matcher matcher = DEVICE_STREAM_PATH.matcher(uri.getPath());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid device stream path");
        }
        return Long.parseLong(matcher.group(1));
    }
}
