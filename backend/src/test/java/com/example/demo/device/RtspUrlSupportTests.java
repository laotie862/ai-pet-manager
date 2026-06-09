package com.example.demo.device;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RtspUrlSupportTests {
    @Test
    void detectsSpecialSources() {
        DeviceProperties properties = new DeviceProperties();
        RtspUrlSupport support = new RtspUrlSupport(properties);

        assertTrue(support.isMockSource("rtsp://mock/live"));
        assertTrue(support.isLocalWebcamSource("rtsp://webcam"));
        assertTrue(support.isLoopVideoSource("video://loop?path=C:/videos/pet.mp4"));
        assertFalse(support.isLocalWebcamSource("rtsp://192.168.1.20/live"));
    }

    @Test
    void decodesQueryParameters() {
        DeviceProperties properties = new DeviceProperties();
        RtspUrlSupport support = new RtspUrlSupport(properties);

        assertEquals("USB Camera", support.queryParameter("rtsp://webcam?device=USB+Camera&index=1", "device"));
        assertEquals("1", support.queryParameter("rtsp://webcam?device=USB+Camera&index=1", "index"));
        assertEquals("/data/videos/sample pet.mp4", support.loopVideoPath("video://loop?path=/data/videos/sample+pet.mp4"));
    }

    @Test
    void rejectsLoopVideoWithoutPath() {
        DeviceProperties properties = new DeviceProperties();
        RtspUrlSupport support = new RtspUrlSupport(properties);

        assertThrows(RuntimeException.class, () -> support.validate("video://loop"));
    }
}
