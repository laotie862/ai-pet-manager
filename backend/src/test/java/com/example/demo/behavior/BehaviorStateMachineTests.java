package com.example.demo.behavior;

import com.example.demo.device.DeviceRecord;
import com.example.demo.device.DeviceStatus;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BehaviorStateMachineTests {
    private final DeviceRecord device = new DeviceRecord(
            2L,
            1L,
            3L,
            "loop",
            "video://loop?path=/data/raw-videos/sample.mp4",
            null,
            null,
            "stream-key",
            DeviceStatus.ANALYZING,
            null,
            null,
            null,
            null,
            OffsetDateTime.now(),
            OffsetDateTime.now()
    );

    @Test
    void quickEventsCanSwitchWithoutWaitingForNormalStableCount() {
        BehaviorRepository repository = mock(BehaviorRepository.class);
        when(repository.createEvent(
                any(), any(), any(), anyDouble(), anyBoolean(), any(OffsetDateTime.class), any()
        )).thenReturn(10L, 11L);
        BehaviorStateMachine machine = new BehaviorStateMachine(properties(), repository);

        machine.acceptDetection(device, 3L, detection("sleeping", 0.85));
        machine.acceptDetection(device, 3L, detection("eating", 0.58));

        verify(repository).createEvent(eq(3L), eq(2L), eq("sleeping"), anyDouble(), eq(true), any(), any());
        verify(repository).completeEvent(eq(10L), any(OffsetDateTime.class));
        verify(repository).createEvent(eq(3L), eq(2L), eq("eating"), anyDouble(), eq(true), any(), any());
    }

    @Test
    void identityMissClosesAndAllowsSameBehaviorToReopen() {
        BehaviorRepository repository = mock(BehaviorRepository.class);
        when(repository.createEvent(
                any(), any(), any(), anyDouble(), anyBoolean(), any(OffsetDateTime.class), any()
        )).thenReturn(10L, 11L);
        BehaviorStateMachine machine = new BehaviorStateMachine(properties(), repository);

        machine.acceptDetection(device, 3L, detection("sleeping", 0.85));
        machine.acceptIdentityMiss(device, 3L);
        verify(repository, never()).completeEvent(eq(10L), any(OffsetDateTime.class));

        machine.acceptIdentityMiss(device, 3L);
        machine.acceptDetection(device, 3L, detection("sleeping", 0.85));

        verify(repository).completeEvent(eq(10L), any(OffsetDateTime.class));
        verify(repository, times(2)).createEvent(eq(3L), eq(2L), eq("sleeping"), anyDouble(), eq(true), any(), any());
    }

    private BehaviorProperties properties() {
        BehaviorProperties properties = new BehaviorProperties();
        properties.setMinConfidence(0.4);
        properties.setStableFrameCount(2);
        properties.setQuickEventStableFrameCount(1);
        properties.setUncertainCloseFrameCount(2);
        properties.setMinEventDurationSeconds(0);
        properties.setQuickEventMinConfidence(0.4);
        properties.setQuickEventBehaviors("eating,drinking,defecating");
        return properties;
    }

    private BehaviorDetectionResponse detection(String behavior, double confidence) {
        return new BehaviorDetectionResponse(true, behavior, confidence, List.of(), "test-model");
    }
}
