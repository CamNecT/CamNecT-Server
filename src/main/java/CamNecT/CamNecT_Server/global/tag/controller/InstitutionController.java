package CamNecT.CamNecT_Server.global.tag.controller;

import CamNecT.CamNecT_Server.global.common.response.ApiResponse;
import CamNecT.CamNecT_Server.global.tag.dto.response.InstitutionListResponse;
import CamNecT.CamNecT_Server.global.tag.dto.response.InstitutionResponse;
import CamNecT.CamNecT_Server.global.tag.dto.response.MajorListResponse;
import CamNecT.CamNecT_Server.global.tag.dto.response.MajorResponse;
import CamNecT.CamNecT_Server.global.tag.service.InstitutionService;
import CamNecT.CamNecT_Server.global.tag.service.MajorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/institutions")
@RequiredArgsConstructor
public class InstitutionController {

    private final InstitutionService institutionService;
    private final MajorService majorService;

    // 대학 전체 조회
    @GetMapping
    public ApiResponse<InstitutionListResponse> searchInstitutions(
            @RequestParam(required = false) String keyword
    ) {
        if (keyword == null || keyword.isBlank()) {
            return ApiResponse.success(InstitutionListResponse.empty());
        }
        return ApiResponse.success(institutionService.searchInstitutions(keyword));
    }

    // 대학 단건 조회
    @GetMapping("/{institutionId}")
    public ApiResponse<InstitutionResponse> getInstitution(
            @PathVariable Long institutionId
    ) {
        return ApiResponse.success(institutionService.getInstitution(institutionId));
    }

    @GetMapping("/{institutionId}/majors")
    public ApiResponse<MajorListResponse> getMajors(
            @PathVariable Long institutionId
    ) {
        return ApiResponse.success(majorService.getMajors(institutionId));
    }

    @GetMapping("/{institutionId}/majors/{majorId}")
    public ApiResponse<MajorResponse> getMajor(
            @PathVariable Long institutionId,
            @PathVariable Long majorId
    ) {
        return ApiResponse.success(majorService.getMajor(institutionId, majorId));
    }
}