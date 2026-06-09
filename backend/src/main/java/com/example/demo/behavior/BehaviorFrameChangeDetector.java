package com.example.demo.behavior;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

@Component
public class BehaviorFrameChangeDetector {
    private static final Logger log = LoggerFactory.getLogger(BehaviorFrameChangeDetector.class);
    private static final int GRID_WIDTH = 32;
    private static final int GRID_HEIGHT = 18;

    public FrameChange compare(double[] previousSignature, byte[] currentJpeg) {
        double[] currentSignature = signature(currentJpeg);
        if (currentSignature == null) {
            return FrameChange.unavailable();
        }
        if (previousSignature == null || previousSignature.length != currentSignature.length) {
            return new FrameChange(currentSignature, 1.0, true);
        }

        double totalDiff = 0.0;
        for (int index = 0; index < currentSignature.length; index++) {
            totalDiff += Math.abs(currentSignature[index] - previousSignature[index]);
        }
        double score = totalDiff / currentSignature.length;
        return new FrameChange(currentSignature, score, true);
    }

    private double[] signature(byte[] jpeg) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(jpeg));
            if (source == null) {
                return null;
            }
            BufferedImage scaled = new BufferedImage(GRID_WIDTH, GRID_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = scaled.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics.drawImage(source, 0, 0, GRID_WIDTH, GRID_HEIGHT, null);
            } finally {
                graphics.dispose();
            }

            double[] values = new double[GRID_WIDTH * GRID_HEIGHT];
            int offset = 0;
            for (int y = 0; y < GRID_HEIGHT; y++) {
                for (int x = 0; x < GRID_WIDTH; x++) {
                    int rgb = scaled.getRGB(x, y);
                    int red = (rgb >> 16) & 0xff;
                    int green = (rgb >> 8) & 0xff;
                    int blue = rgb & 0xff;
                    values[offset++] = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0;
                }
            }
            return values;
        } catch (Exception exception) {
            log.debug("Failed to calculate frame signature", exception);
            return null;
        }
    }

    public record FrameChange(double[] signature, double motionScore, boolean available) {
        public static FrameChange unavailable() {
            return new FrameChange(null, 1.0, false);
        }
    }
}
