package CamNecT.server.domain.auth.service;

import CamNecT.server.domain.auth.dto.login.LoginRequest;
import CamNecT.server.domain.users.model.UserProfile;
import CamNecT.server.domain.users.model.UserStatus;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.repository.UserProfileRepository;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.domain.report.service.UserReportPenaltyService;
import CamNecT.server.domain.verification.document.repository.DocumentVerificationSubmissionRepository;
import CamNecT.server.global.jwt.service.TokenSessionService;
import CamNecT.server.global.jwt.util.JwtFacade;
import CamNecT.server.global.jwt.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceSessionFailureTest {

    @Mock UserRepository userRepository;
    @Mock UserProfileRepository userProfileRepository;
    @Mock DocumentVerificationSubmissionRepository submissionRepo;
    @Mock PasswordEncoder passwordEncoder;
    @Mock UserReportPenaltyService userReportPenaltyService;
    @Mock JwtUtil jwtUtil;
    @Mock JwtFacade jwtFacade;
    @Mock TokenSessionService tokenSessionService;
    @InjectMocks LoginService loginService;

    @Test
    void redisSessionFailureDoesNotConsumeTheOneTimeVerificationNotice() {
        Users user = Users.builder().userId(1L).username("signup").passwordHash("hash")
                .status(UserStatus.ACTIVE).build();
        UserProfile profile = UserProfile.builder().user(user).initialSetupCompleted(true).build();
        when(userRepository.findUserIdByUsername("signup")).thenReturn(Optional.of(1L));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password1", "hash")).thenReturn(true);
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(jwtFacade.createAccessToken(eq(user), anyString())).thenReturn("access");
        when(jwtFacade.createRefreshToken(eq(user), anyString())).thenReturn("refresh");
        doThrow(new RedisConnectionFailureException("test Redis outage"))
                .when(tokenSessionService).create(1L, "access", "refresh");

        assertThrows(RedisConnectionFailureException.class,
                () -> loginService.login(new LoginRequest("signup", "password1")));

        assertThat(profile.isInitialSetupCompleted()).isTrue();
        assertThat(profile.isVerificationCompleteNotified()).isFalse();
    }
}
