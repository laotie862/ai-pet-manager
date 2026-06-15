package com.example.demo.behavior;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BehaviorFrameGate {
    private static final Logger log = LoggerFactory.getLogger(BehaviorFrameGate.class);
    private static final int SAMPLE_SIZE = 32;

    private final BehaviorProperties properties;
    private final Map<Long, FrameGateState> states = new ConcurrentHashMap<>();

    public BehaviorFrameGate(BehaviorProperties properties) {
        this.properties = properties;
    }

    public boolean shouldAnalyze(Long deviceId, byte[] frame) {
        if (!properties.isStaticSkipEnabled()) {
            markAnalyzed(deviceId, frame);
            return true;
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        double[] sample = sampleLuma(frame);
        if (sample.length == 0) {
            markAnalyzed(deviceId, frame);
            return true;
        }

        FrameGateState state = states.computeIfAbsent(deviceId, id -> new FrameGateState());
        if (state.previousSample == null) {
            state.previousSample = sample;
            state.lastAnalyzedAt = now;
            return true;
        }

        double motion = motionScore(state.previousSample, sample);
        state.previousSample = sample;
        long secondsSinceLastAnalysis = Duration.between(state.lastAnalyzedAt, now).toSeconds();
        if (motion >= Math.max(0, properties.getMotionThreshold())) {
            state.lastAnalyzedAt = now;
            return true;
        }
        if (secondsSinceLastAnalysis >= Math.max(1, properties.getMaxStaticApiIntervalSeconds())) {
            state.lastAnalyzedAt = now;
            return true;
        }
        if (secondsSinceLastAnalysis < Math.max(1, properties.getMinApiIntervalSeconds())) {
            log.debug("Skip static frame for device {} motion={} elapsed={}s", deviceId, motion, secondsSinceLastAnalysis);
            return false;
        }
        log.debug("Skip unchanged frame for device {} motion={} elapsed={}s", deviceId, motion, secondsSinceLastAnalysis);
        return false;
    }

    private void markAnalyzed(Long deviceId, byte[] frame) {
        FrameGateState state = states.computeIfAbsent(deviceId, id -> new FrameGateState());
        double[] sample = sampleLuma(frame);
        state.previousSample = sample.length == 0 ? state.previousSample : sample;
        state.lastAnalyzedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    private double motionScore(double[] previous, double[] current) {
        int size = Math.min(previous.length, current.length);
        if (size == 0) {
            return 1;
        }
        double total = 0;
        for (int index = 0; index < size; index++) {
            total += Math.abs(previous[index] - current[index]);
        }
        return total / size;
    }

    private double[] sampleLuma(byte[] frame) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(frame));
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                return new double[0];
            }
            double[] sample = new double[SAMPLE_SIZE * SAMPLE_SIZE];
            int index = 0;
            for (int y = 0; y < SAMPLE_SIZE; y++) {
                int sourceY = Math.min(image.getHeight() - 1, y * image.getHeight() / SAMPLE_SIZE);
                for (int x = 0; x < SAMPLE_SIZE; x++) {
                    int sourceX = Math.min(image.getWidth() - 1, x * image.getWidth() / SAMPLE_SIZE);
                    int rgb = image.getRGB(sourceX, sourceY);
                    int red = (rgb >> 16) & 0xff;
                    int green = (rgb >> 8) & 0xff;
                    int blue = rgb & 0xff;
                    sample[index++] = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0;
                }
            }
            return sample;
        } catch (Exception exception) {
            return new double[0];
        }
    }

    private static final class FrameGateState {
        private double[] previousSample;
        private OffsetDateTime lastAnalyzedAt = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
    }
}
