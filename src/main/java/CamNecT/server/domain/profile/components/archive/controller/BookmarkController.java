package CamNecT.server.domain.profile.components.archive.controller;

import CamNecT.server.domain.profile.components.archive.dto.response.MyArchiveResponse;
import CamNecT.server.domain.profile.components.archive.service.ArchiveQueryService;
import CamNecT.server.global.common.auth.UserId;
import CamNecT.server.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile/bookmarks")
public class BookmarkController {

    private final ArchiveQueryService archiveQueryService;

    // ===== Bookmarks (3) =====
    @GetMapping("/community")
    public ApiResponse<MyArchiveResponse> bookmarkedCommunity(
            @UserId Long userId,
            @RequestParam(defaultValue = "RECOMMENDED") MyArchiveResponse.Sort sort,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(required = false) Long cursorValue,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(
                archiveQueryService.getCommunityArchive(
                        userId,
                        MyArchiveResponse.ArchiveKind.BOOKMARKS,
                        sort,
                        cursorId,
                        cursorValue,
                        size
                )
        );
    }

    @GetMapping("/external")
    public ApiResponse<MyArchiveResponse> bookmarkedExternal(
            @UserId Long userId,
            @RequestParam(defaultValue = "RECOMMENDED") MyArchiveResponse.Sort sort,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(required = false) Long cursorValue,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(
                archiveQueryService.getExternalArchive(
                        userId,
                        MyArchiveResponse.ArchiveKind.BOOKMARKS,
                        sort,
                        cursorId,
                        cursorValue,
                        size
                )
        );
    }

    @GetMapping("/recruitment")
    public ApiResponse<MyArchiveResponse> bookmarkedRecruitment(
            @UserId Long userId,
            @RequestParam(defaultValue = "RECOMMENDED") MyArchiveResponse.Sort sort,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(required = false) Long cursorValue,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(
                archiveQueryService.getRecruitmentArchive(
                        userId,
                        MyArchiveResponse.ArchiveKind.BOOKMARKS,
                        sort,
                        cursorId,
                        cursorValue,
                        size
                )
        );
    }
}
