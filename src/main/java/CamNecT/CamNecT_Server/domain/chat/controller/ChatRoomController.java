package CamNecT.CamNecT_Server.domain.chat.controller;

import CamNecT.CamNecT_Server.domain.chat.dto.room.ChatRoomListDetailDto;
import CamNecT.CamNecT_Server.domain.chat.dto.room.ChatRoomListResponseDto;
import CamNecT.CamNecT_Server.domain.chat.dto.room.ChatRoomWithDetailDto;
import CamNecT.CamNecT_Server.domain.chat.model.ChatRequest;
import CamNecT.CamNecT_Server.domain.chat.repository.ChatRequestRepository;
import CamNecT.CamNecT_Server.domain.chat.service.ChatService;
import CamNecT.CamNecT_Server.global.common.auth.UserId;
import CamNecT.CamNecT_Server.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatRoomController {

    private final ChatService chatService;

    /**
     * [채팅방 목록 조회]
     * URL: GET /api/chat/rooms
     */
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
    @GetMapping("/room/{roomId}")
    public ApiResponse<ChatRoomWithDetailDto> joinRoom(@PathVariable Long roomId, @UserId Long userId) {

        ChatRoomWithDetailDto response = chatService.getRoomWithDetails(roomId, userId);

        return ApiResponse.success(response);
    }

}