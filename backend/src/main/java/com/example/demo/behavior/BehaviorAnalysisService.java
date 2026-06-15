package com.example.demo.behavior;

import com.example.demo.device.DeviceRecord;
import com.example.demo.device.DeviceRepository;
import com.example.demo.device.DeviceStreamManager;
import com.example.demo.device.RoiPoint;
import com.example.demo.pet.PetIdentityMatcher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.List;

@Service
public class BehaviorAnalysisService {
    private static final Logger log = LoggerFactory.getLogger(BehaviorAnalysisService.class);
    private static final TypeReference<List<RoiPoint>> ROI_TYPE = new TypeReference<>() {
    };

    private final BehaviorProperties properties;
    private final DeviceRepository deviceRepository;
    private final DeviceStreamManager streamManager;
    private final CvInferenceClient cvInferenceClient;
    private final PetIdentityMatcher petIdentityMatcher;
    private final BehaviorStateMachine behaviorStateMachine;
    private final BehaviorFrameGate frameGate;
    private final BehaviorSampleCaptureService sampleCaptureService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BehaviorAnalysisService(
            BehaviorProperties properties,
            DeviceRepository deviceRepository,
            DeviceStreamManager streamManager,
            CvInferenceClient cvInferenceClient,
            PetIdentityMatcher petIdentityMatcher,
            BehaviorStateMachine behaviorStateMachine,
            BehaviorFrameGate frameGate,
            BehaviorSampleCaptureService sampleCaptureService
    ) {
        this.properties = properties;
        this.deviceRepository = deviceRepository;
        this.streamManager = streamManager;
        this.cvInferenceClient = cvInferenceClient;
        this.petIdentityMatcher = petIdentityMatcher;
        this.behaviorStateMachine = behaviorStateMachine;
        this.frameGate = frameGate;
        this.sampleCaptureService = sampleCaptureService;
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
            deviceRepository.listBoundPetIds(device.id()).stream()
                    .findFirst()
                    .ifPresent(petId -> analyze(device, petId));
        }
    }

    private void analyze(DeviceRecord device, Long petId) {
        streamManager.latestFrame(device.id()).ifPresent(frame -> {
            try {
                if (!frameGate.shouldAnalyze(device.id(), frame)) {
                    return;
                }
                if (!identityMatched(device, petId, frame)) {
                    behaviorStateMachine.acceptIdentityMiss(device, petId);
                    return;
                }
                BehaviorDetectionResponse detection = cvInferenceClient.detect(new BehaviorDetectionRequest(
                        Base64.getEncoder().encodeToString(frame),
                        String.valueOf(device.id()),
                        readRoi(device.roiPolygonJson())
                ));
                sampleCaptureService.capture(device, petId, detection, frame);
                behaviorStateMachine.acceptDetection(device, petId, detection);
            } catch (Exception exception) {
                log.warn("Behavior analysis failed for device {}", device.id(), exception);
            }
        });
    }

    private boolean identityMatched(DeviceRecord device, Long petId, byte[] frame) {
        if (!properties.isIdentityMatchEnabled()) {
            return true;
        }
        PetIdentityMatcher.PetIdentityMatchDecision decision = petIdentityMatcher.matches(petId, frame);
        if (!decision.matched()) {
            log.debug(
                    "Skip behavior analysis for device {} pet {} because identity similarity {} is below threshold {}",
                    device.id(),
                    petId,
                    decision.similarity(),
                    properties.getIdentityMatchThreshold()
            );
        }
        return decision.matched();
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
}
