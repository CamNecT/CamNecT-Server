package CamNecT.CamNecT_Server.domain.community.controller;

import CamNecT.CamNecT_Server.domain.community.dto.response.CommunityHomeResponse;
import CamNecT.CamNecT_Server.domain.community.service.CommunityHomeService;
import CamNecT.CamNecT_Server.global.common.auth.UserId;
import CamNecT.CamNecT_Server.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Community Home", description = "커뮤니티 홈 화면 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community")
public class CommunityHomeController {

    private final CommunityHomeService communityHomeService;

    @Operation(
            summary = "커뮤니티 홈 화면 데이터 조회",
            description = "커뮤니티 홈에서 보여줄 인기글, 최신글 등을 통합 조회합니다. 특정 태그(tagId)를 전달하면 해당 태그 위주의 맞춤형 데이터를 반환합니다."
    )
    @GetMapping("/home")
    public ApiResponse<CommunityHomeResponse> home(@UserId Long userId) {
        return ApiResponse.success(communityHomeService.getHome(userId));
    }
}
