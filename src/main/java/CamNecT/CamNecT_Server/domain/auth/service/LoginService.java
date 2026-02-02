package CamNecT.CamNecT_Server.domain.auth.service;

import CamNecT.CamNecT_Server.domain.auth.dto.login.LoginRequest;
import CamNecT.CamNecT_Server.domain.auth.dto.login.LoginResponse;
import CamNecT.CamNecT_Server.domain.users.model.UserStatus;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.AuthErrorCode;
import CamNecT.CamNecT_Server.global.jwt.JwtFacade;
import CamNecT.CamNecT_Server.global.jwt.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtFacade jwtFacade;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        Users user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        String enc = user.getPasswordHash();
        log.warn("[LOGIN] userId={}, username={}, encHead={}, encLen={}",
                user.getUserId(),
                user.getUsername(),
                enc == null ? "null" : enc.substring(0, Math.min(12, enc.length())),
                enc == null ? -1 : enc.length());

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new CustomException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        if (!user.isEmailVerified() || user.getStatus() == UserStatus.EMAIL_PENDING) {
            throw new CustomException(AuthErrorCode.EMAIL_NOT_VERIFIED);
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(AuthErrorCode.USER_SUSPENDED);
        }

        String access = jwtFacade.createAccessToken(user);
        String refresh = jwtFacade.createRefreshToken(user);

        return new LoginResponse(
                "Bearer",
                access,
                refresh,
                jwtUtil.getAccessTokenExpirationMs(),
                jwtUtil.getRefreshTokenExpirationMs(),
                user.getUserId(),
                user.getStatus().name()
        );
    }
}