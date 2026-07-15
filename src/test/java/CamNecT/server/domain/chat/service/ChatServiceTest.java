package CamNecT.server.domain.chat.service;

import CamNecT.server.domain.activity.repository.recruitment.TeamRecruitmentRepository;
import CamNecT.server.domain.chat.dto.message.ChatMessageSendRequestDto;
import CamNecT.server.domain.chat.model.Chat;
import CamNecT.server.domain.chat.model.ChatRequest;
import CamNecT.server.domain.chat.model.ChatRoom;
import CamNecT.server.domain.chat.repository.ChatRepository;
import CamNecT.server.domain.chat.repository.ChatRequestRepository;
import CamNecT.server.domain.chat.repository.ChatRoomRepository;
import CamNecT.server.domain.profile.components.majors.repository.MajorRepository;
import CamNecT.server.domain.users.model.UserStatus;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.repository.UserProfileRepository;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.domain.users.repository.UserTagMapRepository;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.CoffeeChatErrorCode;
import CamNecT.server.global.point.service.PointService;
import CamNecT.server.global.storage.service.PublicUrlIssuer;
import CamNecT.server.global.tag.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock UserRepository userRepository;
    @Mock ChatRepository chatRepository;
    @Mock ChatRoomRepository chatRoomRepository;
    @Mock ChatRequestRepository chatRequestRepository;
    @Mock TagRepository tagRepository;
    @Mock UserProfileRepository userProfileRepository;
    @Mock UserTagMapRepository userTagMapRepository;
    @Mock MajorRepository majorRepository;
    @Mock TeamRecruitmentRepository recruitmentRepository;
    @Mock PublicUrlIssuer publicUrlIssuer;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock ChatPresenceService presenceService;
    @Mock PointService pointService;

    @InjectMocks ChatService chatService;

    @Test
    void nonParticipantCannotSendMessageToKnownRoomId() {
        Users requester = activeUser(1L);
        Users receiver = activeUser(2L);
        Users attacker = activeUser(3L);
        ChatRoom room = mock(ChatRoom.class);

        when(chatRoomRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(room));
        when(room.getStatus()).thenReturn(ChatRoom.RoomStatus.OPEN);
        when(room.getRequester()).thenReturn(requester);
        when(room.getReceiver()).thenReturn(receiver);
        when(userRepository.findById(3L)).thenReturn(Optional.of(attacker));

        CustomException ex = assertThrows(CustomException.class,
                () -> chatService.sendMessage(3L, new ChatMessageSendRequestDto(99L, "hello")));

        assertThat(ex.getErrorCode()).isEqualTo(CoffeeChatErrorCode.CHATROOM_ACCESS_DENIED);
        verify(chatRepository, never()).save(any(Chat.class));
        verifyNoInteractions(presenceService, eventPublisher);
    }

    @Test
    void historyMarksCurrentUserMessagesReadNotOpponents() {
        Users reader = activeUser(1L);
        ChatRoom room = mock(ChatRoom.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(reader));
        when(chatRoomRepository.findByUserIdWithDetails(99L, 1L)).thenReturn(Optional.of(room));
        when(chatRoomRepository.existsAccessibleByUserId(99L, 1L)).thenReturn(true);
        when(chatRepository.findUnreadMessages(99L, 1L)).thenReturn(List.of());
        when(chatRepository.findTop1000ByRoomId(eq(99L), any())).thenReturn(List.of());

        assertDoesNotThrow(() -> chatService.getChatHistory(99L, 1L));

        verify(chatRepository).findUnreadMessages(99L, 1L);
        verify(chatRepository, never()).findUnreadMessages(99L, 2L);
    }

    @Test
    void contradictorySecondResponseIsRejectedButSameResponseIsIdempotent() {
        Users receiver = activeUser(2L);
        ChatRequest request = mock(ChatRequest.class);

        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(chatRequestRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(request));
        when(request.getReceiver()).thenReturn(receiver);
        when(request.getStatus()).thenReturn(ChatRequest.RequestStatus.ACCEPTED);

        assertDoesNotThrow(() -> chatService.respondToRequest(10L, 2L, true));

        CustomException ex = assertThrows(CustomException.class,
                () -> chatService.respondToRequest(10L, 2L, false));
        assertThat(ex.getErrorCode()).isEqualTo(CoffeeChatErrorCode.REQUEST_ALREADY_PROCESSED);
    }

    private Users activeUser(Long userId) {
        return Users.builder().userId(userId).status(UserStatus.ACTIVE).build();
    }
}
