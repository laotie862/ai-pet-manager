package com.example.demo.device;

import com.example.demo.common.api.ErrorCode;
import com.example.demo.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RtspProbeService {
    private final DeviceProperties properties;
    private final RtspUrlSupport rtspUrlSupport;
    private final LocalWebcamSupport localWebcamSupport;

    public RtspProbeService(
            DeviceProperties properties,
            RtspUrlSupport rtspUrlSupport,
            LocalWebcamSupport localWebcamSupport
    ) {
        this.properties = properties;
        this.rtspUrlSupport = rtspUrlSupport;
        this.localWebcamSupport = localWebcamSupport;
    }

    public void assertReachable(String rtspUrl, String username, String password) {
        rtspUrlSupport.validate(rtspUrl);
        if (!properties.isConnectionTestEnabled() || rtspUrlSupport.isMockSource(rtspUrl)) {
            return;
        }
        if (rtspUrlSupport.isLocalWebcamSource(rtspUrl)) {
            localWebcamSupport.assertReachable(rtspUrl);
            return;
        }
        if (rtspUrlSupport.isLoopVideoSource(rtspUrl)) {
            assertLoopVideoReachable(rtspUrl);
            return;
        }

        String inputUrl = rtspUrlSupport.withCredentials(rtspUrl, username, password);
        List<String> command = List.of(
                properties.getFfprobePath(),
                "-v", "error",
                "-rtsp_transport", "tcp",
                "-i", inputUrl,
                "-show_entries", "stream=codec_type",
                "-of", "csv=p=0"
        );

        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            boolean completed = process.waitFor(properties.getConnectionTestTimeoutSeconds(), TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new BusinessException(ErrorCode.BAD_REQUEST, "RTSP connection timed out after "
                        + properties.getConnectionTestTimeoutSeconds() + " seconds");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "RTSP connection failed" + detail(output));
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "FFprobe is not available for RTSP connection test");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "RTSP connection test interrupted");
        }
    }

    private void assertLoopVideoReachable(String rtspUrl) {
        String inputPath = rtspUrlSupport.loopVideoPath(rtspUrl);
        List<String> command = List.of(
                properties.getFfprobePath(),
                "-v", "error",
                "-i", inputPath,
                "-show_entries", "stream=codec_type",
                "-of", "csv=p=0"
        );

        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            boolean completed = process.waitFor(properties.getConnectionTestTimeoutSeconds(), TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Loop video probe timed out after "
                        + properties.getConnectionTestTimeoutSeconds() + " seconds");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0 || !output.toLowerCase().contains("video")) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Loop video is not readable" + detail(output));
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "FFprobe is not available for loop video test");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Loop video test interrupted");
        }
    }

    private String detail(String output) {
        if (output == null || output.isBlank()) {
            return "";
        }
        String value = output.length() > 160 ? output.substring(0, 160) : output;
        return ": " + value;
    }
}
