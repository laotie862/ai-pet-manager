package com.example.demo.behavior;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.stream.Stream;

@Component
public class BehaviorStorageCleanupJob {
    private static final Logger log = LoggerFactory.getLogger(BehaviorStorageCleanupJob.class);

    private final BehaviorProperties properties;

    public BehaviorStorageCleanupJob(BehaviorProperties properties) {
        this.properties = properties;
    }

    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanup() {
        if (!properties.isCleanupEnabled()) {
            return;
        }
        cleanupPath(Path.of(properties.getSampleStoragePath(), "_pending_review"), properties.getPendingReviewRetentionDays());
        cleanupPath(Path.of(properties.getExtractedFramesPath()), properties.getExtractedFramesRetentionDays());
    }

    private void cleanupPath(Path root, int retentionDays) {
        if (!Files.exists(root)) {
            return;
        }
        Instant deadline = Instant.now().minus(Math.max(1, retentionDays), ChronoUnit.DAYS);
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(root))
                    .forEach(path -> deleteIfExpired(path, deadline));
        } catch (Exception exception) {
            log.warn("Failed to cleanup behavior storage path {}", root, exception);
        }
    }

    private void deleteIfExpired(Path path, Instant deadline) {
        try {
            if (Files.isDirectory(path)) {
                try (Stream<Path> children = Files.list(path)) {
                    if (children.findAny().isEmpty()) {
                        Files.deleteIfExists(path);
                    }
                }
                return;
            }
            if (Files.getLastModifiedTime(path).toInstant().isBefore(deadline)) {
                Files.deleteIfExists(path);
            }
        } catch (Exception exception) {
            log.debug("Skip cleanup for {}", path, exception);
        }
    }
}
