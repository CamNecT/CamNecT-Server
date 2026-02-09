package CamNecT.CamNecT_Server.domain.profile.components.archive.controller;

import CamNecT.CamNecT_Server.domain.profile.components.archive.dto.response.MyArchiveResponse;
import CamNecT.CamNecT_Server.domain.profile.components.archive.service.MyPostQueryService;
import CamNecT.CamNecT_Server.global.common.auth.UserId;
import CamNecT.CamNecT_Server.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile/posts")
public class MyPostsController {
    private final MyPostQueryService myPostQueryService;
    // ===== My Posts (3) =====
    @GetMapping("/community")
    public ApiResponse<MyArchiveResponse> myCommunityPosts(
            @UserId Long userId,
            @RequestParam(defaultValue = "LATEST") MyArchiveResponse.Sort sort,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(required = false) Long cursorValue,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(myPostQueryService.getMyPosts(
                userId, MyArchiveResponse.Tab.COMMUNITY, sort, cursorId, cursorValue, size
        ));
    }

    @GetMapping("/external")
    public ApiResponse<MyArchiveResponse> myExternalPosts(
            @UserId Long userId,
            @RequestParam(defaultValue = "LATEST") MyArchiveResponse.Sort sort,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(required = false) Long cursorValue,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(myPostQueryService.getMyPosts(
                userId, MyArchiveResponse.Tab.EXTERNAL, sort, cursorId, cursorValue, size
        ));
    }

    @GetMapping("/recruitment")
    public ApiResponse<MyArchiveResponse> myRecruitmentPosts(
            @UserId Long userId,
            @RequestParam(defaultValue = "LATEST") MyArchiveResponse.Sort sort,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(required = false) Long cursorValue,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(myPostQueryService.getMyPosts(
                userId, MyArchiveResponse.Tab.RECRUITMENT, sort, cursorId, cursorValue, size
        ));
    }
}
