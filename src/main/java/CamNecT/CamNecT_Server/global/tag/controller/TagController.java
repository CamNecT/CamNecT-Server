package CamNecT.CamNecT_Server.global.tag.controller;

import CamNecT.CamNecT_Server.global.common.response.ApiResponse;
import CamNecT.CamNecT_Server.global.tag.dto.TagCategoryDto;
import CamNecT.CamNecT_Server.global.tag.service.TagQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Tags", description = "태그/카테고리 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tags")
public class TagController {

    private final TagQueryService tagQueryService;

    @Operation(summary = "태그 카테고리/태그 목록 조회", description = "온보딩/프로필 태그 선택에 필요한 카테고리별 태그 목록을 반환합니다.")
    @GetMapping
    public ApiResponse<List<TagCategoryDto>> list() {
        return ApiResponse.success(tagQueryService.listCategoriesWithTags());
    }
}
