package com.example.demo.device;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DevicePetBindingRequest(
        @NotNull
        @Size(min = 1, max = 20)
        List<Long> petIds
) {
}
