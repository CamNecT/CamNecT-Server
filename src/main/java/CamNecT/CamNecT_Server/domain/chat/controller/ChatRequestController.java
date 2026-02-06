package CamNecT.CamNecT_Server.domain.chat.controller;

import CamNecT.CamNecT_Server.domain.chat.dto.request.ChatRequestResponseDto;
import CamNecT.CamNecT_Server.domain.chat.dto.request.ChatRequestSendDto;
import CamNecT.CamNecT_Server.domain.chat.service.ChatService;
import CamNecT.CamNecT_Server.global.common.auth.UserId;
import CamNecT.CamNecT_Server.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/request")
@Tag(name = "Chat Request", description = "커피챗 요청/응답 API")
public class ChatRequestController {

    private final ChatService chatService;

    @Operation(summary = "커피챗 요청 보내기", description = "상대방에게 커피챗 요청을 보냅니다.")
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

    @Operation(summary = "커피챗 요청 수락/거절", description = "받은 커피챗 요청을 수락하거나 거절합니다. 수락 시 채팅방이 생성됩니다.")
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