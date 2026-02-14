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
import CamNecT.CamNecT_Server.domain.users.model.UserRole;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.UserErrorCode;
import CamNecT.CamNecT_Server.global.storage.model.UploadTicket;
import CamNecT.CamNecT_Server.global.storage.repository.UploadTicketRepository;
import CamNecT.CamNecT_Server.global.storage.service.PresignEngine;
import CamNecT.CamNecT_Server.global.storage.service.PublicUrlIssuer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {
    private static final String DEFAULT_THUMB = "기본이미지";

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioAssetRepository portfolioAssetRepository;
    private final UploadTicketRepository uploadTicketRepository;

    private final PresignEngine presignEngine;
    private final PublicUrlIssuer publicUrlIssuer;
    private final PortfolioAttachmentService portfolioAttachmentService;

    public PortfolioResponse<List<PortfolioPreviewResponse>> portfolioPreview(Long userId, Long portfolioUserId) {
        boolean isMine = Objects.equals(userId, portfolioUserId);

        List<PortfolioPreviewResponse> rows = isMine
                ? portfolioRepository.findPreviewsByUserId(portfolioUserId)
                : portfolioRepository.findPublicPreviewsByUserId(portfolioUserId);

        List<PortfolioPreviewResponse> resultList =  rows.stream()
                .map(r -> new PortfolioPreviewResponse(
                        r.portfolioId(),
                        r.title(),
                        cdnOrNull(r.thumbnailUrl()),
                        r.isPublic(),
                        r.isFavorite()
                ))
                .toList();

        return PortfolioResponse.of(isMine, resultList);
    }

    public PortfolioResponse<PortfolioDetailResponse> portfolioDetail(Long userId, Long portfolioUserId, Long portfolioId) {
        PortfolioProject project = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new CustomException(UserErrorCode.PORTFOLIO_NOT_FOUND));

        assertPathUserMatchesOwner(portfolioUserId, project);

        boolean isMine = Objects.equals(userId, project.getUserId());
        if (!isMine && !project.isPublic()) {
            throw new CustomException(UserErrorCode.PORTFOLIO_FORBIDDEN);
        }

        String thumbUrl = cdnOrNull(project.getThumbnailUrl());
        PortfolioProjectDto projectDto = PortfolioProjectDto.from(project, thumbUrl);

        List<PortfolioAsset> assets = portfolioAssetRepository.findAssetsByPortfolioId(portfolioId);

        List<String> keys = assets.stream()
                .map(PortfolioAsset::getFileKey)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        Map<String, UploadTicket> ticketMap = keys.isEmpty()
                ? Map.of()
                : uploadTicketRepository.findAllByStorageKeyIn(keys).stream()
                .collect(Collectors.toMap(UploadTicket::getStorageKey, t -> t));

        Map<String, String> urlMap = new HashMap<>();
        for (String key : keys) {
            UploadTicket t = ticketMap.get(key);
            String filename = (t == null) ? null : t.getOriginalFilename();
            String contentType = (t == null) ? null : t.getContentType();

            try {
                String url = presignEngine.presignDownload(key, filename, contentType).downloadUrl();
                urlMap.put(key, url);
            } catch (Exception e) {
                log.warn("presignDownload failed. portfolioId={}, key={}", portfolioId, key, e);
                urlMap.put(key, null);
            }
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

        return PortfolioResponse.of(isMine, new PortfolioDetailResponse(projectDto, views));
    }

    @Transactional
    public PortfolioPreviewResponse create(Long userId, Long portfolioUserId, PortfolioRequest request) {

        if (!Objects.equals(userId, portfolioUserId)) throw new CustomException(UserErrorCode.PORTFOLIO_FORBIDDEN);

        // 1. 엔티티 생성 시 누락된 필드(assignedRole, techStack) 추가
        PortfolioProject project = PortfolioProject.builder()
                .userId(userId)
                .title(request.projectTitle())
                .description(request.description())
                .thumbnailUrl(DEFAULT_THUMB)
                .startDate(request.startedAt())
                .endDate(request.endedAt())
                .review(request.review())
                .assignedRole(List.of(request.project_role())) // String을 List로 변환하여 저장
                .techStack(request.techStack()) // 추가
                .isPublic(true)
                .createdAt(LocalDate.now())
                .updatedAt(LocalDate.now())
                .build();

        PortfolioProject saved = portfolioRepository.save(project);

        // 3. S3 관련 키가 존재하는 경우에만 로직 수행
        // thumbnailKey나 attachmentKeys가 둘 다 없으면 skip
        boolean hasThumbnail = StringUtils.hasText(request.thumbnailKey());
        boolean hasAttachments = request.attachmentKeys() != null && !request.attachmentKeys().isEmpty();

        if (hasThumbnail || hasAttachments) {
            portfolioAttachmentService.applyOnCreate(
                    saved,
                    userId,
                    request.thumbnailKey(),
                    request.attachmentKeys()
            );
        }

        return new PortfolioPreviewResponse(
                saved.getPortfolioId(),
                saved.getTitle(),
                cdnOrNull(saved.getThumbnailUrl()),
                saved.isPublic(),
                saved.isFavorite()
        );
    }

    @Transactional
    public PortfolioPreviewResponse update(Long userId, Long portfolioUserId, Long portfolioId, PortfolioRequest request) {
        PortfolioProject project = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new CustomException(UserErrorCode.PORTFOLIO_NOT_FOUND));

        // 권한 체크
        assertPathUserMatchesOwner(portfolioUserId, project);
        assertOwner(userId, project);

        // 1. 일반 정보 업데이트 (프로젝트 역할 및 기술 스택 포함)
        project.updateInfo(
                request.projectTitle(),
                request.description(),
                request.review(),
                request.startedAt(),
                request.endedAt(),
                request.project_role(), // 추가
                request.techStack()     // 추가
        );

        // 2. S3 첨부파일/썸네일 업데이트 로직 (Null 및 빈 값 방어)
        // 수정 시에는 기존 파일을 유지할지, 새로 교체할지 판단이 필요하므로
        // 최소한의 데이터가 있을 때만 요청을 보냅니다.
        boolean hasThumbnail = StringUtils.hasText(request.thumbnailKey());
        boolean hasAttachments = request.attachmentKeys() != null && !request.attachmentKeys().isEmpty();

        // 서비스 기획에 따라 '수정 시 아무것도 안 보내면 기존 유지'라면 아래 처리가 맞습니다.
        if (hasThumbnail || hasAttachments) {
            portfolioAttachmentService.applyOnUpdate(
                    project,
                    userId,
                    request.thumbnailKey(),
                    request.attachmentKeys()
            );
        }

        return new PortfolioPreviewResponse(
                project.getPortfolioId(),
                project.getTitle(),
                cdnOrNull(project.getThumbnailUrl()),
                project.isPublic(),
                project.isFavorite()
        );
    }

    @Transactional
    public void delete(Long userId, Long portfolioUserId, Long portfolioId) {
        PortfolioProject project = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new CustomException(UserErrorCode.PORTFOLIO_NOT_FOUND));

        assertPathUserMatchesOwner(portfolioUserId, project);

        boolean isAdmin = userRepository.existsByUserIdAndRole(userId, UserRole.ADMIN);
        boolean isOwner = Objects.equals(project.getUserId(), userId);

        if (!isOwner && !isAdmin) throw new CustomException(UserErrorCode.PORTFOLIO_FORBIDDEN);

        portfolioAttachmentService.deleteAllFilesAfterCommit(project);
        portfolioRepository.delete(project);
    }

    @Transactional
    public boolean togglePublic(Long userId, Long portfolioUserId, Long portfolioId) {
        PortfolioProject project = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new CustomException(UserErrorCode.PORTFOLIO_NOT_FOUND));

        assertPathUserMatchesOwner(portfolioUserId, project);
        assertOwner(userId, project);

        project.setPublic(!project.isPublic());
        project.setUpdatedAt(LocalDate.now());
        return project.isPublic();
    }

    @Transactional
    public boolean toggleFavorite(Long userId, Long portfolioUserId, Long portfolioId) {
        PortfolioProject project = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new CustomException(UserErrorCode.PORTFOLIO_NOT_FOUND));

        assertPathUserMatchesOwner(portfolioUserId, project);
        assertOwner(userId, project);

        project.setFavorite(!project.isFavorite());
        project.setUpdatedAt(LocalDate.now());
        return project.isFavorite();
    }

    /* util
    */

    private void assertPathUserMatchesOwner(Long portfolioUserId, PortfolioProject project) {
        if (!Objects.equals(project.getUserId(), portfolioUserId)) throw new CustomException(UserErrorCode.PORTFOLIO_NOT_FOUND);
    }

    private void assertOwner(Long userId, PortfolioProject project) {
        if (!Objects.equals(project.getUserId(), userId)) throw new CustomException(UserErrorCode.PORTFOLIO_FORBIDDEN);
    }

    private String cdnOrNull(String key) {
        if (!StringUtils.hasText(key) || DEFAULT_THUMB.equals(key)) return null;
        try {
            return publicUrlIssuer.issuePublicUrl(key);
        } catch (Exception e) {
            log.warn("issuePublicUrl failed. key={}", key, e);
            return null;
        }
    }
}
