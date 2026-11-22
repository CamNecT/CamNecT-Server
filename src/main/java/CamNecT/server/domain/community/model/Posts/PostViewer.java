package CamNecT.server.domain.community.model.Posts;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "post_viewers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostViewer {
    @Embeddable
    public record Key(Long postId, Long userId) implements Serializable {}

    @EmbeddedId
    private Key id;
    @Column(nullable = false)
    private LocalDate lastViewedOn;
}
