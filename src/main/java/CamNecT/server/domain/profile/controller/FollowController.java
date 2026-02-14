package CamNecT.server.domain.profile.controller;

import CamNecT.server.domain.profile.dto.response.FollowListResponse;
import CamNecT.server.domain.profile.service.FollowService;
import CamNecT.server.global.common.auth.UserId;
import CamNecT.server.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Follow", description = "팔로우/팔로워 관련 API")
@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @Operation(
            summary = "사용자 팔로우",
            description = "특정 사용자를 팔로우합니다. 자기 자신은 팔로우할 수 없습니다."
    )
    @PostMapping("/{followingId}")
    public ApiResponse<Void> follow(
            @UserId Long userId,
            @PathVariable Long followingId
    ) {
        followService.follow(userId, followingId);
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "사용자 언팔로우",
            description = "팔로우 중인 사용자를 취소(언팔로우)합니다."
    )
    @DeleteMapping("/{followingId}")
    public ApiResponse<Void> unfollow(
            @UserId Long userId,
            @PathVariable Long followingId
    ) {
        followService.unfollow(userId, followingId);
        return ApiResponse.success(null);
    }


    @Operation(
            summary = "팔로워 목록 조회",
            description = "나를 팔로우하는 유저들의 목록을 반환합니다."
    )
    @GetMapping("/me/followers")
    public ApiResponse<FollowListResponse> getMyFollowers(@UserId Long userId) {
        return ApiResponse.success(followService.getFollowerList(userId));
    }

    @Operation(
            summary = "팔로잉 목록 조회",
            description = "내가 팔로우하는 유저들의 목록을 반환합니다."
    )
    @GetMapping("/me/followings")
    public ApiResponse<FollowListResponse> getMyFollowings(@UserId Long userId) {
        return ApiResponse.success(followService.getFollowingList(userId));
    }
}