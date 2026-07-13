package CamNecT.server.domain.chat.event;

import CamNecT.server.domain.chat.dto.message.ChatMessageResponseDto;

public record ChatMessageCommittedEvent(
        ChatMessageResponseDto message,
        Long senderId,
        Long receiverId,
        String lastMessage,
        String lastMessageTime
) {}
