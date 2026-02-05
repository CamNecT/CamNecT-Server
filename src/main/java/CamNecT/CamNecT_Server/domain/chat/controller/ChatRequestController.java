package CamNecT.CamNecT_Server.domain.chat.controller;

import CamNecT.CamNecT_Server.domain.chat.dto.request.ChatRequestResponseDto;
import CamNecT.CamNecT_Server.domain.chat.dto.request.ChatRequestSendDto;
import CamNecT.CamNecT_Server.domain.chat.service.ChatService;
import CamNecT.CamNecT_Server.global.common.auth.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/request")
public class ChatRequestController {

    private final ChatService chatService;

    /**
     * [커피챗 요청 보내기]
     * HTML Form: <form action="/request/send" method="post">
     */
    @PostMapping("/send")
    public String sendRequest(
            @UserId Long userId,
            @ModelAttribute ChatRequestSendDto request,
            RedirectAttributes rattr) {
        try {
            chatService.sendCoffeeChatRequest(
                    userId,
                    request.receiverId(),
                    request.tagIds(),
                    request.content()
            );
            rattr.addFlashAttribute("message", "커피챗 요청을 성공적으로 보냈습니다.");
        } catch (Exception e) {
            log.error("요청 전송 실패", e);
            rattr.addFlashAttribute("error", "요청 실패: " + e.getMessage());
        }

        return "redirect:/roomList?userId=" + userId;
    }

    /**
     * [요청 수락/거절 처리]
     * HTML Form: <form action="/request/respond" method="post">
     */
    @PostMapping("/respond")
    public String respondRequest(
            @UserId Long userId,
            @ModelAttribute ChatRequestResponseDto response, RedirectAttributes rattr) {
        try {
            chatService.respondToRequest(
                    response.requestId(),
                    userId,
                    response.isAccepted()
            );

            String msg = response.isAccepted() ? "요청을 수락하여 채팅방이 생성되었습니다." : "요청을 거절했습니다.";
            rattr.addFlashAttribute("message", msg);

        } catch (Exception e) {
            log.error("응답 처리 실패", e);
            rattr.addFlashAttribute("error", "처리 실패: " + e.getMessage());
        }

        return "redirect:/roomList?userId=" + userId;
    }
}