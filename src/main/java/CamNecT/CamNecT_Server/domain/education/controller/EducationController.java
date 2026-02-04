package CamNecT.CamNecT_Server.domain.education.controller;

import CamNecT.CamNecT_Server.domain.education.dto.request.EducationRequest;
import CamNecT.CamNecT_Server.domain.education.dto.response.EducationResponse;
import CamNecT.CamNecT_Server.domain.education.service.EducationService;
import CamNecT.CamNecT_Server.global.common.auth.UserId;
import CamNecT.CamNecT_Server.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Education", description = "학력 정보 관련 API")
@RestController
@RequestMapping("/api/user/me/educations")
@RequiredArgsConstructor
public class EducationController {

    private final EducationService educationService;

    @Operation(
            summary = "내 학력 목록 조회",
            description = "현재 로그인한 사용자의 학력(학교, 전공, 상태...) 리스트를 조회합니다."
    )
    @GetMapping
    public ApiResponse<List<EducationResponse>> getMyEducations(
            @UserId Long userId
    ) {
        List<EducationResponse> response = educationService.getMyEducations(userId);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "학력 정보 추가",
            description = "새로운 학력 사항을 추가합니다."
    )
    @PostMapping
    public ApiResponse<Void> addEducation(
            @UserId Long userId,
            @RequestBody @Valid EducationRequest request
    ) {
        educationService.addEducation(userId, request);
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "학력 정보 수정",
            description = "기존에 등록된 특정 학력 정보를 수정합니다."
    )
    @PatchMapping("/{educationId}")
    public ApiResponse<Void> updateEducation(
            @UserId Long userId,
            @PathVariable Long educationId,
            @RequestBody @Valid EducationRequest request
    ) {
        educationService.updateEducation(userId, educationId, request);
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "학력 정보 삭제",
            description = "등록된 학력 정보 중 하나를 삭제합니다."
    )
    @DeleteMapping("/{educationId}")
    public ApiResponse<Void> deleteEducation(
            @UserId Long userId,
            @PathVariable Long educationId
    ) {
        educationService.deleteEducation(userId, educationId);
        return ApiResponse.success(null);
    }
}

