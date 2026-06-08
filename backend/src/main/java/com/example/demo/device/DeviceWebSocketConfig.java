package com.example.demo.device;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class DeviceWebSocketConfig implements WebSocketConfigurer {
    private final DeviceFrameWebSocketHandler frameWebSocketHandler;
    private final DeviceStreamHandshakeInterceptor handshakeInterceptor;

    public DeviceWebSocketConfig(
            DeviceFrameWebSocketHandler frameWebSocketHandler,
            DeviceStreamHandshakeInterceptor handshakeInterceptor
    ) {
        this.frameWebSocketHandler = frameWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(frameWebSocketHandler, "/ws/devices/{deviceId}/stream")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
