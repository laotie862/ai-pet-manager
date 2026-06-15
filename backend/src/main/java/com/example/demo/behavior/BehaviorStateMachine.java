package com.example.demo.behavior;

import com.example.demo.device.DeviceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class BehaviorStateMachine {
    private static final Logger log = LoggerFactory.getLogger(BehaviorStateMachine.class);

    private final BehaviorProperties properties;
    private final BehaviorRepository behaviorRepository;
    private final Map<Long, DeviceBehaviorState> states = new ConcurrentHashMap<>();

    public BehaviorStateMachine(BehaviorProperties properties, BehaviorRepository behaviorRepository) {
        this.properties = properties;
        this.behaviorRepository = behaviorRepository;
    }

    public void acceptDetection(DeviceRecord device, Long petId, BehaviorDetectionResponse detection) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        DeviceBehaviorState state = stateFor(device.id(), petId);
        String behavior = detection.normalizedBehavior(properties.getMinConfidence());

        log.info(
                "Behavior detection device={} pet={} raw={} normalized={} confidence={} found={}",
                device.id(),
                petId,
                detection.behavior(),
                behavior,
                detection.confidence(),
                detection.found()
        );

        if ("uncertain".equals(behavior)) {
            acceptUncertain(device, petId, state, now);
            return;
        }

        state.uncertainCount = 0;
        state.identityMissCount = 0;
        if (behavior.equals(state.activeBehavior) && state.activeEventId != null) {
            clearCandidate(state);
            return;
        }

        acceptCandidate(device, petId, detection, behavior, state, now);
    }

    public void acceptIdentityMiss(DeviceRecord device, Long petId) {
        DeviceBehaviorState state = stateFor(device.id(), petId);
        state.identityMissCount++;
        state.uncertainCount = 0;
        clearCandidate(state);
        if (state.identityMissCount >= closeFrameCount()) {
            closeActiveEvent(device, petId, state, OffsetDateTime.now(ZoneOffset.UTC), true);
        }
    }

    private void acceptUncertain(DeviceRecord device, Long petId, DeviceBehaviorState state, OffsetDateTime now) {
        state.uncertainCount++;
        state.identityMissCount = 0;
        clearCandidate(state);
        if (state.uncertainCount >= closeFrameCount()) {
            closeActiveEvent(device, petId, state, now, true);
        }
    }

    private void acceptCandidate(
            DeviceRecord device,
            Long petId,
            BehaviorDetectionResponse detection,
            String behavior,
            DeviceBehaviorState state,
            OffsetDateTime now
    ) {
        if (!behavior.equals(state.candidateBehavior)) {
            state.candidateBehavior = behavior;
            state.candidateCount = 1;
            state.candidateStartedAt = now;
            state.candidateConfidenceSum = detection.confidence();
        } else {
            state.candidateCount++;
            state.candidateConfidenceSum += detection.confidence();
        }

        int requiredCount = requiredStableCount(behavior, detection.confidence(), state.activeBehavior);
        if (state.candidateCount < requiredCount) {
            return;
        }

        if (syncPersistedOpenEvent(device, petId, state, behavior, now)) {
            clearCandidate(state);
            return;
        }

        double confidence = state.candidateConfidenceSum / Math.max(1, state.candidateCount);
        switchBehavior(device, petId, state, detection, behavior, confidence, now);
    }

    private int requiredStableCount(String behavior, double confidence, String activeBehavior) {
        if (activeBehavior == null) {
            return 1;
        }
        if (quickEventBehaviors().contains(behavior) && confidence >= properties.getQuickEventMinConfidence()) {
            return Math.max(1, properties.getQuickEventStableFrameCount());
        }
        return Math.max(1, properties.getStableFrameCount());
    }

    private boolean syncPersistedOpenEvent(
            DeviceRecord device,
            Long petId,
            DeviceBehaviorState state,
            String nextBehavior,
            OffsetDateTime now
    ) {
        if (state.activeEventId != null) {
            return false;
        }

        List<BehaviorEventRecord> openEvents = behaviorRepository.openByDeviceAndPet(device.id(), petId);
        if (openEvents == null || openEvents.isEmpty()) {
            return false;
        }

        BehaviorEventRecord latest = openEvents.get(0);
        for (int index = 1; index < openEvents.size(); index++) {
            completePersistedEvent(petId, openEvents.get(index), now);
        }

        if (nextBehavior.equals(latest.behaviorType())) {
            state.activeBehavior = latest.behaviorType();
            state.activeEventId = latest.id();
            state.activeStartedAt = latest.startedAt();
            return true;
        }

        completePersistedEvent(petId, latest, now);
        return false;
    }

    private void switchBehavior(
            DeviceRecord device,
            Long petId,
            DeviceBehaviorState state,
            BehaviorDetectionResponse detection,
            String nextBehavior,
            double confidence,
            OffsetDateTime now
    ) {
        closeActiveEvent(device, petId, state, now, false);
        state.activeBehavior = nextBehavior;
        OffsetDateTime startedAt = state.candidateStartedAt == null ? now : state.candidateStartedAt;
        state.activeEventId = behaviorRepository.createEvent(
                petId,
                device.id(),
                nextBehavior,
                confidence,
                detection.found(),
                startedAt,
                detection.modelVersion()
        );
        state.activeStartedAt = startedAt;
        clearCandidate(state);
    }

    private void closeActiveEvent(
            DeviceRecord device,
            Long petId,
            DeviceBehaviorState state,
            OffsetDateTime endedAt,
            boolean clearActiveBehavior
    ) {
        if (state.activeEventId == null || state.activeStartedAt == null) {
            if (clearActiveBehavior) {
                state.activeBehavior = null;
            }
            return;
        }

        behaviorRepository.completeEvent(state.activeEventId, endedAt);
        long durationSeconds = Duration.between(state.activeStartedAt, endedAt).toSeconds();
        if (durationSeconds >= Math.max(0, properties.getMinEventDurationSeconds())) {
            behaviorRepository.accumulateSummary(
                    petId,
                    LocalDate.ofInstant(state.activeStartedAt.toInstant(), ZoneOffset.UTC),
                    state.activeBehavior,
                    durationSeconds
            );
        }

        state.activeEventId = null;
        state.activeStartedAt = null;
        if (clearActiveBehavior) {
            state.activeBehavior = null;
        }
    }

    private void completePersistedEvent(Long petId, BehaviorEventRecord event, OffsetDateTime endedAt) {
        behaviorRepository.completeEvent(event.id(), endedAt);
        if (event.startedAt() == null) {
            return;
        }
        long durationSeconds = Duration.between(event.startedAt(), endedAt).toSeconds();
        if (durationSeconds >= Math.max(0, properties.getMinEventDurationSeconds())) {
            behaviorRepository.accumulateSummary(
                    petId,
                    LocalDate.ofInstant(event.startedAt().toInstant(), ZoneOffset.UTC),
                    event.behaviorType(),
                    durationSeconds
            );
        }
    }

    private Set<String> quickEventBehaviors() {
        return Arrays.stream(properties.getQuickEventBehaviors().split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    private int closeFrameCount() {
        return Math.max(1, properties.getUncertainCloseFrameCount());
    }

    private void clearCandidate(DeviceBehaviorState state) {
        state.candidateBehavior = null;
        state.candidateCount = 0;
        state.candidateStartedAt = null;
        state.candidateConfidenceSum = 0;
    }

    private Long stateKey(Long deviceId, Long petId) {
        return deviceId * 1_000_000L + petId;
    }

    private DeviceBehaviorState stateFor(Long deviceId, Long petId) {
        return states.computeIfAbsent(stateKey(deviceId, petId), id -> new DeviceBehaviorState());
    }

    private static final class DeviceBehaviorState {
        private String candidateBehavior;
        private int candidateCount;
        private OffsetDateTime candidateStartedAt;
        private double candidateConfidenceSum;
        private String activeBehavior;
        private Long activeEventId;
        private OffsetDateTime activeStartedAt;
        private int uncertainCount;
        private int identityMissCount;
    }
}
