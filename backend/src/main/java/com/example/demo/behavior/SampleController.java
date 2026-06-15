package com.example.demo.behavior;

import com.example.demo.common.api.ApiResponse;
import com.example.demo.common.api.ErrorCode;
import com.example.demo.common.api.PageResponse;
import com.example.demo.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/behavior/samples")
public class SampleController {
    private static final Logger log = LoggerFactory.getLogger(SampleController.class);

    private final BehaviorSampleRepository sampleRepository;
    private final BehaviorProperties properties;

    public SampleController(BehaviorSampleRepository sampleRepository, BehaviorProperties properties) {
        this.sampleRepository = sampleRepository;
        this.properties = properties;
    }

    @GetMapping
    public ApiResponse<PageResponse<BehaviorSampleRecord>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String behavior,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var records = sampleRepository.list(status, behavior, page, size);
        int total = sampleRepository.count(status, behavior);
        return ApiResponse.success(new PageResponse<>(records, total, page, size));
    }

    @GetMapping("/{sampleId}/image")
    public ResponseEntity<InputStreamResource> image(@PathVariable Long sampleId) {
        BehaviorSampleRecord sample = sampleRepository.findById(sampleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Sample not found"));

        Path sampleRoot = Path.of(properties.getSampleStoragePath()).normalize();
        Path imagePath = sampleRoot.resolve(sample.imagePath()).normalize();

        // Guard: path must stay within the sample storage root
        if (!imagePath.startsWith(sampleRoot)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Invalid sample path");
        }

        if (!Files.exists(imagePath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Sample image file not found");
        }

        try {
            var resource = new InputStreamResource(Files.newInputStream(imagePath));
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"sample-" + sampleId + ".jpg\"")
                    .body(resource);
        } catch (IOException e) {
            log.warn("Failed to read sample image {} for sample {}", imagePath, sampleId, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to read sample image");
        }
    }

    @PatchMapping("/{sampleId}/review")
    public ApiResponse<BehaviorSampleRecord> review(
            @PathVariable Long sampleId,
            @RequestBody ReviewRequest request
    ) {
        BehaviorSampleRecord sample = sampleRepository.findById(sampleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Sample not found"));

        String finalBehavior = request.finalBehavior() != null
                ? request.finalBehavior().trim().toLowerCase()
                : sample.behaviorType();

        // Validate the final behavior is one of the known labels
        if (!java.util.List.of("eating", "drinking", "exercising", "sleeping", "defecating", "uncertain")
                .contains(finalBehavior)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Invalid behavior label: " + finalBehavior);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        sampleRepository.review(sampleId, finalBehavior,
                BehaviorSampleRecord.STATUS_CONFIRMED, now);

        return ApiResponse.success(sampleRepository.findById(sampleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to reload sample after review")));
    }

    public record ReviewRequest(String finalBehavior) {}
}
