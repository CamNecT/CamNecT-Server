package CamNecT.server.domain.verification.phone;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class PhoneVerificationCleanup {
    private final PhoneChallengeRepository challenges;
    private final SmsQuotaRepository quotas;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.phone-verification.cleanup-delay-ms:300000}")
    @Transactional
    public void cleanup() {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneId.of("Asia/Seoul"));
        // Same budget lock as issuance; retain a day for cooldowns that cross midnight.
        if (quotas.lockBudget().isEmpty()) return;
        quotas.deleteExpired(now.minusDays(1));
        challenges.deleteExpired(now);
    }
}
