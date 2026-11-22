package CamNecT.server.domain.verification.document.service;

import static org.mockito.Mockito.verifyNoInteractions;
import org.springframework.test.util.ReflectionTestUtils;
import CamNecT.server.global.point.model.PointEvent;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.ParameterizedTest;
import CamNecT.server.domain.profile.components.majors.model.Majors;
import CamNecT.server.domain.users.model.UserProfile;
import CamNecT.server.domain.report.service.UserReportPenaltyService;
import CamNecT.server.domain.profile.components.institutions.repository.InstitutionRepository;
import CamNecT.server.domain.profile.components.majors.repository.MajorRepository;
import CamNecT.server.domain.users.model.UserStatus;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.repository.UserProfileRepository;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.domain.verification.document.dto.AdminReviewDocumentVerificationRequest;
import CamNecT.server.domain.verification.document.model.DocumentVerificationSubmission;
import CamNecT.server.domain.verification.document.model.VerificationStatus;
import CamNecT.server.domain.verification.document.repository.DocumentVerificationSubmissionRepository;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.VerificationErrorCode;
import CamNecT.server.global.point.service.PointService;
import CamNecT.server.global.storage.service.PresignEngine;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminDocumentVerificationLockingTest {

    private final DocumentVerificationSubmissionRepository submissionRepository =
            mock(DocumentVerificationSubmissionRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserProfileRepository profiles = mock(UserProfileRepository.class);
    private final InstitutionRepository institutions = mock(InstitutionRepository.class);
    private final MajorRepository majors = mock(MajorRepository.class);
    private final UserReportPenaltyService penalties = mock(UserReportPenaltyService.class);
    private final PointService points = mock(PointService.class);
    private final AdminDocumentVerificationService service = new AdminDocumentVerificationService(
            submissionRepository,
            userRepository,
            profiles, institutions, majors,
            mock(PresignEngine.class),
            mock(ApplicationEventPublisher.class),
            points, penalties
    );

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void approvalGrantsSignupPointsOnlyToAnUnrestrictedRecipient(boolean restricted) {
        var user = Users.builder().userId(1L).status(UserStatus.ADMIN_PENDING).build();
        var submission = DocumentVerificationSubmission.builder()
                .id(10L).userId(1L).status(VerificationStatus.PENDING).build();
        when(submissionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(submission));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(institutions.existsById(3L)).thenReturn(true);
        when(majors.findByMajorIdAndInstitution_InstitutionIdAndIsActiveTrue(4L, 3L))
                .thenReturn(Optional.of(mock(Majors.class)));
        when(profiles.findByUserId(1L)).thenReturn(Optional.of(mock(UserProfile.class)));
        when(penalties.blocksReward(1L, UserStatus.ACTIVE)).thenReturn(restricted);
        ReflectionTestUtils.setField(service, "rewardSignup", 300);
        service.review(99L, 10L, new AdminReviewDocumentVerificationRequest(
                AdminReviewDocumentVerificationRequest.Decision.APPROVE, null, "Student", "2026", 3L, 4L));

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        if (restricted) verifyNoInteractions(points);
        else verify(points).earnPoint(1L, 300, PointEvent.signup(1L));
    }

    @Test
    void approvalRechecksUserStateUnderLock() {
        DocumentVerificationSubmission submission = DocumentVerificationSubmission.builder()
                .id(10L)
                .userId(1L)
                .status(VerificationStatus.PENDING)
                .build();
        Users withdrawn = Users.builder().userId(1L).status(UserStatus.WITHDRAWN).build();
        when(submissionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(submission));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(withdrawn));
        AdminReviewDocumentVerificationRequest request =
                new AdminReviewDocumentVerificationRequest(
                        AdminReviewDocumentVerificationRequest.Decision.APPROVE,
                        null, null, null, null, null);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.review(99L, 10L, request)
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(VerificationErrorCode.ONLY_PENDING_CAN_REVIEW);
        assertThat(withdrawn.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        verify(userRepository).findByIdForUpdate(1L);
    }

    @Test
    void rejectionCanCloseAnOldPendingSubmissionWithoutReactivatingUser() {
        DocumentVerificationSubmission submission = DocumentVerificationSubmission.builder()
                .id(10L)
                .userId(1L)
                .status(VerificationStatus.PENDING)
                .build();
        Users withdrawn = Users.builder().userId(1L).status(UserStatus.WITHDRAWN).build();
        when(submissionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(submission));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(withdrawn));
        AdminReviewDocumentVerificationRequest request =
                new AdminReviewDocumentVerificationRequest(
                        AdminReviewDocumentVerificationRequest.Decision.REJECT,
                        "정리", null, null, null, null);

        service.review(99L, 10L, request);

        assertThat(submission.getStatus()).isEqualTo(VerificationStatus.REJECTED);
        assertThat(withdrawn.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
    }
}
