package com.example.demo.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceCreateRequest(
        @NotBlank
        @Size(max = 64)
        String name,

        Long petId,

        @NotBlank
        @Size(max = 1024)
        String rtspUrl,

        @Size(max = 128)
        String username,

        @Size(max = 256)
        String password
) {
}
