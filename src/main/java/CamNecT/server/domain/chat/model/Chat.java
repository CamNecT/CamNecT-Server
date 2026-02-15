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
@Table(name = "coffee_chat_message")
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

    @Column(name = "is_read", nullable = false)
    private boolean isRead; // 읽음 여부

    @Column(name = "read_at")
    private LocalDateTime readAt; // 읽은 시간

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일시

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 수정일시

    @Builder
    public Chat(ChatRoom room, Users sender, Users receiver, String content) {
        this.room = room;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
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
    public static Chat createChat(ChatRoom room, Users sender, Users receiver, String content) {
        return Chat.builder()
                .room(room)
                .sender(sender)
                .receiver(receiver)
                .content(content)
                .build();
    }

    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}