package CamNecT.CamNecT_Server.domain.chat.model;

import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.global.tag.model.Tag;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "coffee_chat_thread")
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cc_thread_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private ChatRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private Users requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private Users receiver;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('OPEN', 'CLOSE')", nullable = false)
    private RoomStatus status;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum RoomStatus {
        OPEN, CLOSE
    }

    @Builder
    public ChatRoom(ChatRequest request, Users requester, Users receiver) {
        this.request = request;
        this.requester = requester;
        this.receiver = receiver;
        this.status = RoomStatus.OPEN;
        this.lastMessageAt = LocalDateTime.now();
    }

    public static ChatRoom createRoom(ChatRequest request, Users requester, Users receiver) {
        return ChatRoom.builder()
                .request(request)
                .requester(requester)
                .receiver(receiver)
                .build();
    }

    public void updateLastMessageTime() {
        this.lastMessageAt = LocalDateTime.now();
    }

    public void closeRoom() {
        this.status = RoomStatus.CLOSE;
    }


    // 채팅방에 연결된 태그들 객체 반환

    public List<Tag> getTags() {
        return this.request.getRequestInterests();
    }
}