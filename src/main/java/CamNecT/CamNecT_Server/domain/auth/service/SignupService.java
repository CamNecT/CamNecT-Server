package CamNecT.CamNecT_Server.domain.auth.service;

import CamNecT.CamNecT_Server.domain.auth.dto.signup.VerifySignupEmailRequest;
import CamNecT.CamNecT_Server.domain.users.model.UserProfile;
import CamNecT.CamNecT_Server.domain.users.model.UserStatus;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.domain.users.repository.UserProfileRepository;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignupService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordService passwordService;

    @Transactional
    public Users signupVerifiedUser(VerifySignupEmailRequest req) {

        // 약관
        if (!req.agreements().serviceTerms() || !req.agreements().privacyTerms()) {
            throw new CustomException(AuthErrorCode.TERMS_REQUIRED);
        }

        // 비밀번호 정책
        passwordService.validatePassword(req.password());

        // 최종 유니크
        if (userRepository.existsByEmail(req.email())) throw new CustomException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        if (userRepository.existsByUsername(req.username())) throw new CustomException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
        if (userRepository.existsByPhoneNum(req.phoneNum())) throw new CustomException(AuthErrorCode.PHONENUM_ALREADY_EXISTS);

        Users user = Users.builder()
                .email(req.email())
                .username(req.username())
                .name(req.name())
                .phoneNum(req.phoneNum())
                .passwordHash(passwordEncoder.encode(req.password()))
                .termsServiceAgreed(true)
                .termsPrivacyAgreed(true)
                .emailVerified(true)
                .status(UserStatus.ADMIN_PENDING)
                .build();

        Users savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(AuthErrorCode.DUPLICATE_RESOURCE);
        }

        UserProfile emptyProfile = UserProfile.builder()
                .user(savedUser)
                .bio(null)
                .profileImageKey(null)
                .openToCoffeeChat(true)
                .studentNo(null)
                .yearLevel(null)
                .institutionId(null)
                .majorId(null)
                .build();

        userProfileRepository.save(emptyProfile);

        return savedUser;
    }
}
