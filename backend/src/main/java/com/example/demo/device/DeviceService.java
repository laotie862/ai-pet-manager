package com.example.demo.device;

import com.example.demo.common.api.ErrorCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.security.CurrentUser;
import com.example.demo.common.security.SecurityUtils;
import com.example.demo.pet.PetRepository;
import com.example.demo.user.UserAccount;
import com.example.demo.user.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class DeviceService {
    private static final TypeReference<List<RoiPoint>> ROI_TYPE = new TypeReference<>() {
    };

    private final DeviceRepository deviceRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final DeviceProperties deviceProperties;
    private final DeviceCredentialCrypto credentialCrypto;
    private final RtspProbeService rtspProbeService;
    private final RtspUrlSupport rtspUrlSupport;
    private final DeviceStreamManager streamManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DeviceService(
            DeviceRepository deviceRepository,
            PetRepository petRepository,
            UserRepository userRepository,
            DeviceProperties deviceProperties,
            DeviceCredentialCrypto credentialCrypto,
            RtspProbeService rtspProbeService,
            RtspUrlSupport rtspUrlSupport,
            DeviceStreamManager streamManager
    ) {
        this.deviceRepository = deviceRepository;
        this.petRepository = petRepository;
        this.userRepository = userRepository;
        this.deviceProperties = deviceProperties;
        this.credentialCrypto = credentialCrypto;
        this.rtspProbeService = rtspProbeService;
        this.rtspUrlSupport = rtspUrlSupport;
        this.streamManager = streamManager;
    }

    public List<DeviceResponse> list() {
        Long userId = SecurityUtils.currentUser().id();
        return deviceRepository.listByUser(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public DeviceResponse detail(Long deviceId) {
        return toResponse(requireOwned(deviceId));
    }

    @Transactional
    public DeviceResponse create(DeviceCreateRequest request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        UserAccount user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (!user.isVip() && deviceRepository.countByUser(user.id()) >= deviceProperties.getFreeLimit()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Free users can create up to "
                    + deviceProperties.getFreeLimit() + " camera");
        }

        String rtspUrl = request.rtspUrl().trim();
        rtspUrlSupport.validate(rtspUrl);
        ensurePetBelongsToUser(request.petId(), user.id());
        rtspProbeService.assertReachable(rtspUrl, request.username(), request.password());

        Long deviceId = deviceRepository.create(
                user.id(),
                request.petId(),
                request.name().trim(),
                rtspUrl,
                blankToNull(request.username()),
                credentialCrypto.encrypt(request.password())
        );
        DeviceRecord device = deviceRepository.findByIdAndUser(deviceId, user.id()).orElseThrow();
        if (deviceProperties.isAutoStartEnabled()) {
            streamManager.start(device);
        }
        return toResponse(deviceRepository.findByIdAndUser(deviceId, user.id()).orElseThrow());
    }

    public DeviceStatusResponse testConnection(DeviceConnectionTestRequest request) {
        rtspProbeService.assertReachable(request.rtspUrl(), request.username(), request.password());
        return new DeviceStatusResponse(null, DeviceStatus.ONLINE.name(), false, false, null, null, null, null);
    }

    @Transactional
    public DeviceStatusResponse testStoredConnection(Long deviceId) {
        DeviceRecord device = requireOwned(deviceId);
        rtspProbeService.assertReachable(
                device.rtspUrl(),
                device.rtspUsername(),
                credentialCrypto.decrypt(device.rtspPasswordCipher())
        );
        deviceRepository.updateRuntimeState(device.id(), DeviceStatus.ONLINE, null, false);
        return status(device.id());
    }

    @Transactional
    public DeviceResponse updateRoi(Long deviceId, DeviceRoiRequest request) {
        Long userId = SecurityUtils.currentUser().id();
        requireOwned(deviceId);
        String roiJson = writeRoi(request.points());
        return deviceRepository.updateRoi(deviceId, userId, roiJson)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Device not found"));
    }

    public DeviceStatusResponse status(Long deviceId) {
        DeviceRecord device = requireOwned(deviceId);
        return DeviceStatusResponse.from(device, streamManager.snapshot(deviceId));
    }

    @Transactional
    public DeviceStatusResponse startStream(Long deviceId) {
        DeviceRecord device = requireOwned(deviceId);
        DeviceStreamSnapshot snapshot = streamManager.start(device);
        DeviceRecord updated = deviceRepository.findById(deviceId).orElse(device);
        return DeviceStatusResponse.from(updated, snapshot);
    }

    @Transactional
    public DeviceStatusResponse stopStream(Long deviceId) {
        DeviceRecord device = requireOwned(deviceId);
        streamManager.stop(deviceId);
        DeviceRecord updated = deviceRepository.findById(deviceId).orElse(device);
        return DeviceStatusResponse.from(updated, streamManager.snapshot(deviceId));
    }

    @Transactional
    public void delete(Long deviceId) {
        DeviceRecord device = requireOwned(deviceId);
        streamManager.stop(device.id());
        int deleted = deviceRepository.delete(device.id(), device.userId());
        if (deleted == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Device not found");
        }
    }

    private DeviceRecord requireOwned(Long deviceId) {
        Long userId = SecurityUtils.currentUser().id();
        return deviceRepository.findByIdAndUser(deviceId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Device not found"));
    }

    private void ensurePetBelongsToUser(Long petId, Long userId) {
        if (petId == null) {
            return;
        }
        petRepository.findByIdAndUser(petId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "Pet does not belong to current user"));
    }

    private DeviceResponse toResponse(DeviceRecord device) {
        return DeviceResponse.from(device, readRoi(device.roiPolygonJson()));
    }

    private List<RoiPoint> readRoi(String roiJson) {
        if (!StringUtils.hasText(roiJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(roiJson, ROI_TYPE);
        } catch (Exception exception) {
            return List.of();
        }
    }

    private String writeRoi(List<RoiPoint> points) {
        try {
            return objectMapper.writeValueAsString(points);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Unable to serialize ROI polygon");
        }
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
