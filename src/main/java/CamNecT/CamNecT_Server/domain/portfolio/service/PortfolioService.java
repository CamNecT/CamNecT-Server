package CamNecT.CamNecT_Server.domain.portfolio.service;

import CamNecT.CamNecT_Server.domain.portfolio.dto.PortfolioProjectDto;
import CamNecT.CamNecT_Server.domain.portfolio.dto.request.PortfolioRequest;
import CamNecT.CamNecT_Server.domain.portfolio.dto.response.PortfolioAssetView;
import CamNecT.CamNecT_Server.domain.portfolio.dto.response.PortfolioDetailResponse;
import CamNecT.CamNecT_Server.domain.portfolio.dto.response.PortfolioPreviewResponse;
import CamNecT.CamNecT_Server.domain.portfolio.dto.response.PortfolioResponse;
import CamNecT.CamNecT_Server.domain.portfolio.model.PortfolioAsset;
import CamNecT.CamNecT_Server.domain.portfolio.model.PortfolioProject;
import CamNecT.CamNecT_Server.domain.portfolio.repository.PortfolioAssetRepository;
import CamNecT.CamNecT_Server.domain.portfolio.repository.PortfolioRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.UserErrorCode;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioAssetRepository portfolioAssetRepository;

    private final PresignEngine presignEngine;
    private final UploadTicketRepository ticketRepo;
    private final FileStorage fileStorage;

    public PortfolioResponse<List<PortfolioPreviewResponse>> portfolioPreview(Long userId, Long portfolioUserId) {
        List<PortfolioPreviewResponse> rows = portfolioRepository.findPreviewsByUserId(userId);

        List<PortfolioPreviewResponse> resultList =  rows.stream()
                .map(r -> {
                    String key = r.thumbnailUrl(); // DB에 저장된 건 key
                    String url = presignOrNull(key, null, null);
                    return new PortfolioPreviewResponse(r.portfolioId(), r.title(), url, r.isPublic(), r.isFavorite());
                })
                .toList();

        return PortfolioResponse.of(userId.equals(portfolioUserId), resultList);
    }

    public PortfolioResponse<PortfolioDetailResponse> portfolioDetail(Long userId, Long portfolioUserId, Long portfolioId) {

        PortfolioProject portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new CustomException(UserErrorCode.PORTFOLIO_NOT_FOUND));

        boolean isMine = Objects.equals(userId, portfolio.getUserId());

        String thumbKey = portfolio.getThumbnailUrl();
        String thumbUrl = presignOrNull(thumbKey, "thumbnail", null);
        PortfolioProjectDto projectDto = PortfolioProjectDto.from(portfolio, thumbUrl);

        List<PortfolioAsset> assets = portfolioAssetRepository.findAssetsByPortfolioId(portfolioId);

        List<PortfolioAssetView> views = assets.stream()
                .map(a -> new PortfolioAssetView(
                        a.getAssetId(),
                        a.getType(),
                        a.getFileUrl(),
                        presignOrNull(a.getFileUrl(), "asset", a.getType()),
                        a.getSortOrder(),
                        a.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return PortfolioResponse.of(userId.equals(portfolioUserId), new PortfolioDetailResponse(isMine, projectDto, views));
    }

    @Transactional
    public PortfolioPreviewResponse create(Long userId, Long portfolioUserId, PortfolioRequest request) {

        if(!userId.equals(portfolioUserId))
            throw new CustomException(UserErrorCode.PORTFOLIO_FORBIDDEN);

        //요청에 따라 PortfolioProject 생성
        PortfolioProject project = PortfolioProject.builder()
                .userId(userId)
                .title(request.projectTitle())
                .description(request.description())
                .thumbnailUrl("기본이미지")
                .startDate(request.startedAt())
                .endDate(request.endedAt())
                .review(request.review())
                .isPublic(true)
                .createdAt(LocalDate.now())
                .updatedAt(LocalDate.now())
                .build();

        PortfolioProject saved = portfolioRepository.save(project);

        String finalThumbPrefix = "portfolio/user-" + userId + "/portfolio-" + saved.getPortfolioId() + "/thumbnail";
        String finalAssetPrefix = "portfolio/user-" + userId + "/portfolio-" + saved.getPortfolioId() + "/assets";

        // thumbnail
        if (StringUtils.hasText(request.thumbnailKey())) {
            String finalKey = presignEngine.consume(
                    userId,
                    UploadPurpose.PORTFOLIO_ATTACHMENT,
                    UploadRefType.PORTFOLIO,
                    saved.getPortfolioId(),
                    request.thumbnailKey(),
                    finalThumbPrefix
            );
            saved.updateThumbnail(finalKey);
        }

        // attachments
        List<String> keys = (request.attachmentKeys() == null) ? List.of() : request.attachmentKeys();
        int order = 1;

        for (String tempKey : keys) {
            if (!StringUtils.hasText(tempKey)) continue;

            String finalKey = presignEngine.consume(
                    userId,
                    UploadPurpose.PORTFOLIO_ATTACHMENT,
                    UploadRefType.PORTFOLIO,
                    saved.getPortfolioId(),
                    tempKey,
                    finalAssetPrefix
            );

            UploadTicket t = ticketRepo.findByStorageKey(finalKey)
                    .orElseThrow(() -> new CustomException(UserErrorCode.PORTFOLIO_NOT_FOUND));

            saved.getAssets().add(PortfolioAsset.builder()
                    .portfolioProject(saved)
                    .type(t.getContentType())
                    .fileUrl(finalKey)
                    .sortOrder(order++)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        return new PortfolioPreviewResponse(
                saved.getPortfolioId(),
                saved.getTitle(),
                presignOrNull(saved.getThumbnailUrl(), "thumbnail", null),
                saved.isPublic(),
                saved.isFavorite()
        );
    }

    @Transactional
    public PortfolioPreviewResponse update(Long userId, Long portfolioId, PortfolioRequest request) {
        PortfolioProject project = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new CustomException(UserErrorCode.PORTFOLIO_NOT_FOUND));

        // 권한 체크
        if (!project.getUserId().equals(userId)) {
            throw new CustomException(UserErrorCode.PORTFOLIO_FORBIDDEN);
        }

        String finalThumbPrefix = "portfolio/user-" + userId + "/portfolio-" + project.getPortfolioId() + "/thumbnail";
        String finalAssetPrefix = "portfolio/user-" + userId + "/portfolio-" + project.getPortfolioId() + "/assets";


        Set<String> deleteAfterCommit = new HashSet<>();

        // thumbnail 교체
        if (StringUtils.hasText(request.thumbnailKey())
                && !Objects.equals(request.thumbnailKey(), project.getThumbnailUrl())) {

            if (StringUtils.hasText(project.getThumbnailUrl()) && !"기본이미지".equals(project.getThumbnailUrl())) {
                deleteAfterCommit.add(project.getThumbnailUrl());
            }

            String finalKey = presignEngine.consume(
                    userId,
                    UploadPurpose.PORTFOLIO_ATTACHMENT,
                    UploadRefType.PORTFOLIO,
                    project.getPortfolioId(),
                    request.thumbnailKey(),
                    finalThumbPrefix
            );
            project.updateThumbnail(finalKey);
        }

        project.updateInfo(
                request.projectTitle(),
                request.description(),
                request.review(),
                request.startedAt(),
                request.endedAt()
        );

        // attachments 교체(요청이 들어온 경우만)
        if (request.attachmentKeys() != null) {
            Map<String, PortfolioAsset> currentByKey = project.getAssets().stream()
                    .filter(a -> StringUtils.hasText(a.getFileUrl()))
                    .collect(Collectors.toMap(PortfolioAsset::getFileUrl, a -> a));

            Set<String> keepKeys = new HashSet<>();

            LinkedHashSet<String> reqKeys = new LinkedHashSet<>();
            for (String k : request.attachmentKeys()) {
                if (StringUtils.hasText(k)) reqKeys.add(k);
            }

            int order = 1;
            for (String k : reqKeys) {
                PortfolioAsset existing = currentByKey.get(k);
                if (existing != null) {
                    existing.updateSortOrder(order++);
                    keepKeys.add(k);
                    continue;
                }

                String finalKey = presignEngine.consume(
                        userId,
                        UploadPurpose.PORTFOLIO_ATTACHMENT,
                        UploadRefType.PORTFOLIO,
                        project.getPortfolioId(),
                        k,
                        finalAssetPrefix
                );

                UploadTicket t = ticketRepo.findByStorageKey(finalKey)
                        .orElseThrow(() -> new CustomException(UserErrorCode.PORTFOLIO_NOT_FOUND));

                project.getAssets().add(PortfolioAsset.builder()
                        .portfolioProject(project)
                        .type(t.getContentType())
                        .fileUrl(finalKey)
                        .sortOrder(order++)
                        .createdAt(LocalDateTime.now())
                        .build());

                keepKeys.add(finalKey);
            }
            for (String oldKey : currentByKey.keySet()) {
                if (!keepKeys.contains(oldKey)) deleteAfterCommit.add(oldKey);
            }

            project.getAssets().removeIf(a -> {
                String k = a.getFileUrl();
                return StringUtils.hasText(k) && !keepKeys.contains(k);
            });

        }
        registerAfterCommitDelete(deleteAfterCommit);

        return new PortfolioPreviewResponse(
                project.getPortfolioId(),
                project.getTitle(),
                presignOrNull(project.getThumbnailUrl(), "thumbnail", null),
                project.isPublic(),
                project.isFavorite()
        );
    }

    @Transactional
    public boolean togglePublic(Long userId, Long portfolioId) {
        PortfolioProject project = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new CustomException(UserErrorCode.PORTFOLIO_NOT_FOUND)); // 포트폴리오를 찾을 수 없습니다.

        validateOwnership(userId, project);

        project.setPublic(!project.isPublic());
        return project.isPublic();
    }

    @Transactional
    public boolean toggleFavorite(Long userId, Long portfolioId) {
        PortfolioProject project = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new CustomException(UserErrorCode.PORTFOLIO_NOT_FOUND)); // 포트폴리오를 찾을 수 없습니다.

        validateOwnership(userId, project);

        project.setFavorite(!project.isFavorite());
        return project.isFavorite();
    }

    private void validateOwnership(Long userId, PortfolioProject project) {
        if (!project.getUserId().equals(userId)) {
            throw new CustomException(UserErrorCode.PORTFOLIO_FORBIDDEN); // 수정 권한이 없습니다.
        }
    }

    public void delete(Long userId, Long portfolioId) {
        PortfolioProject project = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new CustomException(UserErrorCode.PORTFOLIO_NOT_FOUND));

        if (!project.getUserId().equals(userId)) {
            throw new CustomException(UserErrorCode.PORTFOLIO_FORBIDDEN);
        }

        Set<String> deleteAfterCommit = new HashSet<>();

        if (StringUtils.hasText(project.getThumbnailUrl()) && !"기본이미지".equals(project.getThumbnailUrl())) {
            deleteAfterCommit.add(project.getThumbnailUrl());
        }

        project.getAssets().forEach(a -> {
            if (StringUtils.hasText(a.getFileUrl())) deleteAfterCommit.add(a.getFileUrl());
        });

        portfolioRepository.delete(project);

        registerAfterCommitDelete(deleteAfterCommit);
    }

    private String presignOrNull(String key, String filename, String contentType) {
        if (!StringUtils.hasText(key) || "기본이미지".equals(key)) return null;
        try {
            return presignEngine.presignDownload(key, filename, contentType).downloadUrl();
        } catch (Exception e) {
            return null;
        }
    }

    private void registerAfterCommitDelete(Set<String> keys) {
        if (keys == null || keys.isEmpty()) return;

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (String key : keys) {
                        try {
                            fileStorage.delete(key);
                        } catch (Exception ignored) {
                        }
                    }
                }
            });
        } else {
            for (String key : keys) {
                try {
                    fileStorage.delete(key);
                } catch (Exception ignored) {
                }
            }
        }
    }

}
