package CamNecT.server.global.notification.service;

import CamNecT.server.global.common.auth.AccountAccessGuard;
import CamNecT.server.global.notification.model.PushOptOut;
import CamNecT.server.global.notification.repository.PushOptOutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushPreferenceService {
    private final PushOptOutRepository optOuts;
    private final AccountAccessGuard accountAccess;

    @Transactional(readOnly = true)
    public boolean isEnabled(Long userId) {
        return !optOuts.existsById(userId);
    }

    @Transactional
    public void setEnabled(Long userId, boolean enabled) {
        accountAccess.requireAccessibleForUpdate(userId);
        if (enabled) {
            optOuts.deleteById(userId);
        } else if (!optOuts.existsById(userId)) {
            optOuts.save(new PushOptOut(userId));
        }
    }
}
