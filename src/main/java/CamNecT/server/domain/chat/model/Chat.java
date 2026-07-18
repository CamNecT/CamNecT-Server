package CamNecT.server.domain.chat.model;

import CamNecT.server.domain.users.model.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "coffee_chat_message",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_message_room_sender_client_id",
                columnNames = {"cc_thread_id", "sender_id", "client_message_id"}
        )
)
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cc_message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cc_thread_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private Users sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private Users receiver;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // 본문

    /**
     * 클라이언트가 한 사용자 동작에 한 번만 생성하는 메시지 식별자.
     * 기존 운영 데이터와 구버전 클라이언트의 점진 전환을 위해 DB 컬럼은 nullable로 둔다.
     * 신규 저장 메시지는 서비스에서 항상 값을 채운다.
     */
    @Column(name = "client_message_id", length = 36, updatable = false)
    private String clientMessageId;

    @Column(name = "is_read", nullable = false)
    private boolean isRead; // 읽음 여부

    @Column(name = "read_at")
    private LocalDateTime readAt; // 읽은 시간

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일시

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 수정일시

    @Builder
    public Chat(ChatRoom room, Users sender, Users receiver, String content, String clientMessageId) {
        this.room = room;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.clientMessageId = clientMessageId;
        this.isRead = false; // 기본값: 안 읽음
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 채팅 생성
     *
     * @param room     채팅 방 (Thread)
     * @param sender   요청자 ID (보낸이)
     * @param receiver 수신자 ID (받는이)
     * @param content  내용
     * @return Chat Entity
     */
    public static Chat createChat(
            ChatRoom room,
            Users sender,
            Users receiver,
            String content,
            String clientMessageId
    ) {
        return Chat.builder()
                .room(room)
                .sender(sender)
                .receiver(receiver)
                .content(content)
                .clientMessageId(clientMessageId)
                .build();
    }

    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
