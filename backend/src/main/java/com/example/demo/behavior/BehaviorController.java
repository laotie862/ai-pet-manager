package com.example.demo.behavior;

import com.example.demo.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/behavior/pets/{petId}")
public class BehaviorController {
    private final BehaviorService behaviorService;
    private final BehaviorSampleService behaviorSampleService;

    public BehaviorController(BehaviorService behaviorService, BehaviorSampleService behaviorSampleService) {
        this.behaviorService = behaviorService;
        this.behaviorSampleService = behaviorSampleService;
    }

    @GetMapping("/current")
    public ApiResponse<BehaviorCurrentResponse> current(@PathVariable Long petId) {
        return ApiResponse.success(behaviorService.current(petId));
    }

    @GetMapping("/timeline")
    public ApiResponse<List<BehaviorEventRecord>> timeline(
            @PathVariable Long petId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return ApiResponse.success(behaviorService.timeline(petId, date));
    }

    @GetMapping("/summary")
    public ApiResponse<BehaviorSummaryResponse> summary(
            @PathVariable Long petId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return ApiResponse.success(behaviorService.summary(petId, date));
    }

    @GetMapping("/samples")
    public ApiResponse<List<BehaviorSampleRecord>> samples(
            @PathVariable Long petId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.success(behaviorSampleService.listByPet(petId, limit));
    }

    @PatchMapping("/samples/{sampleId}/review")
    public ApiResponse<Void> reviewSample(
            @PathVariable Long petId,
            @PathVariable Long sampleId,
            @Valid @RequestBody BehaviorSampleReviewRequest request
    ) {
        behaviorSampleService.review(petId, sampleId, request.finalBehavior());
        return ApiResponse.success(null);
    }
}
