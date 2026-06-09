package com.example.demo.behavior;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class BehaviorTrainingDatasetService {
    private static final Logger log = LoggerFactory.getLogger(BehaviorTrainingDatasetService.class);
    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ROOT);
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("HHmmss_SSS", Locale.ROOT);

    private final BehaviorProperties properties;

    public BehaviorTrainingDatasetService(BehaviorProperties properties) {
        this.properties = properties;
    }

    public void copyCandidate(Path sourceImage, String behavior, Long petId, Long deviceId, OffsetDateTime capturedAt) {
        copy(sourceImage, behavior, petId, deviceId, capturedAt, false);
    }

    public void copyPendingReview(Path sourceImage, String behavior, Long petId, Long deviceId, OffsetDateTime capturedAt) {
        copy(sourceImage, behavior, petId, deviceId, capturedAt, true);
    }

    private void copy(
            Path sourceImage,
            String behavior,
            Long petId,
            Long deviceId,
            OffsetDateTime capturedAt,
            boolean pendingReview
    ) {
        try {
            Path target = targetPath(behavior, petId, deviceId, capturedAt, sourceImage, pendingReview);
            Files.createDirectories(target.getParent());
            Files.copy(sourceImage, target);
        } catch (IOException exception) {
            log.warn("Failed to copy behavior image to training dataset: {}", sourceImage, exception);
        }
    }

    private Path targetPath(
            String behavior,
            Long petId,
            Long deviceId,
            OffsetDateTime capturedAt,
            Path sourceImage,
            boolean pendingReview
    ) {
        Path root = Paths.get(properties.getTrainingDatasetPath()).toAbsolutePath().normalize();
        Path labelRoot = pendingReview ? root.resolve("_pending_review").resolve(behavior) : root.resolve(behavior);
        String datePath = DATE_PATH.format(capturedAt.atZoneSameInstant(ZoneOffset.UTC));
        String sourceName = sourceImage.getFileName() == null ? "sample.jpg" : sourceImage.getFileName().toString();
        String fileName = "pet-" + petId
                + "_device-" + deviceId
                + "_" + FILE_TIME.format(capturedAt.atZoneSameInstant(ZoneOffset.UTC))
                + "_" + Long.toUnsignedString(System.nanoTime(), 36)
                + "_" + sourceName;
        return labelRoot.resolve(datePath).resolve(fileName).normalize();
    }
}
