package CamNecT.server.domain.preview;

import CamNecT.server.domain.activity.dto.response.ActivityPreviewResponse;
import CamNecT.server.domain.activity.model.enums.ActivityCategory;
import CamNecT.server.domain.activity.service.ActivityService;
import CamNecT.server.domain.community.model.enums.BoardCode;
import CamNecT.server.global.common.auth.PublicPreview;
import CamNecT.server.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "비로그인 미리보기")
@RestController
@RequestMapping("/api/preview")
@RequiredArgsConstructor
@Validated
public class GuestPreviewController {
    private final GuestPreviewRepository previews;
    private final ActivityService activities;

    @PublicPreview
    @GetMapping("/community/posts")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true)
    @Operation(summary = "공개 게시글 제목 및 반응 수")
    public ApiResponse<Slice<GuestPreviewRepository.PostSummary>> posts(@RequestParam(required = false) BoardCode board,
            @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ApiResponse.success(previews.posts(board, PageRequest.of(page, size)));
    }

    @PublicPreview
    @GetMapping("/recruitments")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true)
    @Operation(summary = "팀원 모집 제목 및 상태", description = "activityId를 전달하면 해당 활동의 팀원 모집만 반환합니다.")
    public ApiResponse<Slice<GuestPreviewRepository.RecruitmentSummary>> recruitments(@RequestParam(required = false) @Positive Long activityId,
            @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ApiResponse.success(previews.recruitments(activityId, PageRequest.of(page, size)));
    }

    @PublicPreview
    @GetMapping("/activities")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true)
    @Operation(summary = "공개 활동 목록")
    public ApiResponse<Slice<ActivityPreviewResponse>> activities(@RequestParam(required = false) ActivityCategory category,
            @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ApiResponse.success(activities.getActivities(null, category, null, null, "LATEST", PageRequest.of(page, size)));
    }

    @PublicPreview
    @GetMapping("/activities/{id}")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true)
    @Operation(summary = "공개 활동 상세")
    public ApiResponse<GuestActivityDetail> activity(@PathVariable @Positive Long id) {
        return ApiResponse.success(activities.getGuestActivityDetail(id));
    }

    public record AlumniCount(long count) {}

    @PublicPreview
    @GetMapping("/alumni/count")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true)
    @Operation(summary = "동문찾기에 표시 가능한 동문 수")
    public ApiResponse<AlumniCount> alumniCount() {
        return ApiResponse.success(new AlumniCount(previews.alumniCount()));
    }
}
