package com.example.demo.behavior;

import com.example.demo.device.DeviceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BehaviorSampleCaptureService {
    private static final Logger log = LoggerFactory.getLogger(BehaviorSampleCaptureService.class);
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final BehaviorProperties properties;
    private final BehaviorSampleRepository sampleRepository;
    private final Map<String, OffsetDateTime> lastSavedAt = new ConcurrentHashMap<>();
    private final Map<String, Integer> dailyCounts = new ConcurrentHashMap<>();

    public BehaviorSampleCaptureService(BehaviorProperties properties,
                                        BehaviorSampleRepository sampleRepository) {
        this.properties = properties;
        this.sampleRepository = sampleRepository;
    }

    public void capture(DeviceRecord device, Long petId, BehaviorDetectionResponse detection, byte[] frame) {
        if (!properties.isSampleCaptureEnabled() || !detection.found()) {
            return;
        }
        String behavior = detection.normalizedBehavior(properties.getMinConfidence());
        if ("uncertain".equals(behavior)) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        LocalDate today = LocalDate.ofInstant(now.toInstant(), ZoneOffset.UTC);
        String key = device.id() + ":" + behavior;
        String dayKey = key + ":" + today;
        if (tooSoon(key, now) || tooManyToday(dayKey)) {
            return;
        }

        // Relative path: <behavior>/device-<id>/<timestamp>_pet-<id>_conf-<pct>.jpg
        String relativeDir = safeSegment(behavior) + "/device-" + device.id();
        String filename = FILE_TIME.format(now) + "_pet-" + petId + "_conf-" + Math.round(detection.confidence() * 100) + ".jpg";
        String relativePath = relativeDir + "/" + filename;

        Path sampleRoot = Path.of(properties.getSampleStoragePath());
        Path absolutePath = sampleRoot.resolve(relativePath).normalize();

        // Guard: ensure the resolved path stays inside the sample root
        if (!absolutePath.startsWith(sampleRoot)) {
            log.warn("Sample path escape attempt for device {} pet {} label {}", device.id(), petId, behavior);
            return;
        }

        try {
            Files.createDirectories(absolutePath.getParent());
            Files.write(absolutePath, frame, StandardOpenOption.CREATE_NEW);
        } catch (Exception exception) {
            log.warn("Failed to write sample file for device {} pet {} label {}", device.id(), petId, behavior, exception);
            return;
        }

        // Determine review status based on confidence thresholds
        String reviewStatus;
        if (detection.confidence() >= properties.getAutoTrainingConfidence()) {
            reviewStatus = BehaviorSampleRecord.STATUS_AUTO_APPROVED;
        } else {
            reviewStatus = BehaviorSampleRecord.STATUS_PENDING;
        }

        try {
            sampleRepository.create(
                    petId,
                    device.id(),
                    behavior,
                    detection.confidence(),
                    relativePath,
                    reviewStatus,
                    detection.modelVersion(),
                    now
            );
            lastSavedAt.put(key, now);
            dailyCounts.merge(dayKey, 1, Integer::sum);
        } catch (Exception exception) {
            // DB write failed — remove the orphan file so we don't have files without records
            try {
                Files.deleteIfExists(absolutePath);
            } catch (Exception deleteException) {
                log.warn("Failed to clean up orphan sample file {}", absolutePath, deleteException);
            }
            log.warn("Failed to write sample DB record for device {} pet {} label {} — file cleaned up",
                    device.id(), petId, behavior, exception);
        }
    }

    private boolean tooSoon(String key, OffsetDateTime now) {
        OffsetDateTime previous = lastSavedAt.get(key);
        if (previous == null) {
            return false;
        }
        long elapsed = java.time.Duration.between(previous, now).toSeconds();
        return elapsed < Math.max(0, properties.getSampleMinIntervalSeconds());
    }

    private boolean tooManyToday(String dayKey) {
        return dailyCounts.getOrDefault(dayKey, 0) >= Math.max(1, properties.getMaxSamplesPerDeviceLabelPerDay());
    }

    private String safeSegment(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
    }
}
