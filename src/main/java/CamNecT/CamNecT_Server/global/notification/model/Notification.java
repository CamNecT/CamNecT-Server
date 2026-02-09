package CamNecT.CamNecT_Server.global.notification.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_receiver_id", columnList = "receiver_user_id"),
                @Index(name = "idx_notifications_receiver_read", columnList = "receiver_user_id,is_read")
        }
)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receiver_user_id", nullable = false)
    private Long receiverUserId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false)
    private String message;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "link", length = 500)
    private String link;


    @Column(name = "is_read", nullable = false)
    private boolean read;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Notification(Long receiverUserId,
                         Long actorUserId,
                         NotificationType type,
                         String message,
                         Long postId,
                         Long commentId,
                         Long requestId,
                         String link) {
        this.receiverUserId = receiverUserId;
        this.actorUserId = actorUserId;
        this.type = type;
        this.message = message;
        this.postId = postId;
        this.commentId = commentId;
        this.requestId = requestId;
        this.link = link;
        this.read = false;
    }


    public static Notification of(Long receiverUserId,
                                  Long actorUserId,
                                  NotificationType type,
                                  String message,
                                  Long postId,
                                  Long commentId,
                                  Long requestId,
                                  String link) {
        return new Notification(receiverUserId, actorUserId, type, message, postId, commentId, requestId, link);
    }

    public void markRead() {
        this.read = true;
    }
}
