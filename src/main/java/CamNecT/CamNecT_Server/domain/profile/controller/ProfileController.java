package CamNecT.CamNecT_Server.domain.profile.controller;

import CamNecT.CamNecT_Server.domain.profile.dto.request.UpdateBioRequest;
import CamNecT.CamNecT_Server.domain.profile.dto.request.UpdateOnboardingRequest;
import CamNecT.CamNecT_Server.domain.profile.dto.request.UpdateProfileTagsRequest;
import CamNecT.CamNecT_Server.domain.profile.dto.response.ProfileStatusResponse;
import CamNecT.CamNecT_Server.domain.profile.dto.response.ProfileResponse;
import CamNecT.CamNecT_Server.domain.profile.service.ProfileService;
import CamNecT.CamNecT_Server.global.common.auth.UserId;
import CamNecT.CamNecT_Server.global.common.response.ApiResponse;
import CamNecT.CamNecT_Server.global.storage.dto.request.PresignUploadRequest;
import CamNecT.CamNecT_Server.global.storage.dto.response.PresignUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Profile", description = "프로필 관련 API")
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @Operation(
            summary = "유저 프로필 조회",
            description = "특정 사용자의 ID를 통해 프로필 정보를 조회합니다. (다른 사용자의 프로필 뿐만 아니라 마이페이지도 조회합니다.)"
    )
    @GetMapping("/{profileUserId}")
    public ApiResponse<ProfileResponse> getUserProfile(@UserId Long loginUserId,
                                                       @PathVariable Long profileUserId) {
        ProfileResponse response = profileService.getUserProfile(loginUserId, profileUserId);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "프로필 이미지 업로드 URL 생성",
            description = "프로필 이미지를 업로드하기 위한 S3 URL을 생성합니다."
    )
    @PostMapping("/uploads/presign")
    public ApiResponse<PresignUploadResponse> presignProfileImageUpload(
            @UserId Long userId,
            @RequestBody @Valid PresignUploadRequest req
    ) {
        PresignUploadResponse response = profileService.presignProfileImageUpload(userId, req);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "온보딩 정보 등록",
            description = "회원가입 후 초기 프로필 설정(온보딩) 정보를 저장합니다."
    )
    @PostMapping("/onboarding")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProfileStatusResponse> createOnboarding(
            @UserId Long userId,
            @RequestBody @Valid UpdateOnboardingRequest req
    ) {
        ProfileStatusResponse response = profileService.createOnboarding(userId, req);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "프로필 태그 수정",
            description = "사용자의 프로필 태그 정보를 업데이트합니다."
    )
    @PutMapping("/tags")
    public ApiResponse<ProfileStatusResponse> updateProfileTags(
            @UserId Long userId,
            @RequestBody @Valid UpdateProfileTagsRequest req
    ) {
        ProfileStatusResponse response = profileService.updateProfileTags(userId, req);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "마이페이지 한줄 소개(bio) 수정",
            description = "자신의 프로필에서 한줄 소개를 수정합니다."
    )
    @PatchMapping("/me/bio")
    public ApiResponse<ProfileStatusResponse> updateMyProfile(
            @UserId Long userId,
            @RequestBody @Valid UpdateBioRequest req
    ) {
        ProfileStatusResponse response = profileService.updateBio(userId, req.bio());
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "마이페이지 팔로워/팔로잉 수 공개 설정 수정",
            description = "팔로워/팔로잉 수 공개 여부 등 개인정보 노출 설정을 변경합니다."
    )
    @PatchMapping("/me/privacy")
    public ApiResponse<ProfileStatusResponse> updatePrivacySettings(
            @UserId Long userId,
            @RequestParam Boolean visible
    ) {
        ProfileStatusResponse response = profileService.updateFollowerVisibility(userId, visible);
        return ApiResponse.success(response);
    }

}
