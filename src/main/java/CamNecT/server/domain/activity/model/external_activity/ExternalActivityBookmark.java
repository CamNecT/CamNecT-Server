package CamNecT.server.domain.activity.model.external_activity;

import CamNecT.server.domain.users.model.Users;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "external_activities_bookmark")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExternalActivityBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_bookmark_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private ExternalActivity activity;

    public static ExternalActivityBookmark of(Users user, ExternalActivity activity) {
        return ExternalActivityBookmark.builder()
                .user(user)
                .activity(activity)
                .build();
    }
}