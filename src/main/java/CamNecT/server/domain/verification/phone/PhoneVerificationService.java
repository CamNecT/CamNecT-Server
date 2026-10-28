package CamNecT.server.domain.verification.phone;

import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.model.UserStatus;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.global.common.auth.AccountAccessGuard;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ZoneId BILLING_ZONE = ZoneId.of("Asia/Seoul");
    private final AccountAccessGuard accessGuard;
    private final UserRepository users;
    private final PhoneChallengeRepository challenges;
    private final SmsQuotaRepository quotas;
    private final PhoneVerificationProperties properties;
    private final PhoneCodeHasher hasher;
    private final SmsSender sender;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public record PhoneStatus(String phoneNum, boolean phoneVerified, LocalDateTime phoneVerifiedAt,
                              boolean phoneVerificationRequired, boolean smsAvailable) {}
    public record SendResponse(String challengeId, int expiresInSeconds, int resendAfterSeconds) {}

    @Transactional(readOnly = true)
    public PhoneStatus status(Long userId) {
        Users user = users.findById(userId).orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_TOKEN));
        return statusOf(user);
    }

    @Transactional
    public PhoneStatus updatePhone(Long userId, String value) {
        String phone = PhoneNumbers.normalize(value);
        Users user = requireActiveForUpdate(userId);
        if (user.changePhone(phone)) {
            // Purchase, replacement and verification all lock the same user row first.
            challenges.deleteById(userId);
        }
        return statusOf(user);
    }

    public SendResponse send(Long userId) {
        requireEnabled();
        String code = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        String challengeId = UUID.randomUUID().toString();
        String phone = Objects.requireNonNull(transactions.execute(tx -> {
            Users user = requireActiveForUpdate(userId);
            if (user.isPhoneVerified()) throw new CustomException(PhoneErrorCode.ALREADY_VERIFIED);
            if (user.getPhoneNum() == null) throw new CustomException(PhoneErrorCode.PHONE_REQUIRED);
            LocalDateTime now = now();
            reserveBudget(userId, user.getPhoneNum(), now);
            challenges.save(PhoneChallenge.issue(userId, challengeId,
                    phoneHash(user.getPhoneNum()),
                    hasher.hash(codeInput(userId, challengeId, user.getPhoneNum(), code)),
                    now.plusSeconds(properties.getExpirationSeconds())));
            return user.getPhoneNum();
        }));

        // The budget and challenge are committed before I/O. Timeouts are NOT automatically retried.
        // A failed/unknown send still consumes the reservation because the provider may have sent it.
        final String providerId;
        try {
            providerId = sender.send(phone, code);
        } catch (RuntimeException e) {
            finishSend(userId, challengeId, null);
            throw new CustomException(PhoneErrorCode.SMS_SEND_FAILED);
        }
        if (!finishSend(userId, challengeId, providerId)) {
            throw new CustomException(PhoneErrorCode.INVALID_CHALLENGE);
        }
        return new SendResponse(challengeId, properties.getExpirationSeconds(), properties.getResendCooldownSeconds());
    }

    public PhoneStatus verify(Long userId, String challengeId, String code) {
        requireEnabled();
        VerificationResult result = Objects.requireNonNull(transactions.execute(tx -> {
            Users user = requireActiveForUpdate(userId);
            PhoneChallenge challenge = challenges.findById(userId).orElse(null);
            LocalDateTime now = now();
            if (challenge == null || !challenge.getChallengeId().equals(challengeId)
                    || challenge.getSendState() != PhoneChallenge.SendState.SENT
                    || !challenge.getExpiresAt().isAfter(now)
                    || user.getPhoneNum() == null
                    || !challenge.getPhoneHash().equals(phoneHash(user.getPhoneNum()))) {
                return new VerificationResult(null, PhoneErrorCode.INVALID_CHALLENGE);
            }
            if (challenge.getAttempts() >= properties.getMaxAttempts()) {
                return new VerificationResult(null, PhoneErrorCode.TOO_MANY_ATTEMPTS);
            }
            if (code == null || !code.matches("[0-9]{6}")
                    || !hasher.matches(challenge.getCodeHash(), codeInput(userId, challengeId, user.getPhoneNum(), code))) {
                challenge.recordFailure();
                return new VerificationResult(null, challenge.getAttempts() >= properties.getMaxAttempts()
                        ? PhoneErrorCode.TOO_MANY_ATTEMPTS : PhoneErrorCode.INVALID_CODE);
            }
            user.verifyPhone(now);
            challenges.delete(challenge);
            return new VerificationResult(statusOf(user), null);
        }));
        // Throw after the transaction commits so invalid attempts cannot roll back their counters.
        if (result.error() != null) throw new CustomException(result.error());
        return result.status();
    }

    private boolean finishSend(Long userId, String challengeId, String providerId) {
        return Boolean.TRUE.equals(transactions.execute(tx -> {
            Users user = users.findByIdForUpdate(userId).orElse(null);
            PhoneChallenge challenge = challenges.findById(userId).orElse(null);
            if (user == null || user.getStatus() != UserStatus.ACTIVE || user.getPhoneNum() == null
                    || challenge == null || !challenge.getChallengeId().equals(challengeId)
                    || !challenge.getPhoneHash().equals(phoneHash(user.getPhoneNum()))) return false;
            if (providerId == null) challenge.failed();
            else challenge.sent(providerId);
            return true;
        }));
    }

    private void reserveBudget(Long userId, String phone, LocalDateTime now) {
        quotas.lockBudget().orElseThrow(() -> new CustomException(PhoneErrorCode.SMS_UNAVAILABLE));
        LocalDateTime tomorrow = now.toLocalDate().plusDays(1).atStartOfDay();
        LocalDateTime nextMonth = now.toLocalDate().withDayOfMonth(1).plusMonths(1).atStartOfDay();
        List<SmsQuota> reserved = List.of(
                checkQuota("month", nextMonth, properties.getMonthlyTotal(), now, false),
                checkQuota("day", tomorrow, properties.getDailyTotal(), now, false),
                checkQuota("user:" + userId, tomorrow, properties.getDailyPerUser(), now, true),
                checkQuota("phone:" + phoneHash(phone), tomorrow, properties.getDailyPerPhone(), now, true)
        );
        for (SmsQuota quota : reserved) {
            quota.consume(now);
            quotas.save(quota);
        }
    }

    private SmsQuota checkQuota(String key, LocalDateTime end, int limit, LocalDateTime now, boolean cooldown) {
        SmsQuota quota = quotas.findForUpdate(key).orElseGet(() -> new SmsQuota(key, end));
        if (cooldown && quota.getLastSentAt() != null
                && quota.getLastSentAt().plusSeconds(properties.getResendCooldownSeconds()).isAfter(now)) {
            throw new CustomException(PhoneErrorCode.RATE_LIMITED);
        }
        quota.resetIfExpired(now, end);
        if (quota.getCount() >= limit) throw new CustomException(PhoneErrorCode.RATE_LIMITED);
        return quota;
    }

    private Users requireActiveForUpdate(Long userId) {
        Users user = accessGuard.requireAccessibleForUpdate(userId);
        if (user.getStatus() != UserStatus.ACTIVE) throw new CustomException(AuthErrorCode.ACTIVE_ACCOUNT_REQUIRED);
        return user;
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) throw new CustomException(PhoneErrorCode.SMS_UNAVAILABLE);
    }

    private String phoneHash(String phone) { return hasher.hash("phone:" + phone); }

    private String codeInput(Long userId, String challengeId, String phone, String code) {
        return "otp:" + userId + ":" + challengeId + ":" + phone + ":" + code;
    }

    private LocalDateTime now() { return LocalDateTime.ofInstant(clock.instant(), BILLING_ZONE); }

    private PhoneStatus statusOf(Users user) {
        return new PhoneStatus(user.getPhoneNum(), user.isPhoneVerified(), user.getPhoneVerifiedAt(),
                properties.isPhoneFlowActive(), properties.isEnabled());
    }

    private record VerificationResult(PhoneStatus status, PhoneErrorCode error) {}
}
