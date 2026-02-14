package CamNecT.server.domain.chat.model;

import CamNecT.server.domain.users.model.Users;
import CamNecT.server.global.tag.model.Tag;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
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

    @BatchSize(size = 100)
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
    @Column(name = "request_type", nullable = false)
    private RequestType type;

    /* 팀원 모집 관련 정보
     * COFFEE_CHAT일 때는 null, TEAM_RECRUIT일 때만 값이 들어감
     */
    @Column(name = "activity_id")
    private Long activityId; // 대외활동 ID

    @Column(name = "recruitment_id")
    private Long recruitmentId; // 팀원 모집 공고 ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 요청 종류 Enum
    public enum RequestType {
        COFFEE_CHAT, TEAM_RECRUIT
    }

    // 상태값 Enum (받았는지, 거절했는지 등)
    public enum RequestStatus {
        WAITING, ACCEPTED, REJECTED, CLOSED
    }

    @Builder
    public ChatRequest(Users requester, Users receiver, List<Tag> requestInterest, String content, RequestType type, Long activityId, Long recruitmentId) {
        this.requester = requester;
        this.receiver = receiver;
        this.requestInterests = requestInterest;
        this.content = content;
        this.type = (type != null) ? type : RequestType.COFFEE_CHAT; // 기본값 커피챗
        this.status = RequestStatus.WAITING; // 기본값 대기중

        this.activityId = activityId;
        this.recruitmentId = recruitmentId;
    }

    public void closeRequest() {
        this.status = RequestStatus.CLOSED;
    }

    public void accept() {
        this.status = RequestStatus.ACCEPTED;
    }

    public void reject() {
        this.status = RequestStatus.REJECTED;
    }
}