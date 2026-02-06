package CamNecT.CamNecT_Server.domain.community.service;

import CamNecT.CamNecT_Server.domain.community.dto.request.AttachmentRequest;
import CamNecT.CamNecT_Server.domain.community.model.Posts.CommunityAttachmentProps;
import CamNecT.CamNecT_Server.domain.community.model.Posts.PostAttachments;
import CamNecT.CamNecT_Server.domain.community.model.Posts.Posts;
import CamNecT.CamNecT_Server.domain.community.repository.Posts.PostAttachmentsRepository;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.StorageErrorCode;
import CamNecT.CamNecT_Server.global.common.service.GlobalPresignMethods;
import CamNecT.CamNecT_Server.global.storage.dto.request.PresignUploadBatchRequest;
import CamNecT.CamNecT_Server.global.storage.dto.response.PresignUploadBatchResponse;
import CamNecT.CamNecT_Server.global.storage.dto.response.PresignUploadResponse;
import CamNecT.CamNecT_Server.global.storage.model.UploadPurpose;
import CamNecT.CamNecT_Server.global.storage.model.UploadRefType;
import CamNecT.CamNecT_Server.global.storage.model.UploadTicket;
import CamNecT.CamNecT_Server.global.storage.repository.UploadTicketRepository;
import CamNecT.CamNecT_Server.global.storage.service.PresignEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PostAttachmentsService {

    private final PostAttachmentsRepository postAttachmentsRepository;
    private final UserRepository userRepository;

    private final PresignEngine presignEngine;
    private final CommunityAttachmentProps props;

    private final UploadTicketRepository ticketRepo;

    private final GlobalPresignMethods globalPresignMethods;


    @Transactional
    public PresignUploadBatchResponse presignBatch(Long userId, PresignUploadBatchRequest req) {
        List<PresignUploadBatchRequest.Item> items =
                (req == null || req.items() == null) ? List.of()
                        : req.items().stream().filter(Objects::nonNull).toList();

        if (items.isEmpty()) throw new CustomException(StorageErrorCode.EMPTY_FILE_NOT_ALLOWED);
        if (items.size() > props.maxFiles()) throw new CustomException(StorageErrorCode.UPLOAD_TICKET_LIMIT_EXCEEDED);

        // 동시성 방지: 같은 userId는 presign 발급을 직렬화
        userRepository.lockUserRow(userId);

        long pending = ticketRepo.countByUserIdAndPurposeAndStatus(
                userId, UploadPurpose.COMMUNITY_POST_ATTACHMENT, UploadTicket.Status.PENDING
        );

        // “현재 pending + 이번에 발급할 개수”로 제한
        if (pending + items.size() > props.maxFiles()) {
            throw new CustomException(StorageErrorCode.UPLOAD_TICKET_LIMIT_EXCEEDED);
        }

        String prefix = "community/user-" + userId + "/attachments";

        List<PresignEngine.IssueItem> issueItems = new ArrayList<>(items.size());

        for (var item : items) {
            String ct = globalPresignMethods.normalize(item.contentType());

            if (item.size() <= 0) throw new CustomException(StorageErrorCode.EMPTY_FILE_NOT_ALLOWED);
            if (item.size() > props.maxFileSizeBytes()) throw new CustomException(StorageErrorCode.FILE_TOO_LARGE);
            if (!StringUtils.hasText(ct) || !props.allowedContentTypes().contains(ct)) {
                throw new CustomException(StorageErrorCode.UNSUPPORTED_CONTENT_TYPE);
            }

            issueItems.add(new PresignEngine.IssueItem(ct, item.size(), item.originalFilename()));
        }
        List<PresignUploadResponse> out = presignEngine.issueUploadBatch(
                userId,
                UploadPurpose.COMMUNITY_POST_ATTACHMENT,
                prefix,
                issueItems,
                props.maxFiles()
        );

        return new PresignUploadBatchResponse(out);
    }



    @Transactional
    public void replace(Posts post, Long userId, List<AttachmentRequest> attachments) {

        if (attachments != null && attachments.size() > props.maxFiles()) {
            throw new CustomException(StorageErrorCode.UPLOAD_TICKET_LIMIT_EXCEEDED);
        }

        List<PostAttachments> oldActive =
                postAttachmentsRepository.findByPost_IdAndStatusTrueOrderBySortOrderAscIdAsc(post.getId());

        Set<String> oldKeys = new HashSet<>();
        for (PostAttachments a : oldActive) {
            if (StringUtils.hasText(a.getFileKey())) oldKeys.add(a.getFileKey());
            if (StringUtils.hasText(a.getThumbnailKey())) oldKeys.add(a.getThumbnailKey());
        }

        postAttachmentsRepository.softDeleteByPostId(post.getId());

        if (attachments == null || attachments.isEmpty()) {
            registerAfterCommitDelete(oldActive, Set.of());
            return;
        }

        String finalFilePrefix = "community/posts/post-" + post.getId() + "/attachments";
        String finalThumbPrefix = "community/posts/post-" + post.getId() + "/thumbnails";

        List<PostAttachments> toSave = new ArrayList<>();
        int order = 0;

        Set<String> newFinalKeys = new HashSet<>();
        Set<String> consumedThisRequest = new HashSet<>();

        for (AttachmentRequest req : attachments) {
            if (req == null) continue;

            String inFileKey = req.fileKey();
            if (!StringUtils.hasText(inFileKey)) continue;

            String inThumbKey = req.thumbnailKey();

            String finalFileKey = resolveFinalKey(
                    userId, post.getId(), oldKeys, consumedThisRequest,
                    inFileKey, finalFilePrefix
            );

            String finalThumbKey = null;
            if (StringUtils.hasText(inThumbKey)) {
                finalThumbKey = resolveFinalKey(
                        userId, post.getId(), oldKeys, consumedThisRequest,
                        inThumbKey, finalThumbPrefix
                );
            }

            newFinalKeys.add(finalFileKey);
            if (StringUtils.hasText(finalThumbKey)) newFinalKeys.add(finalThumbKey);

            toSave.add(PostAttachments.create(
                    post,
                    finalFileKey,
                    finalThumbKey,
                    req.width(),
                    req.height(),
                    req.fileSize(),
                    order++
            ));
        }

        if (!toSave.isEmpty()) {
            postAttachmentsRepository.saveAll(toSave);
        }

        registerAfterCommitDelete(oldActive, newFinalKeys);
    }

    private String resolveFinalKey(
            Long userId,
            Long postId,
            Set<String> oldKeys,
            Set<String> consumedThisRequest,
            String keyFromClient,
            String finalPrefix
    ) {
        if (oldKeys.contains(keyFromClient)) {
            return keyFromClient;
        }

        if (!consumedThisRequest.add(keyFromClient)) {
            return keyFromClient;
        }

        return presignEngine.consume(
                userId,
                UploadPurpose.COMMUNITY_POST_ATTACHMENT,
                UploadRefType.POST,
                postId,
                keyFromClient,
                finalPrefix
        );
    }

    @Transactional
    public void purgeAllByPostId(Long postId) {
        List<PostAttachments> oldActive =
                postAttachmentsRepository.findByPost_IdAndStatusTrueOrderBySortOrderAscIdAsc(postId);

        postAttachmentsRepository.softDeleteByPostId(postId);

        registerAfterCommitDelete(oldActive, Set.of());
    }

    private void registerAfterCommitDelete(List<PostAttachments> oldActive, Set<String> newKeys) {
        Set<String> deleteKeys = new HashSet<>();

        for (PostAttachments a : oldActive) {
            String fk = a.getFileKey();
            String tk = a.getThumbnailKey();

            if (StringUtils.hasText(fk) && !newKeys.contains(fk)) deleteKeys.add(fk);
            if (StringUtils.hasText(tk) && !newKeys.contains(tk)) deleteKeys.add(tk);
        }
        globalPresignMethods.deleteAfterCommit(deleteKeys);
    }
}
