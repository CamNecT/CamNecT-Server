package CamNecT.server.domain.alumni.controller;

import CamNecT.server.domain.alumni.dto.response.AlumniPreviewResponse;
import CamNecT.server.domain.alumni.service.AlumniService;
import CamNecT.server.global.common.auth.UserId;
import CamNecT.server.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Alumni", description = "동문 탐색 및 조회 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alumni")
public class AlumniController {

    private final AlumniService alumniService;

    @Operation(
            summary = "동문 목록 검색 및 조회",
            description = "이름 또는 관심 태그를 필터로 사용하여 동문 목록을 조회합니다. 필터 값이 없으면 추천순 동문 목록을 반환합니다."
    )
    @GetMapping
    public ApiResponse<Slice<AlumniPreviewResponse>> searchAlumni(
            @UserId Long userId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) List<Long> tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(alumniService.searchAlumni(userId, name, tags, pageable));
    }
}
