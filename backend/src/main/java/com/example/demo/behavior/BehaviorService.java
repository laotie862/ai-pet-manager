package com.example.demo.behavior;

import com.example.demo.common.api.ErrorCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.security.SecurityUtils;
import com.example.demo.pet.PetRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.Duration;
import java.util.List;

@Service
public class BehaviorService {
    private final PetRepository petRepository;
    private final BehaviorRepository behaviorRepository;

    public BehaviorService(PetRepository petRepository, BehaviorRepository behaviorRepository) {
        this.petRepository = petRepository;
        this.behaviorRepository = behaviorRepository;
    }

    public BehaviorCurrentResponse current(Long petId) {
        ensureOwnedPet(petId);
        return behaviorRepository.currentByPet(petId)
                .map(event -> new BehaviorCurrentResponse(
                        event.petId(),
                        event.deviceId(),
                        event.behaviorType(),
                        event.confidence(),
                        event.found(),
                        event.startedAt(),
                        OffsetDateTime.now(ZoneOffset.UTC),
                        event.modelVersion()
                ))
                .orElse(BehaviorCurrentResponse.uncertain(petId, null));
    }

    public List<BehaviorEventRecord> timeline(Long petId, LocalDate date) {
        ensureOwnedPet(petId);
        return behaviorRepository.timeline(petId, date == null ? LocalDate.now(ZoneOffset.UTC) : date);
    }

    public BehaviorSummaryResponse summary(Long petId, LocalDate date) {
        ensureOwnedPet(petId);
        LocalDate targetDate = date == null ? LocalDate.now(ZoneOffset.UTC) : date;
        BehaviorSummaryResponse summary = behaviorRepository.summary(petId, targetDate)
                .orElseGet(() -> BehaviorSummaryResponse.empty(petId, targetDate));
        return behaviorRepository.currentByPet(petId)
                .filter(event -> LocalDate.ofInstant(event.startedAt().toInstant(), ZoneOffset.UTC).equals(targetDate))
                .map(event -> summary.withLiveEvent(
                        event,
                        Duration.between(event.startedAt(), OffsetDateTime.now(ZoneOffset.UTC)).toSeconds()
                ))
                .orElse(summary);
    }

    private void ensureOwnedPet(Long petId) {
        Long userId = SecurityUtils.currentUser().id();
        petRepository.findByIdAndUser(petId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Pet not found"));
    }
}
