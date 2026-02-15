package CamNecT.server.domain.home.controller;

import CamNecT.server.domain.home.dto.HomeResponse;
import CamNecT.server.domain.home.service.HomeService;
import CamNecT.server.global.common.auth.UserId;
import CamNecT.server.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/home")
public class HomeController {

    private final HomeService homeService;

    @GetMapping
    public ApiResponse<HomeResponse> home(
            @UserId Long userId
    ) {
        return ApiResponse.success(homeService.getHome(userId));
    }
}