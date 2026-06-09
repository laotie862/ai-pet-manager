package com.example.demo.behavior;

import com.example.demo.device.DeviceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BehaviorSampleCaptureService {
    private static final Logger log = LoggerFactory.getLogger(BehaviorSampleCaptureService.class);
    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ROOT);
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("HHmmss_SSS", Locale.ROOT);

    private final BehaviorProperties properties;
    private final BehaviorSampleRepository sampleRepository;
    private final BehaviorTrainingDatasetService trainingDatasetService;
    private final Map<String, OffsetDateTime> lastSavedAt = new ConcurrentHashMap<>();
    private final Map<String, DailySampleCounter> dailyCounters = new ConcurrentHashMap<>();

    public BehaviorSampleCaptureService(
            BehaviorProperties properties,
            BehaviorSampleRepository sampleRepository,
            BehaviorTrainingDatasetService trainingDatasetService
    ) {
        this.properties = properties;
        this.sampleRepository = sampleRepository;
        this.trainingDatasetService = trainingDatasetService;
    }

    public void capture(
            DeviceRecord device,
            byte[] jpegFrame,
            BehaviorDetectionResponse detection,
            String normalizedBehavior,
            Long eventId,
            OffsetDateTime capturedAt
    ) {
        if (!properties.isSampleCaptureEnabled() || jpegFrame == null || jpegFrame.length == 0) {
            return;
        }
        try {
            String behavior = sanitizeBehavior(normalizedBehavior);
            if (!allowSave(device.id(), behavior, capturedAt)) {
                return;
            }
            String reviewStatus = reviewStatus(behavior, detection.confidence());
            String finalBehavior = "training_candidate".equals(reviewStatus) ? behavior : null;
            Path imagePath = writeImage(device, behavior, capturedAt, jpegFrame);
            copyToClassifierDataset(device, behavior, reviewStatus, imagePath, capturedAt);
            sampleRepository.createSample(
                    device.petId(),
                    device.id(),
                    eventId,
                    imagePath.toString().replace('\\', '/'),
                    behavior,
                    detection.confidence(),
                    detection.found(),
                    providerFromModelVersion(detection.modelVersion()),
                    detection.modelVersion(),
                    reviewStatus,
                    finalBehavior,
                    capturedAt
            );
        } catch (Exception exception) {
            log.warn("Failed to capture behavior sample for device {}", device.id(), exception);
        }
    }

    private synchronized boolean allowSave(Long deviceId, String behavior, OffsetDateTime capturedAt) {
        String key = deviceId + ":" + behavior;
        OffsetDateTime previous = lastSavedAt.get(key);
        if (previous != null) {
            long seconds = java.time.Duration.between(previous, capturedAt).toSeconds();
            if (seconds < Math.max(0, properties.getSampleMinIntervalSeconds())) {
                log.debug("Skip sample capture by interval throttle. key={}, seconds={}", key, seconds);
                return false;
            }
        }

        int dailyMax = properties.getMaxSamplesPerDeviceLabelPerDay();
        if (dailyMax > 0) {
            String dailyKey = key + ":" + LocalDate.ofInstant(capturedAt.toInstant(), ZoneOffset.UTC);
            DailySampleCounter counter = dailyCounters.compute(dailyKey, (ignored, existing) -> {
                LocalDate today = LocalDate.ofInstant(capturedAt.toInstant(), ZoneOffset.UTC);
                if (existing == null || !today.equals(existing.date)) {
                    return new DailySampleCounter(today, 0);
                }
                return existing;
            });
            if (counter.count >= dailyMax) {
                log.debug("Skip sample capture by daily throttle. key={}, count={}", dailyKey, counter.count);
                cleanupOldCounters(capturedAt);
                return false;
            }
            counter.count++;
        }

        lastSavedAt.put(key, capturedAt);
        cleanupOldCounters(capturedAt);
        return true;
    }

    private void cleanupOldCounters(OffsetDateTime now) {
        LocalDate today = LocalDate.ofInstant(now.toInstant(), ZoneOffset.UTC);
        dailyCounters.entrySet().removeIf(entry -> entry.getValue().date.isBefore(today));
    }

    private void copyToClassifierDataset(
            DeviceRecord device,
            String behavior,
            String reviewStatus,
            Path imagePath,
            OffsetDateTime capturedAt
    ) {
        if ("training_candidate".equals(reviewStatus)) {
            trainingDatasetService.copyCandidate(imagePath, behavior, device.petId(), device.id(), capturedAt);
            return;
        }
        trainingDatasetService.copyPendingReview(imagePath, behavior, device.petId(), device.id(), capturedAt);
    }

    private Path writeImage(DeviceRecord device, String behavior, OffsetDateTime capturedAt, byte[] jpegFrame)
            throws IOException {
        Path root = Paths.get(properties.getSampleStoragePath()).toAbsolutePath().normalize();
        String datePath = DATE_PATH.format(capturedAt.atZoneSameInstant(ZoneOffset.UTC));
        Path directory = root
                .resolve("pet-" + device.petId())
                .resolve(behavior)
                .resolve(datePath)
                .normalize();
        Files.createDirectories(directory);
        String fileName = "device-" + device.id() + "_" + FILE_TIME.format(capturedAt.atZoneSameInstant(ZoneOffset.UTC))
                + "_" + Long.toUnsignedString(System.nanoTime(), 36)
                + ".jpg";
        Path imagePath = directory.resolve(fileName).normalize();
        Files.write(imagePath, jpegFrame);
        return imagePath;
    }

    private String reviewStatus(String behavior, double confidence) {
        if ("uncertain".equals(behavior) || confidence < properties.getAutoTrainingConfidence()) {
            return "pending_review";
        }
        return "training_candidate";
    }

    private String sanitizeBehavior(String behavior) {
        if (!StringUtils.hasText(behavior)) {
            return "uncertain";
        }
        return behavior.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z_]", "");
    }

    private String providerFromModelVersion(String modelVersion) {
        if (!StringUtils.hasText(modelVersion)) {
            return "unknown";
        }
        int split = modelVersion.indexOf(':');
        if (split <= 0) {
            return modelVersion;
        }
        return modelVersion.substring(0, split);
    }

    private static final class DailySampleCounter {
        private final LocalDate date;
        private int count;

        private DailySampleCounter(LocalDate date, int count) {
            this.date = date;
            this.count = count;
        }
    }
}
