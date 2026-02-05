package CamNecT.CamNecT_Server.domain.chat.controller;

import CamNecT.CamNecT_Server.domain.chat.dto.room.ChatRoomListDetailDto;
import CamNecT.CamNecT_Server.domain.chat.dto.room.ChatRoomListResponseDto;
import CamNecT.CamNecT_Server.domain.chat.dto.room.ChatRoomWithDetailDto;
import CamNecT.CamNecT_Server.domain.chat.service.ChatService;
import CamNecT.CamNecT_Server.global.common.auth.UserId;
import CamNecT.CamNecT_Server.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
@Tag(name = "Chat Room", description = "채팅방 조회 및 입장 API")
public class ChatRoomController {

    private final ChatService chatService;

    @Operation(summary = "내 채팅방 목록 조회", description = "참여 중인 모든 채팅방 목록과 전체 안 읽은 메시지 수를 반환합니다.")
    @GetMapping("/rooms")
    @Transactional(readOnly = true)
    public ApiResponse<ChatRoomListResponseDto> roomList(@UserId Long userId) {

        List<ChatRoomListDetailDto> roomList = chatService.getChatRoomList(userId);
        long totalUnreadCount = roomList.stream().mapToLong(ChatRoomListDetailDto::getUnreadCount).sum();

        ChatRoomListResponseDto response = ChatRoomListResponseDto.builder()
                .chatRoomList(roomList)
                .totalUnreadCount(totalUnreadCount)
                .build();

        return ApiResponse.success(response);
    }

    /**
     * [채팅방 상세 조회 (입장)]
     * URL: GET /api/chat/room/{roomId}
     * React: 채팅방 클릭 시 호출 -> 이 데이터를 받아서 화면 그리고 소켓 연결(CONNECT)
     */
    @Operation(summary = "채팅방 상세 조회 (입장)", description = "특정 채팅방의 상대방 정보와 채팅 내역을 불러옵니다. 이 API 호출 후 소켓 연결(CONNECT)을 진행해야 합니다.")
    @GetMapping("/room/{roomId}")
    public ApiResponse<ChatRoomWithDetailDto> joinRoom(@Parameter(description = "채팅방 ID") @PathVariable Long roomId, @UserId Long userId) {

        ChatRoomWithDetailDto response = chatService.getRoomWithDetails(roomId, userId);

        return ApiResponse.success(response);
    }

}