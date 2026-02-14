package CamNecT.server.domain.activity.model.recruitment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recruitments_bookmark")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recruit_bookmark_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "recruit_id", nullable = false)
    private Long recruitId;

    @Builder
    public RecruitmentBookmark(Long userId, Long recruitId) {
        this.userId = userId;
        this.recruitId = recruitId;
    }
}