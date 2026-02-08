package CamNecT.CamNecT_Server.domain.portfolio.service;

import CamNecT.CamNecT_Server.domain.portfolio.model.PortfolioAsset;
import CamNecT.CamNecT_Server.domain.portfolio.model.PortfolioProject;
import CamNecT.CamNecT_Server.domain.portfolio.model.props.PortfolioAssetProps;
import CamNecT.CamNecT_Server.domain.portfolio.model.props.PortfolioThumbnailProps;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.StorageErrorCode;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.UserErrorCode;
import CamNecT.CamNecT_Server.global.common.service.GlobalPresignMethods;
import CamNecT.CamNecT_Server.global.storage.dto.request.PresignUploadBatchRequest;
import CamNecT.CamNecT_Server.global.storage.dto.request.PresignUploadRequest;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioAttachmentService {

    private static final String DEFAULT_THUMB = "기본이미지";
    private static final Set<String> THUMB_ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");

    private final UserRepository userRepository;

    private final PresignEngine presignEngine;
    private final UploadTicketRepository ticketRepo;
    private final GlobalPresignMethods globalPresignMethods;

    private final PortfolioAssetProps assetProps;
    private final PortfolioThumbnailProps thumbnailProps;

    /**
     * 썸네일 업로드 presign (단건)
     * - 이미지 타입만 허용
     * - temp 경로로 발급됨
     */
    @Transactional
    public PresignUploadResponse presignThumbnail(Long userId, Long portfolioUserId, PresignUploadRequest req) {
        validateOwner(userId, portfolioUserId);
        userRepository.lockUserRow(userId);

        String ct = globalPresignMethods.normalize(req.contentType());
        if (req.size() == null || req.size() <= 0) throw new CustomException(StorageErrorCode.EMPTY_FILE_NOT_ALLOWED);
        if (req.size() > thumbnailProps.maxFileSizeBytes()) throw new CustomException(StorageErrorCode.FILE_TOO_LARGE);
        if (!THUMB_ALLOWED.contains(ct)) throw new CustomException(StorageErrorCode.UNSUPPORTED_CONTENT_TYPE);

        String prefix = "portfolio/user-" + userId + "/thumbnail";

        return presignEngine.issueUpload(
                userId,
                UploadPurpose.PORTFOLIO_ATTACHMENT,
                prefix,
                ct,
                req.size(),
                req.originalFilename()
        );
    }

    /**
     * assets 업로드 presign (다건)
     * - pdf 포함 가능(allowedContentTypes에 있으면)
     */
    @Transactional
    public PresignUploadBatchResponse presignAssetsBatch(Long userId, Long portfolioUserId, PresignUploadBatchRequest req) {
        validateOwner(userId, portfolioUserId);

        var items = (req == null || req.items() == null)
                ? List.<PresignUploadBatchRequest.Item>of()
                : req.items().stream().filter(Objects::nonNull).toList();

        if (items.isEmpty()) throw new CustomException(StorageErrorCode.EMPTY_FILE_NOT_ALLOWED);
        if (items.size() > assetProps.maxFiles()) throw new CustomException(StorageErrorCode.UPLOAD_TICKET_LIMIT_EXCEEDED);

        userRepository.lockUserRow(userId);

        String prefix = "portfolio/user-" + userId + "/assets";

        List<PresignEngine.IssueItem> issueItems = new ArrayList<>(items.size());
        for (var item : items) {
            String ct = globalPresignMethods.normalize(item.contentType());

            if (item.size() <= 0) throw new CustomException(StorageErrorCode.EMPTY_FILE_NOT_ALLOWED);
            if (item.size() > assetProps.maxFileSizeBytes()) throw new CustomException(StorageErrorCode.FILE_TOO_LARGE);
            if (!hasText(ct) || !assetProps.allowedContentTypes().contains(ct)) {
                throw new CustomException(StorageErrorCode.UNSUPPORTED_CONTENT_TYPE);
            }

            issueItems.add(new PresignEngine.IssueItem(ct, item.size(), item.originalFilename()));
        }

        return new PresignUploadBatchResponse(
                presignEngine.issueUploadBatch(
                        userId,
                        UploadPurpose.PORTFOLIO_ATTACHMENT,
                        prefix,
                        issueItems,
                        assetProps.maxFiles()
                )
        );
    }

    /**
     * create 직후: thumbnailKey/attachmentKeys consume + entity 반영
     * - thumbnail은 이미지 key만 허용
     * - attachmentKeys에 thumbnailKey가 섞여 들어오면 무시(중복 consume 방지)
     */
    @Transactional
    public void applyOnCreate(PortfolioProject project, Long userId, String thumbnailKey, List<String> attachmentKeys) {

        String finalThumbPrefix = "portfolio/user-" + userId + "/portfolio-" + project.getPortfolioId() + "/thumbnail";
        String finalAssetPrefix = "portfolio/user-" + userId + "/portfolio-" + project.getPortfolioId() + "/assets";

        // thumbnail
        if (hasText(thumbnailKey) && !DEFAULT_THUMB.equals(thumbnailKey)) {
            ensureThumbnailIsImage(thumbnailKey);
            String finalKey = consume(userId, project.getPortfolioId(), thumbnailKey, finalThumbPrefix);
            project.updateThumbnail(finalKey);
        }

        // assets (thumbKey는 제거됨)
        LinkedHashSet<String> reqKeys = distinctKeys(attachmentKeys, thumbnailKey);
        int order = 1;

        for (String k : reqKeys) {
            String finalKey = consume(userId, project.getPortfolioId(), k, finalAssetPrefix);

            UploadTicket t = ticketRepo.findByStorageKey(finalKey)
                    .orElseThrow(() -> new CustomException(UserErrorCode.PORTFOLIO_NOT_FOUND));

            project.getAssets().add(PortfolioAsset.builder()
                    .portfolioProject(project)
                    .type(t.getContentType())
                    .fileKey(finalKey)
                    .sortOrder(order++)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
    }

    /**
     * update: thumbnail 교체 + assets 교체(attachmentKeys가 null이 아니면)
     */
    @Transactional
    public void applyOnUpdate(PortfolioProject project, Long userId, String newThumbnailKey, List<String> newAttachmentKeys) {
        Set<String> deleteAfterCommit = new HashSet<>();

        String finalThumbPrefix = "portfolio/user-" + userId + "/portfolio-" + project.getPortfolioId() + "/thumbnail";
        String finalAssetPrefix = "portfolio/user-" + userId + "/portfolio-" + project.getPortfolioId() + "/assets";

        // thumbnail 교체(요청이 있고, 기존과 다를 때만)
        if (DEFAULT_THUMB.equals(newThumbnailKey)) {
            String old = project.getThumbnailUrl();
            if (hasText(old) && !DEFAULT_THUMB.equals(old)) deleteAfterCommit.add(old);
            project.updateThumbnail(DEFAULT_THUMB);
        } else if (hasText(newThumbnailKey) && !Objects.equals(newThumbnailKey, project.getThumbnailUrl())) {
            ensureThumbnailIsImage(newThumbnailKey);

            String old = project.getThumbnailUrl();
            if (hasText(old) && !DEFAULT_THUMB.equals(old)) {
                deleteAfterCommit.add(old);
            }

            String finalKey = consume(userId, project.getPortfolioId(), newThumbnailKey, finalThumbPrefix);
            project.updateThumbnail(finalKey);
        }

        // assets 교체(요청이 들어온 경우만)
        if (newAttachmentKeys != null) {
            Map<String, PortfolioAsset> currentByKey = project.getAssets().stream()
                    .filter(a -> hasText(a.getFileKey()))
                    .collect(Collectors.toMap(PortfolioAsset::getFileKey, a -> a, (a, b) -> a));

            // thumbKey는 제거됨(중복 consume 방지)
            LinkedHashSet<String> reqKeys = distinctKeys(newAttachmentKeys, newThumbnailKey);

            Set<String> keepKeys = new HashSet<>();
            int order = 1;

            for (String k : reqKeys) {
                PortfolioAsset existing = currentByKey.get(k);
                if (existing != null) {
                    existing.updateSortOrder(order++);
                    keepKeys.add(existing.getFileKey());
                    continue;
                }

                String finalKey = consume(userId, project.getPortfolioId(), k, finalAssetPrefix);

                UploadTicket t = ticketRepo.findByStorageKey(finalKey)
                        .orElseThrow(() -> new CustomException(UserErrorCode.PORTFOLIO_NOT_FOUND));

                project.getAssets().add(PortfolioAsset.builder()
                        .portfolioProject(project)
                        .type(t.getContentType())
                        .fileKey(finalKey)
                        .sortOrder(order++)
                        .createdAt(LocalDateTime.now())
                        .build());

                keepKeys.add(finalKey);
            }

            // 제거될 기존 assets 삭제 예약
            for (String oldKey : currentByKey.keySet()) {
                if (!keepKeys.contains(oldKey)) deleteAfterCommit.add(oldKey);
            }

            // 컬렉션에서 제거(고아삭제)
            project.getAssets().removeIf(a -> {
                String k = a.getFileKey();
                return hasText(k) && !keepKeys.contains(k);
            });
        }

        if (!deleteAfterCommit.isEmpty()) {
            globalPresignMethods.deleteAfterCommit(deleteAfterCommit);
        }
    }

    @Transactional
    public void deleteAllFilesAfterCommit(PortfolioProject project) {
        Set<String> deleteAfterCommit = new HashSet<>();

        String thumb = project.getThumbnailUrl();
        if (hasText(thumb) && !DEFAULT_THUMB.equals(thumb)) deleteAfterCommit.add(thumb);

        for (PortfolioAsset a : project.getAssets()) {
            if (hasText(a.getFileKey())) deleteAfterCommit.add(a.getFileKey());
        }

        globalPresignMethods.deleteAfterCommit(deleteAfterCommit);
    }

    private String consume(Long userId, Long portfolioId, String keyFromClient, String finalPrefix) {
        return presignEngine.consume(
                userId,
                UploadPurpose.PORTFOLIO_ATTACHMENT,
                UploadRefType.PORTFOLIO,
                portfolioId,
                keyFromClient,
                finalPrefix
        );
    }

    private void validateOwner(Long userId, Long portfolioUserId) {
        if (userId == null || !Objects.equals(userId, portfolioUserId)) {
            throw new CustomException(UserErrorCode.PORTFOLIO_FORBIDDEN);
        }
    }

    private LinkedHashSet<String> distinctKeys(List<String> keys, String thumbnailKey) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (keys == null) return out;

        for (String k : keys) {
            if (!hasText(k)) continue;
            if (hasText(thumbnailKey) && Objects.equals(k, thumbnailKey)) continue; //thumbKey 중복 제거
            out.add(k);
        }
        return out;
    }

    private void ensureThumbnailIsImage(String key) {
        String ct = ticketRepo.findByStorageKey(key)
                .map(UploadTicket::getContentType)
                .map(globalPresignMethods::normalize)
                .orElse(null);

        if (StringUtils.hasText(ct)) {
            if (!THUMB_ALLOWED.contains(ct)) throw new CustomException(StorageErrorCode.UNSUPPORTED_CONTENT_TYPE);
            return;
        }

        // fallback: 확장자
        String k = key.toLowerCase(Locale.ROOT);
        boolean ok = k.endsWith(".jpg") || k.endsWith(".jpeg") || k.endsWith(".png") || k.endsWith(".webp");
        if (!ok) throw new CustomException(StorageErrorCode.UNSUPPORTED_CONTENT_TYPE);
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}