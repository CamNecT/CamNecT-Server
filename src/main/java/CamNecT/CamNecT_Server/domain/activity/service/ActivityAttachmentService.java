package CamNecT.CamNecT_Server.domain.activity.service;

import CamNecT.CamNecT_Server.domain.activity.model.external_activity.ExternalActivityAttachment;
import CamNecT.CamNecT_Server.domain.activity.model.props.ActivityAttachmentProps;
import CamNecT.CamNecT_Server.domain.activity.model.props.ActivityThumbnailProps;
import CamNecT.CamNecT_Server.domain.activity.repository.external_activity.ExternalActivityAttachmentRepository;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.ActivityErrorCode;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.StorageErrorCode;
import CamNecT.CamNecT_Server.global.common.service.GlobalPresignMethods;
import CamNecT.CamNecT_Server.global.storage.dto.request.PresignUploadBatchRequest;
import CamNecT.CamNecT_Server.global.storage.dto.request.PresignUploadRequest;
import CamNecT.CamNecT_Server.global.storage.dto.response.PresignDownloadResponse;
import CamNecT.CamNecT_Server.global.storage.dto.response.PresignUploadBatchResponse;
import CamNecT.CamNecT_Server.global.storage.dto.response.PresignUploadResponse;
import CamNecT.CamNecT_Server.global.storage.model.UploadPurpose;
import CamNecT.CamNecT_Server.global.storage.model.UploadTicket;
import CamNecT.CamNecT_Server.global.storage.repository.UploadTicketRepository;
import CamNecT.CamNecT_Server.global.storage.service.PresignEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ActivityAttachmentService {

    private static final Set<String> THUMB_ALLOWED = Set.of("image/jpeg","image/png","image/webp");

    private final UserRepository userRepository;
    private final PresignEngine presignEngine;
    private final UploadTicketRepository ticketRepo;
    private final GlobalPresignMethods globalPresignMethods;

    private final ActivityAttachmentProps attachmentProps;
    private final ActivityThumbnailProps thumbnailProps;
    private final ExternalActivityAttachmentRepository attachmentRepository;

    @Transactional
    public PresignUploadResponse presignThumbnail(Long userId, PresignUploadRequest req) {
        userRepository.lockUserRow(userId);

        String ct = globalPresignMethods.normalize(req.contentType());
        if (req.size() == null || req.size() <= 0) throw new CustomException(StorageErrorCode.EMPTY_FILE_NOT_ALLOWED);
        if (req.size() > thumbnailProps.maxFileSizeBytes()) throw new CustomException(StorageErrorCode.FILE_TOO_LARGE);

        if (!THUMB_ALLOWED.contains(ct)) throw new CustomException(StorageErrorCode.UNSUPPORTED_CONTENT_TYPE);

        String prefix = "activity/user-" + userId + "/thumbnail";
        return presignEngine.issueUpload(
                userId,
                UploadPurpose.ACTIVITY_THUMBNAIL,
                prefix,
                ct,
                req.size(),
                req.originalFilename()
        );
    }

    @Transactional
    public PresignUploadBatchResponse presignAttachmentsBatch(Long userId, PresignUploadBatchRequest req) {
        List<PresignUploadBatchRequest.Item> items =
                (req == null || req.items() == null) ? List.of()
                        : req.items().stream().filter(Objects::nonNull).toList();

        if (items.isEmpty()) throw new CustomException(StorageErrorCode.EMPTY_FILE_NOT_ALLOWED);
        if (items.size() > attachmentProps.maxFiles()) throw new CustomException(StorageErrorCode.UPLOAD_TICKET_LIMIT_EXCEEDED);

        userRepository.lockUserRow(userId);

        LocalDateTime now = LocalDateTime.now();
        ticketRepo.bulkExpirePendingByUserPurpose(userId, UploadPurpose.ACTIVITY_ATTACHMENT, now);

        long pendingActive = ticketRepo.countByUserIdAndPurposeAndStatusAndExpiresAtAfter(
                userId, UploadPurpose.ACTIVITY_ATTACHMENT, UploadTicket.Status.PENDING, now
        );
        if (pendingActive + items.size() > attachmentProps.maxFiles()) {
            throw new CustomException(StorageErrorCode.UPLOAD_TICKET_LIMIT_EXCEEDED);
        }

        String prefix = "activity/user-" + userId + "/attachments";
        List<PresignEngine.IssueItem> issueItems = new ArrayList<>(items.size());

        for (var item : items) {
            String ct = globalPresignMethods.normalize(item.contentType());
            if (item.size() <= 0) throw new CustomException(StorageErrorCode.EMPTY_FILE_NOT_ALLOWED);
            if (item.size() > attachmentProps.maxFileSizeBytes()) throw new CustomException(StorageErrorCode.FILE_TOO_LARGE);

            issueItems.add(new PresignEngine.IssueItem(ct, item.size(), item.originalFilename()));
        }

        return new PresignUploadBatchResponse(
                presignEngine.issueUploadBatch(
                        userId,
                        UploadPurpose.ACTIVITY_ATTACHMENT,
                        prefix,
                        issueItems,
                        attachmentProps.maxFiles()
                )
        );
    }

    @Transactional(readOnly = true)
    public PresignDownloadResponse presignDownload(Long activityId, Long attachmentId) {
        ExternalActivityAttachment att = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

        if (!Objects.equals(att.getExternalActivity(), activityId)) {
            throw new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND);
        }

        String key = att.getFileUrl(); // 필드명 정리 전이면
        if (!StringUtils.hasText(key)) throw new CustomException(StorageErrorCode.STORAGE_KEY_REQUIRED);

        var t = ticketRepo.findByStorageKey(key).orElse(null);
        return presignEngine.presignDownload(
                key,
                (t == null) ? null : t.getOriginalFilename(),
                (t == null) ? null : t.getContentType()
        );
    }
}
