package CamNecT.server.global.notification.service;

import CamNecT.server.global.notification.dto.request.RegisterPushTokenRequest;
import CamNecT.server.global.notification.model.PushDevice;
import CamNecT.server.global.notification.repository.PushDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PushDeviceService {

    private final PushDeviceRepository pushDeviceRepository;

    @Transactional
    public RegisterResult register(Long userId, RegisterPushTokenRequest req) {
        PushDevice device = pushDeviceRepository.findByUserIdAndDeviceId(userId, req.deviceId())
                .map(existing -> {
                    existing.updateToken(req.platform(), req.token());
                    return existing;
                })
                .orElseGet(() -> PushDevice.builder()
                        .userId(userId)
                        .deviceId(req.deviceId())
                        .platform(req.platform())
                        .fcmToken(req.token())
                        .enabled(true)
                        .build());

        boolean created = (device.getId() == null);
        PushDevice saved = pushDeviceRepository.save(device);
        return new RegisterResult(saved.getId(), created);
    }

    @Transactional(readOnly = true)
    public List<String> findEnabledTokens(Long userId) {
        return pushDeviceRepository.findAllByUserIdAndEnabledTrue(userId)
                .stream()
                .map(PushDevice::getFcmToken)
                .distinct()
                .toList();
    }

    @Transactional
    public void disableTokens(List<String> invalidTokens) {
        if (invalidTokens == null || invalidTokens.isEmpty()) return;

        List<PushDevice> devices = pushDeviceRepository.findAllByFcmTokenIn(invalidTokens);
        for (PushDevice d : devices) d.disable();
        pushDeviceRepository.saveAll(devices);
    }

    public record RegisterResult(Long pushDeviceId, boolean created) {}
}
