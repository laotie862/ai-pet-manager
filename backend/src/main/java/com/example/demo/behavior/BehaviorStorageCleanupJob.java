package com.example.demo.behavior;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @Scheduled(cron = "0 20 3 * * ?")
    public void cleanup() {
        if (!properties.isCleanupEnabled()) {
            return;
        }
        cleanupOlderThan(
                Paths.get(properties.getTrainingDatasetPath()).resolve("_pending_review"),
                properties.getPendingReviewRetentionDays()
        );
        cleanupOlderThan(
                Paths.get(properties.getExtractedFramesPath()),
                properties.getExtractedFramesRetentionDays()
        );
    }

    private void cleanupOlderThan(Path rawRoot, int retentionDays) {
        if (retentionDays <= 0) {
            return;
        }
        Path root = rawRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            return;
        }
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> isExpired(path, cutoff))
                    .forEach(this::deleteFile);
        } catch (Exception exception) {
            log.warn("Failed to cleanup behavior storage under {}", root, exception);
        }
        pruneEmptyDirectories(root);
    }

    private boolean isExpired(Path path, Instant cutoff) {
        try {
            return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
        } catch (Exception exception) {
            return false;
        }
    }

    private void deleteFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (Exception exception) {
            log.debug("Failed to delete expired behavior file {}", path, exception);
        }
    }

    private void pruneEmptyDirectories(Path root) {
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isDirectory)
                    .sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(root))
                    .forEach(this::deleteDirectoryIfEmpty);
        } catch (Exception exception) {
            log.debug("Failed to prune behavior directories under {}", root, exception);
        }
    }

    private void deleteDirectoryIfEmpty(Path path) {
        try (Stream<Path> children = Files.list(path)) {
            if (children.findAny().isEmpty()) {
                Files.deleteIfExists(path);
            }
        } catch (Exception exception) {
            log.debug("Failed to delete empty behavior directory {}", path, exception);
        }
    }
}
