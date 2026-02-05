package CamNecT.CamNecT_Server.domain.chat.controller;

import CamNecT.CamNecT_Server.domain.chat.dto.request.ChatRequestResponseDto;
import CamNecT.CamNecT_Server.domain.chat.dto.request.ChatRequestSendDto;
import CamNecT.CamNecT_Server.domain.chat.service.ChatService;
import CamNecT.CamNecT_Server.global.common.auth.UserId;
import CamNecT.CamNecT_Server.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/request")
public class ChatRequestController {

    private final ChatService chatService;

    /**
     * [커피챗 요청 보내기]
     */
    @PostMapping("/send")
    public ApiResponse<Void> sendRequest(
            @UserId Long userId,
            @RequestBody ChatRequestSendDto request
    ) {
        chatService.sendCoffeeChatRequest(
                userId,
                request.receiverId(),
                request.tagIds(),
                request.content()
        );

        return ApiResponse.success(null);
    }

    /**
     * [요청 수락/거절 처리]
     */
    @PostMapping("/respond")
    public ApiResponse<Void> respondRequest(
            @UserId Long userId,
            @RequestBody ChatRequestResponseDto response
    ) {
        chatService.respondToRequest(
                response.requestId(),
                userId,
                response.isAccepted()
        );

        return ApiResponse.success(null);
    }
}