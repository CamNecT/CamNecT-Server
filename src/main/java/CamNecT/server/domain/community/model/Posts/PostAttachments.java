package CamNecT.server.domain.community.model.Posts;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "post_attachments",
        indexes = {
                @Index(
                        name = "idx_post_attach_post_status_sort",
                        columnList = "post_id,status,sort_order,attachment_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PostAttachments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Posts post;

    @Column(name = "file_key", nullable = false, length = 500)
    private String fileKey;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "file_size")
    private Long fileSize;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private boolean status = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static PostAttachments create(
            Posts post,
            String fileKey,
            Integer width,
            Integer height,
            Long fileSize,
            int sortOrder
    ) {
        return PostAttachments.builder()
                .post(post)
                .fileKey(fileKey)
                .width(width)
                .height(height)
                .fileSize(fileSize)
                .status(true)
                .sortOrder(sortOrder)
                .build();
    }

    public boolean isActive() {
        return status;
    }
}