package com.example.demo.behavior;

import com.example.demo.device.DeviceRecord;
import com.example.demo.device.DeviceRepository;
import com.example.demo.device.DeviceStreamManager;
import com.example.demo.device.RoiPoint;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BehaviorAnalysisService {
    private static final Logger log = LoggerFactory.getLogger(BehaviorAnalysisService.class);
    private static final TypeReference<List<RoiPoint>> ROI_TYPE = new TypeReference<>() {
    };

    private final BehaviorProperties properties;
    private final DeviceRepository deviceRepository;
    private final DeviceStreamManager streamManager;
    private final CvInferenceClient cvInferenceClient;
    private final BehaviorRepository behaviorRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<Long, DeviceBehaviorState> states = new ConcurrentHashMap<>();

    public BehaviorAnalysisService(
            BehaviorProperties properties,
            DeviceRepository deviceRepository,
            DeviceStreamManager streamManager,
            CvInferenceClient cvInferenceClient,
            BehaviorRepository behaviorRepository
    ) {
        this.properties = properties;
        this.deviceRepository = deviceRepository;
        this.streamManager = streamManager;
        this.cvInferenceClient = cvInferenceClient;
        this.behaviorRepository = behaviorRepository;
    }

    @Scheduled(
            fixedDelayString = "${petcare.behavior.analysis-interval-ms:5000}",
            initialDelayString = "${petcare.behavior.analysis-interval-ms:5000}"
    )
    public void analyzeAssignedDevices() {
        if (!properties.isAnalysisEnabled()) {
            return;
        }
        for (DeviceRecord device : deviceRepository.listAssignedDevices()) {
            analyze(device);
        }
    }

    private void analyze(DeviceRecord device) {
        streamManager.latestFrame(device.id()).ifPresent(frame -> {
            try {
                BehaviorDetectionResponse detection = cvInferenceClient.detect(new BehaviorDetectionRequest(
                        Base64.getEncoder().encodeToString(frame),
                        String.valueOf(device.id()),
                        readRoi(device.roiPolygonJson())
                ));
                acceptDetection(device, detection);
            } catch (Exception exception) {
                log.warn("Behavior analysis failed for device {}", device.id(), exception);
            }
        });
    }

    private void acceptDetection(DeviceRecord device, BehaviorDetectionResponse detection) {
        String behavior = detection.normalizedBehavior(properties.getMinConfidence());
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        DeviceBehaviorState state = states.computeIfAbsent(device.id(), id -> new DeviceBehaviorState());

        if (!behavior.equals(state.candidateBehavior)) {
            state.candidateBehavior = behavior;
            state.candidateCount = 1;
            state.candidateStartedAt = now;
            return;
        }

        state.candidateCount++;
        if (state.candidateCount < Math.max(1, properties.getStableFrameCount())) {
            return;
        }
        if (behavior.equals(state.activeBehavior)) {
            return;
        }

        switchBehavior(device, state, detection, behavior, now);
    }

    private void switchBehavior(
            DeviceRecord device,
            DeviceBehaviorState state,
            BehaviorDetectionResponse detection,
            String nextBehavior,
            OffsetDateTime now
    ) {
        closeActiveEvent(device, state, now);
        state.activeBehavior = nextBehavior;

        if ("uncertain".equals(nextBehavior)) {
            state.activeEventId = null;
            state.activeStartedAt = null;
            return;
        }

        OffsetDateTime startedAt = state.candidateStartedAt == null ? now : state.candidateStartedAt;
        state.activeEventId = behaviorRepository.createEvent(
                device.petId(),
                device.id(),
                nextBehavior,
                detection.confidence(),
                detection.found(),
                startedAt,
                detection.modelVersion()
        );
        state.activeStartedAt = startedAt;
    }

    private void closeActiveEvent(DeviceRecord device, DeviceBehaviorState state, OffsetDateTime endedAt) {
        if (state.activeEventId == null || state.activeStartedAt == null) {
            return;
        }
        behaviorRepository.completeEvent(state.activeEventId, endedAt);
        long durationSeconds = Duration.between(state.activeStartedAt, endedAt).toSeconds();
        if (durationSeconds >= Math.max(0, properties.getMinEventDurationSeconds())) {
            behaviorRepository.accumulateSummary(
                    device.petId(),
                    LocalDate.ofInstant(state.activeStartedAt.toInstant(), ZoneOffset.UTC),
                    state.activeBehavior,
                    durationSeconds
            );
        }
        state.activeEventId = null;
        state.activeStartedAt = null;
    }

    private List<RoiPoint> readRoi(String roiJson) {
        if (!StringUtils.hasText(roiJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(roiJson, ROI_TYPE);
        } catch (Exception exception) {
            return List.of();
        }
    }

    private static final class DeviceBehaviorState {
        private String candidateBehavior;
        private int candidateCount;
        private OffsetDateTime candidateStartedAt;
        private String activeBehavior;
        private Long activeEventId;
        private OffsetDateTime activeStartedAt;
    }
}
