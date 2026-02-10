package CamNecT.CamNecT_Server.domain.profile.components.majors.model;

import CamNecT.CamNecT_Server.domain.profile.components.institutions.model.Institutions;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "Majors")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Majors {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "major_id")
    private Long majorId;

    @ManyToOne(fetch = FetchType.LAZY) // 지연 로딩 적용
    @JoinColumn(name = "institution_id", nullable = false)
    private Institutions institution;

    @Column(name = "major_code", nullable = false, length = 100)
    private String majorCode;

    @Column(name = "major_name_kor", nullable = false, length = 100)
    private String majorNameKor;

    @Column(name = "major_name_eng", nullable = false, length = 100)
    private String majorNameEng;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}