package CamNecT.CamNecT_Server.global.tag.controller;

import CamNecT.CamNecT_Server.global.common.response.ApiResponse;
import CamNecT.CamNecT_Server.global.tag.dto.response.InstitutionListResponse;
import CamNecT.CamNecT_Server.global.tag.dto.response.InstitutionResponse;
import CamNecT.CamNecT_Server.global.tag.dto.response.MajorListResponse;
import CamNecT.CamNecT_Server.global.tag.dto.response.MajorResponse;
import CamNecT.CamNecT_Server.global.tag.service.InstitutionService;
import CamNecT.CamNecT_Server.global.tag.service.MajorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Institution", description = "학교 및 전공 정보 관련 API")
@RestController
@RequestMapping("/api/institutions")
@RequiredArgsConstructor
public class InstitutionController {

    private final InstitutionService institutionService;
    private final MajorService majorService;

    @Operation(
            summary = "대학 검색",
            description = "키워드로 대학을 검색합니다. 키워드가 없거나 공백일 경우 빈 리스트를 반환합니다."
    )
    @GetMapping
    public ApiResponse<InstitutionListResponse> searchInstitutions(
            @Parameter(description = "검색할 대학 이름 키워드 (예: 건국 / Konkuk)", required = false)
            @RequestParam(required = false) String keyword
    ) {
        if (keyword == null || keyword.isBlank()) {
            return ApiResponse.success(InstitutionListResponse.empty());
        }
        return ApiResponse.success(institutionService.searchInstitutions(keyword));
    }

    @Operation(
            summary = "대학 단건 조회",
            description = "ID로 특정 대학의 상세 정보를 조회합니다."
    )
    @GetMapping("/{institutionId}")
    public ApiResponse<InstitutionResponse> getInstitution(
            @Parameter(description = "조회할 대학 ID", required = true)
            @PathVariable Long institutionId
    ) {
        return ApiResponse.success(institutionService.getInstitution(institutionId));
    }

    @Operation(
            summary = "특정 대학의 전공 목록 조회",
            description = "특정 대학에 개설된 모든 전공 리스트를 조회합니다."
    )
    @GetMapping("/{institutionId}/majors")
    public ApiResponse<MajorListResponse> getMajors(
            @Parameter(description = "전공을 조회할 대학 ID", required = true)
            @PathVariable Long institutionId
    ) {
        return ApiResponse.success(majorService.getMajors(institutionId));
    }

    @Operation(
            summary = "전공 단건 조회",
            description = "특정 대학의 특정 전공 상세 정보를 조회합니다."
    )
    @GetMapping("/{institutionId}/majors/{majorId}")
    public ApiResponse<MajorResponse> getMajor(
            @Parameter(description = "대학 ID", required = true)
            @PathVariable Long institutionId,
            @Parameter(description = "조회할 전공 ID", required = true)
            @PathVariable Long majorId
    ) {
        return ApiResponse.success(majorService.getMajor(institutionId, majorId));
    }
}