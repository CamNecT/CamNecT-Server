package CamNecT.CamNecT_Server.domain.profile.controller;

import CamNecT.CamNecT_Server.domain.profile.service.FollowService;
import CamNecT.CamNecT_Server.global.common.auth.UserId;
import CamNecT.CamNecT_Server.global.common.response.ApiResponse;
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
}