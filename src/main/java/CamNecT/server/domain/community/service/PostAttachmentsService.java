package CamNecT.server.domain.community.service;

import CamNecT.server.domain.community.dto.request.AttachmentRequest;
import CamNecT.server.domain.community.model.props.CommunityAttachmentProps;
import CamNecT.server.domain.community.model.Posts.PostAttachments;
import CamNecT.server.domain.community.model.Posts.Posts;
import CamNecT.server.domain.community.repository.Posts.PostAttachmentsRepository;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.StorageErrorCode;
import CamNecT.server.global.storage.model.UploadTicket;
import CamNecT.server.global.storage.repository.UploadTicketRepository;
import CamNecT.server.global.storage.service.GlobalPresignMethods;
import CamNecT.server.global.storage.dto.request.PresignUploadBatchRequest;
import CamNecT.server.global.storage.dto.response.PresignUploadBatchResponse;
import CamNecT.server.global.storage.model.UploadPurpose;
import CamNecT.server.global.storage.model.UploadRefType;
import CamNecT.server.global.storage.service.PresignEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PostAttachmentsService {
    protected static final Set<String> THUMB_ALLOWED = Set.of("image/jpeg","image/png","image/webp");

    private final PostAttachmentsRepository postAttachmentsRepository;
    private final UserRepository userRepository;
    private final UploadTicketRepository uploadTicketRepository;

    private final PresignEngine presignEngine;
    private final CommunityAttachmentProps attachmentProps;
    private final GlobalPresignMethods globalPresignMethods;



    @Transactional
    public PresignUploadBatchResponse presignAttachmentsBatch(Long userId, PresignUploadBatchRequest req) {
        var items = (req == null || req.items() == null) ? List.<PresignUploadBatchRequest.Item>of()
                : req.items().stream().filter(Objects::nonNull).toList();

        if (items.isEmpty()) throw new CustomException(StorageErrorCode.EMPTY_FILE_NOT_ALLOWED);
        if (items.size() > attachmentProps.maxFiles()) throw new CustomException(StorageErrorCode.UPLOAD_TICKET_LIMIT_EXCEEDED);

        userRepository.lockUserRow(userId);

        String tempPrefix = "community/user-" + userId + "/attachments";


        for (PresignUploadBatchRequest.Item value : items) {
            validateAttachmentItem(value);
        }

        List<PresignEngine.IssueItem> issueItems = new ArrayList<>(items.size());
        for (var item : items) {
            String ct = globalPresignMethods.normalize(item.contentType());
            issueItems.add(new PresignEngine.IssueItem(ct, item.size(), item.originalFilename()));
        }

        return new PresignUploadBatchResponse(
                presignEngine.issueUploadBatch(
                        userId,
                        UploadPurpose.COMMUNITY_POST_ATTACHMENT,
                        tempPrefix,
                        issueItems,
                        attachmentProps.maxFiles()
                )
        );
    }

    /**
     * 게시글 저장/수정 시 첨부 교체
     * - 모든 첨부는 /attachments 로 이동
     * - 첫 첨부가 이미지면 /thumbnail 로 별도 copy 후 post.thumbnailKey 저장
     * - 첫 첨부가 이미지가 아니면 post.thumbnailKey = null
     */
    @Transactional
    public void replace(Posts post, Long userId, List<AttachmentRequest> attachments) {

        // update에서 null은 "첨부 수정 안 함"
        // create에서 null이어도 어차피 아무 작업 안 하면 됨
        if (attachments == null) return;
        if (attachments.size() > attachmentProps.maxFiles()) throw new CustomException(StorageErrorCode.UPLOAD_TICKET_LIMIT_EXCEEDED);

        // 기존 활성 첨부파일
        List<PostAttachments> oldActive = postAttachmentsRepository.
                findByPost_IdAndStatusTrueOrderBySortOrderAscIdAsc(post.getId());

        Set<String> oldKeys = new HashSet<>();
        Map<String, PostAttachments> oldByKey = new HashMap<>();
        for (PostAttachments a : oldActive) {
            if (StringUtils.hasText(a.getFileKey())) {
                oldKeys.add(a.getFileKey());
                oldByKey.put(a.getFileKey(), a);
            }
        }
        String oldThumbnailKey = post.getThumbnailKey();

        // 기존 활성건 soft delete
        postAttachmentsRepository.softDeleteByPostId(post.getId());

        // 새 첨부 없으면: 기존 파일 전부 삭제 예약
        if (attachments.isEmpty()) {
            post.updateThumbnailKey(null);
            registerAfterCommitDelete(oldActive, Set.of(), oldThumbnailKey, null);
            return;
        }

        String finalAttachPrefix = "community/posts/post-" + post.getId() + "/attachments";
        String finalThumbPrefix  = "community/posts/post-" + post.getId() + "/thumbnail";

        List<PostAttachments> toSave = new ArrayList<>();
        List<String> finalKeysInOrder = new ArrayList<>();
        Set<String> newFinalKeys = new HashSet<>();
        Map<String, String> resolvedThisRequest = new HashMap<>();
        Set<String> seenInputKeys = new HashSet<>();

        int order = 0;

        for (AttachmentRequest req : attachments) {
            if (req == null) continue;

            String inFileKey = req.fileKey();
            if (!StringUtils.hasText(inFileKey)) continue;

            // 같은 key가 중복으로 오면 1번만 처리
            if (!seenInputKeys.add(inFileKey)) continue;

            String finalFileKey = resolveFinalKey(
                    userId,
                    post.getId(),
                    oldKeys,
                    resolvedThisRequest,
                    req.fileKey(),
                    finalAttachPrefix
            );
            PostAttachments old = oldByKey.get(finalFileKey);

            Integer width = (req.width() != null)
                    ? req.width()
                    : (old != null ? old.getWidth() : null);

            Integer height = (req.height() != null)
                    ? req.height()
                    : (old != null ? old.getHeight() : null);

            Long fileSize = (req.fileSize() != null)
                    ? req.fileSize()
                    : (old != null ? old.getFileSize() : null);

            toSave.add(PostAttachments.create(
                    post,
                    finalFileKey,
                    width,
                    height,
                    fileSize,
                    order
            ));

            finalKeysInOrder.add(finalFileKey);
            newFinalKeys.add(finalFileKey);
            order++;
        }

        if (toSave.isEmpty()) {
            post.updateThumbnailKey(null);
            registerAfterCommitDelete(oldActive, Set.of(), oldThumbnailKey, null);
            return;
        }

        postAttachmentsRepository.saveAll(toSave);

        String newThumbnailKey = null;
        String thumbSourceKey = pickThumbnailSourceKey(finalKeysInOrder);
        if (thumbSourceKey != null) {
            newThumbnailKey = globalPresignMethods.copyToPrefix(thumbSourceKey, finalThumbPrefix);
        }

        post.updateThumbnailKey(newThumbnailKey);
        registerAfterCommitDelete(oldActive, newFinalKeys, oldThumbnailKey, newThumbnailKey);
    }

    private void validateAttachmentItem(PresignUploadBatchRequest.Item item) {
        String ct = globalPresignMethods.normalize(item.contentType());

        if (item.size() <= 0) throw new CustomException(StorageErrorCode.EMPTY_FILE_NOT_ALLOWED);
        if (item.size() > attachmentProps.maxFileSizeBytes()) throw new CustomException(StorageErrorCode.FILE_TOO_LARGE);

        if (!StringUtils.hasText(ct) || !attachmentProps.allowedContentTypes().contains(ct)) {
            throw new CustomException(StorageErrorCode.UNSUPPORTED_CONTENT_TYPE);
        }
    }

    /**
     * 현재 community 정책 유지:
     * "첫 첨부"가 이미지일 때만 썸네일 생성
     */
    private String pickThumbnailSourceKey(List<String> finalKeysInOrder) {
        if (finalKeysInOrder == null || finalKeysInOrder.isEmpty()) return null;

        String firstKey = finalKeysInOrder.getFirst();
        if (isImageKey(firstKey)) return firstKey;

        /*
         * 대체 정책(현재 미사용):
         * 첫 번째 첨부가 이미지가 아니면,
         * 첨부 목록 중 '첫 번째 이미지'를 썸네일로 사용
         *
         * for (String key : finalKeysInOrder) {
         *     if (isImageKey(key)) return key;
         * }
         */

        return null;
    }

    private boolean isImageKey(String key) {
        if (!StringUtils.hasText(key)) return false;

        String ct = uploadTicketRepository.findByStorageKey(key)
                .map(UploadTicket::getContentType)
                .map(globalPresignMethods::normalize)
                .orElse(null);

        if (StringUtils.hasText(ct)) {
            return THUMB_ALLOWED.contains(ct);
        }

        String k = key.toLowerCase(Locale.ROOT);
        return k.endsWith(".jpg") || k.endsWith(".jpeg") || k.endsWith(".png") || k.endsWith(".webp");
    }

    private String resolveFinalKey(
            Long userId,
            Long postId,
            Set<String> oldKeys,
            Map<String, String> resolvedThisRequest,
            String keyFromClient,
            String finalPrefix
    ) {
        if (oldKeys.contains(keyFromClient)) return keyFromClient;

        String alreadyResolved = resolvedThisRequest.get(keyFromClient);
        if (alreadyResolved != null) return alreadyResolved;

        String finalKey = presignEngine.consume(
                userId,
                UploadPurpose.COMMUNITY_POST_ATTACHMENT,
                UploadRefType.POST,
                postId,
                keyFromClient,
                finalPrefix
        );

        resolvedThisRequest.put(keyFromClient, finalKey);
        return finalKey;
    }

    @Transactional
    public void purgeAll(Posts post) {
        List<PostAttachments> oldActive =
                postAttachmentsRepository.findByPost_IdAndStatusTrueOrderBySortOrderAscIdAsc(post.getId());

        String oldThumbnailKey = post.getThumbnailKey();

        postAttachmentsRepository.softDeleteByPostId(post.getId());
        post.updateThumbnailKey(null);

        registerAfterCommitDelete(oldActive, Set.of(), oldThumbnailKey, null);
    }

    private void registerAfterCommitDelete(
            List<PostAttachments> oldActive,
            Set<String> newKeys,
            String oldThumbnailKey,
            String newThumbnailKey
    ) {
        Set<String> deleteKeys = new HashSet<>();

        for (PostAttachments a : oldActive) {
            String fk = a.getFileKey();
            if (StringUtils.hasText(fk) && !newKeys.contains(fk)) {
                deleteKeys.add(fk);
            }
        }

        if (StringUtils.hasText(oldThumbnailKey)
                && !Objects.equals(oldThumbnailKey, newThumbnailKey)
                && !newKeys.contains(oldThumbnailKey)) {
            deleteKeys.add(oldThumbnailKey);
        }

        globalPresignMethods.deleteAfterCommit(deleteKeys);
    }
}
