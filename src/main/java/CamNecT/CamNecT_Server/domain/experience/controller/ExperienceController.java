package CamNecT.CamNecT_Server.domain.experience.controller;

import CamNecT.CamNecT_Server.domain.experience.dto.request.ExperienceRequest;
import CamNecT.CamNecT_Server.domain.experience.dto.response.ExperienceResponse;
import CamNecT.CamNecT_Server.domain.experience.service.ExperienceService;
import CamNecT.CamNecT_Server.global.common.auth.UserId;
import CamNecT.CamNecT_Server.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Experience", description = "경력 정보 관련 API")
@RestController
@RequestMapping("/api/user/me/experiences")
@RequiredArgsConstructor
public class ExperienceController {

    private final ExperienceService experienceService;

    @Operation(
            summary = "내 경력 목록 조회",
            description = "현재 로그인한 사용자가 등록한 모든 경력 리스트를 조회합니다."
    )
    @GetMapping
    public ApiResponse<List<ExperienceResponse>> getMyExperiences(
            @UserId Long userId
    ) {
        List<ExperienceResponse> response = experienceService.getMyExperience(userId);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "경력 정보 추가",
            description = "새로운 경력을 추가합니다."
    )
    @PostMapping
    public ApiResponse<Void> addExperience(
            @UserId Long userId,
            @RequestBody @Valid ExperienceRequest request
    ) {
        experienceService.addExperience(userId, request);
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "경력 정보 수정",
            description = "기존에 등록된 특정 경력을 수정합니다."
    )
    @PatchMapping("/{experienceId}")
    public ApiResponse<Void> updateExperience(
            @UserId Long userId,
            @PathVariable Long experienceId,
            @RequestBody @Valid ExperienceRequest request
    ) {
        experienceService.updateExperience(userId, experienceId, request);
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "경력 정보 삭제",
            description = "등록된 경력 중 하나를 삭제합니다."
    )
    @DeleteMapping("/{experienceId}")
    public ApiResponse<Void> deleteExperience(
            @UserId Long userId,
            @PathVariable Long experienceId
    ) {
        experienceService.deleteExperience(userId, experienceId);
        return ApiResponse.success(null);
    }
}