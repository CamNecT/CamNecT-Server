package CamNecT.CamNecT_Server.domain.chat.controller;

import CamNecT.CamNecT_Server.domain.chat.dto.request.request.ChatRequestAcceptDto;
import CamNecT.CamNecT_Server.domain.chat.dto.request.response.ChatRequestDetailDto;
import CamNecT.CamNecT_Server.domain.chat.dto.request.response.ChatRequestListResponseDto;
import CamNecT.CamNecT_Server.domain.chat.dto.request.request.ChatRequestSendDto;
import CamNecT.CamNecT_Server.domain.chat.model.ChatRequest;
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

    @Operation(summary = "커피챗 요청 보내기 (커피챗)", description = "상대방에게 커피챗 요청을 보냅니다.")
    @PostMapping("/send")
    public ApiResponse<Long> sendRequest(
            @UserId Long userId,
            @RequestBody ChatRequestSendDto request
    ) {
        return ApiResponse.success(
                chatService.sendCoffeeChatRequest(userId, request.receiverId(),
                        request.tagIds(), request.content())
        );
    }

    @Operation(summary = "커피챗 요청 수락/거절", description = "받은 커피챗 요청을 수락하거나 거절합니다. 수락 시 채팅방이 생성됩니다.")
    @PostMapping("/respond")
    public ApiResponse<Void> respondRequest(
            @UserId Long userId,
            @RequestBody ChatRequestAcceptDto response
    ) {
        chatService.respondToRequest(
                response.requestId(),
                userId,
                response.isAccepted()
        );

        return ApiResponse.success(null);
    }

    @Operation(summary = "커피챗 요청 목록 조회", description = "요청 타입(COFFEE_CHAT, TEAM_RECRUIT)에 따라 받은 요청 리스트를 조회합니다.")
    @GetMapping("/list")
    public ApiResponse<ChatRequestListResponseDto> getRequestList(
            @UserId Long userId,
            @RequestParam("type") ChatRequest.RequestType type
    ) {
        ChatRequestListResponseDto response = chatService.getChatRequestList(userId, type);
        return ApiResponse.success(response);
    }

    @Operation(summary = "커피챗 요청 상세 조회", description = "특정 커피챗 요청의 상세 정보를 조회합니다.")
    @GetMapping("/{requestId}")
    public ApiResponse<ChatRequestDetailDto> getRequestDetail(
            @UserId Long userId,
            @PathVariable Long requestId
    ) {
        log.info("커피챗 요청 상세 조회 - 요청자 ID: {}, 요청서 ID: {}", userId, requestId);

        ChatRequestDetailDto detail = chatService.getChatRequestDetail(requestId, userId);

        return ApiResponse.success(detail);
    }

    @Operation(summary = "커피챗 요청 전체 삭제", description = "내가 받은 모든 커피챗 요청(COFFEE_CHAT)을 삭제합니다.")
    @DeleteMapping("/all/coffee-chat")
    public ApiResponse<Void> deleteAllCoffeeChatRequests(@UserId Long userId) {
        chatService.rejectAllCoffeeChatRequests(userId, ChatRequest.RequestType.COFFEE_CHAT);
        return ApiResponse.success(null);
    }

    @Operation(summary = "팀원 모집 요청 전체 삭제 (게시글별)", description = "특정 팀원모집글(Recruitment)과 관련된 모든 팀원 모집 요청을 삭제합니다.")
    @DeleteMapping("/all/team-recruit/{recruitmentId}")
    public ApiResponse<Void> deleteAllTeamRecruitRequests(
            @UserId Long userId,
            @PathVariable Long recruitmentId
    ) {
        chatService.rejectAllTeamRecruitRequestsByRecruitment(userId, recruitmentId);
        return ApiResponse.success(null);
    }
}