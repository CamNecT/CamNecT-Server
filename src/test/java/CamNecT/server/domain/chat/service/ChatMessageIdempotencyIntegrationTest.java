package CamNecT.server.domain.chat.service;

import CamNecT.server.domain.chat.dto.message.ChatMessageAckResponseDto;
import CamNecT.server.domain.chat.dto.message.ChatMessageSendRequestDto;
import CamNecT.server.domain.chat.model.ChatRequest;
import CamNecT.server.domain.chat.model.ChatRoom;
import CamNecT.server.domain.chat.repository.ChatRepository;
import CamNecT.server.domain.chat.repository.ChatRequestRepository;
import CamNecT.server.domain.chat.repository.ChatRoomRepository;
import CamNecT.server.domain.users.model.UserStatus;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ChatMessageIdempotencyIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired ChatRequestRepository chatRequestRepository;
    @Autowired ChatRoomRepository chatRoomRepository;
    @Autowired ChatRepository chatRepository;
    @Autowired ChatService chatService;
    @Autowired ChatPresenceService presenceService;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void concurrentRetriesWithSameClientMessageIdCreateOneMessage() throws Exception {
        Fixture fixture = createFixture();
        String clientMessageId = UUID.randomUUID().toString();
        presenceService.enter(fixture.roomId(), fixture.receiverId(), "receiver-session", "receiver-sub");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<ChatMessageAckResponseDto> first = executor.submit(() ->
                    sendAfterBarrier(fixture, clientMessageId, ready, start));
            Future<ChatMessageAckResponseDto> second = executor.submit(() ->
                    sendAfterBarrier(fixture, clientMessageId, ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            ChatMessageAckResponseDto firstAck = first.get(10, TimeUnit.SECONDS);
            ChatMessageAckResponseDto secondAck = second.get(10, TimeUnit.SECONDS);

            assertThat(List.of(firstAck.duplicate(), secondAck.duplicate()))
                    .containsExactlyInAnyOrder(false, true);
            assertThat(firstAck.messageId()).isEqualTo(secondAck.messageId());
            assertThat(firstAck.clientMessageId()).isEqualTo(clientMessageId);
            assertThat(secondAck.clientMessageId()).isEqualTo(clientMessageId);
            assertThat(chatRepository.countByRoom_IdAndSender_UserIdAndClientMessageId(
                    fixture.roomId(), fixture.senderId(), clientMessageId)).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
            presenceService.leaveSession("receiver-session");
        }
    }

    private ChatMessageAckResponseDto sendAfterBarrier(
            Fixture fixture,
            String clientMessageId,
            CountDownLatch ready,
            CountDownLatch start
    ) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("concurrent send barrier timed out");
        }
        return chatService.sendMessage(
                fixture.senderId(),
                new ChatMessageSendRequestDto(fixture.roomId(), "same logical message", clientMessageId)
        );
    }

    private Fixture createFixture() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return transaction.execute(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            Users sender = userRepository.save(Users.builder()
                    .username("sender-" + suffix)
                    .passwordHash("password")
                    .name("sender")
                    .status(UserStatus.ACTIVE)
                    .build());
            Users receiver = userRepository.save(Users.builder()
                    .username("receiver-" + suffix)
                    .passwordHash("password")
                    .name("receiver")
                    .status(UserStatus.ACTIVE)
                    .build());

            ChatRequest request = chatRequestRepository.save(ChatRequest.builder()
                    .requester(sender)
                    .receiver(receiver)
                    .requestInterest(List.of())
                    .content("request")
                    .type(ChatRequest.RequestType.COFFEE_CHAT)
                    .build());
            request.accept();

            ChatRoom room = chatRoomRepository.save(ChatRoom.createRoom(request, sender, receiver));
            return new Fixture(room.getId(), sender.getUserId(), receiver.getUserId());
        });
    }

    private record Fixture(Long roomId, Long senderId, Long receiverId) {}
}
