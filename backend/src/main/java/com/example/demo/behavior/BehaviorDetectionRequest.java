package com.example.demo.behavior;

import com.example.demo.device.RoiPoint;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record BehaviorDetectionRequest(
        @JsonProperty("image_base64")
        String imageBase64,

        @JsonProperty("device_id")
        String deviceId,

        @JsonProperty("roi_polygon")
        List<RoiPoint> roiPolygon
) {
}
