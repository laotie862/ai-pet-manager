package com.example.demo.device;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DeviceRoiRequest(
        @NotNull
        @Size(min = 3, max = 16)
        @Valid
        List<RoiPoint> points
) {
}
