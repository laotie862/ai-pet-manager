package com.example.demo.device;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalWebcamSupportTests {
    @Test
    void extractsVideoDevicesFromDirectShowListing() {
        String output = """
                [dshow @ 000001] DirectShow video devices
                [dshow @ 000001]  "Integrated Camera"
                [dshow @ 000001]     Alternative name "@device_pnp_\\\\?\\usb#vid_0bda"
                [dshow @ 000001]  "USB Camera"
                [dshow @ 000001] DirectShow audio devices
                [dshow @ 000001]  "Microphone"
                """;

        assertEquals(List.of("Integrated Camera", "USB Camera"), LocalWebcamSupport.extractVideoDeviceNames(output));
    }
}
