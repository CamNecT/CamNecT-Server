package CamNecT.server.domain.chat.service;

import CamNecT.server.domain.chat.event.ChatRoomClosedCommittedEvent;
import CamNecT.server.domain.chat.model.Chat;
import CamNecT.server.domain.chat.model.ChatRequest;
import CamNecT.server.domain.chat.model.ChatRoom;
import CamNecT.server.domain.chat.repository.ChatRepository;
import CamNecT.server.domain.chat.repository.ChatRequestRepository;
import CamNecT.server.domain.chat.repository.ChatRoomRepository;
import CamNecT.server.domain.users.model.UserStatus;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.BaseErrorCode;
import CamNecT.server.global.common.response.errorcode.bydomains.AuthErrorCode;
import CamNecT.server.global.common.response.errorcode.bydomains.CoffeeChatErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@RecordApplicationEvents
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ChatBulkExitIntegrationTest {
    @Autowired ChatService service;
    @Autowired UserRepository users;
    @Autowired ChatRoomRepository rooms;
    @Autowired ChatRequestRepository requests;
    @Autowired ChatRepository messages;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired ApplicationEvents events;

    @Test
    void hidesBothRolesOnlyForCallerPreservingRequestsMessagesAndOpponentVisibility() {
        Fixture f = fixture();
        service.exitClosedChatRooms(List.of(f.second(), f.first(), f.first()), f.caller());
        service.exitClosedChatRooms(List.of(f.first(), f.second()), f.caller());

        assertThat(rooms.findAllByUserIdWithBasicInfo(f.caller())).extracting(ChatRoom::getId)
                .containsExactly(f.open());
        assertThat(rooms.findAllByUserIdWithBasicInfo(f.opponent())).extracting(ChatRoom::getId)
                .contains(f.first(), f.second(), f.open());
        assertThat(rooms.existsAccessibleByUserId(f.first(), f.caller())).isFalse();
        assertThat(rooms.existsAccessibleByUserId(f.second(), f.caller())).isFalse();
        assertThat(rooms.findById(f.first()).orElseThrow().isRequesterExited()).isTrue();
        assertThat(rooms.findById(f.first()).orElseThrow().isReceiverExited()).isFalse();
        assertThat(rooms.findById(f.second()).orElseThrow().isReceiverExited()).isTrue();
        assertThat(rooms.findById(f.second()).orElseThrow().isRequesterExited()).isFalse();
        assertThat(requests.findById(f.firstRequest()).orElseThrow().getStatus())
                .isEqualTo(ChatRequest.RequestStatus.CLOSED);
        // Even a legacy inconsistent request must not be re-closed by an exit-only operation.
        assertThat(requests.findById(f.secondRequest()).orElseThrow().getStatus())
                .isEqualTo(ChatRequest.RequestStatus.ACCEPTED);
        assertThat(messages.findById(f.message()).orElseThrow().getContent()).isEqualTo("retained history");
        assertThat(events.stream(ChatRoomClosedCommittedEvent.class)).isEmpty();
    }

    @Test
    void singleClosedRoomCanBeExited() {
        Fixture f = fixture();
        service.exitClosedChatRooms(List.of(f.first()), f.caller());
        assertThat(rooms.existsAccessibleByUserId(f.first(), f.caller())).isFalse();
        assertThat(rooms.existsAccessibleByUserId(f.second(), f.caller())).isTrue();
    }

    @Test
    void openRoomRejectsEntireSelectionWithoutClosingIt() {
        Fixture f = fixture();
        expectError(() -> service.exitClosedChatRooms(List.of(f.first(), f.open()), f.caller()),
                CoffeeChatErrorCode.CHATROOM_NOT_CLOSED);
        assertUnchanged(f);
    }

    @Test
    void missingRoomRejectsEntireSelection() {
        Fixture f = fixture();
        expectError(() -> service.exitClosedChatRooms(List.of(f.first(), Long.MAX_VALUE), f.caller()),
                CoffeeChatErrorCode.CHATROOM_NOT_FOUND);
        assertUnchanged(f);
    }

    @Test
    void nonParticipantRoomRejectsEntireSelectionEvenWhenItsIdIsKnown() {
        Fixture f = fixture();
        expectError(() -> service.exitClosedChatRooms(List.of(f.first(), f.foreign()), f.caller()),
                CoffeeChatErrorCode.CHATROOM_NOT_FOUND);
        assertUnchanged(f);
        ChatRoom foreign = rooms.findById(f.foreign()).orElseThrow();
        assertThat(foreign.isRequesterExited()).isFalse();
        assertThat(foreign.isReceiverExited()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"WITHDRAWN", "SUSPENDED"})
    void inaccessibleAccountsCannotExit(UserStatus status) {
        Fixture f = fixture();
        new TransactionTemplate(transactionManager).executeWithoutResult(tx ->
                org.springframework.test.util.ReflectionTestUtils.setField(
                        users.findById(f.caller()).orElseThrow(), "status", status));
        expectError(() -> service.exitClosedChatRooms(List.of(f.first()), f.caller()),
                status == UserStatus.WITHDRAWN ? AuthErrorCode.USER_WITHDRAWN : AuthErrorCode.USER_SUSPENDED);
        assertUnchanged(f);
    }

    @Test
    void missingAuthenticationCannotExit() {
        Fixture f = fixture();
        expectError(() -> service.exitClosedChatRooms(List.of(f.first()), null), AuthErrorCode.INVALID_TOKEN);
        assertUnchanged(f);
    }

    @Test
    void transactionRollbackRestoresBothExitFlags() {
        Fixture f = fixture();
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            service.exitClosedChatRooms(List.of(f.first(), f.second()), f.caller());
            rooms.flush();
            throw new IllegalStateException("rollback after flushing exits");
        })).isInstanceOf(IllegalStateException.class);
        assertUnchanged(f);
    }

    @Test
    void concurrentParticipantsCanExitOverlappingSelectionsWithoutLosingEitherFlag() throws Exception {
        Fixture f = fixture();
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> exitAfterBarrier(List.of(f.first(), f.second()), f.caller(), ready, start));
            var second = executor.submit(() -> exitAfterBarrier(List.of(f.second(), f.first()), f.opponent(), ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
        for (Long id : List.of(f.first(), f.second())) {
            ChatRoom room = rooms.findById(id).orElseThrow();
            assertThat(room.isRequesterExited()).isTrue();
            assertThat(room.isReceiverExited()).isTrue();
            assertThat(room.getStatus()).isEqualTo(ChatRoom.RoomStatus.CLOSE);
        }
    }

    private void exitAfterBarrier(List<Long> ids, Long userId, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("barrier timed out");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        service.exitClosedChatRooms(ids, userId);
    }

    private void expectError(Runnable action, BaseErrorCode errorCode) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(CustomException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(errorCode));
    }

    private void assertUnchanged(Fixture f) {
        for (Long id : List.of(f.first(), f.second(), f.open())) {
            ChatRoom room = rooms.findById(id).orElseThrow();
            assertThat(room.isRequesterExited()).isFalse();
            assertThat(room.isReceiverExited()).isFalse();
        }
        assertThat(rooms.findById(f.open()).orElseThrow().getStatus()).isEqualTo(ChatRoom.RoomStatus.OPEN);
        assertThat(events.stream(ChatRoomClosedCommittedEvent.class)).isEmpty();
    }

    private Fixture fixture() {
        return new TransactionTemplate(transactionManager).execute(tx -> {
            Users caller = user("caller");
            Users opponent = user("opponent");
            Users outsider = user("outsider");
            ChatRoom first = room(caller, opponent, true, ChatRequest.RequestType.COFFEE_CHAT);
            ChatRoom second = room(opponent, caller, true, ChatRequest.RequestType.TEAM_RECRUIT);
            second.getRequest().accept();
            ChatRoom open = room(caller, opponent, false, ChatRequest.RequestType.COFFEE_CHAT);
            ChatRoom foreign = room(opponent, outsider, true, ChatRequest.RequestType.COFFEE_CHAT);
            Chat message = messages.save(Chat.createChat(first, caller, opponent, "retained history", UUID.randomUUID().toString()));
            return new Fixture(caller.getUserId(), opponent.getUserId(), first.getId(), second.getId(),
                    open.getId(), foreign.getId(), first.getRequest().getId(), second.getRequest().getId(), message.getId());
        });
    }

    private Users user(String name) {
        return users.save(Users.builder().username(name + "-" + UUID.randomUUID().toString().substring(0, 8))
                .passwordHash("password").name(name).status(UserStatus.ACTIVE).build());
    }

    private ChatRoom room(Users requester, Users receiver, boolean closed, ChatRequest.RequestType type) {
        ChatRequest request = requests.save(ChatRequest.builder().requester(requester).receiver(receiver)
                .requestInterest(List.of()).content("request").type(type).build());
        request.accept();
        ChatRoom room = rooms.save(ChatRoom.createRoom(request, requester, receiver));
        if (closed) {
            room.closeRoom();
            request.closeRequest();
        }
        return room;
    }

    private record Fixture(Long caller, Long opponent, Long first, Long second, Long open, Long foreign,
                           Long firstRequest, Long secondRequest, Long message) {}
}
