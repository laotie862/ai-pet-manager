package com.example.demo.device;

import com.example.demo.common.api.ErrorCode;
import com.example.demo.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LocalWebcamSupport {
    private static final Pattern QUOTED_NAME = Pattern.compile("\"([^\"]+)\"");

    private final DeviceProperties properties;
    private final RtspUrlSupport rtspUrlSupport;

    public LocalWebcamSupport(DeviceProperties properties, RtspUrlSupport rtspUrlSupport) {
        this.properties = properties;
        this.rtspUrlSupport = rtspUrlSupport;
    }

    public void appendInputArguments(List<String> command, String rtspUrl) {
        ensureWindowsSupported();
        command.add("-f");
        command.add("dshow");
        command.add("-i");
        command.add("video=" + resolveDeviceName(rtspUrl));
    }

    public void assertReachable(String rtspUrl) {
        ensureWindowsSupported();
        List<String> command = new ArrayList<>();
        command.add(properties.getFfmpegPath());
        command.add("-hide_banner");
        command.add("-loglevel");
        command.add("error");
        appendInputArguments(command, rtspUrl);
        command.add("-frames:v");
        command.add("1");
        command.add("-f");
        command.add("null");
        command.add("-");

        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            boolean completed = process.waitFor(properties.getConnectionTestTimeoutSeconds(), TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Local webcam timed out after "
                        + properties.getConnectionTestTimeoutSeconds() + " seconds");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Local webcam is not available" + detail(output));
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "FFmpeg is not available for local webcam capture");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Local webcam test interrupted");
        }
    }

    String resolveDeviceName(String rtspUrl) {
        String requestedDevice = rtspUrlSupport.queryParameter(rtspUrl, "device");
        if (StringUtils.hasText(requestedDevice)) {
            return requestedDevice.trim();
        }

        List<String> devices = listVideoDevices();
        String indexValue = rtspUrlSupport.queryParameter(rtspUrl, "index");
        if (StringUtils.hasText(indexValue)) {
            try {
                int index = Integer.parseInt(indexValue.trim());
                if (index >= 0 && index < devices.size()) {
                    return devices.get(index);
                }
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Local webcam index out of range");
            } catch (NumberFormatException exception) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Local webcam index must be a number");
            }
        }

        if (StringUtils.hasText(properties.getLocalWebcamName())) {
            String configured = properties.getLocalWebcamName().trim();
            if (devices.stream().anyMatch(device -> device.equalsIgnoreCase(configured))) {
                return devices.stream()
                        .filter(device -> device.equalsIgnoreCase(configured))
                        .findFirst()
                        .orElse(configured);
            }
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Configured local webcam was not found: " + configured);
        }

        if (devices.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "No local webcam was found on this machine");
        }
        return devices.get(0);
    }

    List<String> listVideoDevices() {
        ensureWindowsSupported();
        List<String> command = List.of(
                properties.getFfmpegPath(),
                "-hide_banner",
                "-list_devices", "true",
                "-f", "dshow",
                "-i", "dummy"
        );

        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            boolean completed = process.waitFor(properties.getConnectionTestTimeoutSeconds(), TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Listing local webcams timed out after "
                        + properties.getConnectionTestTimeoutSeconds() + " seconds");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            List<String> devices = extractVideoDeviceNames(output);
            if (!devices.isEmpty()) {
                return devices;
            }
            if (process.exitValue() != 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Unable to list local webcams" + detail(output));
            }
            return devices;
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "FFmpeg is not available for local webcam capture");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Local webcam discovery interrupted");
        }
    }

    static List<String> extractVideoDeviceNames(String output) {
        if (!StringUtils.hasText(output)) {
            return List.of();
        }

        Set<String> devices = new LinkedHashSet<>();
        boolean inVideoSection = false;
        for (String line : output.split("\\R")) {
            if (line.contains("DirectShow video devices")) {
                inVideoSection = true;
                continue;
            }
            if (line.contains("DirectShow audio devices")) {
                break;
            }
            if (!inVideoSection || line.contains("Alternative name")) {
                continue;
            }
            Matcher matcher = QUOTED_NAME.matcher(line);
            if (matcher.find()) {
                devices.add(matcher.group(1));
            }
        }
        return List.copyOf(devices);
    }

    private void ensureWindowsSupported() {
        String osName = System.getProperty("os.name", "");
        if (!osName.toLowerCase().contains("win")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Local webcam source is currently supported on Windows only");
        }
    }

    private String detail(String output) {
        if (!StringUtils.hasText(output)) {
            return "";
        }
        String value = output.trim();
        if (value.length() > 160) {
            value = value.substring(0, 160);
        }
        return ": " + value;
    }
}
