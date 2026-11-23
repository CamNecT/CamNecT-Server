package CamNecT.server.domain.community.controller;

import CamNecT.server.domain.community.service.PostTagSuggestionService;
import CamNecT.server.domain.community.service.PostTagSuggestionService.Draft;
import CamNecT.server.domain.community.service.PostTagSuggestionService.Suggestion;
import CamNecT.server.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/posts")
public class PostTagSuggestionController {
    private final PostTagSuggestionService suggestions;

    @Operation(summary = "작성 중인 글의 선택 가능한 추천 태그 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추천 목록", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "50351 판독기가 사용 중이거나 응답하지 않습니다.")
    })
    @PostMapping("/tag-suggestions")
    public ApiResponse<List<Suggestion>> suggest(@Valid @RequestBody Draft draft, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return ApiResponse.success(suggestions.suggest(draft));
    }
}
