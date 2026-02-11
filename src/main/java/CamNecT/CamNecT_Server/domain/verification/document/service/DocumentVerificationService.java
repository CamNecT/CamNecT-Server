package CamNecT.CamNecT_Server.domain.verification.document.service;

import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.domain.verification.document.repository.DocumentVerificationSubmissionRepository;
import CamNecT.CamNecT_Server.domain.verification.document.config.DocumentVerificationProperties;
import CamNecT.CamNecT_Server.domain.verification.document.dto.DocumentVerificationDetailResponse;
import CamNecT.CamNecT_Server.domain.verification.document.dto.DocumentVerificationFileDto;
import CamNecT.CamNecT_Server.domain.verification.document.dto.DocumentVerificationListItemResponse;
import CamNecT.CamNecT_Server.domain.verification.document.dto.SubmitDocumentVerificationResponse;
import CamNecT.CamNecT_Server.domain.verification.document.model.DocumentType;
import CamNecT.CamNecT_Server.domain.verification.document.model.DocumentVerificationSubmission;
import CamNecT.CamNecT_Server.domain.verification.document.model.VerificationStatus;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.VerificationErrorCode;
import CamNecT.CamNecT_Server.global.common.service.GlobalPresignMethods;
import CamNecT.CamNecT_Server.global.storage.dto.request.PresignUploadRequest;
import CamNecT.CamNecT_Server.global.storage.dto.response.PresignDownloadResponse;
import CamNecT.CamNecT_Server.global.storage.dto.response.PresignUploadResponse;
import CamNecT.CamNecT_Server.global.storage.model.UploadPurpose;
import CamNecT.CamNecT_Server.global.storage.model.UploadRefType;
import CamNecT.CamNecT_Server.global.storage.model.UploadTicket;
import CamNecT.CamNecT_Server.global.storage.repository.UploadTicketRepository;
import CamNecT.CamNecT_Server.global.storage.service.FileStorage;
import CamNecT.CamNecT_Server.global.storage.service.PresignEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DocumentVerificationService {

    private final DocumentVerificationProperties props;

    private final DocumentVerificationSubmissionRepository submissionRepo;
    private final UserRepository userRepository;

    private final PresignEngine presignEngine;
    private final UploadTicketRepository ticketRepo;
    private final FileStorage fileStorage;
    private final GlobalPresignMethods globalPresignMethods;

    // ===== presign upload =====
    @Transactional
    public PresignUploadResponse presignUpload(Long userId, PresignUploadRequest req) {
        String ct = normalize(req.contentType());

        if (req.size() <= 0) throw new CustomException(VerificationErrorCode.EMPTY_FILE_NOT_ALLOWED);
        if (req.size() > props.maxFileSizeBytes()) throw new CustomException(VerificationErrorCode.FILE_TOO_LARGE);
        if (!StringUtils.hasText(ct) || !props.getAllowedContentTypes().contains(ct)) {
            throw new CustomException(VerificationErrorCode.UNSUPPORTED_CONTENT_TYPE);
        }

        String keyPrefix = "verification/user-" + userId + "/documents";

        // 티켓 만료 정리 + active pending 제한은 PresignEngine.issueUpload()가 담당
        return presignEngine.issueUpload(
                userId,
                UploadPurpose.VERIFICATION_DOCUMENT,
                keyPrefix,
                ct,
                props.maxFileSizeBytes(),     // 티켓에는 "허용 상한"을 저장
                req.originalFilename()
        );
    }

    // ===== submit (단일 key) =====
    @Transactional
    public SubmitDocumentVerificationResponse submit(Long userId, DocumentType docType, String documentKey) {

        if (!StringUtils.hasText(documentKey)) {
            throw new CustomException(VerificationErrorCode.DOCUMENTS_REQUIRED);
        }

        userRepository.lockUserRow(userId);

        DocumentVerificationSubmission oldPending = submissionRepo
                .findTopByUserIdAndStatusOrderBySubmittedAtDesc(userId, VerificationStatus.PENDING)
                .orElse(null);

        UploadTicket t = ticketRepo.findByStorageKey(documentKey)
                .orElseThrow(() -> new CustomException(VerificationErrorCode.FILE_NOT_FOUND));

        String ct = normalize(t.getContentType());
        if (!StringUtils.hasText(ct)) {
            throw new CustomException(VerificationErrorCode.UNSUPPORTED_CONTENT_TYPE);
        }

        DocumentVerificationSubmission sub = DocumentVerificationSubmission.builder()
                .userId(userId)
                .docType(docType)
                .status(VerificationStatus.PENDING)
                .submittedAt(LocalDateTime.now())
                .build();

        sub.attachFile(
                documentKey,
                safeName(t.getOriginalFilename()),
                ct,
                t.getSize()
        );

        submissionRepo.save(sub);

        String finalPrefix = "verification/submission-" + sub.getId() + "/documents";

        String finalKey = presignEngine.consume(
                userId,
                UploadPurpose.VERIFICATION_DOCUMENT,
                UploadRefType.VERIFICATION,
                sub.getId(),
                documentKey,
                finalPrefix
        );

        sub.attachFile(
                finalKey,
                safeName(t.getOriginalFilename()),
                normalize(t.getContentType()),
                t.getSize()
        );

        sub.replaceStorageKey(finalKey);

        if (oldPending != null && !oldPending.getId().equals(sub.getId())) {
            String oldKey = oldPending.getStorageKey();
            if (!StringUtils.hasText(oldKey)) throw new CustomException(VerificationErrorCode.OLD_PENDING_INVALID);

            globalPresignMethods.deleteAfterCommit(Set.of(oldKey));
            oldPending.cancel();
        }

        return new SubmitDocumentVerificationResponse(sub.getId(), sub.getStatus(), sub.getSubmittedAt());
    }

    @Transactional(readOnly = true)
    public List<DocumentVerificationListItemResponse> mySubmissions(Long userId) {
        return submissionRepo.findByUserIdOrderBySubmittedAtDesc(userId).stream()
                .map(r -> new DocumentVerificationListItemResponse(
                        r.getId(), r.getDocType(), r.getStatus(), r.getSubmittedAt(), r.getReviewedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentVerificationDetailResponse mySubmissionDetail(Long userId, Long submissionId) {
        DocumentVerificationSubmission r = submissionRepo.findByIdAndUserId(submissionId, userId)
                .orElseThrow(() -> new CustomException(VerificationErrorCode.SUBMISSION_NOT_FOUND));

        var file = new DocumentVerificationFileDto(
                r.getOriginalFilename(),
                r.getContentType(),
                r.getSize(),
                r.getStorageKey()
        );

        return new DocumentVerificationDetailResponse(
                r.getId(), r.getDocType(), r.getStatus(),
                r.getSubmittedAt(), r.getReviewedAt(), r.getRejectReason(),
                file
        );
    }

    @Transactional(readOnly = true)
    public PresignDownloadResponse myDownloadUrl(Long userId, Long submissionId) {
        DocumentVerificationSubmission r = submissionRepo.findByIdAndUserId(submissionId, userId)
                .orElseThrow(() -> new CustomException(VerificationErrorCode.SUBMISSION_NOT_FOUND));

        if (!StringUtils.hasText(r.getStorageKey())) {
            throw new CustomException(VerificationErrorCode.FILE_NOT_FOUND);
        }

        String filename = safeName(r.getOriginalFilename());
        String ct = normalize(r.getContentType());
        return presignEngine.presignDownload(r.getStorageKey(), filename, ct);
    }

    @Transactional
    public void cancel(Long userId, Long submissionId) {
        DocumentVerificationSubmission r = submissionRepo.findByIdAndUserId(submissionId, userId)
                .orElseThrow(() -> new CustomException(VerificationErrorCode.SUBMISSION_NOT_FOUND));

        if (r.getStatus() != VerificationStatus.PENDING) {
            throw new CustomException(VerificationErrorCode.ONLY_PENDING_CAN_REVIEW);
        }

        String key = r.getStorageKey();
        r.cancel();

        if (StringUtils.hasText(key)) {
            deleteAfterCommit(key);
        }
    }

    private void deleteAfterCommit(String key) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try { fileStorage.delete(key); } catch (Exception ignored) {}
                }
            });
        } else {
            try { fileStorage.delete(key); } catch (Exception ignored) {}
        }
    }

    private String safeName(String name) {
        return StringUtils.hasText(name) ? name : "file";
    }

    private String normalize(String ct) {
        return (ct == null) ? "" : ct.trim().toLowerCase(Locale.ROOT);
    }
}