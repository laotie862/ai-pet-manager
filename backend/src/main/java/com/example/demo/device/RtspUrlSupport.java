package com.example.demo.device;

import com.example.demo.common.api.ErrorCode;
import com.example.demo.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class RtspUrlSupport {
    private final DeviceProperties properties;

    public RtspUrlSupport(DeviceProperties properties) {
        this.properties = properties;
    }

    public URI validate(String rtspUrl) {
        try {
            URI uri = URI.create(rtspUrl.trim());
            String scheme = uri.getScheme();
            if ("video".equalsIgnoreCase(scheme) && properties.isLoopVideoEnabled()) {
                if (!StringUtils.hasText(uri.getHost())) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "Video source must include host, for example video://loop?path=/data/videos/demo.mp4");
                }
                return uri;
            }
            if (!"rtsp".equalsIgnoreCase(scheme) || !StringUtils.hasText(uri.getHost())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "RTSP URL must start with rtsp:// and include host");
            }
            if (StringUtils.hasText(uri.getUserInfo())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Use username and password fields instead of embedding credentials in RTSP URL");
            }
            return uri;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid RTSP URL");
        }
    }

    public boolean isMockSource(String rtspUrl) {
        if (!properties.isMockRtspEnabled()) {
            return false;
        }
        URI uri = validate(rtspUrl);
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        return "mock".equals(host) || "simulated".equals(host) || "demo".equals(host);
    }

    public boolean isLocalWebcamSource(String rtspUrl) {
        URI uri = validate(rtspUrl);
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        return "webcam".equals(host) || "camera".equals(host) || "localcam".equals(host);
    }

    public boolean isLoopVideoSource(String rtspUrl) {
        if (!properties.isLoopVideoEnabled()) {
            return false;
        }
        URI uri = validate(rtspUrl);
        return "video".equalsIgnoreCase(uri.getScheme()) && "loop".equalsIgnoreCase(uri.getHost());
    }

    public String loopVideoPath(String rtspUrl) {
        if (!isLoopVideoSource(rtspUrl)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported video source");
        }
        String path = queryParameter(rtspUrl, "path");
        if (!StringUtils.hasText(path)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Video loop source requires path query parameter");
        }
        return path;
    }

    public String withCredentials(String rtspUrl, String username, String password) {
        URI uri = validate(rtspUrl);
        if (!StringUtils.hasText(username)) {
            return uri.toString();
        }
        try {
            String userInfo = StringUtils.hasText(password) ? username + ":" + password : username;
            return new URI(
                    uri.getScheme(),
                    userInfo,
                    uri.getHost(),
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment()
            ).toString();
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid RTSP credentials");
        }
    }

    public String queryParameter(String rtspUrl, String key) {
        URI uri = validate(rtspUrl);
        if (!StringUtils.hasText(uri.getQuery())) {
            return null;
        }
        for (String part : uri.getQuery().split("&")) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            int separator = part.indexOf('=');
            String name = decode(separator >= 0 ? part.substring(0, separator) : part);
            if (!key.equalsIgnoreCase(name)) {
                continue;
            }
            return separator >= 0 ? decode(part.substring(separator + 1)) : "";
        }
        return null;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
