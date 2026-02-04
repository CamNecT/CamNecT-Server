package CamNecT.CamNecT_Server.domain.chat.model;

import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.global.tag.model.Tag;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "coffee_chat_request")
public class ChatRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long id;

    // 요청자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private Users requester;

    // 수신자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private Users receiver;

    // 관심분야 태그
/*        @ManyToMany(fetch = FetchType.LAZY)
        @JoinColumn(name = "request_interests", nullable = false)
        private List<Tag> requestInterests;*/

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "chat_request_interests",
            joinColumns = @JoinColumn(name = "request_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> requestInterests;

    @Column(name = "request_content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 상태값 Enum (받았는지, 거절했는지 등)
    public enum RequestStatus {
        WAITING, ACCEPTED, REJECTED
    }

    @Builder
    public ChatRequest(Users requester, Users receiver, List<Tag> requestInterest, String content) {
        this.requester = requester;
        this.receiver = receiver;
        this.requestInterests = requestInterest;
        this.content = content;
        this.status = RequestStatus.WAITING; // 기본값 대기중
    }

    public void accept() {
        this.status = RequestStatus.ACCEPTED;
    }

    public void reject() {
        this.status = RequestStatus.REJECTED;
    }
}