package CamNecT.CamNecT_Server.domain.chat.controller;

import CamNecT.CamNecT_Server.domain.chat.dto.room.ChatRoomListDetailDto;
import CamNecT.CamNecT_Server.domain.chat.dto.room.ChatRoomWithDetailDto;
import CamNecT.CamNecT_Server.domain.chat.model.ChatRequest;
import CamNecT.CamNecT_Server.domain.chat.repository.ChatRequestRepository;
import CamNecT.CamNecT_Server.domain.chat.service.ChatService;
import CamNecT.CamNecT_Server.global.common.auth.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatService chatService;
    private final ChatRequestRepository requestRepository;


    /**
     * 통합 채팅 화면 (요청 목록 + 내 채팅방 목록)
     * URL: ~~/roomList?userId=1
     */
    @GetMapping("/roomList")
    @Transactional(readOnly = true)
    public String roomList(@UserId Long userId, Model model) {

        // 나에게 온 대기중(WAITING)인 요청 목록
        List<ChatRequest> requests = requestRepository.findAllByReceiver_UserIdAndStatusOrderByCreatedAtDesc(
                userId,
                ChatRequest.RequestStatus.WAITING
        );

        // 이미 생성된 내 채팅방 목록
        List<ChatRoomListDetailDto> roomList = chatService.getChatRoomList(userId);

        long allUnreadCount = roomList.stream().mapToLong(ChatRoomListDetailDto::getUnreadCount).sum();

        model.addAttribute("requests", requests);
        model.addAttribute("roomList", roomList);
        model.addAttribute("allUnreadCount", allUnreadCount);
        model.addAttribute("userId", userId);

        return "roomList"; // 일단 templates/roomList.html로 연결됨.
    }

    /**
     * 채팅방 입장
     */
    @GetMapping("/chat/room/{roomId}")
    public String joinRoom(@PathVariable Long roomId, @UserId Long userId, Model model) {

        ChatRoomWithDetailDto roomDto = chatService.getRoomWithDetails(roomId, userId);

        model.addAttribute("room", roomDto);
        model.addAttribute("userId", userId);

        model.addAttribute("chatList", roomDto.getChatList());

        return "chatRoom";
    }

}