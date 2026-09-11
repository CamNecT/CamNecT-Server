package CamNecT.server.domain.profile.service;

import CamNecT.server.domain.portfolio.repository.PortfolioRepository;
import CamNecT.server.domain.chat.repository.ChatRoomRepository;
import CamNecT.server.domain.profile.components.certificate.repository.CertificateRepository;
import CamNecT.server.domain.profile.components.education.repository.EducationRepository;
import CamNecT.server.domain.profile.components.experience.repository.ExperienceRepository;
import CamNecT.server.domain.profile.dto.request.UpdateOnboardingRequest;
import CamNecT.server.domain.profile.dto.response.ProfileStatusResponse;
import CamNecT.server.domain.users.model.UserProfile;
import CamNecT.server.domain.users.model.UserStatus;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.repository.UserFollowRepository;
import CamNecT.server.domain.users.repository.UserProfileRepository;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.domain.users.repository.UserTagMapRepository;
import CamNecT.server.global.common.auth.AccountAccessGuard;
import CamNecT.server.global.point.service.PointService;
import CamNecT.server.global.storage.service.GlobalPresignMethods;
import CamNecT.server.global.storage.service.PresignEngine;
import CamNecT.server.global.storage.service.PublicUrlIssuer;
import CamNecT.server.global.tag.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class ProfileOnboardingStateTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AccountAccessGuard accountAccessGuard = mock(AccountAccessGuard.class);
    private final CertificateRepository certificateRepository = mock(CertificateRepository.class);
    private final ExperienceRepository experienceRepository = mock(ExperienceRepository.class);
    private final UserProfileRepository userProfileRepository = mock(UserProfileRepository.class);
    private final UserFollowRepository userFollowRepository = mock(UserFollowRepository.class);
    private final ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
    private final PortfolioRepository portfolioRepository = mock(PortfolioRepository.class);
    private final UserTagMapRepository userTagMapRepository = mock(UserTagMapRepository.class);
    private final EducationRepository educationRepository = mock(EducationRepository.class);
    private final TagRepository tagRepository = mock(TagRepository.class);
    private final PresignEngine presignEngine = mock(PresignEngine.class);
    private final PublicUrlIssuer publicUrlIssuer = mock(PublicUrlIssuer.class);
    private final GlobalPresignMethods globalPresignMethods = mock(GlobalPresignMethods.class);
    private final PointService pointService = mock(PointService.class);
    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(
                userRepository,
                accountAccessGuard,
                certificateRepository,
                experienceRepository,
                userProfileRepository,
                userFollowRepository,
                chatRoomRepository,
                portfolioRepository,
                userTagMapRepository,
                educationRepository,
                tagRepository,
                presignEngine,
                publicUrlIssuer,
                globalPresignMethods,
                pointService
        );
    }

    @Test
    void completesProfileEvenWhenAllOptionalValuesAreAbsent() {
        Users user = Users.builder().userId(1L).status(UserStatus.ACTIVE).build();
        UserProfile profile = UserProfile.builder().user(user).build();
        when(accountAccessGuard.requireAccessibleForUpdate(1L)).thenReturn(user);
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        ProfileStatusResponse response = profileService.createOnboarding(
                1L,
                new UpdateOnboardingRequest(null, null, null)
        );

        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(profile.isInitialSetupCompleted()).isTrue();
    }

    @Test
    void completesOnboardingBeforeAdminApprovalWithoutActivatingAccount() {
        Users user = Users.builder().userId(1L).status(UserStatus.ADMIN_PENDING).build();
        UserProfile profile = UserProfile.builder().user(user).build();
        when(accountAccessGuard.requireAccessibleForUpdate(1L)).thenReturn(user);
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        ProfileStatusResponse response = profileService.createOnboarding(
                1L, new UpdateOnboardingRequest(null, null, List.of()));

        assertThat(response.status()).isEqualTo(UserStatus.ADMIN_PENDING);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ADMIN_PENDING);
        assertThat(profile.isInitialSetupCompleted()).isTrue();
        assertThat(profile.isVerificationCompleteNotified()).isFalse();
    }

    @Test
    void retryAfterCompletionPreservesProfileAndDoesNotConsumeTicketAgain() {
        Users user = Users.builder().userId(1L).status(UserStatus.ACTIVE).build();
        UserProfile profile = UserProfile.builder().user(user).build();
        profile.completeInitialSetup();
        profile.updateOnboardingProfile("saved bio", "saved-image");
        when(accountAccessGuard.requireAccessibleForUpdate(1L)).thenReturn(user);
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        ProfileStatusResponse response = profileService.createOnboarding(
                1L, new UpdateOnboardingRequest("already-consumed-ticket", "replacement", List.of(999L)));

        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(profile.getBio()).isEqualTo("saved bio");
        assertThat(profile.getProfileImageKey()).isEqualTo("saved-image");
        verifyNoInteractions(userTagMapRepository, tagRepository, presignEngine);
    }
}
