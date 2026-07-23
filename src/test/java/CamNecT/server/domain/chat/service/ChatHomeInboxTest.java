package CamNecT.server.domain.chat.service;

import CamNecT.server.domain.chat.model.ChatRequest;
import CamNecT.server.domain.chat.repository.ChatRequestRepository;
import CamNecT.server.domain.home.dto.HomeResponse;
import CamNecT.server.domain.profile.dto.ProfileGlobalDto;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatHomeInboxTest {

    @Mock ChatRequestRepository chatRequestRepository;
    @Mock UserProfileRepository userProfileRepository;

    @InjectMocks ChatService chatService;

    @Test
    void separatesCoffeeChatAndRecruitmentCountsAndPreservesSenderPreviews() {
        Long receiverId = 1L;
        Users coffeeSender = Users.builder().userId(2L).name("커피 발신자").build();
        Users recruitmentSender = Users.builder().userId(3L).name("모집 지원자").build();
        ChatRequest coffeeRequest = request(11L, coffeeSender);
        ChatRequest recruitmentRequest = request(12L, recruitmentSender);

        when(chatRequestRepository.countByReceiver_UserIdAndTypeAndStatus(
                receiverId, ChatRequest.RequestType.COFFEE_CHAT, ChatRequest.RequestStatus.WAITING
        )).thenReturn(2L);
        when(chatRequestRepository.countByReceiver_UserIdAndTypeAndStatus(
                receiverId, ChatRequest.RequestType.TEAM_RECRUIT, ChatRequest.RequestStatus.WAITING
        )).thenReturn(3L);
        when(chatRequestRepository.findLatestReceivedRequestsByType(
                receiverId,
                ChatRequest.RequestType.COFFEE_CHAT,
                ChatRequest.RequestStatus.WAITING,
                PageRequest.of(0, 2)
        )).thenReturn(List.of(coffeeRequest));
        when(chatRequestRepository.findLatestReceivedRequestsByType(
                receiverId,
                ChatRequest.RequestType.TEAM_RECRUIT,
                ChatRequest.RequestStatus.WAITING,
                PageRequest.of(0, 5)
        )).thenReturn(List.of(recruitmentRequest));
        when(userProfileRepository.findGlobalsByUserIdIn(List.of(2L)))
                .thenReturn(List.of(new ProfileGlobalDto(2L, "커피 발신자", "컴퓨터공학", "20240001", null)));
        when(userProfileRepository.findGlobalsByUserIdIn(List.of(3L)))
                .thenReturn(List.of(new ProfileGlobalDto(3L, "모집 지원자", "경영학", "20230002", null)));

        HomeResponse.CoffeeChatSection coffeeChat = chatService.getHomeInbox(receiverId, 2);
        HomeResponse.RecruitmentSection recruitment = chatService.getHomeRecruitmentInbox(receiverId, 5);

        assertThat(coffeeChat.pendingCount()).isEqualTo(2L);
        assertThat(coffeeChat.latest2()).singleElement().satisfies(preview -> {
            assertThat(preview.requestId()).isEqualTo(11L);
            assertThat(preview.senderUserId()).isEqualTo(2L);
            assertThat(preview.senderName()).isEqualTo("커피 발신자");
            assertThat(preview.majorName()).isEqualTo("컴퓨터공학");
            assertThat(preview.studentNo()).isEqualTo("20240001");
        });
        assertThat(recruitment.pendingCount()).isEqualTo(3L);
        assertThat(recruitment.latest5()).singleElement().satisfies(preview -> {
            assertThat(preview.requestId()).isEqualTo(12L);
            assertThat(preview.senderUserId()).isEqualTo(3L);
            assertThat(preview.senderName()).isEqualTo("모집 지원자");
            assertThat(preview.majorName()).isEqualTo("경영학");
            assertThat(preview.studentNo()).isEqualTo("20230002");
        });
    }

    @Test
    void returnsEmptySectionsWithoutLoadingPreviewsWhenNoRequestsAreWaiting() {
        Long receiverId = 1L;
        when(chatRequestRepository.countByReceiver_UserIdAndTypeAndStatus(
                receiverId, ChatRequest.RequestType.COFFEE_CHAT, ChatRequest.RequestStatus.WAITING
        )).thenReturn(0L);
        when(chatRequestRepository.countByReceiver_UserIdAndTypeAndStatus(
                receiverId, ChatRequest.RequestType.TEAM_RECRUIT, ChatRequest.RequestStatus.WAITING
        )).thenReturn(0L);

        HomeResponse.CoffeeChatSection coffeeChat = chatService.getHomeInbox(receiverId, 2);
        HomeResponse.RecruitmentSection recruitment = chatService.getHomeRecruitmentInbox(receiverId, 5);

        assertThat(coffeeChat).isEqualTo(HomeResponse.CoffeeChatSection.empty());
        assertThat(recruitment).isEqualTo(HomeResponse.RecruitmentSection.empty());
        verify(chatRequestRepository, never()).findLatestReceivedRequestsByType(
                receiverId,
                ChatRequest.RequestType.COFFEE_CHAT,
                ChatRequest.RequestStatus.WAITING,
                PageRequest.of(0, 2)
        );
        verify(chatRequestRepository, never()).findLatestReceivedRequestsByType(
                receiverId,
                ChatRequest.RequestType.TEAM_RECRUIT,
                ChatRequest.RequestStatus.WAITING,
                PageRequest.of(0, 5)
        );
    }

    private ChatRequest request(Long requestId, Users requester) {
        ChatRequest request = mock(ChatRequest.class);
        when(request.getId()).thenReturn(requestId);
        when(request.getRequester()).thenReturn(requester);
        return request;
    }
}
