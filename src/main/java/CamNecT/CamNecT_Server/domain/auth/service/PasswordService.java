package CamNecT.CamNecT_Server.domain.auth.service;

import CamNecT.CamNecT_Server.domain.profile.dto.request.UpdatePasswordRequest;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void updateMyPassword(Long userId, UpdatePasswordRequest req) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new CustomException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        validatePassword(req.newPassword());
        user.changePasswordHash(passwordEncoder.encode(req.newPassword()));
        // save는 영속 상태면 생략 가능
    }

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.{8,16}$)(?=.*[a-z])(?=.*\\d)[A-Za-z\\d!@#$%^&*()_+\\[\\]{}\\\\|;:'\",.<>/?`~=-]+$"
    );

    protected void validatePassword(String pw) {
        if (pw == null || !PASSWORD_PATTERN.matcher(pw).matches()) {
            throw new CustomException(AuthErrorCode.INVALID_PASSWORD);
        }
    }
}