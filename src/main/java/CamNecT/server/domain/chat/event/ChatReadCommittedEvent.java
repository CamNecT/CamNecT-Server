package CamNecT.server.domain.chat.event;

import CamNecT.server.domain.chat.dto.message.ChatReadEvent;

public record ChatReadCommittedEvent(
        ChatReadEvent readEvent,
        Long readerId,
        String lastMessage,
        String lastMessageTime
) {}
