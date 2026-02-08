package CamNecT.CamNecT_Server.domain.verification.document.service;

import CamNecT.CamNecT_Server.domain.point.model.PointEvent;
import CamNecT.CamNecT_Server.domain.point.model.TransactionType;
import CamNecT.CamNecT_Server.domain.point.service.PointService;
import CamNecT.CamNecT_Server.domain.users.model.UserProfile;
import CamNecT.CamNecT_Server.domain.users.model.UserStatus;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.domain.users.repository.UserProfileRepository;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.domain.verification.document.dto.AdminDocumentVerificationDetailResponse;
import CamNecT.CamNecT_Server.domain.verification.document.dto.AdminDocumentVerificationListItemResponse;
import CamNecT.CamNecT_Server.domain.verification.document.dto.AdminReviewDocumentVerificationRequest;
import CamNecT.CamNecT_Server.domain.verification.document.event.DocumentVerificationReviewedEvent;
import CamNecT.CamNecT_Server.domain.verification.document.repository.DocumentVerificationSubmissionRepository;
import CamNecT.CamNecT_Server.domain.verification.document.model.DocumentVerificationSubmission;
import CamNecT.CamNecT_Server.domain.verification.document.model.VerificationStatus;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.AuthErrorCode;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.UserErrorCode;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.VerificationErrorCode;
import CamNecT.CamNecT_Server.global.storage.dto.response.PresignDownloadResponse;
import CamNecT.CamNecT_Server.global.storage.service.PresignEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDocumentVerificationService {

    private final DocumentVerificationSubmissionRepository submissionRepo;
    private final UserRepository usersRepository;
    private final UserProfileRepository userProfileRepository;
    private final PresignEngine presignEngine;
    private final ApplicationEventPublisher eventPublisher;
    private final PointService pointService;

    @Transactional(readOnly = true)
    public Page<AdminDocumentVerificationListItemResponse> list(VerificationStatus status, Pageable pageable) {

        Page<DocumentVerificationSubmission> page =
                submissionRepo.findByStatusOrderBySubmittedAtDesc(status, pageable);

        List<Long> userIds = page.getContent().stream()
                .map(DocumentVerificationSubmission::getUserId)
                .distinct()
                .toList();

        Map<Long, Users> usersMap = usersRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(Users::getUserId, u -> u));

        return page.map(s -> {
            Users u = usersMap.get(s.getUserId());
            if (u == null) throw new CustomException(VerificationErrorCode.USER_NOT_FOUND);

            return new AdminDocumentVerificationListItemResponse(
                    s.getId(), s.getStatus(), s.getDocType(), s.getSubmittedAt(),
                    u.getUserId(), u.getUsername(), u.getPhoneNum()
            );
        });
    }

    @Transactional(readOnly = true)
    public AdminDocumentVerificationDetailResponse get(Long submissionId) {
        DocumentVerificationSubmission s = submissionRepo.findById(submissionId)
                .orElseThrow(() -> new CustomException(VerificationErrorCode.SUBMISSION_NOT_FOUND));

        Users u = usersRepository.findById(s.getUserId())
                .orElseThrow(() -> new CustomException(VerificationErrorCode.USER_NOT_FOUND));

        UserProfile p = userProfileRepository.findByUserId(s.getUserId()).orElse(null);

        return new AdminDocumentVerificationDetailResponse(
                s.getId(),
                s.getStatus(),
                s.getDocType(),
                s.getSubmittedAt(),
                s.getReviewedAt(),
                s.getRejectReason(),

                u.getUserId(),
                u.getUsername(),
                u.getPhoneNum(),
                u.getName(),

                p != null ? p.getStudentNo() : null,
                p != null ? p.getYearLevel() : null,
                p != null ? p.getInstitutionId() : null,
                p != null ? p.getMajorId() : null,

                safeName(s.getOriginalFilename()),
                normalize(s.getContentType()),
                s.getSize()
        );
    }

    @Transactional
    public void review(Long adminId, Long submissionId, AdminReviewDocumentVerificationRequest req) {

        DocumentVerificationSubmission s = submissionRepo.findById(submissionId)
                .orElseThrow(() -> new CustomException(VerificationErrorCode.SUBMISSION_NOT_FOUND));

        if (s.getStatus() != VerificationStatus.PENDING) {
            throw new CustomException(VerificationErrorCode.ONLY_PENDING_CAN_REVIEW);
        }

        Users user = usersRepository.findById(s.getUserId())
                .orElseThrow(() -> new CustomException(VerificationErrorCode.USER_NOT_FOUND));

        //APPROVE
        if (req.decision() == AdminReviewDocumentVerificationRequest.Decision.APPROVE) {

            if (user.getStatus() == UserStatus.EMAIL_PENDING) {
                throw new CustomException(AuthErrorCode.EMAIL_NOT_VERIFIED);
            }

            // 정책: ADMIN_PENDING만 승인 가능
            if (user.getStatus() != UserStatus.ADMIN_PENDING) {
                throw new CustomException(VerificationErrorCode.ONLY_PENDING_CAN_REVIEW);
            }

            // 승인 시 관리자 입력값을 UserProfile에 반영
            applyProfileInfoForApprove(user.getUserId(), req);

            s.approve(adminId);
            user.changeStatus(UserStatus.ACTIVE);
            user.markVerificationCompletePending();

            eventPublisher.publishEvent(new DocumentVerificationReviewedEvent(
                    user.getEmail(),
                    s.getDocType(),
                    AdminReviewDocumentVerificationRequest.Decision.APPROVE,
                    null
            ));

            Long receiverId = user.getUserId();
            if (receiverId != null) { pointService.changePoint(receiverId,300, TransactionType.EARN, PointEvent.signup(receiverId)); }



            return;
        }

        // REJECT
        String reason = trimToNull(req.reason());
        if (reason == null) {
            throw new CustomException(VerificationErrorCode.REJECT_REASON_REQUIRED);
        }

        s.reject(adminId, reason);

        eventPublisher.publishEvent(new DocumentVerificationReviewedEvent(
                user.getEmail(),
                s.getDocType(),
                AdminReviewDocumentVerificationRequest.Decision.REJECT,
                reason
        ));
    }

    @Transactional(readOnly = true)
    public PresignDownloadResponse downloadUrl(Long submissionId) {

        DocumentVerificationSubmission s = submissionRepo.findById(submissionId)
                .orElseThrow(() -> new CustomException(VerificationErrorCode.SUBMISSION_NOT_FOUND));

        if (!StringUtils.hasText(s.getStorageKey())) {
            throw new CustomException(VerificationErrorCode.FILE_NOT_FOUND);
        }

        String ct = normalize(s.getContentType());
        String name = safeName(s.getOriginalFilename());

        return presignEngine.presignDownload(s.getStorageKey(), name, ct);
    }

    private void applyProfileInfoForApprove(Long userId, AdminReviewDocumentVerificationRequest req) {

        String studentName = trimToNull(req.studentName());
        String studentNo = trimToNull(req.studentNo());
        Long institutionId = req.institutionId();
        Long majorId = req.majorId();

        if (studentNo == null || studentName == null || institutionId == null || majorId == null) {
            // 승인 버튼은 “관리자 입력값 채운 뒤에만 호출”이지만 서버에서도 방어
            throw new CustomException(VerificationErrorCode.APPROVE_FIELDS_REQUIRED); // 임시. 전용 에러코드 추천
        }

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_PROFILE_NOT_FOUND));

        profile.applyVerifiedInfo(studentName, studentNo, institutionId, majorId);
    }

    private String safeName(String name) {
        return (name == null || name.isBlank()) ? "document" : name;
    }

    private String normalize(String ct) {
        return (ct == null) ? "" : ct.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isBlank() ? null : t;
    }
}
