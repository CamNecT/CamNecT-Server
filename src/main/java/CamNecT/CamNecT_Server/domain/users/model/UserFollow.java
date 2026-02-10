package CamNecT.CamNecT_Server.domain.users.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_follow",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_follow",
                        columnNames = {"follower_id", "following_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_id")
    private Long followId; // 이미지 상의 기본키

    @Column(name = "follower_id", nullable = false)
    private Long followerId; // 팔로우를 하는 유저 ID

    @Column(name = "following_id", nullable = false)
    private Long followingId; // 팔로우를 받는 유저 ID

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일시

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 수정일시
}