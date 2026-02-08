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
import CamNecT.CamNecT_Server.global.storage.model.UploadTicket;
import CamNecT.CamNecT_Server.global.storage.repository.UploadTicketRepository;
import CamNecT.CamNecT_Server.global.storage.service.PresignEngine;
import CamNecT.CamNecT_Server.global.storage.service.PublicUrlIssuer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioService {
    private static final String DEFAULT_THUMB = "기본이미지";

    private final PortfolioRepository portfolioRepository;
    private final PortfolioAssetRepository portfolioAssetRepository;
    private final UploadTicketRepository uploadTicketRepository;

    private final PresignEngine presignEngine;
    private final PublicUrlIssuer publicUrlIssuer;
    private final PortfolioAttachmentService portfolioAttachmentService;

    public PortfolioResponse<List<PortfolioPreviewResponse>> portfolioPreview(Long userId, Long portfolioUserId) {
        List<PortfolioPreviewResponse> rows = portfolioRepository.findPreviewsByUserId(portfolioUserId);

        List<PortfolioPreviewResponse> resultList =  rows.stream()
                .map(r -> {
                    String key = r.thumbnailUrl(); // DB에 저장된 건 key
                    String url = cdnOrNull(key);
                    return new PortfolioPreviewResponse(r.portfolioId(), r.title(), url, r.isPublic(), r.isFavorite());
                })
                .toList();

        return PortfolioResponse.of(userId.equals(portfolioUserId), resultList);
    }

    public PortfolioResponse<PortfolioDetailResponse> portfolioDetail(Long userId, Long portfolioUserId, Long portfolioId) {

        PortfolioProject portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new CustomException(UserErrorCode.PORTFOLIO_NOT_FOUND));

        if (!Objects.equals(portfolio.getUserId(), portfolioUserId)) throw new CustomException(UserErrorCode.PORTFOLIO_NOT_FOUND);

        boolean isMine = Objects.equals(userId, portfolio.getUserId());

        String thumbUrl = cdnOrNull(portfolio.getThumbnailUrl());
        PortfolioProjectDto projectDto = PortfolioProjectDto.from(portfolio, thumbUrl);

        List<PortfolioAsset> assets = portfolioAssetRepository.findAssetsByPortfolioId(portfolioId);

        List<String> keys = assets.stream()
                .map(PortfolioAsset::getFileKey)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        Map<String, UploadTicket> ticketMap = uploadTicketRepository.findAllByStorageKeyIn(keys).stream()
                .collect(Collectors.toMap(UploadTicket::getStorageKey, t -> t));

        Map<String, String> urlMap = new HashMap<>();
        for (String key : keys) {
            UploadTicket t = ticketMap.get(key);
            String filename = (t == null) ? null : t.getOriginalFilename();
            String contentType = (t == null) ? null : t.getContentType();

            urlMap.put(key, presignEngine.presignDownload(key, filename, contentType).downloadUrl());
        }

        List<PortfolioAssetView> views = assets.stream()
                .map(a -> new PortfolioAssetView(
                        a.getAssetId(),
                        a.getType(),
                        a.getFileKey(),
                        urlMap.get(a.getFileKey()),
                        a.getSortOrder(),
                        a.getCreatedAt()
                ))
                .toList();

        return PortfolioResponse.of(isMine, new PortfolioDetailResponse(isMine, projectDto, views));
    }

    @Transactional
    public PortfolioPreviewResponse create(Long userId, Long portfolioUserId, PortfolioRequest request) {

        if (!Objects.equals(userId, portfolioUserId)) throw new CustomException(UserErrorCode.PORTFOLIO_FORBIDDEN);

        //요청에 따라 PortfolioProject 생성
        PortfolioProject project = PortfolioProject.builder()
                .userId(userId)
                .title(request.projectTitle())
                .description(request.description())
                .thumbnailUrl(DEFAULT_THUMB)
                .startDate(request.startedAt())
                .endDate(request.endedAt())
                .review(request.review())
                .isPublic(true)
                .createdAt(LocalDate.now())
                .updatedAt(LocalDate.now())
                .build();

        PortfolioProject saved = portfolioRepository.save(project);

        portfolioAttachmentService.applyOnCreate(
                saved,
                userId,
                request.thumbnailKey(),
                request.attachmentKeys()
        );

        return new PortfolioPreviewResponse(
                saved.getPortfolioId(),
                saved.getTitle(),
                cdnOrNull(saved.getThumbnailUrl()), // CDN
                saved.isPublic(),
                saved.isFavorite()
        );
    }

    @Transactional
    public PortfolioPreviewResponse update(Long userId, Long portfolioId, PortfolioRequest request) {
        PortfolioProject project = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new CustomException(UserErrorCode.PORTFOLIO_NOT_FOUND));

        validateOwnership(userId, project);

        project.updateInfo(
                request.projectTitle(),
                request.description(),
                request.review(),
                request.startedAt(),
                request.endedAt()
        );

        // consume/정렬/삭제예약은 AttachmentService로 위임
        portfolioAttachmentService.applyOnUpdate(
                project,
                userId,
                request.thumbnailKey(),
                request.attachmentKeys()
        );

        return new PortfolioPreviewResponse(
                project.getPortfolioId(),
                project.getTitle(),
                cdnOrNull(project.getThumbnailUrl()), // CDN
                project.isPublic(),
                project.isFavorite()
        );
    }

    @Transactional
    public void delete(Long userId, Long portfolioId) {
        PortfolioProject project = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new CustomException(UserErrorCode.PORTFOLIO_NOT_FOUND));

        validateOwnership(userId, project);

        portfolioAttachmentService.deleteAllFilesAfterCommit(project);

        portfolioRepository.delete(project);
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

    private String cdnOrNull(String key) {
        if (!StringUtils.hasText(key) || DEFAULT_THUMB.equals(key)) return null;
        try {
            return publicUrlIssuer.issuePublicUrl(key);
        } catch (Exception e) {
            return null;
        }
    }
}
