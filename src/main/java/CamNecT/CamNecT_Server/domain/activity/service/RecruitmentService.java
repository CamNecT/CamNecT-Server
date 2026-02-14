package CamNecT.CamNecT_Server.domain.activity.service;

import CamNecT.CamNecT_Server.domain.activity.dto.request.RecruitmentApplyRequest;
import CamNecT.CamNecT_Server.domain.activity.dto.request.RecruitmentRequest;
import CamNecT.CamNecT_Server.domain.activity.dto.response.RecruitmentDetailResponse;
import CamNecT.CamNecT_Server.domain.activity.model.enums.RecruitStatus;
import CamNecT.CamNecT_Server.domain.activity.model.recruitment.RecruitmentBookmark;
import CamNecT.CamNecT_Server.domain.activity.model.recruitment.TeamApplication;
import CamNecT.CamNecT_Server.domain.activity.model.recruitment.TeamRecruitment;
import CamNecT.CamNecT_Server.domain.activity.repository.external_activity.ExternalActivityRepository;
import CamNecT.CamNecT_Server.domain.activity.repository.recruitment.RecruitmentBookmarkRepository;
import CamNecT.CamNecT_Server.domain.activity.repository.recruitment.TeamApplicationRepository;
import CamNecT.CamNecT_Server.domain.activity.repository.recruitment.TeamRecruitmentRepository;
import CamNecT.CamNecT_Server.domain.chat.model.ChatRequest;
import CamNecT.CamNecT_Server.domain.chat.repository.ChatRequestRepository;
import CamNecT.CamNecT_Server.domain.profile.dto.ProfileGlobalDto;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.domain.users.repository.UserProfileRepository;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.ActivityErrorCode;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.AuthErrorCode;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.CoffeeChatErrorCode;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.UserErrorCode;
import CamNecT.CamNecT_Server.global.notification.event.SimpleNotifiableEvent;
import CamNecT.CamNecT_Server.global.notification.model.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentService {

    private final TeamRecruitmentRepository recruitmentRepository;
    private final ExternalActivityRepository activityRepository;
    private final UserProfileRepository userProfileRepository;
    private final RecruitmentBookmarkRepository bookmarkRepository;
    private final TeamApplicationRepository teamApplicationRepository;
    private final UserRepository userRepository;
    private final ChatRequestRepository chatRequestRepository;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TeamRecruitment createRecruitment(Long userId, RecruitmentRequest request) {

        //대외활동 검증
        if (!activityRepository.existsById(request.activityId())) {
            throw new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND);
        }

        TeamRecruitment recruitment = TeamRecruitment.builder()
                .activityId(request.activityId())
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
        ProfileGlobalDto profilePreview = userProfileRepository.findGlobalByUserId(recruitment.getUserId()).orElseThrow(
                ()-> new CustomException(UserErrorCode.USER_NOT_FOUND)
        );

        //북마크 여부 및 본인 글 여부 확인
        String activityTitle = activityRepository.findTitleByActivityId(recruitment.getActivityId()).orElseThrow(
                () -> new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND)
        );
        boolean isBookmarked = bookmarkRepository.existsByUserIdAndRecruitId(currentUserId, recruitmentId);
        boolean isMine = recruitment.getUserId().equals(currentUserId);

        return new RecruitmentDetailResponse(
                profilePreview,
                recruitment,
                activityTitle,
                isMine,
                isBookmarked
        );
    }

    @Transactional
    public void updateRecruitment(Long userId, Long recruitmentId, RecruitmentRequest request) {
        // 1. 모집글 조회
        TeamRecruitment recruitment = recruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new CustomException(ActivityErrorCode.RECRUITMENT_NOT_FOUND));

        // 2. 작성자 본인 확인
        if (!Objects.equals(recruitment.getUserId(), userId)) {
            throw new CustomException(ActivityErrorCode.NOT_AUTHOR);
        }

        // 3. 마감된 모집글은 수정 불가
        if (recruitment.getRecruitStatus() == RecruitStatus.CLOSED) {
            throw new CustomException(ActivityErrorCode.ALREADY_CLOSED);
        }

        // 4. 수정 적용
        recruitment.update(request);
    }

    @Transactional
    public boolean toggleRecruitmentBookmark(Long userId, Long recruitId) {
        //모집글 조회 (북마크 카운트 업데이트를 위해 엔티티 조회)
        TeamRecruitment recruitment = recruitmentRepository.findById(recruitId)
                .orElseThrow(() -> new CustomException(ActivityErrorCode.RECRUITMENT_NOT_FOUND));

        //북마크 존재 여부 확인
        Optional<RecruitmentBookmark> bookmarkOpt = bookmarkRepository.findByUserIdAndRecruitId(userId, recruitId);

        if (bookmarkOpt.isPresent()) {
            // 이미 존재하면 삭제 (북마크 취소)
            bookmarkRepository.delete(bookmarkOpt.get());
            recruitment.decrementBookmarkCount(); // 북마크 수 감소
            return false; // 북마크 해제됨을 의미
        } else {
            // 존재하지 않으면 생성 (북마크 등록)
            RecruitmentBookmark newBookmark = RecruitmentBookmark.builder()
                    .userId(userId)
                    .recruitId(recruitId)
                    .build();
            bookmarkRepository.save(newBookmark);
            recruitment.incrementBookmarkCount(); // 북마크 수 증가
            return true;
        }
    }

    @Transactional
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

        //요청 가능 상태인지 확인
        if (recruitment.getRecruitStatus() == RecruitStatus.CLOSED)
            throw new CustomException(ActivityErrorCode.RECRUITMENT_CLOSED);

        //신청 객체 생성 및 저장
        TeamApplication application = TeamApplication.builder()
                .recruitId(recruitId)
                .userId(userId)
                .content(request.content())
                .build();



        // 커피챗 요청 로직
        Users requester = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
        Users receiver = userRepository.findById(recruitment.getUserId())
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        if (requester.equals(receiver)) {
            throw new CustomException(CoffeeChatErrorCode.SELF_REQUEST_NOT_ALLOWED);
        }

        if (chatRequestRepository.existsByRequester_UserIdAndReceiver_UserIdAndStatusAndTypeAndRecruitmentId(
                userId, recruitment.getUserId(), ChatRequest.RequestStatus.WAITING, ChatRequest.RequestType.TEAM_RECRUIT, recruitId)) {
            throw new CustomException(CoffeeChatErrorCode.DUPLICATE_REQUEST);
        }

        if (chatRequestRepository.existsByRequester_UserIdAndReceiver_UserIdAndStatusAndTypeAndRecruitmentId(
                userId, recruitment.getUserId(), ChatRequest.RequestStatus.ACCEPTED, ChatRequest.RequestType.TEAM_RECRUIT, recruitId)
                || chatRequestRepository.existsByRequester_UserIdAndReceiver_UserIdAndStatusAndTypeAndRecruitmentId(
                recruitment.getUserId(), userId, ChatRequest.RequestStatus.ACCEPTED, ChatRequest.RequestType.TEAM_RECRUIT, recruitId)) {
            throw new CustomException(CoffeeChatErrorCode.CHATROOM_ALREADY_EXISTS);
        }

        ChatRequest chatRequest = ChatRequest.builder()
                .requester(requester)
                .receiver(receiver)
                .content(request.content())
                .type(ChatRequest.RequestType.TEAM_RECRUIT) //팀원 모집으로 타입 설정하기
                .activityId(recruitment.getActivityId())
                .recruitmentId(recruitId)
                .build();



        Long chatRequestId = chatRequestRepository.save(chatRequest).getId();

        // 모집글 작성자에게 알림
        eventPublisher.publishEvent(SimpleNotifiableEvent.of(
                recruitment.getUserId(),                 // receiver = 모집글 작성자
                userId,                                  // actor = 지원자
                NotificationType.TEAM_APPLICATION_RECEIVED,
                "팀원 모집에 지원이 도착했습니다.",
                null,
                null,
                chatRequestId,
                null
        ));


        return teamApplicationRepository.save(application).getApplicationId();
    }

    @Transactional
    public void closeRecruitment(Long userId, Long recruitId) {
        // 1. 모집글 조회
        TeamRecruitment recruitment = recruitmentRepository.findById(recruitId)
                .orElseThrow(() -> new CustomException(ActivityErrorCode.RECRUITMENT_NOT_FOUND));

        // 2. 작성자 본인 확인
        if (!Objects.equals(recruitment.getUserId(), userId)) {
            throw new CustomException(ActivityErrorCode.NOT_AUTHOR);
        }

        // 3. 이미 마감된 경우
        if (recruitment.getRecruitStatus() == RecruitStatus.CLOSED) {
            throw new CustomException(ActivityErrorCode.ALREADY_CLOSED);
        }

        // 4. 상태를 CLOSED로 변경 (더티 체킹으로 자동 업데이트)
        recruitment.close();

        // 5. 대기중인 TEAM_RECRUIT 요청들 전부 거절 처리 + 알림
        List<ChatRequest> targets = chatRequestRepository.findAllNonAcceptedTeamRecruitFetchRequester(
                ChatRequest.RequestType.TEAM_RECRUIT,
                recruitId,
                ChatRequest.RequestStatus.ACCEPTED
        );

        for (ChatRequest r : targets) if (r.getStatus() == ChatRequest.RequestStatus.WAITING) r.reject();
    }
}