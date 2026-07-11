package CamNecT.server.global.notification.service;

import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.AuthErrorCode;
import CamNecT.server.global.notification.dto.request.RegisterPushTokenRequest;
import CamNecT.server.global.notification.model.PushDevice;
import CamNecT.server.global.notification.repository.PushDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PushDeviceService {

    private final PushDeviceRepository pushDeviceRepository;
    private final UserRepository userRepository;

    @Transactional
    public RegisterResult register(Long userId, RegisterPushTokenRequest req) {
        userRepository.lockUserRow(userId);
        if (!userRepository.existsById(userId)) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }

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

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<String> findEnabledTokens(Long userId) {
        return pushDeviceRepository.findAllByUserIdAndEnabledTrue(userId)
                .stream()
                .map(PushDevice::getFcmToken)
                .distinct()
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void disableTokens(List<String> invalidTokens) {
        if (invalidTokens == null || invalidTokens.isEmpty()) return;

        List<PushDevice> devices = pushDeviceRepository.findAllByFcmTokenIn(invalidTokens);
        for (PushDevice d : devices) d.disable();
        pushDeviceRepository.saveAll(devices);
    }

    public record RegisterResult(Long pushDeviceId, boolean created) {}
}
