package CamNecT.CamNecT_Server.domain.activity.service;

import CamNecT.CamNecT_Server.domain.activity.dto.request.RecruitmentApplyRequest;
import CamNecT.CamNecT_Server.domain.activity.dto.request.RecruitmentRequest;
import CamNecT.CamNecT_Server.domain.activity.dto.response.RecruitmentDetailResponse;
import CamNecT.CamNecT_Server.domain.activity.model.recruitment.RecruitmentBookmark;
import CamNecT.CamNecT_Server.domain.activity.model.recruitment.TeamApplication;
import CamNecT.CamNecT_Server.domain.activity.model.recruitment.TeamRecruitment;
import CamNecT.CamNecT_Server.domain.activity.repository.external_activity.ExternalActivityRepository;
import CamNecT.CamNecT_Server.domain.activity.repository.recruitment.RecruitmentBookmarkRepository;
import CamNecT.CamNecT_Server.domain.activity.repository.recruitment.TeamApplicationRepository;
import CamNecT.CamNecT_Server.domain.activity.repository.recruitment.TeamRecruitmentRepository;
import CamNecT.CamNecT_Server.domain.users.model.UserProfile;
import CamNecT.CamNecT_Server.domain.users.repository.UserProfileRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.ErrorCode;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.ActivityErrorCode;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.UserErrorCode;
import CamNecT.CamNecT_Server.domain.profile.components.majors.model.Majors;
import CamNecT.CamNecT_Server.domain.profile.components.majors.repository.MajorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentService {

    private final TeamRecruitmentRepository recruitmentRepository;
    private final ExternalActivityRepository activityRepository;
    private final UserProfileRepository userProfileRepository;
    private final MajorRepository majorRepository;
    private final RecruitmentBookmarkRepository bookmarkRepository;
    private final TeamApplicationRepository teamApplicationRepository;

    @Transactional
    public TeamRecruitment createRecruitment(Long userId, Long activityId, RecruitmentRequest request) {

        //대외활동 검증
        if (!activityRepository.existsById(activityId)) {
            throw new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND);
        }

        TeamRecruitment recruitment = TeamRecruitment.builder()
                .activityId(activityId)
                .userId(userId)
                .title(request.title())
                .content(request.content())
                .recruitCount(request.recruitCount())
                .recruitDeadline(request.recruitDeadline())
                .createdAt(LocalDateTime.now())
                .build();

        return recruitmentRepository.save(recruitment);
    }

    public RecruitmentDetailResponse getRecruitmentDetail(Long currentUserId, Long recruitmentId) {

        //모집글 조회
        TeamRecruitment recruitment = recruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new CustomException(ActivityErrorCode.RECRUITMENT_NOT_FOUND));
        //작성자 프로필 조회
        UserProfile authorProfile = userProfileRepository.findById(recruitment.getUserId())
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_PROFILE_NOT_FOUND));
        //전공 이름 조회
        Majors major = majorRepository.findById(authorProfile.getMajorId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND)); // 에러코드 수정 필요 : 전공 정보를 찾을 수 없습니다.

        //북마크 여부 및 본인 글 여부 확인
        boolean isBookmarked = bookmarkRepository.existsByUserIdAndRecruitId(currentUserId, recruitmentId);
        boolean isMine = recruitment.getUserId().equals(currentUserId);

        return new RecruitmentDetailResponse(
                recruitment.getUserId(),
                major.getMajorNameKor(),
                authorProfile.getYearLevel(),
                recruitment,
                isMine,
                isBookmarked
        );
    }

    public boolean toggleRecruitmentBookmark(Long userId, Long recruitId) {
        //모집글 확인
        if (!recruitmentRepository.existsById(recruitId)) {
            throw new CustomException(ActivityErrorCode.RECRUITMENT_NOT_FOUND);
        }

        //북마크 존재 여부 확인
        Optional<RecruitmentBookmark> bookmarkOpt = bookmarkRepository.findByUserIdAndRecruitId(userId, recruitId);

        if (bookmarkOpt.isPresent()) {
            // 이미 존재하면 삭제 (북마크 취소)
            bookmarkRepository.delete(bookmarkOpt.get());
            return false; // 북마크 해제됨을 의미
        } else {
            // 존재하지 않으면 생성 (북마크 등록)
            RecruitmentBookmark newBookmark = RecruitmentBookmark.builder()
                    .userId(userId)
                    .recruitId(recruitId)
                    .build();
            bookmarkRepository.save(newBookmark);
            return true;
        }
    }

    public Long applyToTeam(Long userId, Long recruitId, RecruitmentApplyRequest request) {

        //공고 존재 여부 확인
        TeamRecruitment recruitment = recruitmentRepository.findById(recruitId)
                .orElseThrow(() -> new CustomException(ActivityErrorCode.RECRUITMENT_NOT_FOUND));

        //본인이 작성한 글인지 확인 (본인 글에는 신청 불가)
        if (recruitment.getUserId().equals(userId)) {
            throw new CustomException(ActivityErrorCode.SELF_APPLY_NOT_ALLOWED);
        }

        //중복 신청 확인
        if (teamApplicationRepository.existsByRecruitIdAndUserId(recruitId, userId)) {
            throw new CustomException(ActivityErrorCode.ALREADY_APPLIED);
        }

        //신청 객체 생성 및 저장
        TeamApplication application = TeamApplication.builder()
                .recruitId(recruitId)
                .userId(userId)
                .content(request.content())
                .build();

        return teamApplicationRepository.save(application).getApplicationId();
    }

}