package CamNecT.server.domain.users.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_tag_map",
        uniqueConstraints = @UniqueConstraint(name="uk_user_tag", columnNames={"user_id","tag_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserTagMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_tag_id")
    private Long userTagId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tag_id", nullable = false)
    private Long tagId;
}