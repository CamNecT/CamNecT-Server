package CamNecT.CamNecT_Server.domain.activity.controller;

import CamNecT.CamNecT_Server.domain.activity.dto.request.RecruitmentApplyRequest;
import CamNecT.CamNecT_Server.domain.activity.dto.request.RecruitmentRequest;
import CamNecT.CamNecT_Server.domain.activity.dto.response.RecruitmentDetailResponse;
import CamNecT.CamNecT_Server.domain.activity.model.recruitment.TeamRecruitment;
import CamNecT.CamNecT_Server.domain.activity.service.RecruitmentService;
import CamNecT.CamNecT_Server.global.common.auth.UserId;
import CamNecT.CamNecT_Server.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Activity Recruitment", description = "대외활동 내 팀원 모집 및 지원 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/activity/recruitment")
public class RecruitmentController {

    private final RecruitmentService recruitmentService;

    @Operation(summary = "팀원 모집글 생성", description = "특정 대외활동(대외활동/취업공고)에 대한 팀원 모집글을 작성합니다.")
    @PostMapping
    public ApiResponse<TeamRecruitment> createRecruitment(
            @UserId Long userId,
            @RequestBody RecruitmentRequest request
    ) {
        return ApiResponse.success(recruitmentService.createRecruitment(userId, request));
    }

    @Operation(summary = "모집글 상세 조회", description = "특정 팀원 모집글의 상세 내용 및 팀 구성 정보를 조회합니다.")
    @GetMapping("/{recruitmentId}")
    public ApiResponse<RecruitmentDetailResponse> getRecruitmentDetail(
            @UserId Long userId,
            @PathVariable Long recruitmentId
    ) {
        return ApiResponse.success(recruitmentService.getRecruitmentDetail(userId, recruitmentId));
    }

    @Operation(summary = "모집글 북마크 설정 (토글 방식)", description = "팀원 모집글에 대한 북마크 상태를 반전(Toggle)시킵니다. 호출 시마다 등록/해제 메시지를 반환합니다.")
    @PostMapping("/{recruitmentId}/bookmark")
    public ApiResponse<String> toggleBookmark(
            @UserId Long userId,
            @PathVariable Long recruitmentId
    ) {
        boolean isBookmarked = recruitmentService.toggleRecruitmentBookmark(userId, recruitmentId);
        String message = isBookmarked ? "북마크가 등록되었습니다." : "북마크가 해제되었습니다.";
        return ApiResponse.success(message);
    }

    @Operation(summary = "팀 지원하기", description = "모집 중인 팀에 지원 신청을 보냅니다.")
    @PostMapping("/{recruitmentId}/apply")
    public ApiResponse<Long> applyToTeam(
            @UserId Long userId,
            @PathVariable Long recruitmentId,
            @RequestBody RecruitmentApplyRequest request
    ) {
        Long applicationId = recruitmentService.applyToTeam(userId, recruitmentId, request);
        return ApiResponse.success(applicationId);
    }

}
